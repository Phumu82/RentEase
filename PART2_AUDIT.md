# RentEase — Part 2 Final Audit

Date of audit: 2026-09-22 (environment date). No Gradle/Android SDK/Maven
access was available in the environment used to perform this audit (all
outbound network requests to Gradle's, Google's and Maven Central's
distribution servers were blocked). Everything in this document was verified
by reading source files, cross-checking signatures between callers and
implementations, and running custom static-analysis scripts (string-resource
parity/placeholder checks, unresolved-import detection, bracket-balance
checks, YAML validation). **None of it constitutes a successful build.**
Anywhere this document says COMPLETE, it means "complete and consistent by
static inspection," not "compiles and runs."

## Fixes made in this audit

See `README.md` for the full write-up. Summary:

1. Gradle wrapper: still missing (cannot fabricate a binary jar). Added
   `scripts/bootstrap-gradle-wrapper.sh` to generate the official one, and
   made CI fail fast with a clear message if it's absent.
2. GitHub Actions: rewritten to cover every required step, with non-secret
   build values from repo variables and a Firebase placeholder fallback.
3. Firebase/FCM: verified manifest/service/channel wiring; added a
   preferences check so pushes honour Settings; documented exactly what a
   developer must add locally (`app/google-services.json`).
4. README: fully rewritten (this repo's `README.md`).
5. Settings: rebuilt — back button, editable name/phone, language, three
   notification toggles, real logout.
6. User-defined features: implemented and tested — comparison, gamified
   verification progress. Analytics was already real. Maintenance photo
   tracking is landlord-only (partial). Saved-search digests: not built.
7. Google Maps: confirmed unused; dependencies removed.
8. Localization: ~30 hardcoded strings moved to resources; added a test that
   guards against regressions; new isiZulu/Tshivenda text needs native review.
9. Testing: 65 original tests preserved unmodified (except one added import
   fixing a latent compile error), 35 added, 100 total — **none executed**.
10. REST API: confirmed Supabase PostgREST/GoTrue/Storage + one Edge
    Function, documented as such (not Node/Express). Fixed a converter bug
    that would have crashed every `Map<String, Any?>` request at runtime.
11. Build verification: **not performed** — no toolchain access.
12. This matrix.

Also fixed, not explicitly requested but blocking: Material3 experimental
API opt-ins (compile error), missing imports in 3 screens (compile error),
invalid theme parent (resource-linking error), wrong XML namespace in the
launcher icon (aapt2 error), missing `proguard-rules.pro` (release build
failure), unhandled `IOException` on requests with no network (crash), an
empty `SUPABASE_URL` default (crash at launch), session restore never being
called (relaunch always showed role-select), logout never clearing tokens.

## Compliance matrix

| Part 2 Requirement | Evidence in Project | Status | Tested |
|---|---|---|---|
| Mobile app | Native Kotlin/Compose app, 3 roles (student/landlord/admin), single `NavHost` | BUILD VERIFICATION REQUIRED | No |
| Sign in | Email/password (student + landlord flows), Google SSO via Credential Manager + GoTrue id_token grant, session restore on launch, real logout | BUILD VERIFICATION REQUIRED | 9 unit tests (`AuthRepositoryImplTest`) + 5 HTTP contract tests (`RestApiContractTest`), not run |
| Settings | Profile edit (name/phone), language (persisted server-side), 3 notification toggles, logout — rebuilt this audit | BUILD VERIFICATION REQUIRED | 4 new unit tests (`ProfileUpdateTest`) + 8 existing (`LanguagePersistenceTest`), not run |
| REST API | Supabase PostgREST + GoTrue + Storage + one Edge Function (`admin-access`). Confirmed NOT Node/Express. | PARTIAL — architecture confirmed from code; the *hosted* Supabase project's live state (schema completeness, RLS policies, Edge Function deployment) could not be verified from this environment | No |
| REST API integration | Retrofit interfaces (`SupabaseRestApi`, `SupabaseAuthApi`, `SupabaseListingsApi`, `SupabaseLandlordApi`, `SupabaseAdminApi`, `SupabaseStorageApi`) + kotlinx.serialization + a new `Map<String,Any?>` converter (fixed a runtime crash) | BUILD VERIFICATION REQUIRED | 5 real-HTTP contract tests via MockWebServer, not run |
| User Defined Feature 1: Landlord analytics | Pre-existing; `AnalyticsViewModel`/`AnalyticsScreen`, real computed data from live API | BUILD VERIFICATION REQUIRED | Pre-existing repository tests, not run |
| User Defined Feature 2: Side-by-side comparison | New this audit: `domain/model/PropertyComparison.kt`, `ComparisonViewModel`, `ComparisonScreen`, entry point (checkboxes + "Compare (n)" bar) added to `ListingsScreen` | BUILD VERIFICATION REQUIRED | 7 new unit tests (`PropertyComparisonTest`), not run |
| User Defined Feature 3: Gamified verification progress | New this audit: `domain/model/VerificationProgress.kt`, `VerificationProgressCard` on the landlord dashboard, derived from real profile/property status | BUILD VERIFICATION REQUIRED | 7 new unit tests (`VerificationProgressTest`), not run |
| UI | Compose screens for all 3 roles; Material3 theme; localized strings | BUILD VERIFICATION REQUIRED | No |
| GitHub | Repo with `.github/workflows/android.yml`; wrapper not yet committed (see limitations) | PARTIAL | No |
| README | Fully rewritten to reflect current implementation, with an explicit "not build-verified" notice | COMPLETE (as a document) | n/a |
| Automated testing | 100 JUnit/MockK/Robolectric unit tests under `app/src/test/`; none under `app/src/androidTest/` | BUILD VERIFICATION REQUIRED | 0 of 100 run |
| Demonstration video | Not created — outside the scope of this audit | MISSING | n/a |

## Test count

| | Count |
|---|---|
| Pre-existing tests (preserved) | 65 |
| Tests added this audit | 35 |
| **Total** | **100** |
| Passed | Unknown — not run |
| Failed | Unknown — not run |
| Could not run | All 100 (no JVM/Gradle/Android toolchain reachable) |

## DO THESE BEFORE SUBMISSION

1. Run `scripts/bootstrap-gradle-wrapper.sh` locally (needs a real Gradle
   8.9+ install) and commit `gradlew`, `gradlew.bat`,
   `gradle/wrapper/gradle-wrapper.jar`.
2. Create `local.properties` from `local.properties.example` with your real
   `SUPABASE_URL`, `SUPABASE_ANON_KEY`, `GOOGLE_WEB_CLIENT_ID`.
3. Download your real `app/google-services.json` from Firebase and place it
   at that path (keep it uncommitted); optionally add its base64 as the
   `GOOGLE_SERVICES_JSON_B64` GitHub Actions secret so CI builds a fully
   working APK.
4. Run `./gradlew testDebugUnitTest lintDebug assembleDebug` locally and fix
   whatever fails — expect some issues, since this exact code has never been
   compiled.
5. In Supabase: confirm the full schema exists (this repo only ships two
   incremental migrations), apply them, deploy the `admin-access` Edge
   Function, set `ADMIN_ACCESS_CODE`, enable the Google Auth provider with
   your `GOOGLE_WEB_CLIENT_ID`.
6. Turn off "Confirm email" in Supabase Auth settings if you want to demo
   sign-up without an email round-trip.
7. Create your own demo accounts — none exist. At minimum one Student, one
   Landlord, and one Admin (via the hidden `admin_access` route + your
   `ADMIN_ACCESS_CODE`).
8. Have a native isiZulu and Tshivenda speaker review the ~90 translation
   keys added during this audit (listed in `README.md`).
9. Decide what to do about the two features that are not fully implemented
   (saved-search digests: missing; student-submitted maintenance tickets:
   missing) — either build them or make sure your Part 2 presentation only
   claims the three features that are genuinely complete (analytics,
   comparison, verification progress).
10. Record the demonstration video.
