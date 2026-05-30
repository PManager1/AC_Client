package com.example.birdy.ui.fooddelivery

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.birdy.data.DeliveryRestaurant
import com.example.birdy.data.FeaturedBanner
import com.example.birdy.data.FeedRestaurant
import com.example.birdy.data.FoodCategory
import kotlinx.coroutines.launch
import java.util.Locale

// ============================================================================
// MARK: - Header View
// Matches iOS: HeaderView (address pin + bell + cart)
// ============================================================================

@Suppress("UNUSED")
@Composable
fun HomeFDHeader(
    selectedAddress: String?,
    isLoadingAddress: Boolean,
    onAddressClick: () -> Unit,
    modifier: Modifier = Modifier,
    onCartClick: () -> Unit = {}
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 6.dp)
    ) {
        // Address section
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .weight(1f)
                .clickable { onAddressClick() }
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = "Location",
                tint = Color.Black,
                modifier = Modifier.size(20.dp)
            )

            if (isLoadingAddress) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = selectedAddress ?: "Select Address",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (selectedAddress != null) Color.Black else Color.Gray,
                    maxLines = 1
                )
            }

            Text(
                text = "▾",
                fontSize = 12.sp,
                color = Color.Gray
            )
        }

        // Notification bell with red dot
        Box(contentAlignment = Alignment.TopEnd) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = "Notifications",
                modifier = Modifier.size(24.dp)
            )
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(Color.Red, CircleShape)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Cart icon
        Icon(
            imageVector = Icons.Default.ShoppingCart,
            contentDescription = "Cart",
            modifier = Modifier
                .size(24.dp)
                .clickable { onCartClick() }
        )
    }
}

// ============================================================================
// MARK: - Search Bar
// Matches iOS: SearchBar (disabled, tappable overlay)
// ============================================================================

@Suppress("UNUSED")
@Composable
fun HomeFDSearchBar(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFFF3F3F3), RoundedCornerShape(25.dp))
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = "Search",
            tint = Color.Gray
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Search \"Dessert\"",
            color = Color.Gray,
            fontSize = 16.sp,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = "Map",
            tint = Color.Black
        )
    }
}

// ============================================================================
// MARK: - Category Item
// Matches iOS: CategoryItem (emoji + name)
// ============================================================================

@Composable
fun HomeFDCategoryItem(
    category: FoodCategory,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.clickable { onClick() }
    ) {
        Text(
            text = category.emoji,
            fontSize = 32.sp,
            modifier = Modifier.padding(6.dp)
        )
        Text(
            text = category.name,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.DarkGray
        )
    }
}

// ============================================================================
// MARK: - Category Horizontal List
// ============================================================================

@Suppress("UNUSED")
@Composable
fun HomeFDCategoryList(
    categories: List<FoodCategory>,
    modifier: Modifier = Modifier,
    onCategoryClick: (FoodCategory) -> Unit = {}
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.padding(horizontal = 16.dp)
    ) {
        itemsIndexed(categories) { _, category ->
            HomeFDCategoryItem(
                category = category,
                onClick = { onCategoryClick(category) }
            )
        }
    }
}

// ============================================================================
// MARK: - Shimmer Skeleton Views (white shimmer, matches IC HomeFD pattern)
// ============================================================================

/// Shimmer effect overlay — white gradient sweep
@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = { it }),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )

    Box(
        modifier = modifier.background(
            Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.0f),
                    Color.White.copy(alpha = 0.4f),
                    Color.White.copy(alpha = 0.0f)
                ),
                start = Offset(translateAnim - 300f, 0f),
                end = Offset(translateAnim, 0f)
            )
        )
    )
}

/// Skeleton block — white rounded rectangle with shimmer
@Composable
fun SkeletonBlock(
    width: androidx.compose.ui.unit.Dp,
    height: androidx.compose.ui.unit.Dp,
    cornerRadius: androidx.compose.ui.unit.Dp = 12.dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(width)
            .height(height)
            .background(Color(0xFFF5F5F5), RoundedCornerShape(cornerRadius))
    ) {
        ShimmerBox(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(cornerRadius))
        )
    }
}

/// Skeleton promo banner placeholder
@Composable
fun SkeletonPromoBanner(
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFFF5F5F5), RoundedCornerShape(15.dp))
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SkeletonBlock(width = 160.dp, height = 18.dp, cornerRadius = 6.dp)
            SkeletonBlock(width = 120.dp, height = 14.dp, cornerRadius = 6.dp)
            SkeletonBlock(width = 100.dp, height = 32.dp, cornerRadius = 16.dp)
        }
        Spacer(modifier = Modifier.width(12.dp))
        SkeletonBlock(width = 100.dp, height = 100.dp, cornerRadius = 12.dp)
    }
}

