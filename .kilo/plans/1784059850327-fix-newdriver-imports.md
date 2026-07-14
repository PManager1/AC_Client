# Fix NewDriverScreen.kt — compilation errors + data loading

## Task 1 — Fix remaining compilation errors

### Problem
`items()` is an extension function on `LazyListScope`, but `NewDriverScreen.kt` lacks `import androidx.compose.foundation.lazy.items`. Every other file in the project that calls `items()` inside a `LazyColumn` imports it explicitly (e.g. `NewHomeBatchScreen.kt:12`).

Without this import, the compiler falls back to the `items(count: Int, itemContent)` overload, which expects `Int` as the first argument. This cascades into the 8 remaining errors (List vs Int, Int vs RestaurantEntry, unresolved `id`, composable invocation).

### Fix
Add **one** missing import to the imports block (after `itemsIndexed`):

```kotlin
import androidx.compose.foundation.lazy.items
```

The two earlier imports (`kotlin.math.max`, `androidx.compose.material.icons.filled.Warning`) are already in place.

---

## Task 2 — Data must come from mock JSON, not hardcoded fallback

### Problem
`loadRestaurants()` at `NewDriverScreen.kt:259` has a fragile implementation:
1. Uses `context.resources.getIdentifier()` instead of a direct `R.raw` reference, making resource lookup fragile.
2. Two nested try-catch blocks silently swallow failures and fall back to `fallbackRestaurants()` — 19 hardcoded entries on lines 299–318. The user wants the JSON file to be the sole source.

### Changes

1. **Add import**: `import com.example.birdy.R`

2. **Replace resource lookup**:  
   `context.resources.getIdentifier("driver_restaurants_mock", "raw", context.packageName)`  
   → `R.raw.driver_restaurants_mock`

3. **Flatten `loadRestaurants`**: remove the inner try-catch around resource loading and the null-check that falls back to `fallbackRestaurants()`. If resource loading or JSON parsing fails, log the error and deliver an empty list.

4. **Remove `fallbackRestaurants()`** entirely (lines 299–318).

### Expected `loadRestaurants` after changes

```kotlin
private suspend fun loadRestaurants(context: android.content.Context, onResult: (List<RestaurantEntry>) -> Unit) {
    try {
        val json = withContext(Dispatchers.IO) {
            context.resources.openRawResource(R.raw.driver_restaurants_mock)
                .bufferedReader().use { it.readText() }
        }
        val obj = JSONObject(json)
        val arr = obj.getJSONArray("restaurants")
        val result = (0 until arr.length()).map { i ->
            val item = arr.getJSONObject(i)
            RestaurantEntry(
                time = item.getString("time"),
                name = item.getString("name"),
                subtitle = item.getString("subtitle"),
                offer = item.getString("offer"),
                timeNdistance = item.optString("timeNdistance", "20 min (2.2 mi) total")
            )
        }
        withContext(Dispatchers.Main) {
            onResult(result.mapIndexed { index, entry -> entry.copy(orderIndex = index) })
        }
    } catch (e: Exception) {
        Log.e("NewDriver", "Failed to load restaurants from JSON", e)
        withContext(Dispatchers.Main) {
            onResult(emptyList())
        }
    }
}
```

## Files changed
Only `app/src/main/java/com/example/birdy/ui/explore/NewDriverScreen.kt`.

## Verification
Run `./gradlew :app:compileDebugKotlin` — should compile cleanly with zero errors.
