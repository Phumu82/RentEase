package com.rentease.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SignUpRequestDto(
    val email: String,
    val password: String,
    // GoTrue stores this on auth.users.raw_user_meta_data; the DB trigger
    // handle_new_user() reads it to create the matching row in public.profiles.
    val data: SignUpMetadataDto
)

/**
 * Matches exactly what the DB trigger `handle_new_user()` reads off
 * `raw_user_meta_data`: full_name, role, phone, company_name. Anything else
 * (student_number, university_id, verification documents) is NOT set by the
 * trigger and must be PATCHed onto the profile row in a follow-up call after
 * sign-up completes — see AuthRepositoryImpl.completeStudentProfile /
 * completeLandlordProfile.
 */
@Serializable
data class SignUpMetadataDto(
    @SerialName("full_name") val fullName: String,
    val role: String,
    val phone: String? = null,
    @SerialName("company_name") val companyName: String? = null,
    // Read by the newer handle_new_user() trigger, which creates the student/landlord child row itself.
    @SerialName("legal_name") val legalName: String? = null,
    @SerialName("student_number") val studentNumber: String? = null,
    @SerialName("university_id") val universityId: String? = null
)

/**
 * Sign-up answer. With email confirmation OFF GoTrue returns a full session; with it ON it
 * returns just the user (id/email at the top level) and NO tokens, which must not crash the app.
 */
@Serializable
data class SignUpResponseDto(
    @SerialName("access_token") val accessToken: String? = null,
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("expires_in") val expiresIn: Long? = null,
    @SerialName("token_type") val tokenType: String? = null,
    val user: GoTrueUserDto? = null,
    val id: String? = null,
    val email: String? = null
) {
    /** The session when GoTrue issued one, otherwise null (account exists but email is unconfirmed). */
    fun toSession(): AuthSessionResponseDto? {
        val access = accessToken ?: return null
        val refresh = refreshToken ?: return null
        val account = user ?: return null
        return AuthSessionResponseDto(access, refresh, expiresIn ?: 3600L, tokenType ?: "bearer", account)
    }
}

@Serializable
data class PasswordGrantRequestDto(
    val email: String,
    val password: String
)

/**
 * Body for GoTrue's `grant_type=id_token`. NOTE: `provider` deliberately has NO
 * default value - kotlinx.serialization omits properties equal to their default
 * (encodeDefaults=false), which would silently drop `provider` from the request
 * and make GoTrue reject the sign-in. `nonce` is the RAW nonce; Google received
 * its SHA-256 hash (see LoginScreen), and GoTrue re-hashes this value to verify
 * the token's nonce claim.
 */
@Serializable
data class GoogleIdTokenGrantRequestDto(
    val provider: String,
    @SerialName("id_token") val idToken: String,
    val nonce: String? = null
)

@Serializable
data class RefreshTokenRequestDto(
    @SerialName("refresh_token") val refreshToken: String
)

@Serializable
data class AuthSessionResponseDto(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("expires_in") val expiresIn: Long,
    @SerialName("token_type") val tokenType: String,
    val user: GoTrueUserDto
)

@Serializable
data class GoTrueUserDto(
    val id: String,
    val email: String = ""
)

/**
 * Row shape of public.profiles — NOTE: role is intentionally NOT here. The real schema
 * keeps role in a separate `user_roles` table (proper normalized RBAC, see
 * `has_role()` in the migrations), so resolving a user's role is a second query
 * (see [RoleDto] / SupabaseRestApi.getRoles) after fetching the profile row.
 */
