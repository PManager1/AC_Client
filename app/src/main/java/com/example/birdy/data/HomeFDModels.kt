package com.example.birdy.data

import com.example.birdy.data.Config.API_BASE_URL
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

// MARK: - Legacy Models (kept for backward compatibility)

data class FoodCategory(
    val name: String,
    val emoji: String
)

data class MainCategory(
    val name: String,
    val subcategories: List<FoodCategory>
)

data class DeliveryRestaurant(
    val name: String,
    val rating: Double,
    val reviews: String,
    val distance: String,
    val deliveryTime: String,
    val deliveryFee: String,
    val imagePlaceholder: Long = 0xFFBB86FC
)

// MARK: - JSON Data Models (match /homefeed API response)

data class HomeFeedData(
    val featuredBanners: List<FeaturedBanner>,
    val sections: List<FeedSection>
)

enum class HomeFeedStatus { SUCCESS, AUTH_ERROR, NETWORK_ERROR }

data class HomeFeedResult(
    val status: HomeFeedStatus,
    val data: HomeFeedData?
)

data class FeaturedBanner(
    val id: String,
    val title: String,
    val subtitle: String,
    val gradientColors: List<String>,
    val actionText: String,
    val imageUrl: String
)

data class FeedSection(
    val heading: String,
    val restaurants: List<FeedRestaurant>
)

data class FeedFoodItem(
    val id: String,
    val name: String,
    val basePrice: Double,
    val imageURL: String,
    val isAvailable: Boolean,
    val promoText: String,
    val isSponsored: Boolean
)

data class FeedRestaurant(
    val id: String,
    val restaurantName: String,
    val logoURL: String,
    val images: List<String>,
    val rating: Double,
    val reviewCount: Int,
    val distance: Double,
    val deliveryTime: Int,
    val deliveryFee: Double,
    val promoText: String,
    val isSponsored: Boolean,
    val foodItems: List<FeedFoodItem>,
    val isNew: Boolean,
    val phone: String,
    val isBrandItem: Boolean = false,
    val isFavorited: Boolean = false
) {
    /** Format review count: 6000 → "6k+", 1200 → "1.2k+", 500 → "500+" */
    val reviewsDisplay: String
        get() {
            return if (reviewCount >= 1000) {
                val formatted = reviewCount / 1000.0
                val stripped = if (formatted % 1.0 == 0.0) {
                    "${formatted.toInt()}"
                } else {
                    String.format("%.1f", formatted)
                }
                "${stripped}k+"
            } else {
                "${reviewCount}+"
            }
        }

    /** "1.5 mi" */
    val distanceDisplay: String get() = "${distance} mi"

    /** "30 min" */
    val deliveryTimeDisplay: String get() = "${deliveryTime} min"

    /** "$0.00" delivery fee */
    val deliveryFeeDisplay: String
        get() = if (deliveryFee == 0.0) "$0" else String.format("$%.2f", deliveryFee)

    /** First image or empty */
    val thumbnailImage: String get() = images.firstOrNull() ?: ""
}

// MARK: - Grocery Store Model (matches BK/models/GroceryStore.go)

data class GroceryStore(
    val id: String,
    val name: String,
    val logoUrl: String,
    val placeholderIcon: String,
    val color: String,
    val order: Int,
    val isActive: Boolean
)

// MARK: - Static Data (categories — hardcoded for now, will come from backend later)

object HomeFDData {

