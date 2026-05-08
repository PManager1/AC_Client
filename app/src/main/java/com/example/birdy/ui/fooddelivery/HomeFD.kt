package com.example.birdy.ui.fooddelivery

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.birdy.data.HomeFDData
import com.example.birdy.data.HomeFeedData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Home FD Screen — replicates iOS HomeFD.swift
 *
 * Loads data from /homefeed API (live backend)
 * Sections (top to bottom):
 * 1. Header (address pin + notification bell + cart)
 * 2. Search bar (disabled, navigates to search on tap)
 * 3. Category horizontal scroll (20 emoji categories)
 * 4. Featured banners (from API)
 * 5. Dynamic sections with restaurant cards (from API)
 */
@Composable
fun HomeFDScreen(
    onNavigateToSearch: () -> Unit = {},
    onNavigateToCart: () -> Unit = {},
    onRestaurantClick: (restaurantId: String) -> Unit = {},
    onCategoryClick: (categoryName: String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Address state (stub — will connect to AddressService later)
    var selectedAddress by remember { mutableStateOf<String?>(null) }
    var selectedAddressId by remember { mutableStateOf<String?>(null) }
    var isLoadingAddress by remember { mutableStateOf(false) }
    var showAddressSheet by remember { mutableStateOf(false) }

    // API-driven data
    var homeFeed by remember { mutableStateOf<HomeFeedData?>(null) }
    var isLoadingFeed by remember { mutableStateOf(true) }

    // Fetch home feed from API on appear
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            homeFeed = HomeFDData.fetchHomeFeed()
        }
        isLoadingFeed = false
        if (homeFeed != null) {
            println("✅ [HomeFDScreen] Loaded home feed: ${homeFeed!!.featuredBanners.size} banners, ${homeFeed!!.sections.size} sections")
        } else {
            println("⚠️ [HomeFDScreen] Home feed is null — API fetch may have failed")
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // MARK: - Header
            HomeFDHeader(
                selectedAddress = selectedAddress,
                isLoadingAddress = isLoadingAddress,
                onAddressClick = {
                    showAddressSheet = true
                },
                onCartClick = { onNavigateToCart() }
            )

            // MARK: - Search Bar
            HomeFDSearchBar(
                onClick = { onNavigateToSearch() },
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // MARK: - Categories
            HomeFDCategoryList(
                categories = HomeFDData.categories,
                onCategoryClick = { category ->
                    onCategoryClick(category.name)
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // MARK: - Featured Banners or Skeleton
            if (isLoadingFeed) {
                SkeletonPromoBanner(modifier = Modifier.padding(horizontal = 16.dp))
            } else {
                homeFeed?.featuredBanners?.forEach { banner ->
                    DynamicPromoBannerView(
                        banner = banner,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // MARK: - Dynamic Sections or Skeletons
            if (isLoadingFeed) {
                SkeletonFeedSection(modifier = Modifier.padding(horizontal = 0.dp))
                SkeletonFeedSection(modifier = Modifier.padding(horizontal = 0.dp))
            } else {
                homeFeed?.sections?.forEach { section ->
                    FeedRestaurantSection(
                        title = section.heading,
                        restaurants = section.restaurants,
                        onRestaurantClick = { onRestaurantClick(it.id) }
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            Spacer(modifier = Modifier.height(60.dp))
        }

        // MARK: - Address Selection Sheet
        if (showAddressSheet) {
            SelectAddressSheet(
                currentAddressId = selectedAddressId,
                onAddressSelected = { address ->
                    selectedAddress = address.street
                    selectedAddressId = address.id
                },
                onDismiss = {
                    showAddressSheet = false
                }
            )
        }
    }
}