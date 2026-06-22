# Compile Error Fixes

## 1. StoreApi.kt — `locationRating`, `locationReviewCount`, `locationId` scoping

**Already fixed.** Vars declared at lines 432–434 before `if (storeObj != null)`, assigned (no `val`) inside at lines 460–462.

## 2. StoreScreen.kt — Duplicate `Config` import

**Already fixed.** Line 83 removed — only one import remains on line 82.

## 3. StoreComponents.kt — `HeaderCircleButton` parameter order

**Already fixed.** `HeaderCircleButton(icon, tint, onClick)` — `onClick` is now last so trailing lambdas bind to it correctly.

## 4. StoreInfo.kt — `optString("id", null)` type mismatch

**Problem** (line 99):
```kotlin
userReviewId = review.optString("id", null)
```
`optString(key: String, defaultValue: String)` expects a `String` default, not `null`. Kotlin infers `null` as `Nothing?`, causing "Java type mismatch: inferred type is 'kotlin.Nothing?', but 'kotlin.String' was expected."

**Fix**: Use empty string default and convert to null:
```kotlin
userReviewId = review.optString("id", "").takeIf { it.isNotEmpty() }
```

## 5. StoreInfo.kt — Deprecated `Geocoder.getFromLocationName`

**Problem** (line 176): The 6-parameter `getFromLocationName(String, Int, Double, Double, Double, Double)` is deprecated in API 33.

**Fix**: Suppress the deprecation at the call site since the async `GeocodeListener` replacement would require non-trivial restructuring:
```kotlin
@Suppress("DEPRECATION")
val results = geocoder.getFromLocationName(data.location_info.address, 1, -90.0, -180.0, 90.0, 180.0)
```

## 6. StoreScreen.kt — `GlobalScope` delicate API warning

**Problem** (line 180): `GlobalScope.launch(Dispatchers.IO)` triggers "This is a delicate API" warning. If warnings-as-errors is enabled, this blocks compilation.

**Fix**: Replace `GlobalScope` with `rememberCoroutineScope()`:
- Add `import androidx.compose.runtime.rememberCoroutineScope`
- Remove `import kotlinx.coroutines.GlobalScope`
- Add `val scope = rememberCoroutineScope()` near the top of the `StoreScreen` composable (after the `var` state declarations)
- Change `GlobalScope.launch(Dispatchers.IO)` → `scope.launch(Dispatchers.IO)`

## 7. HomeFD.kt — `GlobalScope` delicate API warning

**Problem** (line 96): Same `GlobalScope.launch(Dispatchers.IO)` as StoreScreen.kt.

**Fix**: Same approach:
- Add `import androidx.compose.runtime.rememberCoroutineScope`
- Remove `import kotlinx.coroutines.GlobalScope`
- Add `val scope = rememberCoroutineScope()` near the top of the `HomeFDScreen` composable
- Change `GlobalScope.launch(Dispatchers.IO)` → `scope.launch(Dispatchers.IO)`
