# RentEase — Phase 1 Project Audit
**Web app inspected:** rentease-student-housing-main.zip (TanStack Start + React + TypeScript, Supabase JS client, deployed on Vercel)
**Spec inspected:** OPSC6312 Part 1 — Research, Planning and Design (ST10455321)

## 0. Key architectural finding (important — affects Phase 3)

The Part 1 document (Section 5.1) specifies a **custom Node.js/Express REST API on Render**, with the web app and Android app both calling it, backed by Supabase/PostgreSQL.

The actual web app in the ZIP does **not** do this. It talks to Supabase **directly** from the client using `@supabase/supabase-js` (`src/integrations/supabase/client.ts`), protected by Postgres Row-Level-Security policies defined in the migrations. There is no Express server, no `/auth/register`, `/listings`, `/bookings` etc. endpoints anywhere in the repo — those are server *functions* (`createServerFn` from TanStack Start, e.g. `admin-access.functions.ts`) used only for two sensitive operations (admin-code claiming, and presumably a couple of others), not a general REST API.

**Decision needed from you:** for Part 2, do you want me to:
- **(A)** Build the Kotlin app to call Supabase directly (Postgrest REST + Supabase Auth + Realtime + Storage), which matches the *actual* reference implementation and is materially less work, or
- **(B)** Build a real Node/Express REST API layer in front of Supabase (matching what Part 1 *says*, on paper) and have Kotlin call that instead, which matches your written spec exactly but is a second backend project on top of the Android build?

I've defaulted my plan below to **(A)** with a thin repository-layer abstraction so it can be swapped for (B) later without touching ViewModels — but this is your call since it's a POE compliance question, not a technical one. Supabase's REST layer (PostgREST) already **is** a REST API — `GET /rest/v1/properties?...`, JWT bearer auth, role-based RLS — so (A) still satisfies "Kotlin → Retrofit → HTTPS → REST API → Postgres," it's just Supabase's REST API rather than a hand-rolled one.

## 1. Requirements Matrix

