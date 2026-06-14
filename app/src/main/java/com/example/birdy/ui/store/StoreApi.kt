package com.example.birdy.ui.store

import android.content.Context
import android.util.Log
import com.example.birdy.data.Config
import com.example.birdy.data.LocationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream

// MARK: - Helper: Defensive image URL parsing (handles both imageUrl and image_url from Go backend)

/** Resolve image URL from a JSON object, checking both camelCase and snake_case naming conventions */
private fun resolveImageUrl(obj: JSONObject, vararg keys: String): String {
    // Check common naming conventions
    for (key in keys) {
        val value = obj.optString(key, "")
        if (value.isNotEmpty()) return value
    }
    // Fallback: check both conventions automatically
    obj.optString("imageUrl", "").let { if (it.isNotEmpty()) return it }
    obj.optString("image_url", "").let { if (it.isNotEmpty()) return it }
    obj.optString("image", "").let { if (it.isNotEmpty()) return it }
    obj.optString("photoUrl", "").let { if (it.isNotEmpty()) return it }
    obj.optString("photo_url", "").let { if (it.isNotEmpty()) return it }
    return ""
}

/** If item image is empty, fall back to brand images so cards are never blank */
private fun fallbackImageUrl(itemImageUrl: String, brandLogoUrl: String, brandBannerUrl: String): String {
    if (itemImageUrl.isNotEmpty()) return itemImageUrl
    if (brandLogoUrl.isNotEmpty()) return brandLogoUrl
    if (brandBannerUrl.isNotEmpty()) return brandBannerUrl
    return ""
}

// MARK: - Mock Store Data (offline fallback)

