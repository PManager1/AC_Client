# Fix NewHomeBatchScreen — JSON parsing crash after mock data update

## Problem

The `mock_batch_data.json` update removed the `imageUrl` field from every batch item. Line 223 calls `b.getString("imageUrl")` which throws `JSONException` when the key is absent, crashing `loadMockData` before any data reaches the UI. The outer catch (line 345) returns `BatchZoneData("Error", emptyList(), mutableListOf())`, so the page shows nothing.

## Fix

One change in `app/src/main/java/com/example/birdy/ui/explore/NewHomeBatchScreen.kt`:

### Line 223 — make `imageUrl` optional when parsing JSON

```
- imageUrl = b.getString("imageUrl"),
+ imageUrl = b.optString("imageUrl", ""),
```

When the JSON has `imageUrl` (old format), it reads the value. When absent (new format), it defaults to `""`. The brand API fetch (lines 296–352) then patches `imageUrl` with real URLs from `carouselImages` or `logoUrl` for batch items that get a `brandId`.

## Verification

`./gradlew :app:compileDebugKotlin` — zero errors.
On-device: data should render, images load from the backend API.
