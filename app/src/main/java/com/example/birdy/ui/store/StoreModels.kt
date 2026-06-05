package com.example.birdy.ui.store

// MARK: - Data Models (matching backend StoreDetailResponse)

data class StoreBrandInfo(
    val name: String,
    val logo_url: String,
    val banner_image_url: String,
    val rating: Double,
    val review_count: String,
    val cuisine: String,
    val tags: List<String>
)

data class StoreLocationInfo(
    val distance: String,
    val delivery_fee: Double,
    val delivery_time_est: String,
    val address: String,
    val phone: String? = null,
    val operating_hours: Map<String, String>? = null,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val delivery_enabled: Boolean? = null,
    val pickup_enabled: Boolean? = null,
    val delivery_radius: Double? = null
)

data class StoreMenuCategory(
    val category_name: String,
    val items: List<StoreMenuItem>
)

data class StoreMenuItem(
    val id: String,
    val name: String,
    val description: String,
    val price: Double,
    val image_url: String,
    val is_available: Boolean,
    val modifier_groups: List<StoreModifierGroup>
)

data class StoreModifierGroup(
    val id: String,
    val name: String,
    val min_selection: Int,
    val max_selection: Int,
    val is_required: Boolean,
    val options: List<StoreModifierOption>
)

data class StoreModifierOption(
    val id: String,
    val name: String,
    val extra_price: Double,
    val is_default: Boolean
)

data class StoreData(
    val restaurant_id: String,
    val brand_info: StoreBrandInfo,
    val location_info: StoreLocationInfo,
    val menu: List<StoreMenuCategory>
)