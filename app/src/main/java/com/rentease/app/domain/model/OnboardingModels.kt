package com.rentease.app.domain.model

/** Fields that can fail validation on the onboarding forms (mapped to messages in the UI layer). */
enum class OnboardingField {
    FULL_NAME, STUDENT_NUMBER, UNIVERSITY, COURSE, BUDGET, EARLIEST_MOVE_IN,
    CONTACT_PHONE, LEGAL_NAME, ID_NUMBER, BUSINESS_ADDRESS, CITY, PROVINCE
}

/** Student profile as shown on the onboarding screen (profiles + student_profiles merged). */
data class StudentOnboardingProfile(
    val fullName: String = "",
    val phone: String = "",
    val studentNumber: String = "",
    val universityId: String? = null,
    val course: String = "",
    val yearOfStudy: Int = 1,
    val budgetMax: Double? = null,
    val preferredRoomType: String = RoomTypeCatalog.ALL.first(),
    val earliestMoveIn: String? = null,
    val needsFurnished: Boolean = false,
    /** Object path inside the private `booking-documents` bucket, or null if none uploaded. */
    val proofOfRegistrationPath: String? = null,
    val onboardingComplete: Boolean = false
)

/** What the student submits with "Save and continue". Same shape as the profile minus the proof path. */
data class StudentOnboardingInput(
    val fullName: String,
    val phone: String,
    val studentNumber: String,
    val universityId: String?,
    val course: String,
    val yearOfStudy: Int,
    val budgetMax: Double?,
    val preferredRoomType: String,
    val earliestMoveIn: String?,
    val needsFurnished: Boolean
)

/**
 * Mirrors the web `student.onboarding.tsx` required fields: full name, student number,
 * institution, course, monthly budget and earliest move-in date.
 */
fun StudentOnboardingInput.validate(): Set<OnboardingField> = buildSet {
    if (fullName.isBlank()) add(OnboardingField.FULL_NAME)
    if (studentNumber.isBlank()) add(OnboardingField.STUDENT_NUMBER)
    if (universityId.isNullOrBlank()) add(OnboardingField.UNIVERSITY)
    if (course.isBlank()) add(OnboardingField.COURSE)
    if (budgetMax == null || budgetMax <= 0.0) add(OnboardingField.BUDGET)
    if (earliestMoveIn.isNullOrBlank()) add(OnboardingField.EARLIEST_MOVE_IN)
}

/** The four document slots on `landlord_profiles`. ID + ownership are required (as on the web). */
enum class LandlordDocumentKind(val column: String, val required: Boolean) {
    ID_DOCUMENT("id_document_url", true),
    PROOF_OF_OWNERSHIP("proof_of_ownership_url", true),
    PROOF_OF_ADDRESS("proof_of_address_url", false),
    BUSINESS_REGISTRATION("business_registration_doc_url", false)
}

data class LandlordOnboardingProfile(
    val displayName: String = "",
    val contactPhone: String = "",
    val legalName: String = "",
    val idNumber: String = "",
    val businessName: String = "",
    val businessRegistrationNumber: String = "",
    val businessAddress: String = "",
    val city: String = "",
    val province: String = LocationCatalog.PROVINCES.first(),
    val postalCode: String = "",
    val onboardingComplete: Boolean = false,
    val verificationStatus: VerificationStatus = VerificationStatus.PENDING,
    val verificationNotes: String? = null,
    /** True when `profiles.is_verified` is set — the badge shown to students. */
    val isVerified: Boolean = false,
    /** Object paths inside the private `booking-documents` bucket, keyed by slot. */
    val documents: Map<LandlordDocumentKind, String> = emptyMap()
)

data class LandlordOnboardingInput(
    val displayName: String,
    val contactPhone: String,
    val legalName: String,
    val idNumber: String,
    val businessName: String,
    val businessRegistrationNumber: String,
    val businessAddress: String,
    val city: String,
    val province: String,
    val postalCode: String
)

/** Mirrors the web `landlord.onboarding.tsx` required fields. */
fun LandlordOnboardingInput.validate(): Set<OnboardingField> = buildSet {
    if (displayName.isBlank()) add(OnboardingField.FULL_NAME)
    if (contactPhone.isBlank()) add(OnboardingField.CONTACT_PHONE)
    if (legalName.isBlank()) add(OnboardingField.LEGAL_NAME)
    if (idNumber.isBlank()) add(OnboardingField.ID_NUMBER)
    if (businessAddress.isBlank()) add(OnboardingField.BUSINESS_ADDRESS)
    if (city.isBlank()) add(OnboardingField.CITY)
    if (province !in LocationCatalog.PROVINCES) add(OnboardingField.PROVINCE)
}

/** Number of REQUIRED document slots (ID + proof of ownership) that have a file on record. */
fun LandlordOnboardingProfile.requiredDocumentsUploaded(): Int =
    LandlordDocumentKind.entries.filter { it.required }.count { !documents[it].isNullOrBlank() }

val REQUIRED_DOCUMENT_COUNT: Int = LandlordDocumentKind.entries.count { it.required }

/**
 * Whether uploading a document should flip `verification_status` to `submitted`.
 * Only when both required documents are on file AND the account is in a state where
 * a (re)submission makes sense — never downgrading `verified`, and never re-queuing
 * an account that is already `submitted`.
 */
fun shouldMarkSubmitted(
    current: VerificationStatus,
    documents: Map<LandlordDocumentKind, String>
): Boolean {
    val allRequired = LandlordDocumentKind.entries.filter { it.required }.all { !documents[it].isNullOrBlank() }
    if (!allRequired) return false
    return current == VerificationStatus.PENDING ||
        current == VerificationStatus.DOCUMENTS_REQUIRED ||
        current == VerificationStatus.REJECTED
}