    val mainCategories = listOf(
        MainCategory(name = "All", subcategories = listOf(
            FoodCategory("Fast Food", "🍟"),
            FoodCategory("Pizza", "🍕"),
            FoodCategory("Wings", "🌶️"),
            FoodCategory("Burgers", "🍔"),
            FoodCategory("Chicken", "🍗"),
            FoodCategory("Desserts", "🍰"),
            FoodCategory("Healthy", "🥗"),
            FoodCategory("Indian", "🍛"),
            FoodCategory("Chinese", "🥡"),
            FoodCategory("Pho", "🍜"),
            FoodCategory("Mexican", "🌮"),
            FoodCategory("Korean", "🥘"),
            FoodCategory("Soup", "🍲"),
            FoodCategory("Sandwich", "🥪"),
            FoodCategory("Asian", "🥢"),
            FoodCategory("Halal", "🍖"),
            FoodCategory("Thai", "🍛"),
            FoodCategory("Salad", "🥙"),
            FoodCategory("Seafood", "🦐"),
            FoodCategory("Japanese", "🍣"),
            FoodCategory("Stores", "🛒"),
            FoodCategory("Produce", "🥦"),
            FoodCategory("Meat", "🥩"),
            FoodCategory("Bakery", "🍞"),
            FoodCategory("Household", "🧴"),
            FoodCategory("Snacks", "🍿"),
            FoodCategory("Dairy", "🧀"),
            FoodCategory("Frozen", "🧊"),
            FoodCategory("Organic", "🌿"),
            FoodCategory("Coffee", "☕"),
            FoodCategory("Bubble Tea", "🧋"),
            FoodCategory("Juice", "🧃"),
            FoodCategory("Smoothies", "🥤"),
            FoodCategory("Soda", "🥤"),
            FoodCategory("Tea", "🍵"),
            FoodCategory("Energy Drinks", "⚡"),
            FoodCategory("Milkshakes", "🥛"),
            FoodCategory("Lemonade", "🍋")
        )),
        MainCategory(name = "Food", subcategories = listOf(
            FoodCategory("Fast Food", "🍟"),
            FoodCategory("Pizza", "🍕"),
            FoodCategory("Wings", "🌶️"),
            FoodCategory("Burgers", "🍔"),
            FoodCategory("Chicken", "🍗"),
            FoodCategory("Desserts", "🍰"),
            FoodCategory("Healthy", "🥗"),
            FoodCategory("Indian", "🍛"),
            FoodCategory("Chinese", "🥡"),
            FoodCategory("Pho", "🍜"),
            FoodCategory("Mexican", "🌮"),
            FoodCategory("Korean", "🥘"),
            FoodCategory("Soup", "🍲"),
            FoodCategory("Sandwich", "🥪"),
            FoodCategory("Asian", "🥢"),
            FoodCategory("Halal", "🍖"),
            FoodCategory("Thai", "🍛"),
            FoodCategory("Salad", "🥙"),
            FoodCategory("Seafood", "🦐"),
            FoodCategory("Japanese", "🍣")
        )),
        MainCategory(name = "Drinks", subcategories = listOf(
            FoodCategory("Coffee", "☕"),
            FoodCategory("Bubble Tea", "🧋"),
            FoodCategory("Juice", "🧃"),
            FoodCategory("Smoothies", "🥤"),
            FoodCategory("Soda", "🥤"),
            FoodCategory("Tea", "🍵"),
            FoodCategory("Energy Drinks", "⚡"),
            FoodCategory("Milkshakes", "🥛"),
            FoodCategory("Lemonade", "🍋")
        )),
        MainCategory(name = "Grocery", subcategories = listOf(
            FoodCategory("Stores", "🛒"),
            FoodCategory("Produce", "🥦"),
            FoodCategory("Meat", "🥩"),
            FoodCategory("Bakery", "🍞"),
            FoodCategory("Household", "🧴"),
            FoodCategory("Snacks", "🍿"),
            FoodCategory("Dairy", "🧀"),
            FoodCategory("Frozen", "🧊"),
            FoodCategory("Organic", "🌿")
        ))
    )

    // Flat list of all categories (Food only, for backward compatibility)
    val categories = mainCategories.first { it.name == "Food" }.subcategories

    // MARK: - Auth helpers

    /**
     * Ensures a bearer token is available. If one is missing or blank, attempts a guest
     * (demo) login and persists the returned token — mirrors iOS handleDemoLogin().
     * Best-effort: on failure it returns false and does not throw, so callers keep their
     * existing fallback behavior.
     */
    fun ensureToken(): Boolean {
        AuthManager.getToken()?.takeIf { it.isNotBlank() }?.let { return true }
        return try {
            val url = URL("$API_BASE_URL/demo-login")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.connectTimeout = 10_000
            conn.readTimeout = 15_000
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            conn.outputStream.use { it.write("""{"phoneNumber":"6502003406"}""".toByteArray()) }
            val code = conn.responseCode
            val body = if (code in 200..299) conn.inputStream.bufferedReader().use { it.readText() }
                       else conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
            conn.disconnect()
            if (code in 200..299) {
                val token = JSONObject(body).optString("token", "")
                if (token.isNotBlank()) {
                    AuthManager.setToken(token)
                    println("✅ [HomeFDData] Guest demo-login succeeded")
                    true
                } else {
                    println("⚠️ [HomeFDData] Demo-login returned no token")
                    false
                }
            } else {
                println("⚠️ [HomeFDData] Demo-login failed: HTTP $code — $body")
                false
            }
        } catch (e: Exception) {
            println("❌ [HomeFDData] Demo-login error: ${e.message}")
            false
        }
    }