@Serializable
data class ProfileDto(
    val id: String,
    // Null in the database for accounts created without a name (e.g. an admin added in the dashboard);
    // coerceInputValues turns that null into "" instead of failing the whole sign-in.
    @SerialName("full_name") val fullName: String = "",
    val email: String? = null,
    val phone: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    val bio: String? = null,
    @SerialName("university_id") val universityId: String? = null,
    @SerialName("student_number") val studentNumber: String? = null,
    @SerialName("year_of_study") val yearOfStudy: Int? = null,
    @SerialName("company_name") val companyName: String? = null,
    @SerialName("is_verified") val isVerified: Boolean = false,
    @SerialName("is_suspended") val isSuspended: Boolean = false,
    @SerialName("preferred_language") val preferredLanguage: String = "en",
    @SerialName("notify_booking_updates") val notifyBookingUpdates: Boolean = true,
    @SerialName("notify_new_messages") val notifyNewMessages: Boolean = true,
    @SerialName("notify_maintenance_updates") val notifyMaintenanceUpdates: Boolean = true
)

/** Row shape of public.user_roles — a user may in principle hold more than one role row. */
@Serializable
data class RoleDto(
    @SerialName("user_id") val userId: String = "",
    val role: String
)

/**
 * Row shape of public.student_profiles — a CHILD row keyed by user_id, created by a
 * separate INSERT after signup (the DB trigger only creates `profiles` +
 * `user_roles`; it does not know student-specific fields).
 */
@Serializable
data class StudentProfileDto(
    @SerialName("user_id") val userId: String,
    @SerialName("student_number") val studentNumber: String? = null,
    @SerialName("university_id") val universityId: String? = null,
    val course: String? = null,
    val faculty: String? = null,
    @SerialName("year_of_study") val yearOfStudy: Int? = null,
    @SerialName("budget_max") val budgetMax: Double? = null,
    @SerialName("preferred_room_type") val preferredRoomType: String? = null,
    @SerialName("preferred_province") val preferredProvince: String? = null,
    @SerialName("preferred_city") val preferredCity: String? = null,
    @SerialName("preferred_university_id") val preferredUniversityId: String? = null,
    @SerialName("earliest_move_in") val earliestMoveIn: String? = null,
    @SerialName("needs_furnished") val needsFurnished: Boolean = false,
    @SerialName("proof_of_registration_url") val proofOfRegistrationUrl: String? = null,
    @SerialName("onboarding_complete") val onboardingComplete: Boolean = false
)

/**
 * Row shape of public.landlord_profiles. Documents are stored as private-bucket
 * object PATHS (not URLs) and opened through signed URLs. verification_status is the
 * `verification_status` enum: pending | submitted | verified | rejected |
 * documents_required (the last added by migration 20260818085757). Admin writes
 * verification_notes when a decision needs follow-up.
 */
@Serializable
data class LandlordProfileDto(
    @SerialName("user_id") val userId: String,
    @SerialName("legal_name") val legalName: String? = null,
    @SerialName("id_number") val idNumber: String? = null,
    @SerialName("business_name") val businessName: String? = null,
    @SerialName("business_registration_number") val businessRegistrationNumber: String? = null,
    @SerialName("business_address") val businessAddress: String? = null,
    val city: String? = null,
    val province: String? = null,
    @SerialName("postal_code") val postalCode: String? = null,
    @SerialName("contact_phone") val contactPhone: String? = null,
    @SerialName("id_document_url") val idDocumentUrl: String? = null,
    @SerialName("proof_of_ownership_url") val proofOfOwnershipUrl: String? = null,
    @SerialName("proof_of_address_url") val proofOfAddressUrl: String? = null,
    @SerialName("business_registration_doc_url") val businessRegistrationDocUrl: String? = null,
    @SerialName("verification_status") val verificationStatus: String = "pending",
    @SerialName("verification_notes") val verificationNotes: String? = null,
    @SerialName("onboarding_complete") val onboardingComplete: Boolean = false
)

/** Response of POST storage/v1/object/sign/{bucket}/{path}; `signedURL` is relative to /storage/v1. */
@Serializable
data class SignedUrlDto(
    @SerialName("signedURL") val signedUrl: String
)

@Serializable
data class RecoverRequestDto(val email: String)

@Serializable
data class UpdatePasswordRequestDto(val password: String)
