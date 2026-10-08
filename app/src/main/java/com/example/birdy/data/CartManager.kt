package com.example.birdy.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.birdy.kit.chat.OrderChatSocket
import com.example.birdy.ui.store.StoreMenuItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

// Matches iOS CartManager.shared singleton

data class CartItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val dishName: String,
    val restaurantName: String,
    val price: Double,
    var quantity: Int = 1,
    val imageURL: String = "",
    val specialInstructions: String = "",
    val selectedOptions: List<String> = emptyList(),
    val menuItem: StoreMenuItem? = null,  // Full menu item for re-opening customization (matches iOS)
    /** udo3 catalog product id. Saved-cart (grocery) lines are keyed by it. */
    val productId: String? = null,
    /** The saved cart line's id on the server (saved carts only). */
    val serverLineId: String? = null,
    /** False when the store no longer sells it: shown greyed until removed. */
    val isAvailable: Boolean = true
)

// MARK: - Saved cart (udo3 /api/v1/carts)
// Grocery stores keep their cart on the server, the same one the web and iOS show.
// Restaurants keep theirs on the phone (server_cart false) until the server prices
// menu options. Same rules as IC CartView.swift.

data class ServerCartLine(
    val id: String,
    val productKey: String,
    val productId: String?,
    val name: String,
    val price: Double,
    val imageUrl: String,
    val quantity: Int,
    val available: Boolean
)

data class ServerCartSummary(
    val itemCount: Int,
    val subtotal: Double,
    val deliveryFee: Double,
    val serviceFee: Double,
    val tax: Double,
    val total: Double
)

data class ServerCart(
    val id: String?,
    val brandId: String,
    val brandName: String,
    val serverCart: Boolean,
    val items: List<ServerCartLine>,
    val summary: ServerCartSummary
) {
    companion object {
        fun fromJson(obj: JSONObject): ServerCart {
            val items = obj.optJSONArray("items") ?: JSONArray()
            val s = obj.optJSONObject("summary") ?: JSONObject()
            return ServerCart(
                id = obj.stringOrNull("id"),
                brandId = obj.optString("brand_id", ""),
                brandName = obj.stringOrNull("brand_name") ?: "",
                serverCart = obj.optBoolean("server_cart", true),
                items = (0 until items.length()).mapNotNull { i ->
                    val line = items.optJSONObject(i) ?: return@mapNotNull null
                    ServerCartLine(
                        id = line.optString("id", ""),
                        productKey = line.optString("product_key", ""),
                        productId = line.stringOrNull("product_id"),
                        name = line.optString("name", "Item"),
                        price = line.optString("price", "0").toDoubleOrNull() ?: 0.0,
                        imageUrl = line.stringOrNull("image_url") ?: "",
                        quantity = line.optInt("quantity", 0),
                        available = line.optBoolean("available", true)
                    )
                },
                summary = ServerCartSummary(
                    itemCount = s.optInt("item_count", 0),
                    subtotal = s.optString("subtotal", "0").toDoubleOrNull() ?: 0.0,
                    deliveryFee = s.optString("delivery_fee", "0").toDoubleOrNull() ?: 0.0,
                    serviceFee = s.optString("service_fee", "0").toDoubleOrNull() ?: 0.0,
                    tax = s.optString("tax", "0").toDoubleOrNull() ?: 0.0,
                    total = s.optString("total", "0").toDoubleOrNull() ?: 0.0
                )
            )
        }
    }
}

/** One store's saved cart in GET /carts/active (newest first). */
data class ActiveSavedCart(
    val brandId: String,
    val brandName: String,
    val itemCount: Int,
    /** False for restaurants: their cart lives on the phone, so the app doesn't count it here. */
    val serverCart: Boolean = true
)

class CartServiceException(val status: Int, val code: String, override val message: String) : Exception(message)

/** Saved-cart calls. All network calls block: run them on Dispatchers.IO. */
object CartService {
    fun cart(brandId: String): ServerCart = send("GET", "/carts/$brandId")

    /** Sets the product's line to exactly [quantity] (0 removes it). */
    fun setQuantity(brandId: String, productId: String, quantity: Int): ServerCart =
        send("PUT", "/carts/$brandId/items", JSONObject().put("product_id", productId).put("quantity", quantity))

    fun clear(brandId: String): ServerCart = send("DELETE", "/carts/$brandId")

