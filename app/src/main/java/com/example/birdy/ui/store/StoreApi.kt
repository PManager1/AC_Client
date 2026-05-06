package com.example.birdy.ui.store

import com.example.birdy.data.Config
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream

// MARK: - API Fetcher

suspend fun fetchStoreDetail(restaurantId: String): StoreData? {
    return withContext(Dispatchers.IO) {
        try {
            val url = "${Config.API_BASE_URL}/restaurants/$restaurantId"
            val jsonStr = java.net.URL(url).readText()
            parseStoreJson(JSONObject(jsonStr))
        } catch (e: Exception) {
            null
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