    /** Response code from an HttpURLConnection, falling back to -1 on failure. */
    private fun codeOf(conn: HttpURLConnection): Int = try { conn.responseCode } catch (_: Exception) { -1 }

    /** Reads the body whether the response is 2xx or an error — avoids throwing on 401/4xx. */
    private fun bodyOf(conn: HttpURLConnection): String = try {
        if (codeOf(conn) in 200..299) {
            conn.inputStream.bufferedReader().use { it.readText() }
        } else {
            conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
        }
    } catch (e: Exception) {
        println("❌ [HomeFDData] Failed to read response: ${e.message}")
        ""
    }

    /** Opens a GET connection to the given URL, attaching the bearer token (demo-login fallback). */
    private fun openAuthedGet(urlString: String): HttpURLConnection {
        if (AuthManager.getToken().isNullOrBlank()) ensureToken()
        val connection = URL(urlString).openConnection() as HttpURLConnection
        connection.connectTimeout = 10_000
        connection.readTimeout = 15_000
        connection.setRequestProperty("Content-Type", "application/json")
        AuthManager.getToken()?.let { connection.setRequestProperty("Authorization", "Bearer $it") }
        return connection
    }

    // MARK: - Load Brands (grocery/convenience/pharmacy) from API

    /** Blocking network call — must be called from a background thread */
    fun fetchGroceryStores(lat: Double? = null, lng: Double? = null, maxDistance: Double? = null): List<GroceryStore> {
        return try {
            var urlString = "$API_BASE_URL/brands?type=grocery&filterByActivePolygons=true"
            if (lat != null && lng != null) {
                urlString += "&lat=$lat&lng=$lng&maxDistance=${maxDistance ?: 10}"
            }
            val connection = openAuthedGet(urlString)
            val json = bodyOf(connection)
            connection.disconnect()
            parseGroceryStores(json)
        } catch (e: Exception) {
            println("❌ [HomeFDData] Failed to fetch /brands: ${e.message}")
            emptyList()
        }
    }

    private fun parseGroceryStores(json: String): List<GroceryStore> {
        val array = JSONArray(json)
        return (0 until array.length()).mapNotNull { i ->
            val obj = array.getJSONObject(i)
            val brandType = obj.optString("brandType", "")
            if (brandType != "grocery") return@mapNotNull null
            GroceryStore(
                id = obj.optString("id", ""),
                name = obj.optString("name", "Store"),
                logoUrl = obj.optString("logoUrl", ""),
                placeholderIcon = obj.optString("placeholderIcon", ""),
                color = obj.optString("color", "#F5F5F5"),
                order = obj.optInt("order", 0),
                isActive = obj.optBoolean("isActive", true)
            )
        }
    }

    // MARK: - Load Home Feed from API

    /**
     * Same URL iOS builds in Home.swift loadHomeFeed: one endpoint for All / Food / Drinks,
     * filtered server-side by category, active polygons and (when known) distance.
     */
    internal fun homeFeedUrl(category: String, lat: Double?, lng: Double?): String {
        var url = "$API_BASE_URL/homefeed?category=${category.lowercase()}&filterByActivePolygons=true"
        if (lat != null && lng != null) url += "&lat=$lat&lng=$lng&maxDistance=10"
        return url
    }

