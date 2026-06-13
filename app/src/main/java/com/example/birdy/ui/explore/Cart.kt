package com.example.birdy.ui.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.birdy.data.CartItem
import com.example.birdy.data.CartManager
import com.example.birdy.ui.components.shimmer
import com.example.birdy.ui.store.ItemDetailSheet

// MARK: - Cart Screen (full page, matches iOS NavigationLink presentation)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    onBack: () -> Unit,
    onCheckout: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Header with back arrow, title and Clear button
        TopAppBar(
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.Black
                    )
                }
            },
            title = {
                Text(
                    text = "Your Cart",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            },
            actions = {
                if (CartManager.items.isNotEmpty()) {
                    TextButton(
                        onClick = { CartManager.clear() }
                    ) {
                        Text(
                            text = "Clear",
                            color = Color.Red,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.White
            )
        )

        if (CartManager.items.isEmpty()) {
            EmptyCartView(onDismiss = onBack)
        } else {
            CartListView(onCheckout = onCheckout)
        }
    }
}

// MARK: - Empty Cart View (matches iOS emptyCartView)

@Composable
private fun EmptyCartView(onDismiss: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.ShoppingCart,
            contentDescription = "Cart",
            modifier = Modifier.size(80.dp),
            tint = Color.Gray.copy(alpha = 0.5f)
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Your cart is empty",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Add delicious items from the menu",
            fontSize = 15.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Browse Menu button
        Text(
            text = "Browse Menu",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier
                .width(200.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(Color(0xFF4CAF50), Color(0xFF388E3C))
                    ),
                    RoundedCornerShape(16.dp)
                )
                .clickable { onDismiss() }
                .padding(vertical = 14.dp),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(40.dp))
    }
}

// MARK: - Cart List View (matches iOS cartListView)

@Composable
private fun CartListView(onCheckout: () -> Unit = {}) {
    var editingCartItem by remember { mutableStateOf<CartItem?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Scrollable item list
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            // Cart items
            CartManager.items.forEach { item ->
                CartItemRow(
                    item = item,
                    onItemClick = { clickedItem ->
                        if (clickedItem.menuItem != null) {
                            editingCartItem = clickedItem
                        }
                    }
                )
            }

            // Promo Code section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .background(Color.White, RoundedCornerShape(16.dp))
            ) {
                Text(
                    text = "Promo Code",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = CartManager.promoCode,
                        onValueChange = { CartManager.promoCode = it },
                        placeholder = {
                            Text("Enter code", color = Color.Gray)
                        },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f),
                            focusedBorderColor = Color.Gray,
                            cursorColor = Color.Black
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Text(
                        text = "Apply",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(Color(0xFF2196F3), Color(0xFF1976D2))
                                ),
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { CartManager.applyPromoCode() }
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    )
                }
            }

            // Order Summary section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .background(Color.White, RoundedCornerShape(16.dp))
            ) {
                Text(
                    text = "Order Summary",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                PriceRow(title = "Subtotal", amount = CartManager.subtotal)
                Spacer(modifier = Modifier.height(8.dp))
                PriceRow(title = "Delivery Fee", amount = CartManager.deliveryFee)
                Spacer(modifier = Modifier.height(8.dp))
                PriceRow(title = "Service Fee", amount = CartManager.serviceFee)
                Spacer(modifier = Modifier.height(8.dp))
                PriceRow(title = "Tax", amount = CartManager.tax)

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = Color.Gray.copy(alpha = 0.3f)
                )

                PriceRow(title = "Total", amount = CartManager.total, isBold = true)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Proceed to Checkout button
            Text(
                text = "Proceed to Checkout • $${String.format("%.2f", CartManager.total)}",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .background(
                        Color(0xFFCC5500),
                        RoundedCornerShape(10.dp)
                    )
                    .clickable { onCheckout() }
                    .padding(vertical = 10.dp),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(100.dp))
        }
    }

    // Item Detail Sheet for editing cart items (matches iOS tap-to-edit behavior)
    editingCartItem?.let { cartItem ->
        cartItem.menuItem?.let { menuItem ->
            ItemDetailSheet(
                item = menuItem,
                restaurantName = cartItem.restaurantName,
                onDismiss = { editingCartItem = null },
                onAddToCart = { updatedItem ->
                    // Remove old item and add updated one
                    CartManager.removeItem(cartItem)
                    CartManager.addItem(updatedItem)
                    editingCartItem = null
                },
                editingCartItem = cartItem
            )
        }
    }
    } // end Box
}

// MARK: - Cart Item Row (matches iOS CartItemRow)

@Composable
fun CartItemRow(
    item: CartItem,
    onItemClick: (CartItem) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .background(Color.White, RoundedCornerShape(16.dp))
            .border(1.dp, Color.Gray.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
            .clickable { onItemClick(item) },
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Top row: Image + Info + X button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Food image
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                // Shimmer placeholder behind the image
                Box(Modifier.matchParentSize().shimmer())
                // Image loads on top, covering the shimmer
                AsyncImage(
                    model = item.imageURL,
                    contentDescription = item.dishName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )
            }

            // Info column
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = item.dishName,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )

                Text(
                    text = "from ${item.restaurantName}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )

                if (item.selectedOptions.isNotEmpty()) {
                    Text(
                        text = item.selectedOptions.joinToString(" • "),
                        fontSize = 12.sp,
                        color = Color(0xFF2196F3) // Blue
                    )
                }

                if (item.specialInstructions.isNotEmpty()) {
                    Text(
                        text = "Note: ${item.specialInstructions}",
                        fontSize = 12.sp,
                        color = Color(0xFFFF9800), // Orange
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            }

            // Remove button (X)
            IconButton(
                onClick = {
                    CartManager.removeItem(item)
                },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove",
                    tint = Color.Red.copy(alpha = 0.7f),
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // Bottom row: Quantity stepper + Price
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Quantity stepper
            Row(
                modifier = Modifier
                    .shadow(3.dp, RoundedCornerShape(50))
                    .background(Color.White, RoundedCornerShape(50))
                    .padding(horizontal = 6.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Minus button
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .background(Color(0xFFCC5500), CircleShape)
                        .clickable {
                            CartManager.updateQuantity(item, item.quantity - 1)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "−",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // Quantity
                Text(
                    text = "${item.quantity}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color.Black,
                    modifier = Modifier.width(22.dp),
                    textAlign = TextAlign.Center
                )

                // Plus button
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .background(Color(0xFFCC5500), CircleShape)
                        .clickable {
                            CartManager.updateQuantity(item, item.quantity + 1)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Item total price
            Text(
                text = "$${String.format("%.2f", item.price * item.quantity)}",
                fontSize = 20.sp,
                fontWeight = FontWeight.Normal,
                color = Color.Black
            )
        }
    }
}

// MARK: - Price Row (matches iOS PriceRow)

@Composable
fun PriceRow(
    title: String,
    amount: Double,
    isBold: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            fontSize = if (isBold) 20.sp else 17.sp,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = if (isBold) Color.Black else Color.Gray
        )
        Text(
            text = "$${String.format("%.2f", amount)}",
            fontSize = if (isBold) 20.sp else 17.sp,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = if (isBold) Color.Black else Color.Gray
        )
    }
}