/// Skeleton feed section with horizontal restaurant card placeholders
@Composable
fun SkeletonFeedSection(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Section heading skeleton
        SkeletonBlock(
            width = 140.dp, height = 22.dp, cornerRadius = 6.dp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        // Horizontal scrolling restaurant card skeletons
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(15.dp),
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            items(3) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Image skeleton
                    SkeletonBlock(width = 280.dp, height = 160.dp, cornerRadius = 15.dp)
                    // Restaurant name skeleton
                    SkeletonBlock(width = 150.dp, height = 18.dp, cornerRadius = 6.dp)
                    // Rating/details skeleton
                    SkeletonBlock(width = 200.dp, height = 14.dp, cornerRadius = 6.dp)
                    // Delivery fee skeleton
                    SkeletonBlock(width = 80.dp, height = 24.dp, cornerRadius = 5.dp)
                }
            }
        }
    }
}

// ============================================================================
// MARK: - Promo Banner View
// Matches iOS: PromoBannerView (blue gradient card)
// ============================================================================

@Suppress("UNUSED")
@Composable
fun PromoBannerView(
    modifier: Modifier = Modifier,
    onOrderNow: () -> Unit = {}
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.horizontalGradient(
                    colors = listOf(Color(0xFF1565C0), Color(0xFF1976D2), Color(0xFF1E88E5))
                ),
                RoundedCornerShape(15.dp)
            )
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Buy 2, save $2 on select\nPringles and Cheez-It items",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Now - 4/6. Terms apply.",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 13.sp
                )
                Box(
                    modifier = Modifier
                        .background(Color.White, RoundedCornerShape(50.dp))
                        .clickable { onOrderNow() }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Order now",
                        color = Color.Red,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            // Placeholder for promo image
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            )
        }
    }
}

// ============================================================================
// MARK: - Restaurant Card
// Matches iOS: RestaurantCard
// ============================================================================

