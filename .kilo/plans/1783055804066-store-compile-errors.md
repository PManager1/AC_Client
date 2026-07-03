# Fix Store Compile Errors & Show Unlisted Message Banner

## Root Cause of Compile Errors
A **missing closing brace** in `StoreApi.kt` at line 458 causes the compiler to treat all subsequent top-level functions as nested inside `fetchStoreDetail`. This makes all later functions (`parseStoreJson`, `generateFallbackStoreData`, `fetchStoreLocation`, `fetchMenuOnline`, `parseStringArray`, `parseOperatingHours`, `parseModifierGroups`) **unresolved**, and makes `private` modifiers on those "local functions" invalid.

Additionally, the `/nearest-store` handler parses `brandObj` but never parses `storeObj` or `menuObj` — those fields are referenced undeclared in the `StoreData` constructor.

## Root Cause of Unlisted Message Not Showing
The **compile errors prevent the app from compiling at all** — the missing brace at line 458 makes the entire `StoreApi.kt` file unparsable, so no store page can load.

After compiling, the unlisted message WILL show for Yum's II because the API returns:
- `checkoutIssues: true`, `orderOnlineNotAvailable: true`, `unlistedMessage: "We'd love to deliver..."`
- Menu is **empty** (`categories: []`), so `data.menu.isEmpty()` → true

However, the current display logic at `StoreScreen.kt:789` is fragile — it only shows the message when `data.menu.isEmpty()`. If a brand later adds menu items but keeps the flags, the message would disappear. The message should show as a warning banner whenever the flags are set, regardless of menu state.

Additionally, `fetchBrandQuick` (StoreApi.kt:332-356) doesn't parse `checkoutIssues`, `orderOnlineNotAvailable`, or `unlistedMessage` from the brand API response, so the quick-load phase always defaults them to `false`/`null`.

## Files to Fix

### 1. `app/src/main/java/com/example/birdy/ui/store/StoreApi.kt`

#### Fix A — Add missing closing brace
At line 458, one `}` closes `if (brandObj != null)`. A second `}` is needed to close `if (conn.responseCode == 200)` before `conn.disconnect()` at line 459.

#### Fix B — Parse `storeObj` fields in nearest-store handler
Between `val menuObj = ...` and `if (brandObj != null)`, add variable extraction from `storeObj`. API response confirmed `nearestStore` structure:
- `id` (string), `address`, `city`, `state`, `zipCode`, `phone` (may be empty string)
- `distance`: numeric (e.g. 203.2), `deliveryTimeEst`: string
- `deliveryEnabled`, `pickupEnabled`: booleans
- `deliveryRadius`: numeric
- `latitude`, `longitude`: doubles
- **No `deliveryFee` field** — default to 0.0
- Match the existing pattern in `fetchStoreLocation` (lines 1019-1044) which already parses these correctly

#### Fix C — Parse `menuObj` for menu categories in nearest-store handler
Parse `menuObj` structure `{ categories: [{ name, items: [...] }] }` into a `menuCategories` list. API response shows menu with `categories: []` and `modifierGroups: []`.

#### Fix D — Update nearest-store StoreData constructor
Replace undeclared variables (`distance`, `deliveryFee`, etc.) in `StoreLocationInfo` and `StoreData` with the newly parsed variables. Fix trailing comma placement.

#### Fix E — Parse checkout flags in fetchBrandQuick
In `fetchBrandQuick` (line 332-356), add parsing of `checkoutIssues`, `orderOnlineNotAvailable`, `unlistedMessage` from `brandJson` and pass them to `StoreBrandInfo`.

No snake_case fallback needed — API confirmed camelCase field names.

### 2. `app/src/main/java/com/example/birdy/ui/store/StoreScreen.kt`

#### Fix G — Show unlisted message as a warning banner, not just in empty menu state
The current code at line 789 only shows the message inside `if (data.menu.isEmpty())`. For Yum's II this condition IS true (menu is empty), so after compile fixes the message will show. But the logic is fragile — if a brand has menu items AND checkout flags, the message would be hidden.

Fix: Move the unlisted message display OUTSIDE the menu-empty guard. Show it as a warning banner whenever the flags are set, positioned between the delivery info box (line 738) and the menu categories (line 749).

New logic:
```
// Unlisted message warning banner (shown whenever flags are set, regardless of menu)
val hasCheckoutFlags = data.brand_info.checkoutIssues || data.brand_info.orderOnlineNotAvailable
if (hasCheckoutFlags && !data.brand_info.unlistedMessage.isNullOrEmpty()) {
    // Render warning banner card with unlistedMessage text
    // Style: red/orange background card with rounded corners, between delivery info and menu
}

// Menu categories
if (data.menu.isNotEmpty()) {
    data.menu.forEachIndexed { index, category -> ... }
} else if (!hasCheckoutFlags) {
    // Only show "Menu coming soon" when menu is empty AND no checkout flags are set
    Text("Menu coming soon", ...)
}
```

## Validation
- Run `./gradlew :app:compileDebugKotlin` to verify all errors are resolved.
- Test with brand ID `6a46f4758403a1ea736f849a` (Yum's II):
  - `checkoutIssues: true`, `orderOnlineNotAvailable: true`, `unlistedMessage: "We'd love to deliver..."`
  - Menu is empty → after compile fixes, the current code would show the message
  - Verify warning banner appears after the delivery info box
- Test with a brand that has menu items AND checkout flags → verify warning banner still appears above the menu
- Test with a brand that has empty menu AND no flags → verify "Menu coming soon" shows

## Risk Assessment
- Low risk: changes fill in missing parsing logic that was clearly intended but incomplete
- API confirmed camelCase field names: `checkoutIssues`, `orderOnlineNotAvailable`, `unlistedMessage` — no snake_case needed
- `nearestStore` has no `deliveryFee` field — parsing defaults to 0.0 (matches `fetchStoreLocation` behavior)
- `distance` is numeric in the API — `fetchStoreLocation` already handles this (parses as double then formats)
- The banner add is purely cosmetic (new UI element in existing layout)
