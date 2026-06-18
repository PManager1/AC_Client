# Fix: Address options not showing on home page when clicking address

## Problem
In `SelectAddressSheet.kt`, the `if (isLoading)` block at line 179 wraps the loading indicator **and** the entire address list + "Add New Address" button. When `isLoading` transitions to `false`, the whole content inside `if (isLoading)` disappears, so the saved address options are never displayed.

Additionally, `errorMessage` is set but never shown in the UI.

## Root Cause (lines 179–259)
The brace structure is wrong — the address rendering code is nested inside the loading condition:
```kotlin
if (isLoading) {
    // Loading text
    // Address list       <-- should not be here
    // Add New Address    <-- should not be here
}
```

Step 1 — reading current code, I traced:
- Line 179: `if (isLoading) {` — opens the block
- Lines 180–198: Box containing "Loading addresses..." text
- Lines 199–257: Address list, divider, "Add New Address" button, spacer — **all still inside `if (isLoading)`**
- Line 258: `}` closes the Box
- Line 259: `}` closes `if (isLoading)`

## Fix (single file change)

### File: `app/src/main/java/com/example/birdy/ui/fooddelivery/SelectAddressSheet.kt` (lines 179–259)

Replace the current block with a proper `if / else if / else`:

```kotlin
if (isLoading) {
    Box(/* fullscreen, centered */) {
        Text("Loading addresses...")
    }
} else if (errorMessage != null) {
    Box(/* fullscreen, centered */) {
        Text(errorMessage, color = Red)
    }
} else {
    // Saved address list + "Add New Address" button
    localAddresses.forEach { address ->
        AddressSelectionRow(...)
    }
    HorizontalDivider(...)
    Row(/* "Add New Address" */) { ... }
    Spacer(...)
}
```

Key changes:
1. `if (isLoading)` → only the loading indicator (no address list inside it)
2. `else if (errorMessage != null)` → show the error message when loading fails
3. `else` → show the address list and "Add New Address" button
4. Fix brace nesting — the extra `}` at line 258 is removed along with the old structure
