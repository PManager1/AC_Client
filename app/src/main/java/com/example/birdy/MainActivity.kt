package com.example.birdy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
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
import com.example.birdy.data.CartManager
import com.example.birdy.data.Config
import com.stripe.android.PaymentConfiguration
import com.example.birdy.data.ForceUpdateChecker
import com.example.birdy.ui.account.AccountScreen
import com.example.birdy.ui.components.BirdyBottomNavBar
import com.example.birdy.ui.components.ForceUpdateDialog
import com.example.birdy.data.ExploreCategory
import com.example.birdy.ui.explore.ExploreScreen
import com.example.birdy.ui.explore.NewFoodPlacesScreen
import com.example.birdy.ui.explore.SearchFoodScreen
import com.example.birdy.ui.store.StoreScreen
import com.example.birdy.ui.explore.CartScreen
import com.example.birdy.ui.explore.CheckoutScreen
import com.example.birdy.ui.explore.DriverTrackingScreen
import com.example.birdy.ui.fooddelivery.HomeFDScreen
import com.example.birdy.ui.fooddelivery.OrderDetailScreen
import com.example.birdy.ui.home.FastFoodHomeScreen
import com.example.birdy.ui.home.PizzaHomeScreen
import com.example.birdy.ui.inbox.InboxScreen
import com.example.birdy.ui.inbox.RequestDetailScreen
import com.example.birdy.ui.theme.BirdyTheme
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.launch

// Tab indices — matches iOS NavigationFlow.swift selectedTab
private const val TAB_HOME = 0
private const val TAB_EXPLORE = 1
private const val TAB_INBOX = 2
private const val TAB_ACCOUNT = 3

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AuthManager.init(applicationContext)
        // Initialize Stripe SDK — matches iOS AppDelegate stripeInit()
        PaymentConfiguration.init(applicationContext, Config.STRIPE_PUBLISHABLE_KEY)
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
    var selectedRestaurantId by remember { mutableStateOf("") }
    var showCart by remember { mutableStateOf(false) }
    var showCheckout by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf<ExploreCategory?>(null) }
    var showPizzaHome by remember { mutableStateOf(false) }
    var showFastFoodHome by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Force update check
    var showForceUpdate by remember { mutableStateOf(false) }
    var forceUpdateMinVersion by remember { mutableStateOf("0.0.0") }
    LaunchedEffect(Unit) {
        val result = ForceUpdateChecker.check(context)
        if (result.needsUpdate) {
            forceUpdateMinVersion = result.minVersion
            showForceUpdate = true
        }
    }
    if (showForceUpdate) {
        ForceUpdateDialog(minVersion = forceUpdateMinVersion)
        return
    }

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
                    showPizzaHome = false
                    showFastFoodHome = false
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
        ) {
            when (selectedTab) {
                TAB_HOME -> {
                    when {
                        showFastFoodHome -> {
                            FastFoodHomeScreen(
                                onBack = { showFastFoodHome = false },
                                onRestaurantClick = { restaurantId ->
                                    selectedRestaurantId = restaurantId
                                    showStore = true
                                }
                            )
                        }
                        showPizzaHome -> {
                            PizzaHomeScreen(
                                onBack = { showPizzaHome = false },
                                onRestaurantClick = { restaurantId ->
                                    selectedRestaurantId = restaurantId
                                    showStore = true
                                }
                            )
                        }
                        showCart -> {
                            CartScreen(
                                onBack = { showCart = false },
                                onCheckout = {
                                    showCart = false
                                    showCheckout = true
                                }
                            )
                        }
                        showStore -> {
                            StoreScreen(
                                onBack = { showStore = false },
                                onViewCart = { showCart = true },
                                restaurantId = selectedRestaurantId,
                                jsonInputStream = if (selectedRestaurantId.isEmpty()) context.assets.open("storejson.json") else null
                            )
                        }
                        else -> {
                            HomeFDScreen(
                                onNavigateToSearch = {
                                    // TODO: Navigate to Search screen
                                },
                                onNavigateToCart = {
                                    showCart = true
                                },
                                onRestaurantClick = { restaurantId ->
                                    selectedRestaurantId = restaurantId
                                    showStore = true
                                },
                                onCategoryClick = { categoryName ->
                                    if (categoryName == "Pizza") {
                                        showPizzaHome = true
                                    } else if (categoryName == "Fast Food") {
                                        showFastFoodHome = true
                                    }
                                }
                            )
                        }
                    }
                }

                TAB_EXPLORE -> {
                    when {
                        showCheckout -> {
                            CheckoutScreen(
                                onBack = { showCheckout = false },
                                onTrackOrder = {
                                    showCheckout = false
                                    // CartManager.showDriverTracking is already set to true
                                }
                            )
                        }
                        showCart -> {
                            CartScreen(
                                onBack = { showCart = false },
                                onCheckout = {
                                    showCart = false
                                    showCheckout = true
                                }
                            )
                        }
                        showStore -> {
                            StoreScreen(
                                onBack = { showStore = false },
                                onViewCart = { showCart = true },
                                restaurantId = selectedRestaurantId,
                                jsonInputStream = if (selectedRestaurantId.isEmpty()) context.assets.open("storejson.json") else null
                            )
                        }
                        showSearchFood -> {
                            SearchFoodScreen(
                                onBack = { showSearchFood = false },
                                onRestaurantClick = { restaurantId ->
                                    selectedRestaurantId = restaurantId
                                    showStore = true
                                }
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
                                onRestaurantClick = { restaurantId ->
                                    selectedRestaurantId = restaurantId
                                    showStore = true
                                }
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

        // Full-screen driver tracking overlay — matches iOS .fullScreenCover for DriverTracking
        if (CartManager.showDriverTracking) {
            DriverTrackingScreen(
                onBack = {
                    CartManager.showDriverTracking = false
                }
            )
        }

        // Full-screen order detail overlay — matches iOS .fullScreenCover for OrderDetail
        if (CartManager.showOrderDetail) {
            OrderDetailScreen(
                onBack = {
                    CartManager.showOrderDetail = false
                }
            )
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