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
**Never run git commits. Do not commit code changes under any circumstances; stage or leave modified files uncommitted so I can review and commit them myself.**
- Prefer adding to existing modules/folders over creating new files.
- Keep unrelated changes out of a commit. One feature per commit.
- Never commit generated files in priv/static (hashed names, .gz, cache_manifest.json).
- phoenix_static_buildpack.config pins Node. It must stay committed.
- Never log OTPs, phone numbers or secrets.
- Phone numbers go to external services in E.164 (+1...).
- Add tests for new behavior.

- Keep the codebase small: extend before you create

- Files
- Put new code in the file that already owns that feature. Do not create a new file
  just because the code is new.
  - Anything Stripe or payments → the existing payments files (`lib/udo/payments/`,
    the payments screens in the apps). Not a new `stripe_helper`, `card_utils`, etc.
  - SMS login, OTP, verification codes → the existing auth file and its functions.
    SMS is part of auth, not its own module.
- A new file is allowed only when the feature is genuinely new and has no existing
  home, or the existing file would become hard to work with (roughly 800+ lines).
  Before creating one, say in one line why no existing file fits.
- No new "helpers", "utils", "manager", or "service" files for a few functions.
  Put the functions next to the code that uses them.
- Don't split one feature across several small files.
- Shared iOS code goes in BirdyKit only if two or more apps actually use it.





## Database
- There is ONE database: Supabase (Postgres), already connected. Never create, install, or start a local Postgres or any other database, and never add a second Repo or DB config. Use the existing connection in config/ and the DATABASE_URL env var. If a task seems to need a different database, stop and ask first.

### Database tables
- Do not create a new table without asking me first.
- First check whether an existing table can hold it (a new column, or a jsonb
  field for loose settings).
- If you still think a new table is needed, stop and explain: what it stores, why
  existing tables can't hold it, and the migration. Wait for my OK.



# Known gotchas
- Supabase SMS hook now points to udo3 /api/v1/auth/sms-hook (BK hook is the rollback).
- Env vars live on gigalixir, not in git.





## How to explain things to me (Jay)

- Use plain, simple English. Short sentences. Everyday words. No fancy wording. 
- If you use a technical word, explain it in simple words right after.
- After every change, tell me:
  1. Which files you changed.
  2. What you changed in each file, in one or two simple sentences.
  3. Why you changed it.
- If something is complicated, explain it step by step, like you are teaching me.
- At the end, ask me if anything is unclear.



## Environments and databases

- **Android apps (AC and AP) always connect to production** (gigalixir).
  - Never point them at a local server (no `localhost`, no `10.0.2.2`).
  - Never change `ApiConfig` to a local URL, not even "temporarily for testing".
- **No local databases.**
  - Do not create, copy or seed any local database (no `udo_dev`, no dumps, no seed scripts).
  - The only local database is the existing `udo_test`, used by `mix test`. Don't create others.
- **Testing IP and AP together happens on production.**
  - Live updates go through the server's channel, so both apps must talk to the same server.
  - Deploy the server change to gigalixir first, then test with IP and AP both on production.