private val mockStoreJSON = mapOf(
    "pizza-0" to """
    {
        "restaurant_id": "pizza-0",
        "brand_info": {
            "name": "Pizza Hut",
            "logo_url": "",
            "banner_image_url": "https://images.unsplash.com/photo-1565299624946-b28f40a0ae38?w=800",
            "rating": 4.3,
            "review_count": "520+",
            "cuisine": "Pizza",
            "tags": ["U-DO Pass", "Pizza", "Italian"]
        },
        "location_info": {
            "distance": "0.5 mi",
            "delivery_fee": 0.0,
            "delivery_time_est": "18 min",
            "address": "123 Main St, New York, NY 10001",
            "phone": "(212) 555-0199",
            "operating_hours": {
                "Mon": "10:00 AM - 11:00 PM",
                "Tue": "10:00 AM - 11:00 PM",
                "Wed": "10:00 AM - 11:00 PM",
                "Thu": "10:00 AM - 11:00 PM",
                "Fri": "10:00 AM - 12:00 AM",
                "Sat": "10:00 AM - 12:00 AM",
                "Sun": "11:00 AM - 10:00 PM"
            }
        },
        "menu": [
            {
                "category_name": "Featured Items",
                "items": [
                    {"id": "ph-1", "name": "Large Pepperoni Pizza", "description": "Classic pepperoni on our signature pan crust with mozzarella cheese", "price": 11.99, "image_url": "https://images.unsplash.com/photo-1628840042765-356cda07504e?w=400", "is_available": true, "modifier_groups": null},
                    {"id": "ph-2", "name": "Meat Lovers Pizza", "description": "Pepperoni, Italian sausage, ham, bacon, and seasoned pork", "price": 14.99, "image_url": "https://images.unsplash.com/photo-1513104890138-7c749659a591?w=400", "is_available": true, "modifier_groups": null},
                    {"id": "ph-3", "name": "Supreme Pizza", "description": "Pepperoni, Italian sausage, green peppers, onions, mushrooms", "price": 13.99, "image_url": "https://images.unsplash.com/photo-1574071318508-1cdbab80d002?w=400", "is_available": true, "modifier_groups": null},
                    {"id": "ph-4", "name": "BBQ Chicken Pizza", "description": "Grilled chicken, BBQ sauce, red onions, cilantro", "price": 13.49, "image_url": "https://images.unsplash.com/photo-1604382355076-af4b0eb60143?w=400", "is_available": true, "modifier_groups": null}
                ]
            },
            {
                "category_name": "Popular Items",
                "items": [
                    {"id": "ph-5", "name": "Cheese Sticks", "description": "Warm, gooey mozzarella cheese sticks with marinara dipping sauce", "price": 6.99, "image_url": "https://images.unsplash.com/photo-1548340748-6d2b7d7da280?w=400", "is_available": true, "modifier_groups": null},
                    {"id": "ph-6", "name": "Garlic Knots (6pc)", "description": "Freshly baked garlic knots with buttery garlic sauce", "price": 4.99, "image_url": "https://images.unsplash.com/photo-1619535860434-ba1d8fa12536?w=400", "is_available": true, "modifier_groups": null},
                    {"id": "ph-7", "name": "Honey BBQ Wings (8pc)", "description": "Crispy chicken wings tossed in sweet honey BBQ sauce", "price": 10.99, "image_url": "https://images.unsplash.com/photo-1608039829572-9b1234ef1321?w=400", "is_available": true, "modifier_groups": null},
                    {"id": "ph-8", "name": "Cinnabon Mini Rolls", "description": "Warm cinnamon rolls with cream cheese frosting", "price": 5.99, "image_url": "https://images.unsplash.com/photo-1609126979532-7f7b1e0e1c67?w=400", "is_available": true, "modifier_groups": null}
                ]
            },
            {
                "category_name": "Most Ordered",
                "items": [
                    {"id": "ph-9", "name": "Margherita Pizza", "description": "Fresh mozzarella, tomato sauce, basil on thin crust", "price": 10.99, "image_url": "https://images.unsplash.com/photo-1593560708920-61dd98c46a4e?w=400", "is_available": true, "modifier_groups": null},
                    {"id": "ph-10", "name": "Veggie Lovers Pizza", "description": "Green peppers, red onions, mushrooms, tomatoes, black olives", "price": 12.99, "image_url": "https://images.unsplash.com/photo-1528137871618-79d2761e3fd5?w=400", "is_available": true, "modifier_groups": null},
                    {"id": "ph-11", "name": "Buffalo Chicken Pizza", "description": "Buffalo sauce, grilled chicken, red onions, drizzle of ranch", "price": 14.49, "image_url": "https://images.unsplash.com/photo-1565299585323-38d6b0865b47?w=400", "is_available": true, "modifier_groups": null},
                    {"id": "ph-12", "name": "Ultimate Cheese Pizza", "description": "Mozzarella, cheddar, parmesan, provolone blend", "price": 11.49, "image_url": "https://images.unsplash.com/photo-1588315029754-2dd089d39a1a?w=400", "is_available": true, "modifier_groups": null}
                ]
            }
        ]
    }
    """,

    "pizza-1" to """
    {
        "restaurant_id": "pizza-1",
        "brand_info": {
            "name": "Mario's Pizzeria",
            "logo_url": "",
            "banner_image_url": "https://images.unsplash.com/photo-1565299624946-b28f40a0ae38?w=800",
            "rating": 4.4,
            "review_count": "230+",
            "cuisine": "Pizza",
            "tags": ["U-DO Pass", "Pizza", "Italian"]
        },
        "location_info": {
            "distance": "0.9 mi",
            "delivery_fee": 0.0,
            "delivery_time_est": "23 min",
            "address": "456 Broadway, New York, NY 10013",
            "phone": "(212) 555-0245",
            "operating_hours": {
                "Mon": "11:00 AM - 10:00 PM",
                "Tue": "11:00 AM - 10:00 PM",
                "Wed": "11:00 AM - 10:00 PM",
                "Thu": "11:00 AM - 11:00 PM",
                "Fri": "11:00 AM - 12:00 AM",
                "Sat": "11:00 AM - 12:00 AM",
                "Sun": "12:00 PM - 10:00 PM"
            }
        },
        "menu": [
            {
                "category_name": "Signature Pies",
                "items": [
                    {"id": "mp-1", "name": "Margherita Pizza", "description": "Fresh mozzarella, San Marzano tomato sauce, basil on thin crust", "price": 12.99, "image_url": "https://images.unsplash.com/photo-1593560708920-61dd98c46a4e?w=400", "is_available": true, "modifier_groups": null},
                    {"id": "mp-2", "name": "Mario's Special", "description": "Pepperoni, sausage, mushrooms, onions, green peppers", "price": 15.99, "image_url": "https://images.unsplash.com/photo-1565299585323-38d6b0865b47?w=400", "is_available": true, "modifier_groups": null},
                    {"id": "mp-3", "name": "White Pizza", "description": "Ricotta, mozzarella, garlic, spinach, olive oil", "price": 14.99, "image_url": "https://images.unsplash.com/photo-1574071318508-1cdbab80d002?w=400", "is_available": true, "modifier_groups": null},
                    {"id": "mp-4", "name": "Grandma's Square", "description": "Thick square pie with fresh mozzarella and basil", "price": 13.99, "image_url": "https://images.unsplash.com/photo-1513104890138-7c749659a591?w=400", "is_available": true, "modifier_groups": null}
                ]
            },
            {
                "category_name": "Sides & Drinks",
                "items": [
                    {"id": "mp-5", "name": "Garlic Bread", "description": "Toasted Italian bread with garlic butter and herbs", "price": 4.99, "image_url": "https://images.unsplash.com/photo-1619535860434-ba1d8fa12536?w=400", "is_available": true, "modifier_groups": null},
                    {"id": "mp-6", "name": "Caesar Salad", "description": "Crisp romaine, parmesan, croutons, house dressing", "price": 7.99, "image_url": "https://images.unsplash.com/photo-1548340748-6d2b7d7da280?w=400", "is_available": true, "modifier_groups": null},
                    {"id": "mp-7", "name": "Cannoli (2pc)", "description": "Crispy shells filled with sweet ricotta cream", "price": 5.99, "image_url": "https://images.unsplash.com/photo-1609126979532-7f7b1e0e1c67?w=400", "is_available": true, "modifier_groups": null}
                ]
            }
        ]
    }
    """,

    "pizza-2" to """
    {
        "restaurant_id": "pizza-2",
        "brand_info": {
            "name": "Papa John's",
            "logo_url": "",
            "banner_image_url": "https://images.unsplash.com/photo-1513104890138-7c749659a591?w=800",
            "rating": 4.3,
            "review_count": "380+",
            "cuisine": "Pizza",
            "tags": ["U-DO Pass", "Pizza", "American"]
        },
        "location_info": {
            "distance": "0.7 mi",
            "delivery_fee": 0.0,
            "delivery_time_est": "22 min",
            "address": "789 5th Ave, New York, NY 10022",
            "phone": "(212) 555-0378",
            "operating_hours": {
                "Mon": "10:00 AM - 11:00 PM",
                "Tue": "10:00 AM - 11:00 PM",
                "Wed": "10:00 AM - 11:00 PM",
                "Thu": "10:00 AM - 11:00 PM",
                "Fri": "10:00 AM - 12:00 AM",
                "Sat": "10:00 AM - 12:00 AM",
                "Sun": "11:00 AM - 10:00 PM"
            }
        },
        "menu": [
            {
                "category_name": "Featured Pizzas",
                "items": [
                    {"id": "pj-1", "name": "The Works", "description": "Pepperoni, sausage, mushrooms, onions, green peppers, black olives", "price": 14.99, "image_url": "https://images.unsplash.com/photo-1574071318508-1cdbab80d002?w=400", "is_available": true, "modifier_groups": null},
                    {"id": "pj-2", "name": "Pepperoni Pizza", "description": "Classic pepperoni with signature sauce and mozzarella", "price": 12.99, "image_url": "https://images.unsplash.com/photo-1628840042765-356cda07504e?w=400", "is_available": true, "modifier_groups": null},
                    {"id": "pj-3", "name": "BBQ Chicken Bacon", "description": "Grilled chicken, bacon, BBQ sauce, mozzarella", "price": 15.49, "image_url": "https://images.unsplash.com/photo-1604382355076-af4b0eb60143?w=400", "is_available": true, "modifier_groups": null},
                    {"id": "pj-4", "name": "Garden Fresh", "description": "Tomatoes, onions, mushrooms, green peppers, black olives", "price": 13.49, "image_url": "https://images.unsplash.com/photo-1528137871618-79d2761e3fd5?w=400", "is_available": true, "modifier_groups": null}
                ]
            },
            {
                "category_name": "Sides",
                "items": [
                    {"id": "pj-5", "name": "Garlic Parmesan Breadsticks", "description": "Warm breadsticks with garlic parmesan and pizza sauce", "price": 5.99, "image_url": "https://images.unsplash.com/photo-1619535860434-ba1d8fa12536?w=400", "is_available": true, "modifier_groups": null},
                    {"id": "pj-6", "name": "Chicken Poppers", "description": "Bite-sized chicken poppers with your choice of dipping sauce", "price": 7.99, "image_url": "https://images.unsplash.com/photo-1608039829572-9b1234ef1321?w=400", "is_available": true, "modifier_groups": null},
                    {"id": "pj-7", "name": "Chocolate Chip Cookie", "description": "Warm, fresh-baked chocolate chip cookie", "price": 4.99, "image_url": "https://images.unsplash.com/photo-1609126979532-7f7b1e0e1c67?w=400", "is_available": true, "modifier_groups": null}
                ]
            }
        ]
    }
    """,

    "pizza-3" to """
    {
        "restaurant_id": "pizza-3",
        "brand_info": {
            "name": "Little Caesars",
            "logo_url": "",
            "banner_image_url": "https://images.unsplash.com/photo-1588315029754-2dd089d39a1a?w=800",
            "rating": 4.0,
            "review_count": "290+",
            "cuisine": "Pizza",
            "tags": ["U-DO Pass", "Pizza", "Value"]
        },
        "location_info": {
            "distance": "1.0 mi",
            "delivery_fee": 1.99,
            "delivery_time_est": "25 min",
            "address": "321 8th Ave, New York, NY 10001",
            "phone": "(212) 555-0421",
            "operating_hours": {
                "Mon": "10:30 AM - 10:00 PM",
                "Tue": "10:30 AM - 10:00 PM",
                "Wed": "10:30 AM - 10:00 PM",
                "Thu": "10:30 AM - 10:00 PM",
                "Fri": "10:30 AM - 11:00 PM",
                "Sat": "10:30 AM - 11:00 PM",
                "Sun": "11:00 AM - 9:00 PM"
            }
        },
        "menu": [
            {
                "category_name": "Hot-N-Ready",
                "items": [
                    {"id": "lc-1", "name": "Classic Pepperoni", "description": "Large pepperoni pizza ready when you are", "price": 7.99, "image_url": "https://images.unsplash.com/photo-1628840042765-356cda07504e?w=400", "is_available": true, "modifier_groups": null},
                    {"id": "lc-2", "name": "Cheese Pizza", "description": "Classic cheese pizza with signature sauce", "price": 6.99, "image_url": "https://images.unsplash.com/photo-1588315029754-2dd089d39a1a?w=400", "is_available": true, "modifier_groups": null},
                    {"id": "lc-3", "name": "Sausage Pizza", "description": "Italian sausage with mozzarella and signature sauce", "price": 7.99, "image_url": "https://images.unsplash.com/photo-1565299585323-38d6b0865b47?w=400", "is_available": true, "modifier_groups": null}
                ]
            },
            {
                "category_name": "Combo Deals",
                "items": [
                    {"id": "lc-4", "name": "Crazy Bread Combo", "description": "Crazy Bread with marinara dipping sauce and a 2-liter drink", "price": 7.99, "image_url": "https://images.unsplash.com/photo-1619535860434-ba1d8fa12536?w=400", "is_available": true, "modifier_groups": null},
                    {"id": "lc-5", "name": "Deep Dish Pizza", "description": "Thick, deep dish pizza with extra cheese and toppings", "price": 10.99, "image_url": "https://images.unsplash.com/photo-1513104890138-7c749659a591?w=400", "is_available": true, "modifier_groups": null},
                    {"id": "lc-6", "name": "Stuffed Crust", "description": "Stuffed crust with cheese and your choice of toppings", "price": 11.49, "image_url": "https://images.unsplash.com/photo-1574071318508-1cdbab80d002?w=400", "is_available": true, "modifier_groups": null}
                ]
            },
            {
                "category_name": "Sides",
                "items": [
                    {"id": "lc-7", "name": "Crazy Bread", "description": "Warm breadsticks with garlic butter and parmesan", "price": 3.99, "image_url": "https://images.unsplash.com/photo-1548340748-6d2b7d7da280?w=400", "is_available": true, "modifier_groups": null},
                    {"id": "lc-8", "name": "Caesar Wings (8pc)", "description": "Crispy chicken wings tossed in your choice of sauce", "price": 8.99, "image_url": "https://images.unsplash.com/photo-1608039829572-9b1234ef1321?w=400", "is_available": true, "modifier_groups": null}
                ]
            }
        ]
    }
    """,

    "pizza-4" to """
    {
        "restaurant_id": "pizza-4",
        "brand_info": {
            "name": "Luigi's Wood Fire",
            "logo_url": "",
            "banner_image_url": "https://images.unsplash.com/photo-1604382355076-af4b0eb60143?w=800",
            "rating": 4.7,
            "review_count": "567+",
            "cuisine": "Pizza",
            "tags": ["U-DO Pass", "Pizza", "Artisan", "Wood Fire"]
        },
        "location_info": {
            "distance": "1.5 mi",
            "delivery_fee": 2.99,
            "delivery_time_est": "35 min",
            "address": "55 Mulberry St, New York, NY 10013",
            "phone": "(212) 555-0555",
            "operating_hours": {
                "Mon": "11:00 AM - 10:00 PM",
                "Tue": "11:00 AM - 10:00 PM",
                "Wed": "11:00 AM - 10:00 PM",
                "Thu": "11:00 AM - 11:00 PM",
                "Fri": "11:00 AM - 12:00 AM",
                "Sat": "11:00 AM - 12:00 AM",
                "Sun": "12:00 PM - 10:00 PM"
            }
        },
        "menu": [
            {
                "category_name": "Wood Fire Specials",
                "items": [
                    {"id": "lf-1", "name": "Truffle Mushroom Pizza", "description": "Wild mushrooms, truffle oil, fontina, fresh thyme on wood-fired crust", "price": 18.99, "image_url": "https://images.unsplash.com/photo-1604382355076-af4b0eb60143?w=400", "is_available": true, "modifier_groups": null},
                    {"id": "lf-2", "name": "Prosciutto & Arugula", "description": "Prosciutto di Parma, fresh arugula, shaved parmesan, balsamic glaze", "price": 19.99, "image_url": "https://images.unsplash.com/photo-1593560708920-61dd98c46a4e?w=400", "is_available": true, "modifier_groups": null},
                    {"id": "lf-3", "name": "Burrata Pizza", "description": "Fresh burrata, cherry tomatoes, pesto, sea salt", "price": 17.99, "image_url": "https://images.unsplash.com/photo-1574071318508-1cdbab80d002?w=400", "is_available": true, "modifier_groups": null},
                    {"id": "lf-4", "name": "Quattro Formaggi", "description": "Mozzarella, gorgonzola, fontina, parmesan blend", "price": 16.99, "image_url": "https://images.unsplash.com/photo-1588315029754-2dd089d39a1a?w=400", "is_available": true, "modifier_groups": null}
                ]
            },
            {
                "category_name": "Classic Pies",
                "items": [
                    {"id": "lf-5", "name": "Wood-Fired Margherita", "description": "San Marzano tomatoes, fresh mozzarella, basil in a 900° oven", "price": 14.99, "image_url": "https://images.unsplash.com/photo-1565299624946-b28f40a0ae38?w=400", "is_available": true, "modifier_groups": null},
                    {"id": "lf-6", "name": "Diavola", "description": "Spicy salami, roasted peppers, mozzarella, chili oil", "price": 15.99, "image_url": "https://images.unsplash.com/photo-1513104890138-7c749659a591?w=400", "is_available": true, "modifier_groups": null},
                    {"id": "lf-7", "name": "Calzone Luigi", "description": "Folded pizza stuffed with ricotta, salami, ham, and mozzarella", "price": 16.99, "image_url": "https://images.unsplash.com/photo-1565299585323-38d6b0865b47?w=400", "is_available": true, "modifier_groups": null}
                ]
            },
            {
                "category_name": "Antipasti",
                "items": [
                    {"id": "lf-8", "name": "Bruschetta Trio", "description": "Tomato basil, roasted pepper, and olive tapenade on crostini", "price": 9.99, "image_url": "https://images.unsplash.com/photo-1548340748-6d2b7d7da280?w=400", "is_available": true, "modifier_groups": null},
                    {"id": "lf-9", "name": "Tiramisu", "description": "Classic Italian tiramisu with espresso-soaked ladyfingers", "price": 8.99, "image_url": "https://images.unsplash.com/photo-1609126979532-7f7b1e0e1c67?w=400", "is_available": true, "modifier_groups": null}
                ]
            }
        ]
    }
    """
)

