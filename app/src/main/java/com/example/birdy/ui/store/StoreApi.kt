package com.example.birdy.ui.store

import com.example.birdy.data.Config
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream

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

// MARK: - API Fetcher (with mock fallback)

suspend fun fetchStoreDetail(restaurantId: String): StoreData? {
    return withContext(Dispatchers.IO) {
        // 1. Try mock data first for known IDs
        val mockJson = mockStoreJSON[restaurantId]
        if (mockJson != null) {
            try {
                return@withContext parseStoreJson(JSONObject(mockJson))
            } catch (e: Exception) {
                // Fall through to API
            }
        }

        // 2. Try API
        try {
            val url = "${Config.API_BASE_URL}/restaurants/$restaurantId"
            val jsonStr = java.net.URL(url).readText()
            parseStoreJson(JSONObject(jsonStr))
        } catch (e: Exception) {
            // 3. API failed — try mock fallback
            if (mockJson != null) {
                try {
                    parseStoreJson(JSONObject(mockJson))
                } catch (e2: Exception) {
                    null
                }
            } else {
                null
            }
        }
    }
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