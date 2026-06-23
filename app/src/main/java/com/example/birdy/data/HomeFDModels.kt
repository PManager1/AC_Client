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

    // MARK: - Load Brands (grocery/convenience/pharmacy) from API

    /** Blocking network call — must be called from a background thread */
    fun fetchGroceryStores(lat: Double? = null, lng: Double? = null, maxDistance: Double? = null): List<GroceryStore> {
        return try {
            var urlString = "$API_BASE_URL/brands?type=grocery&filterByActivePolygons=true"
            if (lat != null && lng != null) {
                urlString += "&lat=$lat&lng=$lng&maxDistance=${maxDistance ?: 10}"
            }
            val url = URL(urlString)
            val connection = url.openConnection() as java.net.HttpURLConnection
            connection.connectTimeout = 10_000
            connection.readTimeout = 15_000
            connection.setRequestProperty("Content-Type", "application/json")
            AuthManager.getToken()?.let { connection.setRequestProperty("Authorization", "Bearer $it") }
            val json = connection.inputStream.bufferedReader().use { it.readText() }
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

    // Drink tags matching Drinks subcategories
    val drinkTags = listOf("coffee", "bubble_tea", "juice", "smoothie", "soda", "tea", "energy_drink", "milkshake", "lemonade")

    // Food tags matching Food subcategories
    val foodTags = listOf("fast_food", "pizza", "burger", "chicken", "dessert", "healthy", "indian", "chinese", "pho", "mexican", "korean", "soup", "sandwich", "asian", "halal", "thai", "salad", "seafood", "japanese")

    /** Fetch brands from /brands and filter by any of the given tags. Converts to FeedRestaurant */
    fun fetchTaggedFeedRestaurants(tags: List<String>): List<FeedRestaurant> {
        return try {
            val url = URL("$API_BASE_URL/brands")
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 10_000
            connection.readTimeout = 15_000
            AuthManager.getToken()?.let { connection.setRequestProperty("Authorization", "Bearer $it") }
            val json = connection.inputStream.bufferedReader().use { it.readText() }
            val array = JSONArray(json)
            (0 until array.length()).mapNotNull { i ->
                val obj = array.getJSONObject(i)
                val brandTags = obj.optJSONArray("tags")
                if (brandTags == null) return@mapNotNull null
                val matched = (0 until brandTags.length()).any { j -> tags.contains(brandTags.getString(j)) }
                if (!matched) return@mapNotNull null

                val brandId = obj.optString("id", "")
                val brandName = obj.optString("name", "")
                val logoUrl = obj.optString("logoUrl", "")
                val carouselArray = obj.optJSONArray("carouselImages")
                val carousel = if (carouselArray != null) {
                    (0 until carouselArray.length()).map { carouselArray.getString(it) }
                } else emptyList()
                val images = if (carousel.isEmpty()) {
                    if (logoUrl.isEmpty()) emptyList() else listOf(logoUrl)
                } else carousel

                FeedRestaurant(
                    id = brandId,
                    restaurantName = brandName,
                    logoURL = logoUrl,
                    images = images,
                    rating = 4.5,
                    reviewCount = 100,
                    distance = 1.0,
                    deliveryTime = 20,
                    deliveryFee = 0.0,
                    promoText = "",
                    isSponsored = false,
                    foodItems = emptyList(),
                    isNew = false,
                    phone = "",
                    isBrandItem = true,
                    isFavorited = obj.optBoolean("is_favorite", false)
                )
            }
        } catch (e: Exception) {
            println("❌ [HomeFDData] Failed to fetch /brands for tags: ${e.message}")
            emptyList()
        }
    }

    // MARK: - Load Home Feed from API

    /** Blocking network call — must be called from a background thread (e.g. IO dispatcher) */
    fun fetchHomeFeed(): HomeFeedData? {
        return try {
            val url = URL("$API_BASE_URL/homefeed")
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 10_000
            connection.readTimeout = 15_000
            AuthManager.getToken()?.let { connection.setRequestProperty("Authorization", "Bearer $it") }
            val json = connection.inputStream.bufferedReader().use { it.readText() }
            parseHomeFeed(json)
        } catch (e: Exception) {
            println("❌ [HomeFDData] Failed to fetch /homefeed: ${e.message}")
            null
        }
    }

    private fun parseHomeFeed(json: String): HomeFeedData {
        val root = JSONObject(json)

        // Parse banners
        val bannersArray = root.optJSONArray("featured_banners") ?: org.json.JSONArray()
        val banners = (0 until bannersArray.length()).map { i ->
            val b = bannersArray.getJSONObject(i)
            val colorsArray = b.optJSONArray("gradient_colors")
            val colors = if (colorsArray != null) {
                (0 until colorsArray.length()).map { colorsArray.getString(it) }
            } else emptyList()

            FeaturedBanner(
                id = b.optString("id"),
                title = b.optString("title"),
                subtitle = b.optString("subtitle"),
                gradientColors = colors,
                actionText = b.optString("action_text"),
                imageUrl = b.optString("image_url")
            )
        }

        // Parse sections
        val sectionsArray = root.getJSONArray("sections")
        val sections = (0 until sectionsArray.length()).map { i ->
            val s = sectionsArray.getJSONObject(i)
            val heading = s.optString("heading")
            val restaurantsArray = s.getJSONArray("restaurants")
            val restaurants = (0 until restaurantsArray.length()).map { j ->
                val r = restaurantsArray.getJSONObject(j)
                val imagesArray = r.optJSONArray("images")
                val images = if (imagesArray != null) {
                    (0 until imagesArray.length()).map { imagesArray.getString(it) }
                } else emptyList()

                // Parse food items (API returns "items" array of objects)
                val itemsArray = r.optJSONArray("items") ?: r.optJSONArray("foodItems")
                val foodItems = if (itemsArray != null) {
                    (0 until itemsArray.length()).map { k ->
                        val item = itemsArray.getJSONObject(k)
                        FeedFoodItem(
                            id = item.optString("id"),
                            name = item.optString("name"),
                            basePrice = item.optDouble("basePrice", 0.0),
                            imageURL = item.optString("imageUrl", ""),
                            isAvailable = item.optBoolean("isAvailable", true),
                            promoText = item.optString("promoText", ""),
                            isSponsored = item.optBoolean("isSponsored", false)
                        )
                    }
                } else emptyList()

                FeedRestaurant(
                    id = r.optString("id"),
                    restaurantName = r.optString("restaurantName"),
                    logoURL = r.optString("logoURL", ""),
                    images = images,
                    rating = r.optDouble("rating", 0.0),
                    reviewCount = r.optInt("reviewCount", 0),
                    distance = r.optDouble("distance", 0.0),
                    deliveryTime = r.optInt("deliveryTime", 30),
                    deliveryFee = r.optDouble("deliveryFee", 0.0),
                    promoText = r.optString("promoText", ""),
                    isSponsored = r.optBoolean("isSponsored", false),
                    foodItems = foodItems,
                    isNew = r.optBoolean("isNew", false),
                    phone = r.optString("phone", ""),
                    isFavorited = r.optBoolean("is_favorite", false)
                )
            }
            FeedSection(heading = heading, restaurants = restaurants)
        }

        return HomeFeedData(featuredBanners = banners, sections = sections)
    }
}