// MARK: - API Fetcher (live API first, mock fallback)

// MARK: - Quick Brand Fetch (banner + name only, for fast skeleton dismiss)

suspend fun fetchBrandQuick(restaurantId: String): StoreData? {
    return withContext(Dispatchers.IO) {
        try {
            val brandUrl = "${Config.API_BASE_URL}/brands/$restaurantId"
            val conn = java.net.URL(brandUrl).openConnection() as java.net.HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            if (conn.responseCode == 200) {
                val brandJson = JSONObject(conn.inputStream.bufferedReader().readText())
                val brandName = brandJson.optString("name", "")
                val brandType = brandJson.optString("brandType", "")
                val logoUrl = brandJson.optString("logoUrl", "")
                val bannerUrl = brandJson.optString("bannerUrl", "")
                val carouselArr = brandJson.optJSONArray("carouselImages")
                val carouselImages = if (carouselArr != null) {
                    (0 until carouselArr.length()).mapNotNull { carouselArr.optString(it).takeIf { s -> s.isNotEmpty() } }
                } else emptyList<String>()
                val effectiveBanner = bannerUrl.ifEmpty { carouselImages.firstOrNull() ?: "" }
                val tagsArr = brandJson.optJSONArray("tags")
                val tags = if (tagsArr != null) (0 until tagsArr.length()).map { tagsArr.getString(it) } else emptyList<String>()
                conn.disconnect()

                if (brandName.isNotEmpty()) {
                    return@withContext StoreData(
                        restaurant_id = restaurantId,
                        brand_info = StoreBrandInfo(
                            name = brandName,
                            logo_url = logoUrl,
                            banner_image_url = effectiveBanner,
                            rating = 4.5,
                            review_count = "100+",
                            cuisine = brandType.replaceFirstChar { it.uppercase() },
                            tags = tags.ifEmpty { listOf(brandType.replaceFirstChar { it.uppercase() }) }
                        ),
                        location_info = StoreLocationInfo(
                            distance = "", delivery_fee = 0.0, delivery_time_est = "20-35 min",
                            address = "", phone = null, operating_hours = null,
                            latitude = 0.0, longitude = 0.0
                        ),
                        menu = emptyList() // Menu loaded in full fetch
                    )
                }
            }
            conn.disconnect()
        } catch (e: Exception) {
            Log.w("StoreApi", "⚠️ Quick brand fetch failed: ${e.message}")
        }
        null
    }
}

