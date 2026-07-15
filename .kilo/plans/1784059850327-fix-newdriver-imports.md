# Fix NewHomeBatchScreen — wire onNavigateToStore to StoreScreen

## Problem

`Account.kt:177-180` has a TODO placeholder for `onNavigateToStore`:
```kotlin
onNavigateToStore = { brandId ->
    Log.d("NewHomeBatch", "Navigate to brand store: $brandId")
    // TODO: navigate to brand store screen when available
}
```

Clicking a restaurant card logs the `brandId` but does nothing visible.

## Fix — 4 edits in `Account.kt`

### 1. Add Store import (after line 63)
```
import com.example.birdy.ui.store.StoreScreen
```

### 2. Add Store to AccountPage enum (line 78)
Change:
```kotlin
enum class AccountPage {
    Main, Help, Wallet, Pass, ManageAccount, SignIn, SignOut, DeleteAccount, Profile,
    Settings, Referral, ReferralCode, Notifications, Language, BugReporter,
    TestPages, ChatView, NewHomeBatch, NewDriver, NewDriverDetail, VerifyOtp
}
```
To:
```kotlin
enum class AccountPage {
    Main, Help, Wallet, Pass, ManageAccount, SignIn, SignOut, DeleteAccount, Profile,
    Settings, Referral, ReferralCode, Notifications, Language, BugReporter,
    TestPages, ChatView, NewHomeBatch, NewDriver, NewDriverDetail, VerifyOtp, Store
}
```

### 3. Add state variable for restaurantId (after line 93)
```
var storeRestaurantId by remember { mutableStateOf("") }
```

### 4. Replace the TODO placeholder (lines 177-180)
Change:
```kotlin
onNavigateToStore = { brandId ->
    Log.d("NewHomeBatch", "Navigate to brand store: $brandId")
    // TODO: navigate to brand store screen when available
}
```
To:
```kotlin
onNavigateToStore = { brandId ->
    storeRestaurantId = brandId
    currentPage = AccountPage.Store
}
```

### 5. Add Store route case (after the NewHomeBatch case, before NewDriver)
```kotlin
AccountPage.Store -> StoreScreen(
    restaurantId = storeRestaurantId,
    onBack = { currentPage = AccountPage.NewHomeBatch }
)
```

## Files changed

Only `app/src/main/java/com/example/birdy/ui/account/Account.kt`.

## Verification

`./gradlew :app:compileDebugKotlin` — zero errors.
On-device: clicking a restaurant card in NewHomeBatchScreen navigates to StoreScreen with the correct brandId.
