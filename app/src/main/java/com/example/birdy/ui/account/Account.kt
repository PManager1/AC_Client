package com.example.birdy.ui.account

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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Adb
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.birdy.data.AuthManager
import java.util.Locale

// iOS color constants
private val OrangeSecNavyBlue = Color(0xFF1B2A4A)
private val OrangeTitle = Color(0xFFF27836)
private val OrangeSec2 = Color(0xFF8E8E93)
private val OrangeSec5 = Color(0xFFF5F0EB)
private val OrangeSec6 = Color(0xFFE5E5EA)
private val OrangeSec7 = Color(0xFF1C1C1E)
private val OrangeSec3 = Color(0xFFFF9500)

// Navigation pages for Account sub-screens
enum class AccountPage {
    Main, Help, Wallet, Pass, ManageAccount, SignIn, SignOut, DeleteAccount, Profile,
    Settings, Referral, ReferralCode, Notifications, Language, BugReporter,
    TestPages, ChatView, VerifyOtp
}

// Matches iOS Account.swift
@Composable
fun AccountScreen(
    modifier: Modifier = Modifier
) {
    var currentPage by remember { mutableStateOf(AccountPage.Main) }
    // Bump this after login/logout to force recomposition of profile card
    var refreshKey by remember { mutableStateOf(0) }
    // Store phone number for OTP verification screen
    var otpPhoneNumber by remember { mutableStateOf("") }
    val context = LocalContext.current

    // Route to the correct sub-page
    when (currentPage) {
        AccountPage.Help -> HelpScreen(onBack = { currentPage = AccountPage.Main })
        AccountPage.Wallet -> Wallet(onBack = { currentPage = AccountPage.Main })
        AccountPage.Pass -> PassScreen(onBack = { currentPage = AccountPage.Main })
        AccountPage.ManageAccount -> ManageAccountScreen(
            onBack = { currentPage = AccountPage.Main },
            onSignIn = { currentPage = AccountPage.SignIn },
            onSignOut = { currentPage = AccountPage.SignOut },
            onDeleteAccount = { currentPage = AccountPage.DeleteAccount }
        )
        AccountPage.SignIn -> SignInScreen(
            onBack = { currentPage = AccountPage.ManageAccount },
            onOtpSent = { phone ->
                otpPhoneNumber = phone
                currentPage = AccountPage.VerifyOtp
            },
            onGuestLogin = {
                refreshKey++                        // Force profile refresh
                currentPage = AccountPage.Main      // Matches iOS: dismiss → home tab
            }
        )
        AccountPage.VerifyOtp -> VerifyOtpScreen(
            phoneNumber = otpPhoneNumber,
            onBack = { currentPage = AccountPage.SignIn },
            onVerified = {
                refreshKey++                        // Force profile refresh
                currentPage = AccountPage.Main      // Matches iOS: dismiss → home tab
            }
        )
        AccountPage.SignOut -> SignOutScreen(
            onBack = { currentPage = AccountPage.ManageAccount },
            onConfirmSignOut = {
                AuthManager.clearAll()               // Clear stored token & profile
                refreshKey++                          // Force profile refresh
                currentPage = AccountPage.Main        // Matches iOS: sign out → home
            }
        )
        AccountPage.DeleteAccount -> DeleteAccountScreen(
            onBack = { currentPage = AccountPage.ManageAccount },
            onAccountDeleted = { currentPage = AccountPage.SignIn }
        )
        AccountPage.Profile -> ProfileScreen(
            onBack = {
                refreshKey++
                currentPage = AccountPage.Main
            }
        )
        AccountPage.Settings -> SettingsScreen(
            onBack = { currentPage = AccountPage.Main },
            onNavigateToNotifications = { currentPage = AccountPage.Notifications },
            onNavigateToLanguage = { currentPage = AccountPage.Language },
            onNavigateToBugReporter = { currentPage = AccountPage.BugReporter }
        )
        AccountPage.Referral -> ReferralScreen(
            onBack = { currentPage = AccountPage.Main },
            onNavigateToReferralCode = { currentPage = AccountPage.ReferralCode }
        )
        AccountPage.ReferralCode -> ReferralCodeView(
            onBack = { currentPage = AccountPage.Referral }
        )
        AccountPage.Notifications -> NotificationsScreen(
            onBack = { currentPage = AccountPage.Main }
        )
        AccountPage.Language -> LanguageSettingsScreen(
            onBack = { currentPage = AccountPage.Main }
        )
        AccountPage.BugReporter -> BugReporterScreen(
            onBack = { currentPage = AccountPage.Main }
        )
        AccountPage.TestPages -> TestPagesScreen(
            onBack = { currentPage = AccountPage.Main },
            onNavigateToChatView = { currentPage = AccountPage.ChatView }
        )
        AccountPage.ChatView -> ChatScreen(
            onBack = { currentPage = AccountPage.TestPages }
        )
        AccountPage.Main -> {
            // Read user info from AuthManager — re-read when refreshKey changes (after login/logout)
            val displayName = remember(refreshKey) {
                if (AuthManager.isLoggedIn(context)) {
                    val first = AuthManager.getUserFirstName()
                    val last = AuthManager.getUserLastName()
                    val fullName = "$first $last".trim()
                    if (fullName.isNotBlank()) fullName else "Guest User"
                } else {
                    "Sign In"
                }
            }
            val isLoggedIn = remember(refreshKey) { AuthManager.isLoggedIn(context) }
            val profileImageUrl = remember(refreshKey) { AuthManager.getProfileImageUrl() }
            val userRating = remember(refreshKey) { AuthManager.getUserRating() }

            Column(
                modifier = modifier
                    .fillMaxSize()
                    .background(OrangeSec5)
                    .verticalScroll(rememberScrollState())
            ) {
                // MARK: - Profile Section
                val isProfileLoaded = remember(refreshKey) {
                    AuthManager.isLoggedIn(context) && AuthManager.getUserFirstName().isNotBlank()
                }

                if (isProfileLoaded) {
                    ProfileCard(
                        name = displayName,
                        rating = userRating,
                        isLoggedIn = isLoggedIn,
                        profileImageUrl = profileImageUrl,
                        onClick = {
                            if (isLoggedIn) {
                                currentPage = AccountPage.Profile
                            }
                        }
                    )
                } else {
                    ProfileSkeletonCard()
                }

                Spacer(modifier = Modifier.height(20.dp))

                // MARK: - Action Buttons Row (Help, Wallet, PassView, Settings)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    ActionButton(
                        title = "Help",
                        icon = Icons.AutoMirrored.Filled.HelpOutline,
                        onClick = { currentPage = AccountPage.Help },
                        modifier = Modifier.weight(1f)
                    )
                    ActionButton(
                        title = "PassView",
                        icon = Icons.Default.Shield,
                        onClick = { currentPage = AccountPage.Pass },
                        modifier = Modifier.weight(1f)
                    )
                    ActionButton(
                        title = "Settings",
                        icon = Icons.Default.Settings,
                        onClick = { currentPage = AccountPage.Settings },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // MARK: - More ways to earn
                SectionHeader(title = "More ways to earn")
                SectionCard {
                    ListItemRow(
                        title = "Settings Test pages",
                        icon = Icons.Default.HourglassEmpty,
                        onClick = { currentPage = AccountPage.TestPages }
                    )
                    ListItemRow(
                        title = "Referral",
                        icon = Icons.Default.CardGiftcard,
                        showDivider = false,
                        onClick = { currentPage = AccountPage.Referral }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // MARK: - Account
                SectionHeader(title = "Account")
                SectionCard {
                    ListItemRow(
                        title = "Sign In or Register",
                        icon = Icons.Default.Person,
                        onClick = { currentPage = AccountPage.ManageAccount }
                    )
                    ListItemRow(
                        title = "Wallet",
                        icon = Icons.Default.Wallet,
                        showDivider = false,
                        onClick = { currentPage = AccountPage.Wallet }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // MARK: - Manage
                SectionHeader(title = "Manage")
                SectionCard {
                    ListItemRow(
                        title = "Notifications",
                        icon = Icons.Default.Notifications,
                        onClick = { currentPage = AccountPage.Notifications }
                    )
                    ListItemRow(
                        title = "Language",
                        icon = Icons.Default.Language,
                        showDivider = false,
                        onClick = { currentPage = AccountPage.Language }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // MARK: - Resources
                SectionHeader(title = "Resources")
                SectionCard {
                    ListItemRow(
                        title = "Bug Reporter",
                        icon = Icons.Default.Adb,
                        showDivider = false,
                        onClick = { currentPage = AccountPage.BugReporter }
                    )
                }

                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

// MARK: - Profile Skeleton Card (shimmer loading)

@Composable
fun ProfileSkeletonCard() {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200),
        ),
        label = "shimmerAlpha"
    )

    Surface(
        shape = RoundedCornerShape(15.dp),
        color = Color.White,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp)
        ) {
            // Skeleton avatar
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color.Gray.copy(alpha = shimmerAlpha))
            )

            Spacer(modifier = Modifier.width(15.dp))

            Column {
                // Skeleton name
                Box(
                    modifier = Modifier
                        .size(width = 150.dp, height = 28.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Gray.copy(alpha = shimmerAlpha))
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Skeleton rating
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color.Gray.copy(alpha = shimmerAlpha))
                    )
                    Box(
                        modifier = Modifier
                            .size(width = 60.dp, height = 20.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.Gray.copy(alpha = shimmerAlpha))
                    )
                }
            }
        }
    }
}

// MARK: - Profile Card (matches iOS profileSectionView)

@Composable
fun ProfileCard(
    name: String,
    rating: Float,
    isLoggedIn: Boolean = false,
    profileImageUrl: String = "",
    onClick: () -> Unit = {}
) {
    Surface(
        shape = RoundedCornerShape(15.dp),
        color = Color.White,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable { onClick() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp)
        ) {
            // Avatar circle — loads profile image if URL exists, else shows Person icon
            Box {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(
                            if (isLoggedIn) OrangeTitle.copy(alpha = 0.15f)
                            else Color.Gray.copy(alpha = 0.2f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoggedIn && profileImageUrl.isNotBlank()) {
                        // Load actual profile picture from URL (matches iOS AsyncImage)
                        AsyncImage(
                            model = profileImageUrl,
                            contentDescription = "Profile Photo",
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profile",
                            tint = if (isLoggedIn) OrangeTitle else Color.Gray.copy(alpha = 0.5f),
                            modifier = Modifier
                                .size(60.dp)
                        )
                    }
                }
                // Diamond badge (only show when logged in)
                if (isLoggedIn) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = 4.dp, y = 4.dp)
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Diamond,
                            contentDescription = "Pro",
                            tint = OrangeSecNavyBlue,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(15.dp))

            Column {
                Text(
                    text = name,
                    fontSize = if (isLoggedIn) 34.sp else 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = OrangeSecNavyBlue
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Rating",
                        tint = OrangeSec3,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isLoggedIn) String.format(Locale.US, "%.2f", rating) else "Tap to sign in",
                        fontSize = if (isLoggedIn) 24.sp else 16.sp,
                        fontWeight = FontWeight.Normal,
                        color = if (isLoggedIn) OrangeSecNavyBlue else OrangeSec2
                    )
                }
            }
        }
    }
}

// MARK: - Action Button (matches iOS ActionButton_DP)

@Composable
fun ActionButton(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = OrangeSec5,
        modifier = modifier.clickable { onClick() }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 15.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = OrangeSecNavyBlue,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                color = OrangeSecNavyBlue
            )
        }
    }
}

// MARK: - Section Header (matches iOS PS_SectionHeader)

@Composable
fun SectionHeader(title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = title.uppercase(),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = OrangeSecNavyBlue
        )
    }
}

// MARK: - Section Card (white rounded container)

@Composable
fun SectionCard(
    content: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(15.dp),
        color = Color.White,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column {
            content()
        }
    }
}

// MARK: - List Item Row (matches iOS ListItem)

@Composable
fun ListItemRow(
    title: String,
    icon: ImageVector,
    subtitle: String? = null,
    showDivider: Boolean = true,
    onClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = OrangeTitle,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = OrangeSec7
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        color = OrangeSec2
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Navigate",
                tint = OrangeSec2,
                modifier = Modifier.size(20.dp)
            )
        }
        if (showDivider) {
            HorizontalDivider(
                color = OrangeSec6,
                thickness = 1.dp,
                modifier = Modifier.padding(start = 50.dp)
            )
        }
    }
}
