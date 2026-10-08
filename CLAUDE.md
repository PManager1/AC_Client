# Project map
- udo3/ = Elixir/Phoenix backend + web (this is the main codebase)
- BK/   = old Go backend (SMS hook still lives here as rollback)
- IC/   = iOS app (SwiftUI, min iOS 16.7: use ObservableObject/@Published, NOT @Observable)
- AC/   = Android app (Jetpack Compose)
- SUS-this/   = separate web app for back office work, mainly for this url /admin/

# Where things live (udo3)
- Auth: lib/udo/auth.ex, lib/udo/auth/ (supabase_client.ex, sns_client.ex)
- Auth controllers: lib/udo_web/controllers/auth/
- Router: lib/udo_web/router.ex
- Config: config/runtime.exs (env vars), config/test.exs
- Checkout: lib/udo_web/live/CheckoutLive.ex
- Grocery cart handlers: lib/udo_web/live/Gstore/gstore_cart_handlers.ex
- Item card component: lib/udo_web/components/gstore/item_card.ex
- Search JSON: lib/udo_web/controllers/api/search/search_json.ex

# Commands
- Run: mix phx.server
- Test: mix test   (5 known failures: address backfill, PageController GET /, 3 SearchBar)
- Android: cd AC && ./gradlew :app:assembleDebug
- Deploy: git push gigalixir main

# Rules
- Prefer adding to existing modules/folders over creating new files.
- Keep unrelated changes out of a commit. One feature per commit.
- Never commit generated files in priv/static (hashed names, .gz, cache_manifest.json).
- phoenix_static_buildpack.config pins Node. It must stay committed.
- Never log OTPs, phone numbers or secrets.
- Phone numbers go to external services in E.164 (+1...).
- Add tests for new behavior.

# Known gotchas
- Supabase SMS hook now points to udo3 /api/v1/auth/sms-hook (BK hook is the rollback).
- Env vars live on gigalixir, not in git.
