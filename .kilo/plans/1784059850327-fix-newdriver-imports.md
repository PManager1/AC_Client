# Fix NewHomeBatchScreen — images not loading from backend

## Problem

The private `Config` object at line 351 uses `http://10.0.2.2:8090/api/v1` (local emulator), but the actual backend is the AWS Dev URL (`https://tcdlm857gf.execute-api.us-east-1.amazonaws.com/dev/api/v1`). The brand API calls at lines 253 and 291 hit the wrong server, so `carouselImages`/`logoUrl` are never received and `imageUrl` stays empty.

Other pages (StoreScreen, SearchFood, etc.) work because they import `com.example.birdy.data.Config` which uses the correct AWS Dev URL.

## Fix

Two changes in `app/src/main/java/com/example/birdy/ui/explore/NewHomeBatchScreen.kt`:

### 1. Add import for the real Config (after line 5 `import com.example.birdy.R`)

```
import com.example.birdy.data.Config
```

### 2. Remove the private Config override (lines 349–353)

Delete:
```
// MARK: - Config API Base URL (matches BirdyKit)
// Standalone copy so no BirdyKit dependency needed on AC
private object Config {
    const val API_BASE_URL = "http://10.0.2.2:8090/api/v1"
}
```

All existing references to `Config.API_BASE_URL` in `loadMockData` will now resolve to the imported `com.example.birdy.data.Config` with the correct AWS Dev URL.

No other code changes needed — the brandId-based approach (fetch `/brands/{brandId}`, extract `carouselImages[0]`/`logoUrl`) is already correct per the iOS flow.

## Files changed

Only `app/src/main/java/com/example/birdy/ui/explore/NewHomeBatchScreen.kt`.

## Verification

`./gradlew :app:compileDebugKotlin` — zero errors.
On-device: brand API calls hit the correct server, images load from `carouselImages`/`logoUrl`.