| # | Requirement (Part 1) | Web implementation (found in ZIP) | Kotlin implementation | Status | Action required |
|---|---|---|---|---|---|
| 1 | Register as Student/Landlord/Admin, role chosen at signup | `signup.index.tsx`, `signup.student.tsx`, `signup.landlord.tsx` — separate forms; role stored via `raw_user_meta_data.role`, applied server-side in `handle_new_user()` trigger | Not started | MISSING | Build separate Student/Landlord registration screens (Compose) + ViewModels; Admin never a public option (matches PDF's admin-access.tsx flow, see #5) |
| 2 | Passwords hashed with bcrypt, never stored plaintext | Delegated entirely to Supabase Auth (Supabase itself bcrypt-hashes) | N/A — client never sees password hashing | IMPLEMENTED (by platform) | None — just don't roll custom auth; use Supabase Auth SDK/GoTrue REST endpoints from Kotlin |
| 3 | JWT access token (1h) + refresh token, stored in EncryptedSharedPreferences | Supabase session stored in browser localStorage (`client.ts`) — web equivalent, not directly reusable pattern | Not started | MISSING (Android-specific) | Use Supabase Kotlin/GoTrue REST `/auth/v1/token`; store `access_token`/`refresh_token` in `EncryptedSharedPreferences`; refresh on 401 |
| 4 | Role-based route/endpoint protection | Enforced at DB layer via Postgres RLS policies (`has_role()`, `owns_property()` functions) + client-side `role-guard.tsx` | Not started | PARTIALLY IMPLEMENTED (server-side already solid; client guard needs Kotlin equivalent) | Mirror role-guard logic in Jetpack Navigation graph (role-gated start destinations); real enforcement already exists in RLS, so Android just needs to respect roles for UX, not security |
| 5 | Secure Admin flow, never public, server-side authorization, audit logged | `admin-access.tsx` route + `admin-access.functions.ts` (server fn) + `admin-access.server.ts`: takes a code, `timingSafeEqual` hash compare against `ADMIN_ACCESS_CODE` env var (server-only), grants `admin` role via service-role client, logs to `audit_logs` | Not started | MISSING | Recreate as a hidden/deep-link-only screen calling the same server-side mechanism (cannot be a Supabase Edge Function called with anon key + code — must stay server-secret-gated exactly as web does) |
| 6 | Google SSO via Credential Manager | Not present in web app at all (web has no Google button in what I inspected) | Not started | MISSING (this is explicitly "PoE only" per §3.2, i.e., Android-only requirement, not expected on web) | Implement Credential Manager + Google ID token → Supabase `signInWithIdToken` |
| 7 | Student dashboard, bookings, favourites, payments | `student.index.tsx`, `student.bookings.tsx`, `student.favourites.tsx`, `student.payments.tsx`, `student.onboarding.tsx` | Not started | MISSING | Core Student feature set — see Phase 5 plan below |
| 8 | Landlord dashboard, property/room management, booking approval | `landlord.index.tsx`, `landlord.properties.tsx`, `landlord.bookings.tsx`, `landlord.onboarding.tsx` | Not started | MISSING | Core Landlord feature set |
| 9 | Admin dashboard: landlord verification, property moderation, oversight | `admin.index.tsx`, `admin.landlords.tsx`, `admin.properties.tsx` | Not started | MISSING | Core Admin feature set |
| 10 | Properties: images, amenities, price, deposit, rooms, distance, map | `properties` + `property_images` + `rooms` tables; `property-card.tsx`; distance stored as `distance_km` column (pre-computed, not live-geocoded in what I've seen so far) | Not started | MISSING | Confirm whether distance is computed via Google Geocoding at listing-creation time or is static seed data — need to check landlord property-create flow before building Maps integration |
| 11 | Search & filters (province, city, university, rent, amenities) | `listings.index.tsx`, `data/locations.ts` | Not started | MISSING | Straightforward PostgREST query params via Retrofit |
| 12 | Reviews, moderated by admin | `reviews` table (public read, no `status` column — **note: schema has no pending/approved state for reviews**, contradicts §3.10 "queued with status = pending") | Not started | MISSING + spec/implementation gap | Flag: either add a `status` column via new migration to match §3.10, or treat review moderation as a documented "planned but not yet implemented" gap in the compliance report |
| 13 | Favourites | `favorites` table, `favorite-button.tsx` | Not started | MISSING | Simple join table CRUD |
| 14 | Booking workflow: pending→accepted/rejected→active, documents | `bookings` table has `status` enum (`pending,approved,rejected,cancelled,active,completed` — note: uses `approved` not `accepted` as the enum literal, differs slightly from §3.5 wording), `id_document_url`, `proof_of_registration_url`, `booking-dialog.tsx` | Not started | MISSING | Match Kotlin enum to actual DB enum (`approved`, not `accepted`) |
| 15 | Payments/invoices | `payments` table with `kind`(deposit/rent/fee) and `status` | Not started | MISSING | No PayFast/Stripe integration found yet in web code — likely UI-only/mocked in prototype; needs confirmation before Android claims "real payment processing" |
| 16 | Messaging, real-time, read receipts | `conversations` + `messages` tables, `REPLICA IDENTITY FULL` + added to `supabase_realtime` publication — **real Supabase Realtime is wired at the DB level** | Not started | MISSING | Use Supabase Realtime (websocket) from Kotlin, or poll — Realtime is genuinely available server-side, so this is a good candidate for actual real-time chat, not just push |
| 17 | Maintenance requests with photos, status timeline | `maintenance_requests` table (`open→in_progress→resolved→closed`), single `photo_url` (not multi-photo despite §2.2 "photo-based... carry photos" wording — schema only has one photo column) | Not started | MISSING + spec/implementation gap | Either add `maintenance_photos` child table to match the multi-photo ambition, or scope Android to one photo per ticket like the current schema |
| 18 | Notifications, real-time feed | `notifications` table, also added to realtime publication | Not started | MISSING (FCM layer entirely absent from web, expected — it's Android-only per §3.8) | Kotlin: FCM device registration + local Room-backed notification feed reading from `notifications` table |
| 19 | Audit logs on admin actions | `audit_logs` table, written to by `admin-access.server.ts`; only one action type actually logs today (`admin.access_granted`) | N/A (server-side) | PARTIALLY IMPLEMENTED on web | Ensure every admin PATCH from Kotlin (verify landlord, approve/reject property) also inserts an audit row — needs new server-side coverage, not just an Android concern |
| 20 | Settings: name/phone/photo/password, notification prefs, language | `settings.tsx` route exists; DB has `preferred_language` column (`en/zu/ve`, DB-constrained) but **no notification-preference columns found yet** in schema inspected so far | Not started | PARTIALLY IMPLEMENTED / gap | Language preference is real and persisted server-side — good. Notification toggle storage needs to be located or added |
| 21 | Multi-language: English, isiZulu, Tshivenda, all strings | Web only stores/display a `LanguageProvider` context + DB column; I found **no actual translated UI strings** in the web app (`language.tsx` sets `document.documentElement.lang` but there's no i18n string dictionary in the repo) | Not started | MISSING on both web and Android — this is a from-scratch translation job | This is the single biggest genuine gap: Android will need real `strings.xml` / `strings-zu.xml` / `strings-ve.xml` with every user-facing string translated. The web app gives us zero free translations to reuse. |
| 22 | Offline mode + outbox sync | Not applicable to web (PoE/Android-only per §3.9) | Not started | MISSING (expected — Android-only) | Room entities + WorkManager outbox, per §3.9 |
| 23 | Unit tests | Not inspected yet (need to check for a `tests/` dir on web side — not relevant to Kotlin tests anyway) | Not started | MISSING | JUnit + MockK + Room in-memory DB tests |
| 24 | GitHub Actions CI | No `.github/workflows` found in the web repo | Not started | MISSING | New workflow file for the Android repo |

## 2. Summary of genuine gaps between the PDF spec and the actual web app

These aren't Android tasks — they're places where Part 1's narrative and the shipped web prototype disagree, worth knowing before Part 2 write-up so your compliance report doesn't overclaim:

1. **No custom REST API exists** — web calls Supabase directly (see §0 above).
2. **No PayFast/Stripe integration found yet** in the code I've read — needs confirming before claiming real payment processing anywhere.
3. **Reviews have no moderation status column** — contradicts §3.10's "queued as pending."
4. **Maintenance tickets store one photo, not multiple** — contradicts §2.2's "carry photos" (plural) framing.
5. **No i18n string dictionary anywhere** — the "language selector" is a persisted preference with no actual translations behind it yet, on web or Android.
6. **Notification-preference storage** not yet located in schema — needs confirming.

None of these are blockers — they're just things worth deciding on (add migrations vs. document as known limitation) before Phase 16's compliance audit, so it's accurate rather than optimistic.

## 3. Recommended real plan for phases 2–17

Given the actual size of this (properly done, this is a multi-week final-year-style build), I'd suggest we work it as a sequence of concrete deliverables rather than one mega-response:

1. **Android project scaffold** — Gradle setup, MVVM package structure, Retrofit/Supabase client, DI (Hilt), navigation graph with role-based start destinations, Material 3 theme matching RentEase navy/gold branding.
2. **Auth vertical slice** — Student + Landlord registration, login, Google SSO, encrypted token storage, role routing. (This alone is a substantial, testable chunk.)
3. **Student core** — search/filters, listing detail, booking flow, favourites.
4. **Landlord core** — property/room CRUD, booking approval.
5. **Admin core** — verification queue, moderation, secure admin-access screen.
6. **Messaging + notifications** — Supabase Realtime chat, FCM.
7. **Offline + sync** — Room cache + outbox + WorkManager.
8. **Localization** — full `strings.xml` set × 3 languages.
9. **Tests + CI** — JUnit suite, GitHub Actions workflow.
10. **Compliance report + README** — final Phase 16/17 writeup, done honestly against what actually got built and what actually ran (I can't execute Gradle here — no network — so build verification has to happen on your machine and I'll give you exact commands to run).

Where would you like to start — the project scaffold + auth slice (item 1–2), or do you want to settle the open questions in §0 and §2 first?
