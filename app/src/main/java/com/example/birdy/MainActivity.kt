package com.example.birdy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.example.birdy.data.AuthManager
import com.example.birdy.ui.account.AccountScreen
import com.example.birdy.ui.components.BirdyBottomNavBar
import com.example.birdy.data.ExploreCategory
import com.example.birdy.ui.explore.ExploreScreen
import com.example.birdy.ui.explore.NewFoodPlacesScreen
import com.example.birdy.ui.explore.SearchFoodScreen
import com.example.birdy.ui.explore.StoreScreen
import com.example.birdy.ui.explore.CartSheet
import com.example.birdy.ui.fooddelivery.FoodDeliveryScreen
import com.example.birdy.ui.inbox.InboxScreen
import com.example.birdy.ui.inbox.RequestDetailScreen
import com.example.birdy.ui.theme.BirdyTheme
import androidx.compose.ui.platform.LocalContext

// Tab indices — matches iOS NavigationFlow.swift selectedTab
private const val TAB_HOME = 0
private const val TAB_EXPLORE = 1
private const val TAB_INBOX = 2
private const val TAB_ACCOUNT = 3

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AuthManager.init(applicationContext)
        enableEdgeToEdge()
        setContent {
            BirdyTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.White
                ) {
                    BirdyApp()
                }
            }
        }
    }
}

@Composable
fun BirdyApp() {
    var selectedTab by remember { mutableIntStateOf(TAB_HOME) }
    var showRequestDetail by remember { mutableStateOf(false) }
    var showSearchFood by remember { mutableStateOf(false) }
    var showFoodPlaces by remember { mutableStateOf(false) }
    var showStore by remember { mutableStateOf(false) }
    var showCart by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf<ExploreCategory?>(null) }
    val context = LocalContext.current

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.White,
        bottomBar = {
            BirdyBottomNavBar(
                selectedIndex = selectedTab,
                onTabSelected = { index ->
                    selectedTab = index
                    showRequestDetail = false
                    showSearchFood = false
                    showFoodPlaces = false
                    showStore = false
                    selectedCategory = null
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                TAB_HOME -> FoodDeliveryScreen(
                    onNavigateToSearch = {
                        // TODO: Navigate to Search screen
                    },
                    onNavigateToCart = {
                        // TODO: Navigate to Cart screen
                    },
                    onRestaurantClick = { restaurantName ->
                        // TODO: Navigate to restaurant detail
                    },
                    onCategoryClick = { categoryName ->
                        // TODO: Navigate to category results
                    }
                )

                TAB_EXPLORE -> {
                    when {
                        showStore -> {
                            StoreScreen(
                                onBack = { showStore = false },
                                onViewCart = { showCart = true },
                                jsonInputStream = context.assets.open("storejson.json")
                            )
                            if (showCart) {
                                CartSheet(
                                    onDismiss = { showCart = false }
                                )
                            }
                        }
                        showSearchFood -> {
                            SearchFoodScreen(
                                onBack = { showSearchFood = false },
                                onRestaurantClick = { showStore = true }
                            )
                        }
                        showFoodPlaces && selectedCategory != null -> {
                            NewFoodPlacesScreen(
                                category = selectedCategory!!.title,
                                onBack = {
                                    showFoodPlaces = false
                                    selectedCategory = null
                                },
                                onSearchClick = { showSearchFood = true },
                                onRestaurantClick = { showStore = true }
                            )
                        }
                        else -> {
                            ExploreScreen(
                                onNavigateToSearch = { showSearchFood = true },
                                onCategoryClick = { category ->
                                    selectedCategory = category
                                    showFoodPlaces = true
                                }
                            )
                        }
                    }
                }

                TAB_INBOX -> {
                    if (showRequestDetail) {
                        RequestDetailScreen(
                            onBack = { showRequestDetail = false }
                        )
                    } else {
                        InboxScreen(
                            onNavigateToRequestDetail = { showRequestDetail = true }
                        )
                    }
                }

                TAB_ACCOUNT -> AccountScreen()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun BirdyAppPreview() {
    BirdyTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.White
        ) {
            BirdyApp()
        }
    }
}