    /**
     * Blocking network call — must be called from a background thread (e.g. IO dispatcher).
     * Returns a result that distinguishes success, auth failures, and network errors so the
     * UI can show a meaningful error/retry instead of a silent blank feed.
     */
    fun fetchHomeFeed(category: String, lat: Double? = null, lng: Double? = null): HomeFeedResult {
        return try {
            val connection = openAuthedGet(homeFeedUrl(category, lat, lng))
            val code = codeOf(connection)
            val json = bodyOf(connection)
            connection.disconnect()
            if (code == 401 || (code !in 200..299 && json.contains("token", ignoreCase = true))) {
                println("⚠️ [HomeFDData] /homefeed auth failed: HTTP $code")
                HomeFeedResult(status = HomeFeedStatus.AUTH_ERROR, data = null)
            } else if (code !in 200..299) {
                println("⚠️ [HomeFDData] /homefeed failed: HTTP $code — $json")
                HomeFeedResult(status = HomeFeedStatus.NETWORK_ERROR, data = null)
            } else {
                HomeFeedResult(status = HomeFeedStatus.SUCCESS, data = parseHomeFeed(json))
            }
        } catch (e: Exception) {
            println("❌ [HomeFDData] Failed to fetch /homefeed: ${e.message}")
            HomeFeedResult(status = HomeFeedStatus.NETWORK_ERROR, data = null)
        }
    }

    /** optString returns the literal "null" for JSON null; this returns the fallback instead. */
    private fun JSONObject.str(key: String, fallback: String = ""): String =
        if (isNull(key)) fallback else optString(key, fallback)

    private fun JSONArray?.objects(): List<JSONObject> =
        if (this == null) emptyList() else (0 until length()).mapNotNull { optJSONObject(it) }

    private fun JSONArray?.strings(): List<String> =
        if (this == null) emptyList() else (0 until length()).mapNotNull { i -> if (isNull(i)) null else optString(i) }

    /**
     * Parses /homefeed (udo3 HomeFeedJSON). Missing or null fields fall back to defaults,
     * so one odd card can't throw away the whole feed.
     */
    internal fun parseHomeFeed(json: String): HomeFeedData {
        val root = JSONObject(json)

        val banners = root.optJSONArray("featured_banners").objects().map { b ->
            FeaturedBanner(
                id = b.str("id"),
                title = b.str("title"),
                subtitle = b.str("subtitle"),
                gradientColors = b.optJSONArray("gradient_colors").strings(),
                actionText = b.str("action_text"),
                imageUrl = b.str("image_url")
            )
        }

        val sections = root.optJSONArray("sections").objects().map { s ->
            val restaurants = s.optJSONArray("restaurants").objects().mapNotNull { r ->
                val id = r.str("id")
                if (id.isEmpty()) return@mapNotNull null

                // iOS decodes "foodItems"; older payloads used "items".
                val foodItems = (r.optJSONArray("foodItems") ?: r.optJSONArray("items")).objects().map { item ->
                    FeedFoodItem(
                        id = item.str("id"),
                        name = item.str("name"),
                        basePrice = item.optDouble("basePrice", 0.0),
                        imageURL = item.str("imageUrl"),
                        isAvailable = item.optBoolean("isAvailable", true),
                        promoText = item.str("promoText"),
                        isSponsored = item.optBoolean("isSponsored", false)
                    )
                }

                FeedRestaurant(
                    id = id,
                    restaurantName = r.str("restaurantName"),
                    logoURL = r.str("logoURL"),
                    images = r.optJSONArray("images").strings(),
                    rating = r.optDouble("rating", 0.0),
                    reviewCount = r.optInt("reviewCount", 0),
                    distance = r.optDouble("distance", 0.0),
                    deliveryTime = r.optInt("deliveryTime", 30),
                    deliveryFee = r.optDouble("deliveryFee", 0.0),
                    promoText = r.str("promoText"),
                    isSponsored = r.optBoolean("isSponsored", false),
                    foodItems = foodItems,
                    isNew = r.optBoolean("isNew", false),
                    phone = r.str("phone"),
                    isBrandItem = r.optBoolean("isBrandItem", false),
                    isFavorited = r.optBoolean("isFavorited", r.optBoolean("is_favorite", false))
                )
            }
            FeedSection(heading = s.str("heading"), restaurants = restaurants)
        }

        return HomeFeedData(featuredBanners = banners, sections = sections)
    }
}