// MARK: - Full Store Detail (brand + location + menu)

suspend fun fetchStoreDetail(restaurantId: String, storeName: String = "", context: Context? = null): StoreData? {
    return withContext(Dispatchers.IO) {
        // 0. Prefetch GPS coordinates if context is provided
        var lat = 0.0
        var lng = 0.0
        if (context != null) {
            try {
                val coords = LocationManager.fetchLocation(context)
                lat = coords.first
                lng = coords.second
            } catch (e: Exception) {
                Log.w("StoreApi", "⚠️ Location fetch failed: ${e.message}")
            }
        }

        // 1. Try /nearest-store?lat=&lng= (single call → brand + location + menu)
        try {
            val nearestUrl = "${Config.API_BASE_URL}/brands/$restaurantId/nearest-store?lat=$lat&lng=$lng"
            Log.d("StoreApi", "➡️ Trying nearest-store: $nearestUrl")
            val conn = java.net.URL(nearestUrl).openConnection() as java.net.HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            if (conn.responseCode == 200) {
                val rawJson = conn.inputStream.bufferedReader().readText()
                Log.d("StoreApi", "📋 /nearest-store raw JSON (first 500): ${rawJson.take(500)}")
                val root = JSONObject(rawJson)
                val brandObj = root.optJSONObject("brand")
                val storeObj = root.optJSONObject("nearestStore")
                val menuObj = root.optJSONObject("menu")

                if (brandObj != null) {
                    val brandName = brandObj.optString("name", "")
                    val brandType = brandObj.optString("brandType", "")
                    val logoUrl = brandObj.optString("logoUrl", "")
                    val bannerUrl = brandObj.optString("bannerUrl", "")
                    val carouselArr = brandObj.optJSONArray("carouselImages")
                    val carouselImages = if (carouselArr != null) {
                        (0 until carouselArr.length()).mapNotNull { carouselArr.optString(it).takeIf { s -> s.isNotEmpty() } }
                    } else emptyList<String>()
                    val effectiveBanner = bannerUrl.ifEmpty { carouselImages.firstOrNull() ?: "" }
                    val tagsArr = brandObj.optJSONArray("tags")
                    val tags = if (tagsArr != null) (0 until tagsArr.length()).map { tagsArr.getString(it) } else emptyList<String>()

                    // Parse location info from nearestStore
                    var distance = ""
                    var address = ""
                    var phone: String? = null
                    var deliveryFee = 0.0
                    var deliveryTimeEst = "20-35 min"
                    var operatingHours: Map<String, String>? = null
                    var storeLat = 0.0
                    var storeLng = 0.0
                    var deliveryEnabled: Boolean? = null
                    var pickupEnabled: Boolean? = null
                    var deliveryRadius: Double? = null

                    if (storeObj != null) {
                        val dist = storeObj.optDouble("distance", 0.0)
                        distance = if (dist > 0) "${String.format("%.1f", dist)} mi" else ""
                        // Build full address: address, city, state zipCode
                        val addrParts = mutableListOf<String>()
                        val rawAddr = storeObj.optString("address", "")
                        val rawCity = storeObj.optString("city", "")
                        val rawState = storeObj.optString("state", "")
                        val rawZipCode = storeObj.optString("zipCode", "")
                        if (rawAddr.isNotEmpty()) addrParts.add(rawAddr)
                        if (rawCity.isNotEmpty() && !rawAddr.contains(rawCity)) addrParts.add(rawCity)
                        if (rawState.isNotEmpty() && !rawAddr.contains(rawState)) {
                            addrParts.add(if (rawZipCode.isNotEmpty() && !rawAddr.contains(rawZipCode)) "$rawState $rawZipCode" else rawState)
                        } else if (rawZipCode.isNotEmpty() && !rawAddr.contains(rawZipCode)) {
                            addrParts.add(rawZipCode)
                        }
                        address = addrParts.joinToString(", ")
                        phone = storeObj.optString("phone", "").takeIf { it.isNotEmpty() }
                        deliveryTimeEst = storeObj.optString("deliveryTimeEst", "20-35 min")
                        storeLat = storeObj.optDouble("latitude", 0.0)
                        storeLng = storeObj.optDouble("longitude", 0.0)
                        deliveryEnabled = if (storeObj.has("deliveryEnabled")) storeObj.optBoolean("deliveryEnabled", false) else null
                        pickupEnabled = if (storeObj.has("pickupEnabled")) storeObj.optBoolean("pickupEnabled", false) else null
                        deliveryRadius = if (storeObj.has("deliveryRadius")) storeObj.optDouble("deliveryRadius", 0.0) else null
                        // Parse hours — handles both structured {monday: {open, close, closed}} and flat {Mon: "10 AM - 11 PM"}
                        val hoursObj = storeObj.optJSONObject("hours")
                        if (hoursObj != null) {
                            val dayKeyMap = mapOf(
                                "monday" to "Monday", "tuesday" to "Tuesday", "wednesday" to "Wednesday",
                                "thursday" to "Thursday", "friday" to "Friday", "saturday" to "Saturday",
                                "sunday" to "Sunday"
                            )
                            val hoursMap = mutableMapOf<String, String>()
                            val keys = hoursObj.keys()
                            while (keys.hasNext()) {
                                val key = keys.next()
                                val value = hoursObj.get(key)
                                if (value is JSONObject) {
                                    // Structured format: {open: "10:00", close: "23:00", closed: false}
                                    val closed = value.optBoolean("closed", false)
                                    if (closed) {
                                        hoursMap[dayKeyMap[key] ?: key.replaceFirstChar { it.uppercase() }] = "Closed"
                                    } else {
                                        val open = value.optString("open", "")
                                        val close = value.optString("close", "")
                                        if (open.isNotEmpty() && close.isNotEmpty()) {
                                            hoursMap[dayKeyMap[key] ?: key.replaceFirstChar { it.uppercase() }] = "$open - $close"
                                        }
                                    }
                                } else {
                                    // Flat string format: "10:00 AM - 11:00 PM"
                                    hoursMap[key] = value.toString()
                                }
                            }
                            operatingHours = hoursMap.ifEmpty { null }
                        }
                    }

                    // Parse menu categories
                    val menuCategories = mutableListOf<StoreMenuCategory>()
                    if (menuObj != null) {
                        val categoriesArr = menuObj.optJSONArray("categories")
                        val modGroupsArr = menuObj.optJSONArray("modifierGroups")

                        // Parse modifier groups
                        val modGroupMap = HashMap<String, StoreModifierGroup>()
                        if (modGroupsArr != null) {
                            for (mg in 0 until modGroupsArr.length()) {
                                val mgObj = modGroupsArr.getJSONObject(mg)
                                val optsArr = mgObj.optJSONArray("options") ?: JSONArray()
                                modGroupMap[mgObj.optString("id", "")] = StoreModifierGroup(
                                    id = mgObj.optString("id", ""),
                                    name = mgObj.optString("name", ""),
                                    min_selection = mgObj.optInt("minSelect", 0),
                                    max_selection = mgObj.optInt("maxSelect", 1),
                                    is_required = mgObj.optBoolean("required", false),
                                    options = (0 until optsArr.length()).map { oi ->
                                        val oObj = optsArr.getJSONObject(oi)
                                        StoreModifierOption(
                                            id = oObj.optString("id", ""),
                                            name = oObj.optString("name", ""),
                                            extra_price = oObj.optDouble("price", 0.0),
                                            is_default = false
                                        )
                                    }
                                )
                            }
                        }

                        if (categoriesArr != null) {
                            for (ci in 0 until categoriesArr.length()) {
                                val catObj = categoriesArr.getJSONObject(ci)
                                val catName = catObj.optString("name", "")
                                val itemsArr = catObj.optJSONArray("items")
                                val catItems = mutableListOf<StoreMenuItem>()
                                if (itemsArr != null) {
                                    for (ii in 0 until itemsArr.length()) {
                                        val iObj = itemsArr.getJSONObject(ii)
                                        val itemModGroupIds = iObj.optJSONArray("modifierGroupIds")
                                        val itemModGroups = mutableListOf<StoreModifierGroup>()
                                        if (itemModGroupIds != null) {
                                            for (gi in 0 until itemModGroupIds.length()) {
                                                modGroupMap[itemModGroupIds.getString(gi)]?.let { itemModGroups.add(it) }
                                            }
                                        }
                                        val resolvedImg = fallbackImageUrl(resolveImageUrl(iObj), logoUrl, effectiveBanner)
                                        catItems.add(StoreMenuItem(
                                            id = iObj.optString("id", ""),
                                            name = iObj.optString("name", ""),
                                            description = iObj.optString("description", ""),
                                            price = iObj.optDouble("basePrice", iObj.optDouble("price", 0.0)),
                                            image_url = resolvedImg,
                                            is_available = iObj.optBoolean("isAvailable", true),
                                            modifier_groups = itemModGroups
                                        ))
                                    }
                                }
                                if (catItems.isNotEmpty()) {
                                    menuCategories.add(StoreMenuCategory(category_name = catName, items = catItems))
                                }
                            }
                        }
                    }

                    val storeData = StoreData(
                        restaurant_id = restaurantId,
                        brand_info = StoreBrandInfo(
                            name = brandName,
                            logo_url = logoUrl,
                            banner_image_url = effectiveBanner,
                            rating = 4.5,
                            review_count = "100+",
                            cuisine = brandType.replaceFirstChar { it.uppercase() },
                            tags = tags.ifEmpty { listOf(brandType.replaceFirstChar { it.uppercase() }) }
                        ),
                        location_info = StoreLocationInfo(
                            distance = distance,
                            delivery_fee = deliveryFee,
                            delivery_time_est = deliveryTimeEst,
                            address = address,
                            phone = phone,
                            operating_hours = operatingHours,
                            latitude = storeLat,
                            longitude = storeLng,
                            delivery_enabled = deliveryEnabled,
                            pickup_enabled = pickupEnabled,
                            delivery_radius = deliveryRadius
                        ),
                        menu = menuCategories.ifEmpty {
                            listOf(StoreMenuCategory(
                                category_name = "Menu",
                                items = listOf(StoreMenuItem(
                                    id = "placeholder",
                                    name = "Menu coming soon",
                                    description = "This restaurant's menu is being updated",
                                    price = 0.0,
                                    image_url = "",
                                    is_available = false,
                                    modifier_groups = emptyList()
                                ))
                            ))
                        }
                    )
                    Log.d("StoreApi", "✅ Loaded from /nearest-store: $brandName, ${menuCategories.size} categories, distance=$distance")
                    return@withContext storeData
                }
            }
            conn.disconnect()
        } catch (e: Exception) {
            Log.w("StoreApi", "⚠️ /nearest-store failed: ${e.message}")
        }

        // 2. Try brand API — fetches brand info + menu from /brands/{id}
        try {
            val brandUrl = "${Config.API_BASE_URL}/brands/$restaurantId"
            val brandConn = java.net.URL(brandUrl).openConnection() as java.net.HttpURLConnection
            brandConn.requestMethod = "GET"
            brandConn.connectTimeout = 10000
            brandConn.readTimeout = 10000
                    if (brandConn.responseCode == 200) {
                        val brandRawJson = brandConn.inputStream.bufferedReader().readText()
                        Log.d("StoreApi", "📋 /brands/{id} raw JSON (first 500): ${brandRawJson.take(500)}")
                        val brandJson = JSONObject(brandRawJson)
                val brandName = brandJson.optString("name", "")
                val brandType = brandJson.optString("brandType", "")
                val logoUrl = brandJson.optString("logoUrl", "")
                val bannerUrl = brandJson.optString("bannerUrl", "")
                val carouselArr = brandJson.optJSONArray("carouselImages")
                val carouselImages = if (carouselArr != null) {
                    (0 until carouselArr.length()).mapNotNull { carouselArr.optString(it).takeIf { s -> s.isNotEmpty() } }
                } else emptyList<String>()
                val tagsArr = brandJson.optJSONArray("tags")
                val tags = if (tagsArr != null) (0 until tagsArr.length()).map { tagsArr.getString(it) } else emptyList<String>()

                // Use banner, or first carousel image
                val effectiveBanner = bannerUrl.ifEmpty { carouselImages.firstOrNull() ?: "" }

                // Try fetching menu for restaurant brands, or catalog for grocery brands
                val menuCategories = mutableListOf<StoreMenuCategory>()

                // Try restaurant menu: /brands/{brandId}/menu
                try {
                    val menuUrl = "${Config.API_BASE_URL}/brands/$restaurantId/menu"
                    val menuConn = java.net.URL(menuUrl).openConnection() as java.net.HttpURLConnection
                    menuConn.requestMethod = "GET"
                    menuConn.connectTimeout = 10000
                    menuConn.readTimeout = 10000
                    if (menuConn.responseCode == 200) {
                        val menuJson = JSONObject(menuConn.inputStream.bufferedReader().readText())
                        val categoriesArr = menuJson.optJSONArray("categories")
                        val itemsArr = menuJson.optJSONArray("items")
                        val modGroupsArr = menuJson.optJSONArray("modifierGroups")

                        // Parse modifier groups for lookup
                        val modGroupMap = HashMap<String, StoreModifierGroup>()
                        if (modGroupsArr != null) {
                            for (mg in 0 until modGroupsArr.length()) {
                                val mgObj = modGroupsArr.getJSONObject(mg)
                                val optsArr = mgObj.optJSONArray("options") ?: JSONArray()
                                modGroupMap[mgObj.optString("id", "")] = StoreModifierGroup(
                                    id = mgObj.optString("id", ""),
                                    name = mgObj.optString("name", ""),
                                    min_selection = mgObj.optInt("minSelect", 0),
                                    max_selection = mgObj.optInt("maxSelect", 1),
                                    is_required = mgObj.optBoolean("required", false),
                                    options = (0 until optsArr.length()).map { oi ->
                                        val oObj = optsArr.getJSONObject(oi)
                                        StoreModifierOption(
                                            id = oObj.optString("id", ""),
                                            name = oObj.optString("name", ""),
                                            extra_price = oObj.optDouble("price", 0.0),
                                            is_default = false
                                        )
                                    }
                                )
                            }
                        }

                        // Parse all items
                        val allItems = mutableListOf<Pair<StoreMenuItem, List<String>>>() // item + categoryIds
                        if (itemsArr != null) {
                            for (ii in 0 until itemsArr.length()) {
                                val iObj = itemsArr.getJSONObject(ii)
                                val catIdsArr = iObj.optJSONArray("categoryIds")
                                val catIds = if (catIdsArr != null) {
                                    (0 until catIdsArr.length()).map { catIdsArr.getString(it) }
                                } else emptyList<String>()

                                // Resolve modifier groups
                                val itemModGroupIds = iObj.optJSONArray("modifierGroupIds")
                                val itemModGroups = mutableListOf<StoreModifierGroup>()
                                if (itemModGroupIds != null) {
                                    for (gi in 0 until itemModGroupIds.length()) {
                                        modGroupMap[itemModGroupIds.getString(gi)]?.let { itemModGroups.add(it) }
                                    }
                                }

                                val resolvedImg = fallbackImageUrl(resolveImageUrl(iObj), logoUrl, effectiveBanner)
                                val item = StoreMenuItem(
                                    id = iObj.optString("id", ""),
                                    name = iObj.optString("name", ""),
                                    description = iObj.optString("description", ""),
                                    price = iObj.optDouble("basePrice", 0.0),
                                    image_url = resolvedImg,
                                    is_available = iObj.optBoolean("isAvailable", true),
                                    modifier_groups = itemModGroups
                                )
                                allItems.add(item to catIds)
                            }
                        }

                        // Build category map
                        val categoryMap = linkedMapOf<String, Pair<String, MutableList<StoreMenuItem>>>()
                        if (categoriesArr != null) {
                            for (ci in 0 until categoriesArr.length()) {
                                val cObj = categoriesArr.getJSONObject(ci)
                                val catId = cObj.optString("id", "")
                                val catName = cObj.optString("name", "")
                                if (cObj.optBoolean("isActive", true)) {
                                    categoryMap[catId] = (catName to mutableListOf())
                                }
                            }
                        }

                        // Assign items to categories
                        for ((item, catIds) in allItems) {
                            for (catId in catIds) {
                                categoryMap[catId]?.second?.add(item)
                            }
                        }

                        // Convert to StoreMenuCategory list
                        for ((_, pair) in categoryMap) {
                            if (pair.second.isNotEmpty()) {
                                menuCategories.add(StoreMenuCategory(category_name = pair.first, items = pair.second))
                            }
                        }

                        println("✅ [StoreApi] Loaded brand menu: $brandName with ${allItems.size} items in ${menuCategories.size} categories")
                    }
                    menuConn.disconnect()
                } catch (e: Exception) {
                    println("⚠️ [StoreApi] Brand menu fetch failed: ${e.message}")
                }

                // Try grocery catalog if no menu categories found: /brands/{brandId}/catalog
                if (menuCategories.isEmpty()) {
                    try {
                        val catalogUrl = "${Config.API_BASE_URL}/brands/$restaurantId/catalog"
                        val catalogConn = java.net.URL(catalogUrl).openConnection() as java.net.HttpURLConnection
                        catalogConn.requestMethod = "GET"
                        catalogConn.connectTimeout = 10000
                        catalogConn.readTimeout = 10000
                        if (catalogConn.responseCode == 200) {
                            val catalogJson = JSONObject(catalogConn.inputStream.bufferedReader().readText())
                            val catArr = catalogJson.optJSONArray("categories")
                            val cItemsArr = catalogJson.optJSONArray("items")

                            val catMap = linkedMapOf<String, String>()
                            if (catArr != null) {
                                for (ci in 0 until catArr.length()) {
                                    val cObj = catArr.getJSONObject(ci)
                                    catMap[cObj.optString("id", "")] = cObj.optString("name", "")
                                }
                            }

                            val itemsByCategory = linkedMapOf<String, MutableList<StoreMenuItem>>()
                            if (cItemsArr != null) {
                                for (ii in 0 until cItemsArr.length()) {
                                    val iObj = cItemsArr.getJSONObject(ii)
                                    val catIdsArr = iObj.optJSONArray("categoryIds")
                                    val catIds = if (catIdsArr != null) {
                                        (0 until catIdsArr.length()).map { catIdsArr.getString(it) }
                                    } else emptyList<String>()

                                    val resolvedImg = fallbackImageUrl(resolveImageUrl(iObj), logoUrl, effectiveBanner)
                                    val item = StoreMenuItem(
                                        id = iObj.optString("id", ""),
                                        name = iObj.optString("name", ""),
                                        description = iObj.optString("description", ""),
                                        price = iObj.optDouble("price", 0.0),
                                        image_url = resolvedImg,
                                        is_available = iObj.optBoolean("isAvailable", true),
                                        modifier_groups = emptyList()
                                    )
                                    for (catId in catIds) {
                                        val catName = catMap[catId] ?: "Other"
                                        itemsByCategory.getOrPut(catName) { mutableListOf() }.add(item)
                                    }
                                }
                            }

                            for ((catName, items) in itemsByCategory) {
                                menuCategories.add(StoreMenuCategory(category_name = catName, items = items))
                            }
                            println("✅ [StoreApi] Loaded brand catalog: $brandName with ${menuCategories.size} categories")
                        }
                        catalogConn.disconnect()
                    } catch (e: Exception) {
                        println("⚠️ [StoreApi] Brand catalog fetch failed: ${e.message}")
                    }
                }

                // Build StoreData from brand info
                if (brandName.isNotEmpty()) {
                    return@withContext StoreData(
                        restaurant_id = restaurantId,
                        brand_info = StoreBrandInfo(
                            name = brandName,
                            logo_url = logoUrl,
                            banner_image_url = effectiveBanner,
                            rating = 4.5,
                            review_count = "100+",
                            cuisine = brandType.replaceFirstChar { it.uppercase() },
                            tags = tags.ifEmpty { listOf(brandType.replaceFirstChar { it.uppercase() }) }
                        ),
                        location_info = StoreLocationInfo(
                            distance = "",
                            delivery_fee = 0.0,
                            delivery_time_est = "20-35 min",
                            address = "",
                            phone = null,
                            operating_hours = null,
                            latitude = 0.0,
                            longitude = 0.0
                        ),
                        menu = menuCategories.ifEmpty {
                            listOf(StoreMenuCategory(
                                category_name = "Menu",
                                items = listOf(StoreMenuItem(
                                    id = "placeholder",
                                    name = "Menu coming soon",
                                    description = "This restaurant's menu is being updated",
                                    price = 0.0,
                                    image_url = "",
                                    is_available = false,
                                    modifier_groups = emptyList()
                                ))
                            ))
                        }
                    )
                }
            }
            brandConn.disconnect()
        } catch (e: Exception) {
            println("⚠️ [StoreApi] Brand fetch failed: ${e.message}")
        }

        // 3. Try restaurant API
        try {
            val url = "${Config.API_BASE_URL}/restaurants/$restaurantId"
            val jsonStr = java.net.URL(url).readText()
            return@withContext parseStoreJson(JSONObject(jsonStr))
        } catch (_: Exception) { }

        // 4. Try grocery store API — fetch store info + items separately (matches iOS fetchGroceryStore)
        try {
            val storeUrl = "${Config.API_BASE_URL}/grocery-stores/$restaurantId"
            val storeJsonStr = java.net.URL(storeUrl).readText()
            val storeJson = JSONObject(storeJsonStr)

            val gStoreName = storeJson.optString("name", storeName.ifEmpty { "Grocery Store" })
            val gLogoUrl = storeJson.optString("logoUrl", "")
            val gBannerUrl = storeJson.optString("bannerUrl", "")

            // Fetch items from /grocery-stores/{id}/items
            val itemsUrl = "${Config.API_BASE_URL}/grocery-stores/$restaurantId/items"
            val itemsJsonStr = java.net.URL(itemsUrl).readText()
            val itemsRoot = JSONObject(itemsJsonStr)

            // Items may be in {"items": [...]} or a bare array
            val itemsArr = if (itemsRoot.has("items")) itemsRoot.getJSONArray("items") else JSONArray(itemsJsonStr)

            // Build menu items and preserve category info for grouping
            data class ItemWithCategory(val menuItem: StoreMenuItem, val category: String)

            val itemsWithCats = (0 until itemsArr.length()).map { i ->
                val item = itemsArr.getJSONObject(i)
                val cat = item.optString("category", "")
                val resolvedImg = fallbackImageUrl(resolveImageUrl(item), gLogoUrl, gBannerUrl)
                ItemWithCategory(
                    menuItem = StoreMenuItem(
                        id = item.optString("id", "g-$i"),
                        name = item.optString("name", "Item"),
                        description = item.optString("description", ""),
                        price = item.optDouble("price", 0.0),
                        image_url = resolvedImg,
                        is_available = true,
                        modifier_groups = emptyList()
                    ),
                    category = cat
                )
            }

            // Group items by category (dynamic — categories come from the store's items) — matches iOS
            val categoryMap = linkedMapOf<String, MutableList<StoreMenuItem>>()
            for (iwc in itemsWithCats) {
                val key = if (iwc.category.isEmpty()) "Other" else iwc.category
                categoryMap.getOrPut(key) { mutableListOf() }.add(iwc.menuItem)
            }
            val menuCategories = categoryMap.map { (cat, items) ->
                StoreMenuCategory(category_name = cat, items = items)
            }

            val groceryStoreData = StoreData(
                restaurant_id = restaurantId,
                brand_info = StoreBrandInfo(
                    name = gStoreName,
                    logo_url = gLogoUrl,
                    banner_image_url = gBannerUrl,
                    rating = 4.5,
                    review_count = "Grocery",
                    cuisine = "Grocery",
                    tags = listOf("Grocery", "Delivery")
                ),
                location_info = StoreLocationInfo(
                    distance = "",
                    delivery_fee = 0.0,
                    delivery_time_est = "30 min",
                    address = "",
                    phone = null,
                    operating_hours = null,
                    latitude = 0.0,
                    longitude = 0.0
                ),
                menu = menuCategories
            )
            println("✅ [StoreApi] Loaded grocery store: $gStoreName with ${itemsWithCats.size} items in ${menuCategories.size} categories")
            return@withContext groceryStoreData
        } catch (e: Exception) {
            println("⚠️ [StoreApi] Grocery store fetch failed: ${e.message}")
        }

        // 5. Mock fallback — try mock data again
        val mockFallback = mockStoreJSON[restaurantId]
        if (mockFallback != null) {
            try {
                return@withContext parseStoreJson(JSONObject(mockFallback))
            } catch (_: Exception) { }
        }

        // 6. Generate fallback store data using storeName (for grocery stores without API)
        if (storeName.isNotEmpty()) {
            return@withContext generateFallbackStoreData(restaurantId, storeName)
        }

        null
    }
}

