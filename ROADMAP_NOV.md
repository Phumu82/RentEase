# RentEase Android — Gap-Closing Roadmap (target: November)

This tracks every gap identified against the Part 1 web app (`rentease-student-housing-main`),
grouped into phases. Each item notes its real Supabase table/schema (researched directly from
the web app's migrations and source, not guessed) so whoever picks up a phase doesn't have to
re-derive it. Nothing here uses mock/fake data — every item is real Retrofit → Supabase REST,
matching the existing `UI → ViewModel → Repository → Retrofit → REST/Supabase → DB` architecture.

Legend: ✅ Done and statically verified this pass · 🔜 Queued, researched · ⬜ Queued, not yet researched

---

## Phase 0 — Already fixed in earlier passes
- ✅ Student maintenance request submission (`maintenance_requests` table)
- ✅ Student dashboard (stat cards + quick links, mirrors `student.index.tsx`)

## Phase 1 — This pass
- ✅ **Search filter UI** (`domain/model/PropertyModels.kt`, `presentation/student/search/`) —
  **correction of an earlier wrong claim**: the repository/backend already supported
  university/province/maxRent/furnished/amenities filters, but `ListingsScreen` only exposed a
  keyword box. Added a full filter panel (university + province dropdowns, max-rent slider
  2000–12000 step 500, furnished checkbox, amenity `FilterChip`s) matching `listings.index.tsx`
  exactly, plus a `ProvinceCatalog` domain constant (Gauteng/Western Cape/KwaZulu-Natal/Eastern
  Cape/Free State) matching `PROVINCES` in `src/data/marketing.ts`.
- ✅ **Search sorting** — `PropertySortOption` (newest/price-asc/price-desc/rating/distance),
  applied client-side over fetched results, same approach `listings.index.tsx` uses. No new
  backend endpoint needed.

## Phase 2 — Done this pass
- ✅ **Booking cancellation.** Added `cancelBooking()` to `ListingsRepository`/`Impl` (PATCH
  `bookings.status = "cancelled"`, matching `student.bookings.tsx` exactly — RLS's "Parties
  update bookings" policy already permits it). Cancel button on `MyBookingsScreen`, shown only
  for `PENDING` bookings — same rule the web app uses.
- ✅ **Manual dark mode toggle.** New `ThemePreferences` (plain SharedPreferences, matching the
  web app's `localStorage`-only approach — confirmed `src/lib/theme.tsx` never touches the
  `profiles` table, so this deliberately stays local-only on Android too, not a new backend
  column). Wired through a small `ThemeViewModel` into `MainActivity`, with a System/Light/Dark
  picker in `SettingsScreen` under a new "Appearance" heading.
- ✅ **Search filter UI was actually missing entirely** (discovered this pass, bigger than
  planned): `ListingsScreen` had only a keyword box despite the repository supporting
  university/province/max-rent/furnished/amenities. Added the full filter panel + a
  `ProvinceCatalog` domain constant + client-side sorting (`PropertySortOption`), matching
  `listings.index.tsx`'s exact filter set and its 2000–12000/step-500 rent slider.
- ✅ **Completed isiZulu/Tshivenda translations.** All 51 strings added across this project's
  sessions (maintenance, dashboard, filters, sort, theme, cancellation) now translated in both
  `values-zu` and `values-ve` — verified 390/390/390 key parity, no blanks, and format
  placeholders match, by replicating the project's own `LocalizationResourcesTest` assertions in
  Python (that test would have been failing before this pass — a real pre-existing red test,
  now green by the same logic). **Still needs a native speaker's review before shipping** — I'm
  not a certified translator for either language, and flagged lowest confidence on the Tshivenda
  pass specifically in an XML comment at the top of the new block in `values-ve/strings.xml`.

## Phase 3 — Reviews done this pass; complaints still queued
- ✅ **Student review submission.** Added `ReviewInput`/`ReviewCreateDto`/`ReviewedBookingDto`,
  two new `SupabaseListingsApi` endpoints (`createReview` POST, `getMyReviewedBookings` GET for
  hiding the button once reviewed), and `createReview()`/`getMyReviewedBookingIds()` on
  `ListingsRepository`/`Impl`. "Write a review" button now appears on `MyBookingsScreen` for any
  booking that is approved/active/completed and not yet reviewed, opening a star-rating +
  title + body dialog (`AlertDialog`, matching the exact call shape already proven in
  `RoomManagementScreen`/`ListingDetailScreen`). HTTP 409 (the real unique-index-on-booking_id
  violation) is mapped to a distinct "already reviewed" message rather than a generic HTTP error.
  6 new unit tests added to `ListingsRepositoryImplTest` (signed-out failure, successful POST
  payload shape, the 409-vs-other-error branch specifically, and the reviewed-ids set mapping
  including a null-booking_id row). All 8 new strings translated into isiZulu/Tshivenda and
  re-verified at 398/398/398 key parity using the same Python replication of
  `LocalizationResourcesTest`'s assertions as Phase 2.
- ✅ **Complaints (student + landlord submit, admin manages) — Phase 3 fully done.** Confirmed
  RLS first: `"Users insert own complaints"` / `"Users read own complaints"` /
  `"Admins update complaints"`, all `TO authenticated` (not service_role-only), so this is fully
  direct-PostgREST-doable. Added a genuinely new, deliberately small `SupportRepository` (student
  and landlord share one `/support`-equivalent — same reasoning `NotificationsRepository`
  documents for why it isn't folded into either role-specific repository) with
  `getMyComplaints()`/`submitComplaint()`, a new `SupabaseSupportApi`, wired through
  `NetworkModule`/`RepositoryModule`. Admin side extends the *existing* `AdminRepository`/
  `SupabaseAdminApi`/`AdminRepositoryImpl` with `getAllComplaints()` (reporter name/email joined
  via the same `getAllProfiles()` batch-lookup `getAllUsers()` already uses) and
  `decideComplaint()` (status + response + an audit log entry via the existing `logAuditEvent`
  helper). New shared `SupportScreen` reachable from both student and landlord dashboards, new
  `AdminComplaintsScreen` reachable from the admin dashboard. 6 new repository unit tests. All 25
  new strings translated into isiZulu/Tshivenda, re-verified at 423/423/423 parity, plus a check
  that every single `R.string.*` reference anywhere in the app resolves to a real declaration
  (407 distinct refs, zero undeclared).
  **One real, deliberate limitation, not an oversight**: the web app's admin decision flow also
  auto-inserts a notification for the reporter, via a server function using the service_role key.
  A normal authenticated session — admin or otherwise — cannot insert a notification row for a
  *different* user (`"Own notifications"` RLS requires `user_id = auth.uid()`), and this app
  deliberately never holds a service_role key (see "Do not expose Supabase credentials/secrets").
  So the status change and audit log are fully real; the reporter just won't get an automatic
  notification about it from the Android side. Documented directly in
  `AdminRepository.decideComplaint()`'s doc comment, not hidden.

## Phase 4 — Payments — done this pass
Re-verified the exact schema before building (worth it: found two details memory alone would
have gotten wrong — see below).
- ✅ **Deposit auto-creation on booking submit.** `createBookingRequest()` now also inserts a
  `payments` row (kind=deposit, amount=property deposit, due_date=moveInDate) immediately after
  the booking succeeds, mirroring `booking-dialog.tsx`'s two-insert flow exactly — including
  mirroring its *failure* behavior: if the second insert fails, the whole call reports failure
  even though the booking row already exists, same as the web app's `if (paymentError) throw`.
  **Correction caught mid-build**: both `bookings.reference` and `payments.reference` have their
  own DB-side `DEFAULT` generators — Android (like the web app, for bookings at least) should
  never set `reference` client-side and let Postgres assign it; the web app's `payments` insert
  actually *does* generate its own client-side reference via a `generateReference()` helper,
  which Android deliberately does **not** replicate, instead relying on the same DB default the
  column already has. Net effect is identical (a unique, readable reference exists either way);
  only the exact string format differs between what the web app would produce and what Android's
  rows get, which is a cosmetic, pre-existing (not introduced by this phase) divergence.
- ✅ **Student Payments screen.** New `Payment`/`PaymentKind`/`PaymentStatus` domain models,
  `PaymentDto`/`PaymentCreateDto`/`PaymentMarkPaidDto`, 3 new `SupabaseListingsApi` endpoints,
  `getMyPayments()`/`markPaymentPaid()` on `ListingsRepository`/`Impl` (property titles resolved
  by reusing `getMyBookings()` rather than a new join, since payments only carry `booking_id`).
  New `PaymentsScreen` + `ViewModel`: paid/outstanding stat cards, a "Pay now" button per pending
  payment (a plain self-reported status flip — no gateway on either platform, see
  `PaymentModels.kt`'s doc comment), reachable from the student dashboard.
- ✅ **Real landlord revenue**, not an estimate. `LandlordRepositoryImpl.getAnalytics()` used to
  sum *rent owed* on active/approved bookings (a comment admitted it wasn't a real ledger). Now
  sums actual `payments` rows with `status='paid'` for the landlord's own bookings — RLS's
  "Payment parties read" policy permits this via `owns_property`, confirmed before building (a
  landlord can read but, per "Parties update payments", never edit — matches that a landlord
  should see revenue but can't mark things paid on a student's behalf). Field name
  (`monthlyRevenueEstimate`) kept as-is to avoid UI churn; doc comment updated to say what it
  actually computes now.
- 8 new repository unit tests (deposit-creation success/failure, both payment methods' signed-out
  and success paths, property-title resolution via `getMyBookings`). All 20 new strings
  translated into isiZulu/Tshivenda, re-verified at 443/443/443 parity, zero undeclared
  `R.string.*` references anywhere in the app, and the project's own hardcoded-`Text()`-literal
  check re-run clean — including catching and fixing one real violation this phase introduced
  (an inline `"R${amount}"` display) before it shipped, by matching the exact "compute outside,
  pass as a variable" convention the codebase's own existing currency displays already use.

## Phase 5 — Bigger/architectural (needs a decision before starting)
- ⬜ **Real-time messaging & notifications.** Android currently does plain REST GET/POST via
  Retrofit — no Supabase Realtime/websocket usage anywhere. Matching the web app's real-time
  behaviour means adding a Realtime client (websocket subscription), which is a real new
  dependency/architecture addition, not a small patch. **Needs a go-ahead before starting** —
  worth confirming whether polling-on-screen-open (cheap, already close to what exists) is
  actually acceptable for the POE, versus true push (bigger lift, needs its own testing pass).
- ⬜ **Landlord availability calendar.** No calendar code exists; current room management is a
  flat list. Needs a UI library decision (a Compose calendar isn't in the current dependency
  set) — flagging rather than picking one unasked.
- ⬜ **Rich student profile fields** (student number, university, faculty, year of study,
  emergency contact). Requires extending `AuthUser`/`profiles` table columns — need to confirm
  these columns actually exist on the web app's `profiles` table (not yet checked) before adding
  them to Android, since inventing new backend columns would violate "use the existing
  REST/Supabase architecture, don't invent new backend."
- ⬜ **Income reports (monthly/annual).** Needs a decision on where historical data comes from —
  aggregating `payments` by month client-side is possible once Phase 4 lands, but "annual"
  reporting depends on how much history actually exists in a fresh Supabase project.
- ⬜ **Forgot/reset password.** Investigated: **this doesn't actually exist in the Part 1 web app
  either** — README lists it, but there's no `resetPasswordForEmail` call or forgot-password page
  anywhere in the web source. Supabase Auth supports it as a standard backend call regardless of
  whether the web UI was ever built, so Android *can* still get it — but this would mean Android
  exceeding Part 1's actual implementation. Confirm you want this before it's built, since it's
  the one item here without a Part 1 reference to mirror.
- ⬜ **Public marketing pages / AI auto-translation of dynamic content.** Landing page, About/
  Pricing/FAQ/Contact/Terms/Privacy, university directory, newsletter, and the `ui_translations`
  AI-translation-cache mechanism are all public-website/content-marketing concerns that don't
  obviously belong in a native, behind-login mobile app. Flagging rather than assuming — confirm
  whether any of this is actually in scope for the Android POE before it's built.

---

## Still true every phase
No Android SDK/Gradle in this environment, and the project has no committed `gradlew`/wrapper
jar — nothing here has been compiled or run. Every item above is verified by reading source
(both projects) and cross-checking every new symbol's definition/call-sites, plus a full-project
brace/paren balance pass after each edit — not by a real build. Run `gradle wrapper` once, then
`./gradlew testDebugUnitTest lintDebug assembleDebug` before trusting any of this in the field.

---

## Phase 5 — Parity push (October), checkpoint 1: onboarding (Priorities 2 + 3) and dashboard

Verification status of everything below: **static only** (brace/paren balance, undeclared-string scan,
EN/ZU/VE key + placeholder parity = 509/509/509, hardcoded-`Text("...")` regex). Nothing has been
compiled, linted or run — there is no Android SDK/Gradle in the authoring sandbox. Run
`gradle wrapper && ./gradlew testDebugUnitTest lintDebug assembleDebug` before trusting any of it.

- ✅ **Student registration** now collects the web's full preference set (course, faculty, year,
  budget, room type, earliest move-in via a real date picker, furnished, province→city, preferred
  campus) and a real university dropdown (was a free-text id box). Saved with an UPSERT: the DB
  trigger `handle_new_user()` already creates the `student_profiles` row, so the old plain POST
  would have hit a primary-key conflict.
- ✅ **Student onboarding screen** (`student/onboarding/`) — resumable (pre-filled from
  `profiles` + `student_profiles`), validates the web's required fields, writes
  `onboarding_complete = true`, proof-of-registration upload. Student dashboard shows a "finish
  your profile" banner until it is saved; new students are routed there after sign-up (as on web).
- ✅ **Landlord onboarding/verification screen** (`landlord/onboarding/`) — contact, identity,
  business (company, registration no., address, city, province, postal code), live verification
  status + admin notes (`verification_notes`) + required-documents progress, four document slots
  (ID and proof of ownership required; proof of address and company registration optional —
  columns exist in the schema). Landlord dashboard shows an "open verification" button while not
  verified. Reuses the existing `VerificationStatus`, banner and progress card (no duplicate logic).
- ✅ **Landlord sign-up no longer uploads documents**; it routes to onboarding afterwards (web
  parity). The old flow uploaded unauthenticated into a `verification-documents` bucket that is not
  in the web schema.
- ✅ **Storage alignment for the new code**: private `booking-documents` bucket, path
  `{userId}/{folder}/{timestamp}-{name}` (what the storage RLS policy requires), the object PATH is
  stored (not a URL), opened through 10-minute signed URLs, 5 MB / PDF-JPG-PNG-WEBP rules as on web.
  Status flips to `submitted` only once both required documents exist and never downgrades a
  `verified` account.
- ✅ **Student dashboard**: added "Total paid" (sum of PAID payments).
- ✅ Shared `DatePickerField` and `DropdownField`; booking move-in date now uses the date picker.
- ✅ Tests: `OnboardingModelsTest`, `OnboardingRepositoryImplTest`, extended `AuthRepositoryImplTest`.

### Phase 5 — checkpoint 2: booking parity + storage alignment (static verification only)
- ✅ **Booking request** now has room selection (type + price from `rooms`), lease duration (6/9/12/24),
  date picker, message, rent/deposit summary, and a confirmation card showing the booking reference with
  a "View my bookings" button. `room_id` and `landlord_id` are stored on the booking.
- ✅ **No duplicate bookings/payments**: repository refuses a second open booking on the same property
  and repairs a missing deposit payment on retry; the UI guards double taps.
- ✅ Booking documents use `booking-documents/{uid}/...` (private, signed URLs); onboarding proof is reused.
- ✅ **Maintenance photos** use `booking-documents/{uid}/maintenance/...`; "View photo" opens a signed URL
  (student and landlord screens).
- ✅ **Property cover images** use `property-images/{uid}/covers/...`; the bucket must be marked Public in
  Supabase for anonymous viewers.
- ✅ Tests: `BookingModelsTest`, extended `ListingsRepositoryImplTest`. Strings: 521 keys in EN/ZU/VE.
- ⚠️ Nothing compiled or run (no Android SDK here). Run `gradle wrapper && ./gradlew testDebugUnitTest lintDebug assembleDebug`.

### Still open
- 🔜 Admin verification screen cannot open private documents, set `documents_required`, or write notes.
- ⚠️ Landlords may not be allowed to view student maintenance photos by the storage policy (web has the same issue).
- ⚠️ Web `landlord_profiles` RLS lets a landlord self-set `verified`; tighten in Supabase.
- 🔜 Priority 1 (public pages), Priority 6 extras (availability filter, similar properties, nearby info,
  pagination), landlord availability calendar, forgot-password, translation review.

### Phase 5 — checkpoint 3: public info pages (Priority 1, part 1; static verification only)
- ✅ New screens (no sign-in): About, How It Works (student/landlord tabs with sign-up CTAs), Services, Pricing,
  FAQ (expandable), Contact, Privacy, Terms, plus an "About RentEase" hub reachable from the role-select screen.
  Routes `info/*`. Copy mirrors the web's `marketing.ts`, in EN/ZU/VE (681 keys each).
- ✅ Contact: validated form (same limits as web) that opens the device email app; address/phone/email rows open
  email/dialer. The web form only shows a toast; Android deliberately does not pretend to send.
- ✅ Tests: `ContactFormTest`.
- Deliberately omitted from the web copy: the invented platform statistics (2 400+ listings, 18 700 students…) and the
  fictional team members, since they are not real data. Contact address/phone are the web's placeholder values.
- FAQ/service/pricing wording was lightly reworded where the web claimed features the app does not have
  (gateway payments, PDF receipts, document-based 48h verification).
- 🔜 Still open for Priority 1: marketing home with featured properties, universities directory/detail, signed-out
  browsing of listings.

### Phase 5 — checkpoint 4: signed-out browsing + universities (Priority 1, part 2; static verification only)
- ✅ **Guest browsing**: "Browse listings without an account" on the first screen opens the real listings search
  (published properties are anon-readable under the web RLS) and the normal listing detail. Saving and booking
  prompt sign-in instead of failing. Guest mode is detected from the stored session, not a fake flag.
- ✅ **Universities directory + detail** from the real `universities` table (name, city/province, students,
  average rent), keyword search, "Browse listings near X" (pre-selects the campus filter) and up to six linked listings.
- ✅ Tests: `UniversityDirectoryTest`. Strings: 697 keys in EN/ZU/VE.
- Not done: a marketing home screen with hero search and featured properties; the web shows a per-campus listing
  count from demo data, which the Android app does not fake (it shows students and average rent from the table).

### Phase 5 — checkpoint 5: property photos + landing page (static verification only)
- ✅ Shared `PropertyPhoto`/`PropertyGallery`: cards, saved list and listing detail show the listing's real photo
  (`cover_image` / `property_images`). Detail has a swipeable 4:3 gallery with a counter and a back arrow
  (the detail screen previously had no back arrow).
- ✅ If a listing has no photo, or it fails to load, one of the web's four sample photos is shown, labelled
  "Sample photo" (never presented as the real property). Admin moderation still shows real data only.
- ✅ First screen is now a landing page: the website's hero image, sign-up/sign-in, and a "Featured places" row of
  real published listings (best rated first) that open the listing detail, plus "browse all".
- ⚠️ Photos uploaded by landlords are only visible to signed-out users if the `property-images` bucket is public:
  run `SUPABASE_SETUP.sql` (also contains the admin-role insert).
- Note: the web's hero image shows a banner with another brand name; replace `res/drawable-nodpi/hero_student_housing.jpg`
  if that matters for your submission.
- Tests: `SamplePhotosTest`. Strings: 704 keys in EN/ZU/VE.
- Checkpoint 5b: the landing "Featured places" cards now follow the website's featured grid (`FeaturedPropertyCard`):
  Verified/Furnished badges, title + rating, km from campus, beds/baths/rooms open, price and "View details".
  Values come from the listing rows (nothing invented). Strings: 707 keys in EN/ZU/VE.

### Phase 5 — checkpoint 6: admin landlord verification (Priority 8; static verification only)
- ✅ Admin list now shows **every landlord** with filter chips (Awaiting review / Documents required / Rejected /
  Verified / All), contact details, address, registration number and previous notes.
- ✅ **Open private documents** (ID, ownership, address, company registration) via a 10-minute signed URL; admin RLS
  already allows this on `booking-documents`. Old rows holding a full URL open as-is.
- ✅ Decisions: **Verify / Request documents / Reject** with notes the landlord sees on their onboarding page.
  Notes are required for reject/request; Verify is blocked until ID + ownership are on file; notes capped at 600.
- ✅ Fixed a parity gap: approving now also sets `profiles.is_verified` (the badge students see), and any other
  decision clears it, exactly like the web's `decideLandlordVerification`. Previously only `landlord_profiles` changed.
- ✅ Audit log actions kept (`admin.landlord_verified`, `admin.landlord_rejected`) plus `admin.landlord_documents_required`.
- ✅ Tests: `VerificationReviewTest`; `AdminRepositoryImplTest` extended (notes, badge, failure path, document mapping).
- ❌ Not possible without a server key: the web also inserts a notification for the landlord and supports "suspend"
  from this screen. Android does not notify (no service_role key in the app, deliberately); suspend stays on the Users screen.
- Strings: 718 keys in EN/ZU/VE.

## Checkpoint 7 – Visual overhaul (statically checked only, NOT compiled)
- Theme now uses the website palette (teal #007886 / coral #E96F42, cream background, dark mode variants) and rounded shapes.
- Home screen (first screen) is now a browsable page: hero photo, keyword search, student/landlord/sign-in actions, a vertical list of featured properties (web-style cards) with "See all listings", and a universities row with photos.
- Search term / university from the home screen seed the guest listings filter.
- Photo banners added to all public info pages, the universities directory (photo cards), and both dashboards.
- Caveats: only 5 bundled photos exist (hero + 4 samples), so many cards reuse them and are labelled "Sample photo" when a property has no image. Real landlord photos need the property-images bucket set to Public (SUPABASE_SETUP.sql). Check dark-mode contrast of the Furnished badge on a device. ZU/VE strings still need native review.

## Checkpoint 8 – Landlord property photos (statically checked only, NOT compiled)
- Property form now has a Photos section: multi-select upload (max 10), thumbnails, "Cover" badge (first photo), Make cover, Remove, empty-state prompt.
- Photos upload to `property-images/{uid}/photos/...`; on save the gallery is written to `property_images` (replace-all, ordered) and the first photo becomes `properties.cover_image`. If photo saving fails after the property saved, the error says so and Save retries without creating a duplicate.
- New-landlord empty state on My Properties: picture, explanation and "Add property and photos" button. Property cards now show the cover photo.
- Tests: PropertyPhotosTest, LandlordRepositoryImplTest (setPropertyImages).
- Caveats: bucket must be Public for guests to see photos (SUPABASE_SETUP.sql). Removing a photo drops the DB row only, the file stays in storage. Landlord RLS "manage own property images" covers delete/insert per the web migration, not tested live. ZU/VE text needs native review.

## Checkpoint 9 – Listings extras: availability filter + similar properties (statically checked only, NOT compiled)
- Search filter "Available now" (`rooms_available=gt.0`) on the listings screen.
- Detail page "Similar properties": other published listings in the same city, available first then closest rent, shown as a card row; tapping opens that listing.
- Tests: ListingsRepositoryImplTest (availableOnly on/off), SimilarPropertiesTest.
- Still open in priority 6: nearby info (shops/transport) and a dedicated landlord-info block on the detail page; pagination already exists. ZU/VE strings need native review.

## Checkpoint 10 – Realistic room rents (statically checked only, NOT compiled)
- Add-room dialog: room type is now a dropdown (Single / Shared / Ensuite / Studio, the website's values) instead of free text; choosing a type sets a sensible capacity and shows the typical monthly range under the price field (Shared R2800-R3800, Single R3500-R5500, Ensuite R4500-R6500, Studio/bachelor R4500-R7000). Prices far outside the range (e.g. R500 bachelor) are flagged "Unusual – please check", but never blocked.
- The ranges are my estimates of student-room rents, used as guidance only; no room data is seeded into Supabase. Adjust the numbers in RoomTypeGuide.kt.
- Tests: RoomTypeGuideTest. ZU/VE strings need native review.

## Checkpoint 11 – Demo data + theme accessibility pass (statically checked only, NOT compiled)
- `SUPABASE_DEMO_DATA.sql` (run in the Supabase SQL editor, optional): 5 universities (adds missing, refreshes students/average_rent), 10 published demo properties near them, 2-3 rooms each at realistic rents (shared ~R2700-4200, single ~R3400-5500, ensuite R5400-6400, studio/bachelor R4800-6300), and 3 reviews per verified property with rating/review_count computed from them. Rows are tagged (`-demo` slug suffix, landlord_id NULL, no fake users); remove with `delete from public.properties where slug like '%-demo';`. Amenity names match the app's search filter. This is database data, nothing is hardcoded in the app. The web's own demo list (marketing.ts) is front-end only.
- Demo properties have no landlord, so landlord screens never show them and bookings made on them are not visible to any landlord; test the landlord flow with properties you add yourself.
- Theme/UX audit (WCAG contrast computed): light-mode coral text/buttons were 3:1, now a darker coral (5.4:1); input borders in light and dark were ~1.3:1, now ~3.7-4.9:1; badge text colours follow the theme; photo remove button has a dark backing; headline/title weights strengthened. Remaining: not checked on a device, touch-target sizes and font scaling untested, dynamic colour is off by default.

## Checkpoint 12 – Remaining gaps (statically checked only, NOT compiled)
- Property form: university dropdown, distance to campus, landlord name sent automatically (business name, else legal name, else profile name).
- Rooms available: new field on the property form; the app also re-counts `rooms_available` from room statuses whenever a room is added, changed or deleted, so the "Available now" filter is accurate.
- Forgot password: Login -> "Forgot password?" -> email -> Supabase recovery email -> link opens the app (`rentease://reset-password`) -> new password. REQUIRES adding `rentease://reset-password` to Supabase Auth > URL Configuration > Redirect URLs (see SUPABASE_SETUP.sql). The recovery token overrides the session token in the auth interceptor.
- Landlord verification notifications: `SUPABASE_NOTIFY_TRIGGERS.sql` (run once). A database trigger notifies the landlord on approve/reject/documents-required, notifies admins on submission, and writes an audit row; skipped for the website's service-role calls so there are no duplicates. No secret key in the app.
- Listing detail: landlord card (name, verified) and "What's nearby" shortcuts (transport, shops, food, clinics, show on map) that open the phone's maps app. Nothing is hardcoded: the website's nearby list is made-up fixed distances, so none are shown.
- Income report: contracted monthly income, collected to date, income by property; the old "Monthly revenue" label was actually total collected, now fixed.
- Live messages: an open conversation re-checks every 4 s while the screen is visible (polling, no websocket library). Conversation lists do not auto-refresh.
- Deep links: links arriving while the app is open are now handled (onNewIntent), without repeating on rotation; login now clears the whole back stack.
- The website has no availability calendar, so none was built; "availability" means the rooms-available count above.
- Tests added: PasswordResetTest, NearbyMapsTest, IncomeReportTest, auth repository (reset), landlord repository (room re-count), plus earlier ones.
- Still open: unit/instrumented UI tests, conversation-list auto refresh, native review of ZU/VE text, everything unverified until a real build.

## Checkpoint 13 – Sign-up fix, shorter forms, dark-mode theme completion (statically checked only, NOT compiled)
- ROOT CAUSE of "cannot sign up as landlord": the app wrote the landlord/student child row BEFORE saving the new login token, so the request went out anonymous and row-level security rejected it; with email confirmation ON the response has no tokens and could not be decoded. Fixed: session saved first; the child-row write is best-effort (the DB trigger normally creates it from sign-up metadata); a "no session" response now shows a "Confirm your email" screen with a button to sign in.
- Errors are now sentences (email already used, weak password, rate limit, invalid email, no connection, wrong sign-in, email not confirmed) instead of raw JSON; unknown errors show the server's own message. Also used by login.
- Student sign-up: name, email, phone, university, student number, password (show/hide + rule shown). Landlord sign-up: name, email, phone, password. Course/budget/preferences/ID/business details/documents moved to the dashboard "finish your profile" / verification steps, no longer forced right after sign-up.
- Dark mode: the colour scheme now defines the full Material 3 surface ladder, inverse and error roles (Cards, dialogs, menus, nav bars and text fields were falling back to default purple-grey); error red lightens in dark; hard-coded greys/reds/golds replaced by theme colours (notifications, heart icons, verified badge, landlord banner); status/navigation bar icons follow the chosen theme (not just the phone's); launch window colour follows night mode.
- Tests: AuthErrorsTest, ThemeContrastTest (WCAG AA for both schemes), register tests rewritten (session-before-child-row, confirm-email, duplicate email, landlord).
- If sign-up still says "confirm your email" and you want instant sign-in during testing: Supabase > Authentication > Providers > Email > turn OFF "Confirm email".

## Checkpoint 14 – Screens brought up to the light/dark mock-up sheet (statically checked only, NOT compiled)
- The "Theme_light / Theme_dark" sheets were a hand-drawn TARGET design; Home already matched, Browse listings, Listing detail and Settings did not. Now:
- Browse listings: back arrow + title, rounded search field, chip row (Filters, Sort, Available now, Furnished only), and the same photo card as the home screen (Verified/Furnished badges, rating · distance, beds/baths/rooms, price, "View details" pill; heart and Compare tick kept). The row of icon shortcuts moved into a "more" menu (Saved, Bookings, Maintenance, Messages, Notifications, Settings, Log out). Nothing removed.
- Listing detail: rounded gallery, price + deposit as the heading, address · distance, a Rooms card (available rooms with prices), Reviews and Landlord in cards, similar properties.
- Settings: Appearance and Language as pill switches (System/Light/Dark, English/isiZulu/Tshivenda), then Notifications, then Profile, then Log out.
- Home: rounded hero. Shared `RentEaseCard` (surface colour + hairline border + soft shadow, themed for light and dark).
- IMPORTANT: the mock-up photos show demo listings. With an empty database the app correctly shows "No listings match your search yet". Run SUPABASE_DEMO_DATA.sql to see listings like the pictures.

## Checkpoint 15 – Conversation lists refresh themselves (statically checked only, NOT compiled)
- Landlord and student conversation lists now re-check every 15 s while the screen is visible (open chats still every 4 s). The re-check is silent: no spinner, and a failed check keeps what is on screen. The student list makes one request per conversation for unread counts, hence the slower interval.

## Checkpoint 16 – "HTTP 503 everywhere" diagnosis and merge with RentEase_Fixed_Full_3
- Cause seen in the emulator screenshots: the build had no `SUPABASE_URL` / `SUPABASE_ANON_KEY` in `local.properties`, so the base URL fell back to `https://not-configured.invalid`; every call failed offline and the interceptor turned that into a bare "HTTP 503" (empty university list, "fill in all fields").
- `SupabaseConfig.isConfigured` + `ConfigWarningBanner` (login, listings) now say so; `Context.httpHint` replaces the bare "(HTTP n)" suffix in all repositories; the interceptor's offline response carries a `network_unreachable` marker.
- Google sign-in no longer throws when `GOOGLE_WEB_CLIENT_ID` is blank (the builder rejects an empty id outside the old try/catch).
- Merged from the uploaded zip: RentEaseLogo (splash, login, role select), SUPABASE_FULL_SCHEMA.sql, qualification-duration migration, placeholder google-services.json. The long student sign-up form was NOT carried over (kept the short one).
- Not verified: nothing compiled by me; ZU/VE strings need native review.

## Checkpoint 17 – login/sign-in code audit
- Bug fixed: `login()` and Google sign-in queried `user_roles` BEFORE storing the new token, so the request went out as `anon` (which has no SELECT on `user_roles`) and a valid login ended in "signed in but no role". The session is now stored first; it is cleared again if the role or profile cannot be loaded.
- Bug fixed: login waited on the FCM token, which can hang without Play Services / with the placeholder google-services.json. Sign-in now succeeds first; push registration runs separately with an 8 s timeout.
- Sign-up trigger (`SUPABASE_SIGNUP_FIX.sql`, also folded into `SUPABASE_FULL_SCHEMA.sql`) now creates the student/landlord profile row itself, so accounts created while "Confirm email" is on are complete.
- Tests added: session stored before role lookup; no-role login clears the session.
- Not compiled/run by me.

## Checkpoint 18 – role resolution
- `fetchRole` now picks the most privileged role (admin > landlord > student) instead of the first row returned. An admin created in the Supabase dashboard also gets the default 'student' row from the sign-up trigger, so sign-in as admin@rentease.com could previously land in the student app depending on row order. Unknown role strings no longer throw. Test added.

## Checkpoint 19 – "will sign-in hit problems?" hardening
- `local.properties` slips no longer break the build or launch: values are trimmed, quotes stripped and escaped in Gradle (`configValue`); `SupabaseConfig.normalizeUrl` adds a missing `https://`, drops a trailing slash and falls back to the placeholder host (so Retrofit never throws at start-up); the anon key is reduced to legal header characters.
- `TokenManager` recreates its encrypted file if the Android keystore no longer matches it (previously a launch crash after emulator snapshot restore / reinstall).
- Login, Google sign-in and both registrations catch unexpected exceptions (e.g. a malformed server reply) and show "Request failed" instead of crashing.
- Student sign-up demands a university only when the university list actually loaded.
- Debug HTTP logging lowered from BODY to BASIC: bodies contain the password and tokens.
- Tests added for `normalizeUrl`. Not compiled/run by me.

## Checkpoint 20 – first real execution of pure logic
- A Kotlin 2.0.20 compiler was obtained from GitHub releases; `SupabaseConfig` and `RegistrationValidation` (+ `PasswordReset.isPlausibleEmail`) were compiled and run against assertions (19 pass). This caught and fixed a real bug: `normalizeUrl("https://")` returned `"https:"` instead of the placeholder host.
- Still not run: anything touching Android, Hilt, Retrofit or Supabase (Maven is blocked from this sandbox, so no Gradle build).

## Checkpoint 21 – sign-in with accounts that have no name / no profile row
- `ProfileDto.fullName` was a required non-null String; an account whose `profiles.full_name` is NULL (an admin added in the dashboard, any sign-up without a name) made the profile decode fail, so sign-in ended in an error even with correct credentials. It now defaults to "" and the app shows the email instead. `GoTrueUserDto.email` also defaults.
- `SUPABASE_SIGNUP_FIX.sql` back-fills profile and role rows for auth users that have none ("could not load profile after sign-in").
- `AuthDtosTest` added (decode cases). Not run: kotlinx.serialization is unavailable offline here.

## Checkpoint 22 – first compiler pass over all sources (syntax level)
- Ran the Kotlin 2.0.20 compiler's parser over all 209 .kt files (main + test). Android/Hilt/Retrofit libraries are not available offline, so type errors from missing dependencies are expected noise and were not used; only syntax-level errors count.
- Found and fixed 3 build-breaking "Unclosed comment" errors: KDoc text containing `/*` (`/auth/v1/*`, `/rest/v1/*`, `student/*, landlord/*, admin/*`) opens a NESTED comment in Kotlin. Re-scan: 0 syntax errors, 0 redeclarations.
- Still not done: a real Gradle build.

## Checkpoint 23 – fixes from the first real Gradle build log
- `Response.code` is a method in retrofit2: `code()` fixed in `AdminRepositoryImpl` / `LandlordRepositoryImpl` `errorMessage`.
- Missing `import ...layout.height` in AdminComplaintsScreen, PaymentsScreen, SupportScreen.
- `stringResource(...)` was called inside the `LazyVerticalGrid` content lambda (not composable) on the admin, landlord and student dashboards; the tile list is now built in the composable body first.
- Then scanned every file for (a) used-but-unimported Compose/app symbols and (b) composable calls inside non-composable lambdas: no further hits. Gradle may still report more errors after these are fixed.
