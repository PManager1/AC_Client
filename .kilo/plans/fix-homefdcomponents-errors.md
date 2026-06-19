# Fix: HomeFDComponents.kt compilation errors

## Problem

Two issues in `app/src/main/java/com/example/birdy/ui/fooddelivery/HomeFDComponents.kt`:

1. **`DynamicPromoBannerView` function is missing** — The function definition with `banner: FeaturedBanner` and `onAction: () -> Unit` parameters was deleted. It's referenced from `HomeFD.kt` lines 249-252.

2. **`RestaurantSection` body is corrupted** — Its proper body (section header + horizontal LazyRow scroll) was replaced with promo banner body content that references undefined `banner` and `onAction` symbols.

## Root Cause

The break starts at line 665 inside `RestaurantSection`. Instead of the correct section header Row with title + arrow + LazyRow of restaurant cards, the body contains promo banner content:
- `banner.title`, `banner.subtitle`, `banner.actionText` (undefined — `banner` is not a parameter of RestaurantSection)
- `onAction()` (undefined — no such parameter)
- `.background(Color.White, RoundedCornerShape(15.dp))` instead of `.padding(horizontal = 16.dp)`
- No `LazyRow` with restaurant cards

The missing `DynamicPromoBannerView` function (with `banner` and `onAction` parameters, gradient color parsing, and promo banner layout) needs to be re-inserted after `RestaurantSection`.

## Fix (single file, 2 logical changes)

### File: `app/src/main/java/com/example/birdy/ui/fooddelivery/HomeFDComponents.kt`

**Change 1:** Replace the broken `RestaurantSection` body (lines 665-708) with the correct body:

```
// Section header Row with title + arrow button
Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp)
) {
    Text(text = title, ...)          // section heading
    Icon(ArrowForward, clickable = {  // next arrow
        scroll to next card
    })
}

// Horizontal restaurant cards
LazyRow(...) {
    itemsIndexed(restaurants) { _, restaurant ->
        RestaurantCard(...)
    }
}
```

**Change 2:** Re-add `DynamicPromoBannerView` function between `RestaurantSection` and `FeedRestaurantCard`:

```kotlin
@Composable
fun DynamicPromoBannerView(
    banner: FeaturedBanner,
    modifier: Modifier = Modifier,
    onAction: () -> Unit = {}
) {
    // Parse gradient colors from hex strings
    val gradientColors = ...
    
    Box(
        modifier.fillMaxWidth()
            .background(horizontalGradient(colors = gradientColors), roundedCornerShape)
            .padding(16.dp)
    ) {
        Row(...) {
            Column(weight = 1f) {
                Text(banner.title)
                Text(banner.subtitle)
                Box(clickable = { onAction() }) {
                    Text(banner.actionText)
                }
            }
            // Image or placeholder
            if (banner.imageUrl.startsWith("http")) {
                AsyncImage(model = banner.imageUrl, ...)
            } else {
                Box(...)
            }
        }
    }
}
```

**Exact replacement content** (lines 649-725):

Replace everything from `@Suppress("UNUSED")` at line 649 through the end comment line `// MARK: - Feed Restaurant Card` with the combined correct versions of both functions.