/**
 * Generate fallback StoreData for stores that don't have mock data or API endpoints.
 * Used primarily for grocery stores like 7-Eleven, Target, etc.
 */
private fun generateFallbackStoreData(storeId: String, storeName: String): StoreData {
    return StoreData(
        restaurant_id = storeId,
        brand_info = StoreBrandInfo(
            name = storeName,
            logo_url = "",
            banner_image_url = "https://images.unsplash.com/photo-1604719312566-8912e9227c6a?w=800",
            rating = 4.2,
            review_count = "100+",
            cuisine = "Grocery",
            tags = listOf("Grocery", "Convenience", "Delivery")
        ),
        location_info = StoreLocationInfo(
            distance = "0.5 mi",
            delivery_fee = 1.99,
            delivery_time_est = "15-25 min",
            address = "",
            phone = null,
            operating_hours = mapOf(
                "Mon" to "6:00 AM - 11:00 PM",
                "Tue" to "6:00 AM - 11:00 PM",
                "Wed" to "6:00 AM - 11:00 PM",
                "Thu" to "6:00 AM - 11:00 PM",
                "Fri" to "6:00 AM - 12:00 AM",
                "Sat" to "6:00 AM - 12:00 AM",
                "Sun" to "7:00 AM - 10:00 PM"
            ),
            latitude = 0.0,
            longitude = 0.0
        ),
        menu = listOf(
            StoreMenuCategory(
                category_name = "Popular Items",
                items = listOf(
                    StoreMenuItem(id = "$storeId-item-1", name = "Bottled Water (6pk)", description = "Purified bottled water, 6 pack", price = 3.99, image_url = "", is_available = true, modifier_groups = emptyList()),
                    StoreMenuItem(id = "$storeId-item-2", name = "Chips Assortment", description = "Your favorite chip brands", price = 2.49, image_url = "", is_available = true, modifier_groups = emptyList()),
                    StoreMenuItem(id = "$storeId-item-3", name = "Energy Drink", description = "Popular energy drink, 16oz", price = 3.29, image_url = "", is_available = true, modifier_groups = emptyList()),
                    StoreMenuItem(id = "$storeId-item-4", name = "Fresh Fruit Cup", description = "Seasonal fresh fruit, pre-cut", price = 4.99, image_url = "", is_available = true, modifier_groups = emptyList())
                )
            ),
            StoreMenuCategory(
                category_name = "Snacks",
                items = listOf(
                    StoreMenuItem(id = "$storeId-item-5", name = "Candy Bar", description = "Assorted candy bars", price = 1.99, image_url = "", is_available = true, modifier_groups = emptyList()),
                    StoreMenuItem(id = "$storeId-item-6", name = "Granola Bar", description = "Healthy granola snack bar", price = 2.49, image_url = "", is_available = true, modifier_groups = emptyList()),
                    StoreMenuItem(id = "$storeId-item-7", name = "Trail Mix", description = "Mixed nuts and dried fruit", price = 4.49, image_url = "", is_available = true, modifier_groups = emptyList()),
                    StoreMenuItem(id = "$storeId-item-8", name = "Cookies Pack", description = "Fresh baked cookies, 4 pack", price = 3.99, image_url = "", is_available = true, modifier_groups = emptyList())
                )
            ),
            StoreMenuCategory(
                category_name = "Drinks",
                items = listOf(
                    StoreMenuItem(id = "$storeId-item-9", name = "Soda (2L)", description = "Assorted soda flavors, 2 liter", price = 2.99, image_url = "", is_available = true, modifier_groups = emptyList()),
                    StoreMenuItem(id = "$storeId-item-10", name = "Orange Juice", description = "Fresh orange juice, 52oz", price = 4.99, image_url = "", is_available = true, modifier_groups = emptyList()),
                    StoreMenuItem(id = "$storeId-item-11", name = "Iced Coffee", description = "Cold brew iced coffee", price = 3.49, image_url = "", is_available = true, modifier_groups = emptyList()),
                    StoreMenuItem(id = "$storeId-item-12", name = "Milk (1 Gallon)", description = "Whole milk, 1 gallon", price = 4.49, image_url = "", is_available = true, modifier_groups = emptyList())
                )
            )
        )
    )
}

