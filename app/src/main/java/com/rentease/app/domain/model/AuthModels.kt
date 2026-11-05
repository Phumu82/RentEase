package com.rentease.app.domain.model

/**
 * The authenticated user as the rest of the app sees it — already resolved to a role,
 * regardless of whether they signed in with email/password or Google SSO.
 */
data class AuthUser(
    val id: String,
    val email: String,
    val fullName: String,
    val role: UserRole,
    val phone: String?,
    val profilePhotoUrl: String?,
    val preferredLanguage: AppLanguage,
    val isVerified: Boolean,
    val notificationPreferences: NotificationPreferences = NotificationPreferences()
)

data class NotificationPreferences(
    val bookingUpdates: Boolean = true,
    val newMessages: Boolean = true,
    val maintenanceUpdates: Boolean = true
)

/**
 * Maps an incoming push/notification `category` (booking | message | maintenance |
 * anything else) to the matching Settings toggle. Unknown/system categories are
 * always allowed so announcements are never silently dropped.
 */
fun NotificationPreferences.allowsCategory(category: String): Boolean = when (category) {
    "booking" -> bookingUpdates
    "message" -> newMessages
    "maintenance" -> maintenanceUpdates
    else -> true
}

enum class AppLanguage(val dbCode: String) {
    ENGLISH("en"),
    ISIZULU("zu"),
    TSHIVENDA("ve");

    companion object {
        fun fromDb(code: String?): AppLanguage =
            entries.firstOrNull { it.dbCode == code } ?: ENGLISH
    }
}

/**
 * Payload for the Student-specific registration form.
 * Fields map onto `profiles` (fullName/email/phone) + a child `student_profiles`
 * row (studentNumber/universityId) created right after sign-up.
 */
data class StudentRegistrationInput(
    val fullName: String,
    val email: String,
    val password: String,
    val phone: String,
    val studentNumber: String,
    val universityId: String,
    // Optional preference block — mirrors the web signup form. All nullable so the
    // minimal registration (and existing callers/tests) keep working unchanged.
    val course: String? = null,
    val faculty: String? = null,
    val yearOfStudy: Int? = null,
    val budgetMax: Double? = null,
    val preferredRoomType: String? = null,
    val earliestMoveIn: String? = null,
    val needsFurnished: Boolean = false,
    val preferredProvince: String? = null,
    val preferredCity: String? = null,
    val preferredUniversityId: String? = null
)

/**
 * Payload for the Landlord-specific registration form (separate from Student per §3.1/Phase 6).
 * Fields map onto `profiles` (fullName/email/phone/companyName) + a child
 * `landlord_profiles` row. Like the web signup, NO documents are collected here:
 * the verification documents are uploaded after sign-in from the landlord
 * onboarding screen, because the private storage buckets only accept
 * authenticated uploads under the user's own id folder.
 */
data class LandlordRegistrationInput(
    val fullName: String,
    val email: String,
    val password: String,
    val phone: String,
    val businessName: String? = null,
    val idNumber: String? = null
)

sealed interface AuthOutcome {
    data class Success(val user: AuthUser) : AuthOutcome
    data class Failure(val message: String) : AuthOutcome
}

/**
 * Result of a sign-up call. Separate from [AuthOutcome] because sign-up has a third
 * legitimate result: when the Supabase project requires email confirmation the account
 * exists but there is no session yet, and the person must open the emailed link first.
 */
sealed interface RegistrationOutcome {
    data class Success(val user: AuthUser) : RegistrationOutcome
    data class ConfirmEmail(val email: String) : RegistrationOutcome
    data class Failure(val message: String) : RegistrationOutcome
}
