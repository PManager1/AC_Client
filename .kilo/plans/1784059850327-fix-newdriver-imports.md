# Fix NewHomeBatchScreen.kt — missing `clip` import

## Problem

12 compilation errors all cascade from a single missing import.

The file uses `.clip(CircleShape)` and `.clip(RoundedCornerShape(...))` at 8 locations (lines 696, 719, 728, 775, 777, 801, 813, 845) but never imports `androidx.compose.ui.draw.clip`. When `clip` is unresolved:
- 8× `Unresolved reference 'clip'` — direct error at each usage
- `background` candidate errors — the broken modifier chain makes `.background(...)` unparseable
- "No value passed for parameter 'modifier'" — the broken modifier expression fails to provide a valid `Modifier` to the enclosing composable

## Fix

One change in `NewHomeBatchScreen.kt` — add the missing import:

```
import androidx.compose.ui.draw.clip
```

Place it alongside the other `androidx.compose.ui.*` imports, e.g. after line 29 (`import androidx.compose.ui.Modifier`).

## File

`app/src/main/java/com/example/birdy/ui/explore/NewHomeBatchScreen.kt`

## Verification

`./gradlew :app:compileDebugKotlin` — zero errors.