// MARK: - JSON Parser (shared by API and local file)

fun parseStoreJson(root: JSONObject): StoreData {
    val brandObj = root.getJSONObject("brand_info")
    val locObj = root.getJSONObject("location_info")
    val menuArr = root.getJSONArray("menu")

    val brandInfo = StoreBrandInfo(
        name = brandObj.getString("name"),
        logo_url = brandObj.optString("logo_url", ""),
        banner_image_url = brandObj.optString("banner_image_url", ""),
        rating = brandObj.optDouble("rating", 0.0),
        review_count = brandObj.optString("review_count", "New"),
        cuisine = brandObj.optString("cuisine", ""),
        tags = parseStringArray(brandObj.optJSONArray("tags"))
    )

    // Parse operating_hours if present
    val operatingHours = parseOperatingHours(locObj.optJSONObject("operating_hours"))

    val phone = locObj.optString("phone", "").ifEmpty { null }

    val locationInfo = StoreLocationInfo(
        distance = locObj.optString("distance", ""),
        delivery_fee = locObj.optDouble("delivery_fee", 2.99),
        delivery_time_est = locObj.optString("delivery_time", "20-35 min"),
        address = locObj.optString("address", ""),
        phone = phone,
        operating_hours = operatingHours,
        latitude = locObj.optDouble("latitude", 0.0),
        longitude = locObj.optDouble("longitude", 0.0)
    )

    val menu = (0 until menuArr.length()).map { i ->
        val catObj = menuArr.getJSONObject(i)
        val itemsArr = catObj.getJSONArray("items")
        StoreMenuCategory(
            category_name = catObj.getString("category_name"),
            items = (0 until itemsArr.length()).map { j ->
                val itemObj = itemsArr.getJSONObject(j)
                val modArr = itemObj.optJSONArray("modifier_groups")
                StoreMenuItem(
                    id = itemObj.optString("id", ""),
                    name = itemObj.getString("name"),
                    description = itemObj.optString("description", ""),
                    price = itemObj.getDouble("price"),
                    image_url = itemObj.optString("image_url", ""),
                    is_available = itemObj.optBoolean("is_available", true),
                    modifier_groups = if (modArr != null) parseModifierGroups(modArr) else emptyList()
                )
            }
        )
    }

    return StoreData(
        restaurant_id = root.optString("restaurant_id", ""),
        brand_info = brandInfo,
        location_info = locationInfo,
        menu = menu
    )
}