@Composable
fun RestaurantCard(
    restaurant: DeliveryRestaurant,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Column(
        modifier = modifier
            .width(280.dp)
            .clickable { onClick() }
    ) {
        // Image placeholder with heart icon
        Box(
            modifier = Modifier
                .width(280.dp)
                .height(160.dp)
                .background(Color(restaurant.imagePlaceholder).copy(alpha = 0.3f), RoundedCornerShape(15.dp))
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
                    .background(Color.White, CircleShape)
                    .padding(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FavoriteBorder,
                    contentDescription = "Favorite",
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Restaurant name
        Text(
            text = restaurant.name,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )

        Spacer(modifier = Modifier.height(2.dp))

        // Rating + details
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = String.format(Locale.US, "%.1f", restaurant.rating),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Gray
            )
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = Color.Yellow,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = "(${restaurant.reviews}) • ${restaurant.distance} • ${restaurant.deliveryTime}",
                fontSize = 12.sp,
                color = Color.Gray
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Delivery fee badge
        Text(
            text = "${restaurant.deliveryFee} delivery fee",
            fontSize = 12.sp,
            color = Color.Gray,
            modifier = Modifier
                .background(Color(0xFFF3F3F3), RoundedCornerShape(5.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

// ============================================================================
// MARK: - Restaurant Section (reusable for all 4 sections)
// Matches iOS: Section 1-4 pattern (title + arrow + horizontal scroll)
// ============================================================================

@Suppress("UNUSED")
@Composable
fun RestaurantSection(
    title: String,
    restaurants: List<DeliveryRestaurant>,
    modifier: Modifier = Modifier,
    onRestaurantClick: (DeliveryRestaurant) -> Unit = {}
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var currentCard by remember { mutableIntStateOf(0) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Section header with title + arrow button
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Next",
                tint = Color.Gray,
                modifier = Modifier
                    .size(28.dp)
                    .clickable {
                        currentCard = (currentCard + 1) % restaurants.size
                        coroutineScope.launch {
                            listState.animateScrollToItem(currentCard)
                        }
                    }
            )
        }

        // Horizontal restaurant cards
        LazyRow(
            state = listState,
            horizontalArrangement = Arrangement.spacedBy(15.dp),
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            itemsIndexed(restaurants) { _, restaurant ->
                RestaurantCard(
                    restaurant = restaurant,
                    onClick = { onRestaurantClick(restaurant) }
                )
            }
        }
    }
}

// ============================================================================
// MARK: - Dynamic Promo Banner View (JSON-driven)
// Matches iOS: PromoBannerView(banner:) — takes FeaturedBanner from JSON
// ============================================================================

@Composable
fun DynamicPromoBannerView(
    banner: FeaturedBanner,
    modifier: Modifier = Modifier,
    onAction: () -> Unit = {}
) {
    // Parse gradient colors from hex strings (e.g. "#4facfe" → Color)
    val gradientColors = if (banner.gradientColors.isNotEmpty()) {
        banner.gradientColors.mapNotNull { hex ->
            try {
                Color(android.graphics.Color.parseColor(hex))
            } catch (_: Exception) { null }
        }
    } else {
        listOf(Color(0xFF1565C0), Color(0xFF1E88E5))
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.horizontalGradient(colors = gradientColors),
                RoundedCornerShape(15.dp)
            )
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = banner.title,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = banner.subtitle,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 13.sp
                )
                Box(
                    modifier = Modifier
                        .background(Color.White, RoundedCornerShape(50.dp))
                        .clickable { onAction() }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = banner.actionText,
                        color = Color.Red,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            // Image or placeholder
            if (banner.imageUrl.isNotEmpty() && banner.imageUrl.startsWith("http")) {
                AsyncImage(
                    model = banner.imageUrl,
                    contentDescription = banner.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(100.dp)
                        .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                )
            }
        }
    }
}

// ============================================================================
// MARK: - Feed Restaurant Card (JSON-driven)
// Matches iOS: RestaurantCard(restaurant:) — takes FeedRestaurant from JSON
// ============================================================================

@Composable
fun FeedRestaurantCard(
    restaurant: FeedRestaurant,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val validImages = restaurant.images.filter { it.isNotEmpty() }

    Column(
        modifier = modifier
            .width(280.dp)
            .clickable { onClick() }
    ) {
        // Image carousel (shows first image + heart button)
        Box(
            modifier = Modifier
                .width(280.dp)
                .height(160.dp)
                .background(Color(0xFFF5F5F5), RoundedCornerShape(15.dp))
        ) {
            if (validImages.isEmpty()) {
                // No images — show placeholder
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = "No image",
                        tint = Color.Gray.copy(alpha = 0.5f),
                        modifier = Modifier.size(32.dp)
                    )
                }
            } else {
                // Show first image (URL or asset name)
                val imageSource = validImages.first()
                if (imageSource.startsWith("http")) {
                    AsyncImage(
                        model = imageSource,
                        contentDescription = restaurant.restaurantName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Asset placeholder — show colored box with name
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFFE8E8E8))
                    ) {
                        Text(
                            text = restaurant.restaurantName.take(2),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray
                        )
                    }
                }

                // Page indicator (if multiple images)
                if (validImages.size > 1) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 12.dp, bottom = 12.dp)
                            .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(50))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "1/${validImages.size}",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Heart button (top right)
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
                    .background(Color.White, CircleShape)
                    .padding(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FavoriteBorder,
                    contentDescription = "Favorite",
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Restaurant name
        Text(
            text = restaurant.restaurantName,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )

        Spacer(modifier = Modifier.height(2.dp))

        // Rating + details
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = String.format(Locale.US, "%.1f", restaurant.rating),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Gray
            )
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = Color.Yellow,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = "(${restaurant.reviewsDisplay}) • ${restaurant.distanceDisplay} • ${restaurant.deliveryTimeDisplay}",
                fontSize = 12.sp,
                color = Color.Gray
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Delivery fee badge
        Text(
            text = "${restaurant.deliveryFeeDisplay} delivery fee",
            fontSize = 12.sp,
            color = Color.Gray,
            modifier = Modifier
                .background(Color(0xFFF3F3F3), RoundedCornerShape(5.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

// ============================================================================
// MARK: - Feed Restaurant Section (JSON-driven)
// Matches iOS: FeedSectionView — dynamic section with heading + scrollable cards
// ============================================================================

@Composable
fun FeedRestaurantSection(
    title: String,
    restaurants: List<FeedRestaurant>,
    modifier: Modifier = Modifier,
    onRestaurantClick: (FeedRestaurant) -> Unit = {}
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var currentCard by remember { mutableIntStateOf(0) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Section header with title + arrow button
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.weight(1f)
            )
            if (restaurants.size > 1) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Next",
                    tint = Color.Gray,
                    modifier = Modifier
                        .size(28.dp)
                        .clickable {
                            currentCard = (currentCard + 1) % restaurants.size
                            coroutineScope.launch {
                                listState.animateScrollToItem(currentCard)
                            }
                        }
                )
            }
        }

        // Horizontal restaurant cards
        LazyRow(
            state = listState,
            horizontalArrangement = Arrangement.spacedBy(15.dp),
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            itemsIndexed(restaurants) { _, restaurant ->
                FeedRestaurantCard(
                    restaurant = restaurant,
                    onClick = { onRestaurantClick(restaurant) }
                )
            }
        }
    }
}
