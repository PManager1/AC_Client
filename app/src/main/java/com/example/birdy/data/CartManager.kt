package com.example.birdy.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.birdy.ui.store.StoreMenuItem

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
    val menuItem: StoreMenuItem? = null  // Full menu item for re-opening customization (matches iOS)
)

object CartManager {
    var items by mutableStateOf(mutableListOf<CartItem>())
        private set

    var promoCode by mutableStateOf("")

    // Current restaurant context — matches iOS CartManager.shared.restaurantId
    var restaurantId by mutableStateOf("")
    var restaurantName by mutableStateOf("")

    // Bridge to present driver tracking map — matches iOS CartManager.shared.showDriverTracking
    var showDriverTracking by mutableStateOf(false)

    // Bridge to present order detail screen — matches iOS CartManager.shared.showOrderDetail
    var showOrderDetail by mutableStateOf(false)

    val subtotal: Double
        get() = items.sumOf { it.price * it.quantity }

    val deliveryFee: Double
        get() = 3.99

    val serviceFee: Double
        get() = 2.99

    val tax: Double
        get() = subtotal * 0.08 // 8% tax

    val total: Double
        get() = subtotal + deliveryFee + serviceFee + tax

    val itemCount: Int
        get() = items.sumOf { it.quantity }

    fun addItem(item: CartItem) {
        val existing = items.find {
            it.dishName == item.dishName && it.selectedOptions == item.selectedOptions
        }
        if (existing != null) {
            existing.quantity += item.quantity
            items = items.toMutableList() // trigger recomposition
        } else {
            items = (items + item).toMutableList()
        }
    }

    fun removeItem(item: CartItem) {
        items = items.filter { it.id != item.id }.toMutableList()
    }

    fun updateQuantity(item: CartItem, quantity: Int) {
        val index = items.indexOfFirst { it.id == item.id }
        if (index >= 0) {
            if (quantity <= 0) {
                items = items.filter { it.id != item.id }.toMutableList()
            } else {
                items[index].quantity = quantity
                items = items.toMutableList() // trigger recomposition
            }
        }
    }

    fun decrementItem(dishName: String) {
        val index = items.indexOfFirst { it.dishName == dishName }
        if (index >= 0) {
            val item = items[index]
            if (item.quantity > 1) {
                item.quantity -= 1
                items = items.toMutableList() // trigger recomposition
            } else {
                items = items.toMutableList().also { it.removeAt(index) }
            }
        }
    }

    fun applyPromoCode() {
        if (promoCode.lowercase() == "free") {
            // In a real app, this would apply the promo logic
            // For now, matching iOS behavior
        }
    }

    fun clear() {
        items = mutableListOf()
        promoCode = ""
    }
}