private fun parseModifierGroups(arr: JSONArray): List<StoreModifierGroup> {
    return (0 until arr.length()).map { i ->
        val g = arr.getJSONObject(i)
        val optsArr = g.optJSONArray("options") ?: JSONArray()
        StoreModifierGroup(
            id = g.getString("id"),
            name = g.getString("name"),
            min_selection = g.optInt("min_selection", 0),
            max_selection = g.optInt("max_selection", 1),
            is_required = g.optBoolean("is_required", false),
            options = (0 until optsArr.length()).map { j ->
                val o = optsArr.getJSONObject(j)
                StoreModifierOption(
                    id = o.getString("id"),
                    name = o.getString("name"),
                    extra_price = o.optDouble("extra_price", 0.0),
                    is_default = o.optBoolean("is_default", false)
                )
            }
        )
    }
}

private fun parseStringArray(arr: JSONArray?): List<String> {
    if (arr == null) return emptyList()
    return (0 until arr.length()).map { arr.getString(it) }
}

private fun parseOperatingHours(obj: JSONObject?): Map<String, String>? {
    if (obj == null) return null
    val map = mutableMapOf<String, String>()
    val keys = obj.keys()
    while (keys.hasNext()) {
        val key = keys.next()
        map[key] = obj.getString(key)
    }
    return if (map.isEmpty()) null else map
}

// Legacy loader (backward compat)
fun loadStoreData(inputStream: InputStream): StoreData? {
    return try {
        parseStoreJson(JSONObject(inputStream.bufferedReader().use { it.readText() }))
    } catch (e: Exception) {
        null
    }
}