# RentEase — Android

Student housing marketplace: students search and book verified off-campus
accommodation; landlords list and manage properties, tenants and maintenance;
admins verify landlords, moderate listings and manage users. Native Android
app (Kotlin, Jetpack Compose), talking directly to a hosted Supabase backend.

> **Build status: NOT build-verified.** This README was rewritten after a
> source-level audit and a round of fixes (see `PART2_AUDIT.md` for the full
> list). No sandbox in the environment used to write this could reach Maven
> Central, Google's Maven repo, or run the Android Gradle Plugin, so
> `./gradlew testDebugUnitTest lintDebug assembleDebug` has **not** been run
> against this code. Run it yourself before submitting — see
> [Known limitations](#known-limitations).

---

## Contents
- [Architecture](#architecture)
- [Authentication](#authentication)
- [Student](#student)
- [Landlord](#landlord)
- [Admin](#admin)
- [REST API](#rest-api)
- [Database](#database)
- [Offline support (Room + WorkManager)](#offline-support-room--workmanager)
- [Push notifications (FCM)](#push-notifications-fcm)
- [Google Sign-In (SSO)](#google-sign-in-sso)
- [Multilingual support](#multilingual-support)
- [Tests](#tests)
- [GitHub Actions](#github-actions)
- [Setup](#setup)
- [Demo accounts](#demo-accounts)
- [Known limitations](#known-limitations)

---

## Architecture

Single Android app module (`app/`), MVVM, Jetpack Compose UI, Hilt for DI.

```
presentation/   Compose screens + ViewModels, one package per role/feature
                (auth, student/*, landlord/*, admin/*, settings, session, common)
domain/         Plain Kotlin models and repository interfaces — no Android imports
data/           Repository implementations, Retrofit API interfaces + DTOs,
                Room database, WorkManager sync, auth/token storage
notifications/  FCM service, notification channels, tray notifications
di/             Hilt modules (Network, Database, Repository)
util/           Small stateless helpers (e.g. Google Sign-In nonce hashing)
```

Navigation is a single Compose `NavHost` (`RentEaseNavGraph.kt`) with three
role-gated destination trees (`student/*`, `landlord/*`, `admin/*`) reached
through `Routes.kt`. There is no WebView anywhere in the app — every screen is
a native Compose destination.

**Language/runtime:** Kotlin, target/compile SDK 35, min SDK 26. Networking
via Retrofit 2 + OkHttp + kotlinx.serialization. Images via Coil. Local
persistence via Room. Background work via WorkManager. DI via Hilt.

## Authentication

Backed by Supabase GoTrue (`SupabaseAuthApi`) plus a `profiles` /
`user_roles` row per account. `AuthRepositoryImpl` owns the session:

- **Email + password** — separate registration flows for Student and
  Landlord (`StudentRegisterScreen`, `LandlordRegisterScreen`), each writing
  role-specific profile fields (student number/university; business
  name/ID/ownership documents for landlords).
- **Google Sign-In (SSO)** — see [dedicated section](#google-sign-in-sso)
  below; only ever creates Student or Landlord accounts, never Admin.
- **Session persistence** — `TokenManager` stores the access/refresh token
  pair; `AuthInterceptor` attaches `Authorization`/`apikey` to every request
  and silently refreshes on a `401`. A `SplashScreen` calls
  `AuthRepository.restoreSession()` on launch and routes straight to the
  signed-in role's home screen, or to role-select if there is no session.
- **Logout** — every "Log out" entry point (student/landlord/admin home,
  Settings) calls `AuthRepository.logout()`, which clears the stored tokens,
  then returns to role-select and clears the back stack. (Earlier in
  development this button only navigated back without clearing tokens; that
  was fixed as part of this audit.)
- **Admin** — has no visible sign-up. `AdminAccessScreen` is a hidden route
  (`auth/admin_access`, never linked from any visible UI) that takes an
  existing email/password plus a server-side admin access code, calls the
  `admin-access` Supabase Edge Function (which verifies the code with a
  timing-safe compare against a secret nobody in the client can see), and on
  success grants the `admin` role.

## Student

- **Search & browse** (`presentation/student/search`) — keyword, university,
  max rent, furnished-only and amenity filters against `properties`, with
  pagination.
- **Listing detail** (`student/detail`) — full description, amenities,
  images, landlord name, reviews, and a booking-request form (uploads ID +
  proof-of-registration documents to Supabase Storage first, then submits the
  booking).
- **Compare** — see [user-defined features](#user-defined-features-what-is-genuinely-demoable).
- **Saved listings** (`student/saved`) — favourite/unfavourite with offline
  queueing (see [Offline support](#offline-support-room--workmanager)).
- **Bookings** (`student/bookings`) — the student's own booking requests and
  their status.
- **Messages** (`student/messages`) — one conversation per booking/property
  with the landlord.
- **Notifications** (`student/notifications`) — in-app feed backed by the
  `notifications` table (independent of the FCM tray notification — see
  below).

## Landlord

- **Dashboard** (`landlord/dashboard`) — verification status banner,
  gamified verification progress card (see below), analytics summary.
- **Properties & rooms** (`landlord/properties`, `landlord/rooms`) — create,
  edit, publish/unpublish, delete; per-room pricing and capacity for shared
  properties.
- **Booking requests** (`landlord/bookings`) — approve/reject, assign a room.
- **Tenants** (`landlord/tenants`) — current tenants, lease summary, status.
- **Maintenance** (`landlord/maintenance`) — view tickets with photos,
  change status (open → in progress → resolved). **Landlord-only**: there is
  no student-facing screen to *submit* a maintenance ticket in this build
  (see [Known limitations](#known-limitations)).
- **Messages** (`landlord/messages`) — conversations with students.
- **Analytics** — see [user-defined features](#user-defined-features-what-is-genuinely-demoable).
- **Notifications** (`landlord/notifications`) — in-app feed.

## Admin

- **Landlord verification** (`admin/verification`) — approve/reject based on
  submitted ID and proof-of-ownership documents.
- **Property moderation** (`admin/moderation`) — approve/reject listings
  before they go live.
- **User management** (`admin/users`) — suspend/reinstate accounts.
- **Bookings** (`admin/bookings`) — read-only oversight of all bookings.
- **Audit log** (`admin/audit-log`) — every admin action (verify, moderate,
  suspend) is written to `audit_logs` and shown here.

## REST API

**The backend is Supabase, accessed directly over PostgREST/GoTrue/Storage —
this is *not* a Node/Express API**, and this README does not describe it as
one. Concretely:

| Concern | Mechanism |
|---|---|
| Data (properties, bookings, rooms, profiles, …) | PostgREST auto-generated REST endpoints (`rest/v1/<table>`), called via Retrofit (`SupabaseRestApi`, `SupabaseListingsApi`, `SupabaseLandlordApi`, `SupabaseAdminApi`, …) |
| Auth | GoTrue (`auth/v1/token`, `auth/v1/signup`) via `SupabaseAuthApi` |
| File storage | Supabase Storage (`storage/v1/object/...`) via `SupabaseStorageApi` |
| Admin-code grant | One Supabase **Edge Function** (`supabase/functions/admin-access`), the one place that genuinely needs server-only secrets (`ADMIN_ACCESS_CODE`, the service-role key) that Postgres/PostgREST access alone cannot provide |

Row Level Security in Postgres (not shown to the Android client) is what
actually enforces "a student can only see their own bookings",
"a landlord can only edit their own properties", etc. — the app trusts
Postgres to enforce this, and does not re-implement authorization logic
client-side.

Every Retrofit interface is real: request/response bodies are
`kotlinx.serialization` DTOs, and `data/remote/RestApiContractTest.kt` proves
this by sending genuine HTTP requests through the production
`AuthInterceptor` + `RetrofitFactory` configuration to a local
`MockWebServer`, asserting the exact path, headers and JSON body PostgREST
and GoTrue receive.

**Known wire-format gotcha (fixed in this audit):** several endpoints
(profile updates, property/room create/update) send a partial row as
`Map<String, Any?>` because the set of changed columns differs per call.
`kotlinx.serialization`'s Retrofit converter cannot serialise a bare `Any`;
without a custom converter, every one of those calls threw at runtime. A
`JsonMapConverterFactory` was added (see `data/remote/JsonMapConverterFactory.kt`)
and is covered by `JsonMapConverterFactoryTest.kt` and the contract test above.

## Database

Two things share the name "database" in this project — do not confuse them:

1. **Supabase Postgres** (source of truth; `properties`, `bookings`,
   `profiles`, `landlord_profiles`, `rooms`, `messages`, `conversations`,
   `maintenance_tickets`, `notifications`, `device_tokens`, `audit_logs`,
   `user_roles`, …). Two migrations exist under `supabase/migrations/`:
   `add_device_tokens` and `add_notification_preferences`. **The rest of the
   schema is not included in this repository** — it must already exist in
   your Supabase project (see [Known limitations](#known-limitations)).
   `supabase/seed/demo_data.sql` seeds sample rows into tables that must
   already exist; it does not create any `auth.users`, so it cannot create
   sign-in-able demo accounts by itself.
2. **Room** (on-device SQLite) — a small local cache/outbox, not a mirror of
   the whole backend. See below.

## Offline support (Room + WorkManager)

Room (`data/local/RentEaseDatabase`, version 1) has exactly two tables:

- `cached_properties` — the last successful search/listing results, so the
  Student search screen can show something when offline instead of a blank
  error state.
- `favourite_outbox` — favourite/unfavourite actions taken while offline are
  queued here.

`SavedListingsRepositoryImpl.toggleFavourite()` tries the network call first;
on failure it writes to `favourite_outbox` and enqueues `FavouriteSyncWorker`
via WorkManager with a `NetworkType.CONNECTED` constraint and exponential
backoff. The worker replays queued actions against PostgREST once connectivity
returns and clears them from the outbox on success. This is genuine
offline-first behaviour for one feature (favourites), not a general offline
mode for the whole app — most other screens simply show an error state
without connectivity.

## Push notifications (FCM)

`RentEaseMessagingService` (`FirebaseMessagingService`) handles both
foreground and background delivery, builds the tray notification itself via
`NotificationPoster`, and routes four categories to four `NotificationChannel`s
(`booking`, `message`, `maintenance`, else → `system`). `DeviceTokenRegistrar`
registers the current FCM token with the backend on login and on token
rotation (`onNewToken`). Tapping a notification deep-links
(`rentease://...`) into the relevant screen via `RentEaseNavGraph`'s
`navigateToDeepLink`.

As of this audit, the service also checks the signed-in user's Settings →
Notification preferences before posting: if the person switched a category
off, the push is dropped client-side (the in-app feed, written server-side,
is unaffected).

**Firebase project setup required** (not included in this repo):
1. Create/connect a Firebase project, add an Android app with package name
   `com.rentease.app`.
2. Download the real `app/google-services.json` and place it at that exact
   path (**gitignored** — never commit it). A **placeholder** with no real
   credentials lives at `app/google-services.json.example`; CI copies it in
   automatically when no real one is supplied, so the build compiles but push
   notifications will not actually work in that APK.
3. The backend must be configured to call the Firebase Admin SDK (server
   side, outside this repo) to actually *send* messages — this app only
   receives and displays them.

## Google Sign-In (SSO)

Uses Credential Manager + `GoogleIdTokenCredential`, then Supabase GoTrue's
native `id_token` grant (`auth/v1/token?grant_type=id_token`) — no browser
tab, no WebView.

**Nonce handling (fixed in this audit):** Google embeds whatever nonce string
it's given into the ID token's `nonce` claim; GoTrue verifies
`sha256_hex(nonce sent in the request)` against that claim. So the app must
hash the nonce before giving it to Google, and send GoTrue the *raw* value.
`util/GoogleNonce.kt` does exactly this (tested against a known SHA-256 test
vector in `GoogleNonceTest.kt`); previously the same nonce was sent to both
sides unhashed, which would fail GoTrue's verification.

`GOOGLE_WEB_CLIENT_ID` (the **web** OAuth client ID from Google Cloud
Console, not an Android client ID) must be set locally — see
[Setup](#setup) — and the Google provider must be enabled in
Supabase Auth with that same client ID.

## Multilingual support

English, isiZulu (`values-zu`) and Tshivenda (`values-ve`) — 338 string keys,
one-to-one across all three languages (`values/strings.xml` is the source of
truth for keys). Language is switched in Settings via
`AppCompatDelegate.setApplicationLocales()` and persisted server-side
(`profiles.preferred_language`), so it survives reinstall/re-login on another
device. `MainActivity` extends `AppCompatActivity` (not plain
`ComponentActivity`) specifically so this recomposes the UI on API 26–32 as
well as 33+; `res/xml/locales_config.xml` lists the three languages for
Android 13's own per-app language picker.

`LocalizationResourcesTest.kt` guards this automatically: it fails if any key
is missing in isiZulu/Tshivenda, if any translation is blank, if a `%s`/`%d`
placeholder doesn't match across languages, or if a Compose `Text(...)` call
anywhere in the app uses a hardcoded English literal instead of
`stringResource(...)`.

> ⚠️ **The isiZulu and Tshivenda text added during this audit (~90 of the 338
> keys — mostly validation/error messages, the comparison feature, the
> verification-progress feature, and priority/notification-channel labels)
> is a best-effort machine draft and has not been reviewed by a native
> speaker.** Treat it as a placeholder to be corrected before relying on it
> for grading or real users. The pre-existing ~248 keys were already in the
> project before this audit.

## Tests

Unit tests only (JUnit + MockK + Robolectric + kotlinx-coroutines-test),
under `app/src/test/`. **`app/src/androidTest/` does not exist in this
project** — there are no instrumented/UI tests.

| | Count |
|---|---|
| Tests present before this audit | 65 |
| Tests added during this audit | 35 |
| **Total** | **100** |
| Passed | not run — see the build-status notice at the top |
| Failed | not run |
| Could not run | all 100 (no Gradle/JVM/Android toolchain available in the environment used for this audit) |

New test files added in this audit, and why:

- `data/remote/RestApiContractTest.kt` — real HTTP (MockWebServer) contract
  tests for the REST layer described above (GET/PATCH wire format, the
  Google id_token grant body, error and offline handling).
- `data/remote/JsonMapConverterFactoryTest.kt` — unit tests for the new
  `Map<String, Any?>` → JSON converter.
- `util/GoogleNonceTest.kt` — SHA-256 nonce hashing, verified against a known
  test vector.
- `domain/model/PropertyComparisonTest.kt` — pure logic for the comparison
  feature (selection limits, best-value ranking, ties, summary picks).
- `domain/model/VerificationProgressTest.kt` — pure logic for the gamified
  verification-progress feature (every milestone/badge transition).
- `domain/model/NotificationPreferencesTest.kt` — the category → toggle
  mapping used by the FCM service.
- `data/repository/ProfileUpdateTest.kt` — the new editable-profile Settings
  feature (trimming, blank-name rejection, phone-clearing, server-error
  handling).
- `resources/LocalizationResourcesTest.kt` — the localisation guard above.

All 65 pre-existing tests are unchanged except one added import
(`io.mockk.every`) in `AuthRepositoryImplTest.kt`, needed because that file
already called `every {}` without importing it — a latent compile error in
the original project that would have failed the whole test source set the
first time anyone tried to run it.

## GitHub Actions

`.github/workflows/android.yml` runs on push/PR/manual dispatch:
checkout → verify the Gradle wrapper is committed (fails fast with a clear
message if not) → validate the wrapper checksum → JDK 17 → Android SDK →
Gradle setup/cache → write `local.properties` from optional repo *variables*
(falls back to harmless placeholders so the build still compiles without any
configuration) → write `google-services.json` from an optional repo *secret*
(`GOOGLE_SERVICES_JSON_B64`, base64-encoded) or fall back to the
credential-free placeholder → `testDebugUnitTest` → `lintDebug` →
`assembleDebug` → upload test results, lint report, and the debug APK as
build artifacts.

**This workflow cannot succeed yet** — see the very first item in
[Known limitations](#known-limitations). It was reviewed for correctness
(step order, secrets/variables usage, artifact paths) but, like the rest of
this project, has not actually been executed.

## Setup

1. **Generate the Gradle wrapper** (not included — see limitations):
   ```
   scripts/bootstrap-gradle-wrapper.sh
   ```
   This needs a local Gradle 8.9+ install (or Android Studio's bundled one).
2. **Copy `local.properties.example` → `local.properties`** and fill in:
   - `sdk.dir` — your Android SDK path
   - `SUPABASE_URL`, `SUPABASE_ANON_KEY` — Supabase dashboard → Project
     Settings → API
   - `GOOGLE_WEB_CLIENT_ID` — Google Cloud Console OAuth **web** client ID
3. **Firebase**: download your real `app/google-services.json` and place it
   there (see [Push notifications](#push-notifications-fcm)).
4. **Supabase**: apply the two migrations under `supabase/migrations/`
   against your project, deploy the `admin-access` Edge Function
   (`supabase functions deploy admin-access`) and set its secret
   (`supabase secrets set ADMIN_ACCESS_CODE=...`). Enable the Google Auth
   provider with the same `GOOGLE_WEB_CLIENT_ID`. The rest of the schema
   (properties, bookings, profiles, etc.) is assumed to already exist in your
   project — this repo does not include a from-scratch schema migration.
5. Open in Android Studio (or run `./gradlew assembleDebug` once the wrapper
   exists) and let Gradle sync.

## Demo accounts

**None exist.** `supabase/seed/demo_data.sql` seeds sample *data* rows
(properties, bookings, etc.) but does not create any `auth.users`, so it
creates no sign-in-able accounts. Create at least one Student, one Landlord,
and one Admin account yourself (Admin needs the hidden `admin_access` route
plus your `ADMIN_ACCESS_CODE`) before recording a demo or grading this
submission — this is listed again in the "before submission" checklist.

## Known limitations

- **Not build-verified.** No environment reachable while preparing this
  audit could run Gradle, download the Android Gradle Plugin, or reach
  Maven Central/Google's Maven repo. Everything above was checked by reading
  every relevant source file, cross-checking method signatures between
  callers and implementations, running string-resource/import/bracket-balance
  scripts, and validating the GitHub Actions YAML — **not** by compiling.
  Run `./gradlew testDebugUnitTest lintDebug assembleDebug` yourself and fix
  whatever surfaces; there will likely be something, since this exact code
  has never been compiled.
- **Gradle wrapper is not committed** (`gradlew`, `gradlew.bat`,
  `gradle/wrapper/gradle-wrapper.jar`) — run
  `scripts/bootstrap-gradle-wrapper.sh` and commit the result before pushing;
  otherwise GitHub Actions fails immediately (by design, with a clear error).
- **Google Maps is not implemented.** The Gradle dependencies
  (`maps-compose`, `play-services-maps`) were present in the original project
  but nothing in the codebase used them and no Maps API key was configured;
  they have been removed rather than left in place implying a feature that
  doesn't exist. Listing detail shows distance-to-campus as a number
  (`km_from_campus`), not a map.
- **Saved-search digests are not implemented** — no code for this feature
  exists anywhere in the project.
- **Students cannot submit maintenance tickets** in this build — the
  Maintenance feature is landlord-view/status-change only; there is no
  student-facing "report a maintenance issue" screen or upload flow.
- **isiZulu/Tshivenda translations added during this audit need native-speaker
  review** (see [Multilingual support](#multilingual-support)).
- **No instrumented/UI tests** (`androidTest`) exist in this project — only
  the 100 JVM unit tests described above.
- **Room's offline support is narrow**: it covers the saved-listings/favourite
  feature only, not a general offline mode.
- **This repository does not include the full Postgres schema** — only two
  incremental migrations. The rest of the schema (properties, bookings,
  profiles, etc.) must already exist in your Supabase project.