    /** Sign-in: the phone's lines join the saved cart; the server's quantity wins. */
    fun merge(brandId: String, lines: List<Pair<String, Int>>): ServerCart {
        val arr = JSONArray()
        lines.forEach { (pid, qty) -> arr.put(JSONObject().put("product_id", pid).put("quantity", qty)) }
        return send("POST", "/carts/$brandId/merge", JSONObject().put("lines", arr))
    }

    /** Every store with a non-empty saved cart, most recently changed first; null if unreadable. */
    fun activeCarts(): List<ActiveSavedCart>? = try {
        val (status, body) = request("GET", "/carts/active", null)
        if (status != 200) null else {
            val carts = JSONObject(body).optJSONArray("carts") ?: JSONArray()
            (0 until carts.length()).mapNotNull { i ->
                val c = carts.optJSONObject(i) ?: return@mapNotNull null
                ActiveSavedCart(
                    c.optString("brand_id", ""),
                    c.stringOrNull("brand_name") ?: "Store",
                    c.optInt("item_count", 0),
                    c.optBoolean("server_cart", true)
                )
            }
        }
    } catch (_: Exception) {
        null
    }

    private fun send(method: String, path: String, body: JSONObject? = null): ServerCart {
        val (status, text) = try {
            request(method, path, body)
        } catch (e: Exception) {
            throw CartServiceException(0, "offline", "We couldn't reach the server. Please try again.")
        }
        val json = try { JSONObject(text) } catch (_: Exception) { JSONObject() }
        if (status == 200 && json.has("cart")) return ServerCart.fromJson(json.getJSONObject("cart"))
        throw CartServiceException(
            status,
            json.optString("error", "cart_failed"),
            json.stringOrNull("message") ?: "We couldn't update your cart. Please try again."
        )
    }

    private fun request(method: String, path: String, body: JSONObject?): Pair<Int, String> {
        val token = AuthManager.getToken()
        if (token.isNullOrEmpty()) throw IllegalStateException("not signed in")
        val connection = URL("${Config.API_BASE_URL}$path").openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = method
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.connectTimeout = 10_000
            connection.readTimeout = 15_000
            if (body != null) {
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true
                connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            status to (stream?.bufferedReader()?.use { it.readText() } ?: "")
        } finally {
            connection.disconnect()
        }
    }
}

object CartManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /** The phone's own cart: restaurants, and grocery while signed out. */
    private var localItems by mutableStateOf(listOf<CartItem>())
    private var localRestaurantId by mutableStateOf("")
    /** The saved cart of the grocery store that's open ([serverBrandId]). */
    private var serverItems by mutableStateOf(listOf<CartItem>())
    var serverBrandId by mutableStateOf<String?>(null)
        private set
    var serverSummary by mutableStateOf<ServerCartSummary?>(null)
        private set
    var serverCartId: String? = null
        private set
    /** Every store with a saved cart (newest first), and their items in total, for badges. */
    var activeCarts by mutableStateOf(listOf<ActiveSavedCart>())
        private set
    var savedCartCount by mutableStateOf(0)
        private set
    /** A message for the open screen (unavailable item, cart changed, offline). */
    var cartMessage by mutableStateOf<String?>(null)

    var promoCode by mutableStateOf("")

    // Current restaurant context — matches iOS CartManager.shared.restaurantId
    var restaurantName by mutableStateOf("")

    // Order ID from backend — matches iOS CartManager.shared.orderId
    var orderId by mutableStateOf("")
    var orderNumber by mutableStateOf("")   // e.g. "UDO-A1B2C3" from udo3

    // Bridge to present driver tracking map — matches iOS CartManager.shared.showDriverTracking
    var showDriverTracking by mutableStateOf(false)

    // Bridge to present order detail screen — matches iOS CartManager.shared.showOrderDetail
    var showOrderDetail by mutableStateOf(false)

    // Fast taps: the quantity last asked for per product, and which ones have a
    // request out. One request per product at a time; when it returns, the latest
    // tap is sent if it differs. A response or refresh never overwrites a line
    // that's still being edited.
    private val desired = mutableMapOf<String, Int>()
    private val inFlight = mutableSetOf<String>()
    private var editTick by mutableStateOf(0)  // recomposes totals when edits start/finish
    private val refreshing = mutableMapOf<String, Job>()

    /** The open store's lines: its saved cart for a grocery store, else the phone's cart. */
    val items: List<CartItem>
        get() = if (serverBrandId != null) serverItems else localItems

    /**
     * The store the open cart belongs to. Screens set it before adding; a store other
     * than the open saved cart's means the phone's cart (a restaurant).
     */
    var restaurantId: String
        get() = serverBrandId ?: localRestaurantId
        set(value) {
            if (serverBrandId != null && serverBrandId == value) return
            serverBrandId = null
            localRestaurantId = value
        }

    val isSavedCart: Boolean get() = serverBrandId != null
    val hasUnavailableItems: Boolean get() = items.any { !it.isAvailable }
    private val editing: Boolean get() = editTick >= 0 && (desired.isNotEmpty() || inFlight.isNotEmpty())

    val subtotal: Double
        get() = items.sumOf { it.price * it.quantity }

    // The server's fees (Udo.Payments.Checkout.fees/2). A saved cart shows the server's
    // own numbers; the phone's cart uses the same placeholder rule, and the server's
    // total is what the Pay step shows and charges.
    private val serverNumbers: ServerCartSummary?
        get() = serverSummary.takeIf { isSavedCart && !editing }

    val deliveryFee: Double
        get() = serverNumbers?.deliveryFee ?: if (items.isEmpty()) 0.0 else 5.00

    val serviceFee: Double
        get() = serverNumbers?.serviceFee ?: if (items.isEmpty()) 0.0 else 1.50

    val tax: Double
        get() = serverNumbers?.tax ?: (Math.round(subtotal * 0.08 * 100) / 100.0)

    val total: Double
        get() = serverNumbers?.total ?: (subtotal + deliveryFee + serviceFee + tax)

    val itemCount: Int
        get() = items.sumOf { it.quantity }

    /**
     * Badge count: the other stores' saved carts, the open saved cart as shown (so a tap
     * counts at once, before the server answers), and the phone's restaurant cart.
     */
    val badgeCount: Int
        get() = activeCarts.filter { it.brandId != serverBrandId }.sumOf { it.itemCount } +
            (if (serverBrandId == null) 0 else serverItems.sumOf { it.quantity }) +
            localItems.sumOf { it.quantity }

    // MARK: Opening a store

    /**
     * A grocery store (GStore) opens its saved cart when signed in; a restaurant uses
     * the phone's cart. The server's `server_cart` has the last word.
     */
    fun openStore(brandId: String, savedCart: Boolean) {
        if (brandId.isEmpty()) return
        if (savedCart && AuthManager.isLoggedIn()) {
            if (serverBrandId != brandId) {
                serverBrandId = brandId
                serverItems = emptyList()
                serverSummary = null
                serverCartId = null
            }
            refresh()
        } else {
            serverBrandId = null
        }
    }

    /**
     * Re-reads the open store's saved cart (store open, cart open, app resume).
     * Joins a refresh already running for that store instead of starting another.
     */
    fun refresh() {
        val brandId = serverBrandId ?: return
        if (refreshing[brandId]?.isActive == true) return
        refreshing[brandId] = scope.launch {
            try {
                val cart = withContext(Dispatchers.IO) { CartService.cart(brandId) }
                apply(cart)
            } catch (_: Exception) {
                // Offline: keep what's shown.
            }
            refreshCount()
        }
    }

    fun refreshCount() {
        scope.launch { loadActiveCarts() }
    }

    private suspend fun loadActiveCarts() {
        withContext(Dispatchers.IO) { CartService.activeCarts() }?.let { carts ->
            // Restaurant carts made on the web stay on the web; the phone keeps its own.
            activeCarts = carts.filter { it.serverCart }
            savedCartCount = activeCarts.sumOf { it.itemCount }
        }
    }

    // MARK: Live updates (udo3 CartChannel "cart:<userId>")

    private var listening = false

    /**
     * Joins the signed-in user's cart topic, which also keeps the socket open on every
     * screen. Web (or other device) changes refresh the cart at once; a join or rejoin
     * after a drop re-reads too, since nothing that changed meanwhile was pushed.
     */
    fun startLiveUpdates() {
        if (!listening) {
            listening = true
            scope.launch {
                OrderChatSocket.cartUpdated.collect { brandId ->
                    if (brandId == serverBrandId) refresh()
                    refreshCount()
                }
            }
            scope.launch {
                OrderChatSocket.cartJoined.collect {
                    refresh()
                    refreshCount()
                }
            }
        }
        val userId = AuthManager.getUserID()
        if (AuthManager.isLoggedIn() && userId.isNotEmpty()) OrderChatSocket.joinCart(userId)
    }

    /**
     * Opened without a store (or after a restart) nothing is open: show the most
     * recently changed saved cart, as the web drawer does, unless the phone's cart has items.
     */
    fun showSavedCartIfNothingOpen() {
        if (serverBrandId != null || localItems.isNotEmpty() || !AuthManager.isLoggedIn()) return
        scope.launch {
            loadActiveCarts()
            val newest = activeCarts.firstOrNull()
            if (serverBrandId == null && localItems.isEmpty() && newest != null) openStore(newest.brandId, savedCart = true)
        }
    }

    /** Other carts the Cart screen can switch to: saved carts of other stores. */
    val otherSavedCarts: List<ActiveSavedCart>
        get() = activeCarts.filter { it.brandId != serverBrandId }

    /** The phone's restaurant cart, when a saved cart is shown and the phone's has items. */
    val hasHiddenPhoneCart: Boolean get() = serverBrandId != null && localItems.isNotEmpty()
    val phoneCartName: String get() = localItems.firstOrNull()?.restaurantName ?: "Restaurant"
    val phoneCartCount: Int get() = localItems.sumOf { it.quantity }

    /** Switches the Cart screen to the phone's restaurant cart. */
    fun showPhoneCart() {
        serverBrandId = null
    }

    /** App came to the foreground. */
    fun foreground() {
        startLiveUpdates()
        refresh()
        if (localItems.isNotEmpty()) signedIn() else refreshCount()
    }

    // MARK: Changing lines

    fun addItem(item: CartItem) {
        val brandId = serverBrandId
        if (brandId != null && restaurantId == brandId) {
            val pid = item.productId ?: item.menuItem?.id
            if (pid.isNullOrEmpty()) {
                cartMessage = "This item isn't available right now."
                return
            }
            setSaved(pid, quantityOfProduct(pid) + item.quantity, item)
            return
        }
        val line = if (item.productId == null) item.copy(productId = item.menuItem?.id) else item
        val index = localItems.indexOfFirst {
            it.dishName == line.dishName && it.selectedOptions == line.selectedOptions
        }
        localItems = if (index >= 0) {
            localItems.toMutableList().apply { set(index, this[index].copy(quantity = this[index].quantity + line.quantity)) }
        } else {
            localItems + line
        }
    }

    fun removeItem(item: CartItem) {
        val pid = item.productId
        if (isSavedCart && pid != null) {
            setSaved(pid, 0, null)
        } else {
            localItems = localItems.filter { it.id != item.id }
        }
    }

    fun updateQuantity(item: CartItem, quantity: Int) {
        val pid = item.productId
        if (isSavedCart && pid != null) {
            setSaved(pid, quantity, item)
            return
        }
        val index = localItems.indexOfFirst { it.id == item.id }
        if (index >= 0) {
            localItems = if (quantity <= 0) {
                localItems.filter { it.id != item.id }
            } else {
                localItems.toMutableList().apply { set(index, this[index].copy(quantity = quantity)) }
            }
        }
    }

    fun decrementItem(dishName: String) {
        val line = items.firstOrNull { it.dishName == dishName } ?: return
        updateQuantity(line, line.quantity - 1)
    }

    fun incrementItem(dishName: String) {
        val line = items.firstOrNull { it.dishName == dishName } ?: return
        updateQuantity(line, line.quantity + 1)
    }

    /** One tap: removes every line the store no longer sells. */
    fun removeUnavailableItems() {
        items.filter { !it.isAvailable }.forEach { removeItem(it) }
    }

    /** How many of this store item are in the cart. */
    fun quantity(of: StoreMenuItem, storeName: String): Int =
        if (isSavedCart) quantityOfProduct(of.id)
        else localItems.filter { it.dishName == of.name && it.restaurantName == storeName }.sumOf { it.quantity }

    private fun quantityOfProduct(pid: String): Int =
        serverItems.firstOrNull { it.productId == pid }?.quantity ?: 0

    fun applyPromoCode() {
        // Promo codes aren't priced by the server yet; fees come from the server.
    }

    fun clear() {
        val brandId = serverBrandId
        if (brandId != null) {
            serverItems = emptyList()
            desired.clear()
            scope.launch {
                try {
                    apply(withContext(Dispatchers.IO) { CartService.clear(brandId) })
                } catch (_: Exception) {
                    refresh()
                }
                refreshCount()
            }
        } else {
            localItems = emptyList()
            localRestaurantId = ""
        }
        promoCode = ""
    }

    /**
     * After an order: the server took the ordered lines out of the saved cart; the
     * phone's cart is emptied as before.
     */
    fun afterOrderPlaced() {
        if (serverBrandId != null) {
            refresh()
        } else {
            localItems = emptyList()
            localRestaurantId = ""
        }
        promoCode = ""
    }

    // Absolute quantity, shown at once, sent one request at a time per product.
    private fun setSaved(pid: String, quantity: Int, template: CartItem?) {
        val qty = quantity.coerceIn(0, 99)
        desired[pid] = qty
        editTick++

        val index = serverItems.indexOfFirst { it.productId == pid }
        serverItems = when {
            index >= 0 && qty == 0 -> serverItems.filterIndexed { i, _ -> i != index }
            index >= 0 -> serverItems.toMutableList().apply { set(index, this[index].copy(quantity = qty)) }
            qty > 0 && template != null -> serverItems + template.copy(quantity = qty, productId = pid)
            else -> serverItems
        }
        send(pid)
    }

    private fun send(pid: String) {
        val brandId = serverBrandId ?: return
        if (pid in inFlight) return
        val qty = desired[pid] ?: return
        inFlight += pid

        scope.launch {
            val result = runCatching { withContext(Dispatchers.IO) { CartService.setQuantity(brandId, pid, qty) } }
            inFlight -= pid
            // A newer tap arrived: send that instead and ignore this answer.
            if (desired[pid] != qty) {
                send(pid)
                return@launch
            }
            desired -= pid
            editTick++

            result.onSuccess { cart ->
                apply(cart)
                refreshCount()
            }.onFailure { e ->
                cartMessage = e.message
                refresh()
            }
        }
    }

    /** The server's cart replaces what's shown, except lines still being edited. */
    private fun apply(cart: ServerCart) {
        if (cart.brandId != serverBrandId) return
        if (!cart.serverCart) {
            // This store keeps its cart on the phone.
            serverBrandId = null
            serverItems = emptyList()
            return
        }

        val pending = desired.keys + inFlight
        val previous = serverItems.filter { it.productId != null }.associateBy { it.productId!! }

        val lines = cart.items.mapNotNull { line ->
            val pid = line.productId ?: line.productKey
            if (pid in pending) return@mapNotNull previous[pid]
            CartItem(
                id = previous[pid]?.id ?: java.util.UUID.randomUUID().toString(),
                dishName = line.name,
                restaurantName = cart.brandName,
                price = line.price,
                quantity = line.quantity,
                imageURL = line.imageUrl,
                menuItem = previous[pid]?.menuItem,
                productId = pid,
                serverLineId = line.id,
                isAvailable = line.available
            )
        }.toMutableList()
        // Lines added on this phone that the server hasn't answered for yet.
        pending.filter { pid -> lines.none { it.productId == pid } }.forEach { pid -> previous[pid]?.let { lines += it } }

        serverItems = lines
        serverSummary = cart.summary
        serverCartId = cart.id
    }

    // MARK: Signing in and out

    /**
     * The phone's grocery lines (added while signed out) join the saved cart. They
     * leave the phone only after the server took them; on failure or offline they
     * stay and are tried again on the next resume or sign-in.
     */
    fun signedIn() {
        startLiveUpdates()
        val brandId = localRestaurantId
        val lines = localItems.mapNotNull { item ->
            val pid = item.productId
            if (pid.isNullOrEmpty() || item.selectedOptions.isNotEmpty()) null else pid to item.quantity
        }
        if (brandId.isEmpty() || lines.isEmpty() || !AuthManager.isLoggedIn()) {
            refreshCount()
            return
        }

        scope.launch {
            try {
                val cart = withContext(Dispatchers.IO) { CartService.merge(brandId, lines) }
                val sent = lines.map { it.first }.toSet()
                localItems = localItems.filter { it.productId !in sent }
                if (localItems.isEmpty()) localRestaurantId = ""
                if (serverBrandId == null || serverBrandId == brandId) {
                    serverBrandId = brandId
                    apply(cart)
                }
            } catch (_: CartServiceException) {
                // A restaurant (local_cart_store), offline or refused: keep the lines, try again later.
            }
            refreshCount()
        }
    }

    fun signedOut() {
        OrderChatSocket.leaveCart()
        serverBrandId = null
        serverItems = emptyList()
        serverSummary = null
        serverCartId = null
        savedCartCount = 0
        activeCarts = emptyList()
        desired.clear()
    }
}

// org.json's optString returns "null"/"" for missing values
private fun JSONObject.stringOrNull(key: String): String? =
    if (!has(key) || isNull(key)) null else optString(key).ifEmpty { null }
