package com.example.birdy.ui.store

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.util.Log
import coil.compose.SubcomposeAsyncImage
import com.example.birdy.data.CartItem
import com.example.birdy.data.CartManager

// MARK: - Store Food Card (matches iOS StoreFoodCardView)

@Composable
fun StoreFoodCard(
    menuItem: StoreMenuItem,
    restaurantName: String,
    onItemTap: () -> Unit = {},
    cardWidth: Dp = 180.dp
) {
    val cartQuantity = CartManager.items
        .filter { it.dishName == menuItem.name }
        .sumOf { it.quantity }

    val hasModifiers = menuItem.modifier_groups.isNotEmpty()

    // Log image URL type for debugging (data: URI vs https://)
    val imgUrl = menuItem.image_url
    if (imgUrl.isNotEmpty()) {
        val type = when {
            imgUrl.startsWith("data:", ignoreCase = true) -> "base64-data-uri (${imgUrl.length} chars)"
            imgUrl.startsWith("http", ignoreCase = true) -> "url"
            else -> "unknown"
        }
        Log.d("StoreFoodCard", "🖼️ [${menuItem.name}] image type=$type, preview=${imgUrl.take(80)}")
    } else {
        Log.w("StoreFoodCard", "⚠️ [${menuItem.name}] NO image URL — hiding image box")
    }

    Column(
        modifier = Modifier.width(cardWidth),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Image with add/stepper button — only show if image_url is not empty
        if (menuItem.image_url.isNotEmpty()) {
        Box(
            modifier = Modifier
                .size(172.dp, 170.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable { onItemTap() }
        ) {
            SubcomposeAsyncImage(
                model = menuItem.image_url,
                contentDescription = menuItem.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp)),
                loading = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFFF0F0F0))
                    )
                },
                error = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFFE8E8E8))
                    )
                }
            )

            if (hasModifiers) {
                // Show "Add" button that opens detail sheet
                if (cartQuantity == 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .shadow(2.dp, CircleShape)
                            .background(Color.White, CircleShape)
                            .clickable {
                                CartManager.addItem(
                                    CartItem(
                                        dishName = menuItem.name,
                                        restaurantName = restaurantName,
                                        price = menuItem.price,
                                        imageURL = menuItem.image_url
                                    )
                                )
                            }
                            .padding(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Customize",
                            modifier = Modifier.size(16.dp),
                            tint = Color.Black
                        )
                    }
                } else {
                    // Stepper for items with modifiers (still opens detail on image tap)
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp)
                            .shadow(3.dp, RoundedCornerShape(50))
                            .background(Color.White, RoundedCornerShape(50))
                            .padding(horizontal = 6.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .background(Color(0xFFCC5500), CircleShape)
                                .clickable { CartManager.decrementItem(menuItem.name) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("−", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Text(
                            text = "$cartQuantity",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            modifier = Modifier.width(22.dp)
                        )
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .background(Color(0xFFCC5500), CircleShape)
                                .clickable { CartManager.incrementItem(menuItem.name) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("+", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            } else {
                // No modifiers: direct add/stepper
                if (cartQuantity == 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .shadow(2.dp, CircleShape)
                            .background(Color.White, CircleShape)
                            .clickable {
                                CartManager.addItem(
                                    CartItem(
                                        dishName = menuItem.name,
                                        restaurantName = restaurantName,
                                        price = menuItem.price,
                                        imageURL = menuItem.image_url
                                    )
                                )
                            }
                            .padding(8.dp)
                    ) {
                        Text(text = "+", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp)
                            .shadow(3.dp, RoundedCornerShape(50))
                            .background(Color.White, RoundedCornerShape(50))
                            .padding(horizontal = 6.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .background(Color(0xFFCC5500), CircleShape)
                                .clickable { CartManager.decrementItem(menuItem.name) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("−", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Text(
                            text = "$cartQuantity",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            modifier = Modifier.width(22.dp)
                        )
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .background(Color(0xFFCC5500), CircleShape)
                                .clickable { CartManager.incrementItem(menuItem.name) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("+", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
        }

        // Name
        Text(
            text = menuItem.name,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.clickable { onItemTap() }
        )

        // Description (if present, show 1 line)
        if (menuItem.description.isNotEmpty()) {
            Text(
                text = menuItem.description,
                fontSize = 12.sp,
                color = Color.Gray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clickable { onItemTap() }
            )
        }

        // Price
        Text(
            text = "$${String.format("%.2f", menuItem.price)}",
            fontSize = 14.sp,
            color = Color.Gray
        )
    }
}