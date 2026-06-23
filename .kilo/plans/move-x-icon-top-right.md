# Move X (Close) Icon to Top-Right in TagHomeFilterViews.kt

Each of the 4 filter sheet composables currently uses a `Column` as the root layout. The X close `IconButton` is missing entirely (it was never ported from the iOS version). This plan adds it to the top-right corner of each sheet.

## Files affected

- `app/src/main/java/com/example/birdy/ui/home/TagHomeFilterViews.kt`

## Transformation pattern (applied 4 times)

For every sheet function (`DeliveryFeeFilterSheet`, `ScheduleFilterSheet`, `RatingsFilterSheet`, `PriceFilterSheet`):

Current structure:
```
fun XxxFilterSheet(...) {
    ...
    Column(...) {
        // sheet content
    }
}
```

New structure:
```
fun XxxFilterSheet(...) {
    ...
    Box(modifier = Modifier.fillMaxWidth()) {
        Column(...) {
            // sheet content (unchanged)
        }
        IconButton(
            onClick = onDismiss,
            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
        ) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
        }
    }
}
```

- The existing `Column` is wrapped in a `Box(modifier = Modifier.fillMaxWidth())`.
- An `IconButton` is added after the `Column` inside the Box, using `Modifier.align(Alignment.TopEnd)` to position it at the top-right.
- The `onDismiss` callback from the function params drives the close behavior.

## Detailed edit targets

### 1. DeliveryFeeFilterSheet (lines ~75–131)

Wrap the `Column(...) { ... }` in `Box(modifier = Modifier.fillMaxWidth()) { ... }`.
Insert `IconButton(...)` between Column's closing `}` and the Box's closing `}`.

Current end:
```
    }
}
```
New end:
```
    }
    IconButton(
        onClick = onDismiss,
        modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
    ) {
        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
    }
}
```

### 2. ScheduleFilterSheet (lines ~143–306)

Same transformation — wrap `Column` in `Box`, add `IconButton` before the closing `}`.

### 3. RatingsFilterSheet (lines ~316–383)

Same transformation.

### 4. PriceFilterSheet (lines ~393–448)

Same transformation.

## Risk: Brace matching

To avoid the "Expecting '}'" error from the previous attempt:
- Each edit must be self-contained — replace the entire `Column(` ... `}` block (including its closing brace) with the new `Box(` ... `IconButton ... }` block.
- Do **not** rely on indentation-based matching; match on exact code strings.
- After all 4 edits, verify the file has balanced braces by running:
  ```
  python3 -c "
  import re
  with open('app/src/main/java/com/example/birdy/ui/home/TagHomeFilterViews.kt') as f:
      lines = f.readlines()
  depth = 0
  for i, line in enumerate(lines, 1):
      stripped = re.sub(r'//.*', '', line)
      stripped = re.sub(r'\".*?\"', '', stripped)
      opens = stripped.count('{')
      closes = stripped.count('}')
      depth += opens - closes
      if depth < 0:
          print(f'Line {i}: NEGATIVE depth {depth}')
  print(f'Final depth: {depth}')
  ```
- Final depth should be `0`.

## Validation

- `./gradlew :app:compileDebugKotlin` — must succeed with zero errors and zero warnings.
- Visually, each filter sheet should show an X icon in the top-right corner that calls `onDismiss` on tap.
- The X should not overlap or clip sheet content (the `padding` in the Column already provides enough buffer).
