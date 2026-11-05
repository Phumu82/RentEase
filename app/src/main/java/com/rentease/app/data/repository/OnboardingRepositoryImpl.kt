package com.rentease.app.data.repository

import com.rentease.app.data.remote.httpHint
import android.content.Context
import android.net.Uri
import com.rentease.app.R
import com.rentease.app.data.auth.TokenManager
import com.rentease.app.data.remote.api.SupabaseRestApi
import com.rentease.app.data.remote.dto.LandlordProfileDto
import com.rentease.app.data.remote.dto.StudentProfileDto
import com.rentease.app.domain.model.AuthOutcome
import com.rentease.app.domain.model.DocumentRules
import com.rentease.app.domain.model.LandlordDocumentKind
import com.rentease.app.domain.model.LandlordOnboardingInput
import com.rentease.app.domain.model.LandlordOnboardingProfile
import com.rentease.app.domain.model.LocationCatalog
import com.rentease.app.domain.model.StudentOnboardingInput
import com.rentease.app.domain.model.StudentOnboardingProfile
import com.rentease.app.domain.model.VerificationStatus
import com.rentease.app.domain.model.shouldMarkSubmitted
import com.rentease.app.domain.model.validate
import com.rentease.app.domain.repository.AuthRepository
import com.rentease.app.domain.repository.OnboardingRepository
import com.rentease.app.domain.repository.OnboardingResult
import dagger.hilt.android.qualifiers.ApplicationContext
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OnboardingRepositoryImpl @Inject constructor(
    private val restApi: SupabaseRestApi,
    private val documents: DocumentUploadRepository,
    private val authRepository: AuthRepository,
    private val tokenManager: TokenManager,
    @ApplicationContext private val context: Context
) : OnboardingRepository {

    // ---------------------------------------------------------------- student

    override suspend fun getStudentOnboarding(): OnboardingResult<StudentOnboardingProfile> {
        val userId = tokenManager.getUserId() ?: return notSignedIn()
        return loadStudent(userId)
    }

    private suspend fun loadStudent(userId: String): OnboardingResult<StudentOnboardingProfile> {
        val profileResponse = restApi.getProfile(idFilter = "eq.$userId")
        if (!profileResponse.isSuccessful) return loadFailed(profileResponse.code())
        val studentResponse = restApi.getStudentProfile(userIdFilter = "eq.$userId")
        if (!studentResponse.isSuccessful) return loadFailed(studentResponse.code())

        val profile = profileResponse.body()?.firstOrNull()
        val row = studentResponse.body()?.firstOrNull()
        return OnboardingResult.Success(
            StudentOnboardingProfile(
                fullName = profile?.fullName.orEmpty(),
                phone = profile?.phone.orEmpty(),
                studentNumber = row?.studentNumber.orEmpty(),
                universityId = row?.universityId,
                course = row?.course.orEmpty(),
                yearOfStudy = row?.yearOfStudy ?: 1,
                budgetMax = row?.budgetMax,
                preferredRoomType = row?.preferredRoomType ?: StudentOnboardingProfile().preferredRoomType,
                earliestMoveIn = row?.earliestMoveIn,
                needsFurnished = row?.needsFurnished ?: false,
                proofOfRegistrationPath = row?.proofOfRegistrationUrl,
                onboardingComplete = row?.onboardingComplete ?: false
            )
        )
    }

    override suspend fun saveStudentOnboarding(input: StudentOnboardingInput): OnboardingResult<StudentOnboardingProfile> {
        val invalid = input.validate()
        if (invalid.isNotEmpty()) return OnboardingResult.Invalid(invalid)
        val userId = tokenManager.getUserId() ?: return notSignedIn()

        // 1) profiles: name + phone (also refreshes the cached signed-in user)…
        val nameOutcome = authRepository.updateProfile(input.fullName, input.phone)
        if (nameOutcome is AuthOutcome.Failure) return OnboardingResult.Failure(nameOutcome.message)

        // 2) …and the academic columns the web also mirrors onto profiles.
        val profilePatch = restApi.updateProfile(
            idFilter = "eq.$userId",
            body = mapOf(
                "university_id" to input.universityId,
                "student_number" to input.studentNumber.trim(),
                "year_of_study" to input.yearOfStudy
            )
        )
        if (!profilePatch.isSuccessful) return saveFailed(profilePatch.code())

        // 3) student_profiles upsert with onboarding_complete = true (web parity).
        val upsert = restApi.upsertStudentProfile(
            body = mapOf(
                "user_id" to userId,
                "student_number" to input.studentNumber.trim(),
                "university_id" to input.universityId,
                "course" to input.course.trim(),
                "year_of_study" to input.yearOfStudy,
                "budget_max" to input.budgetMax,
                "preferred_room_type" to input.preferredRoomType,
                "earliest_move_in" to input.earliestMoveIn,
                "needs_furnished" to input.needsFurnished,
                "onboarding_complete" to true
            )
        )
        if (!upsert.isSuccessful) return saveFailed(upsert.code())
        return loadStudent(userId)
    }

    override suspend fun uploadProofOfRegistration(uri: Uri): OnboardingResult<StudentOnboardingProfile> {
        val userId = tokenManager.getUserId() ?: return notSignedIn()
        val path = when (val result = documents.uploadPrivate(context, uri, BUCKET, "proof-of-registration")) {
            is PrivateUploadResult.Success -> result.path
            is PrivateUploadResult.Rejected -> return OnboardingResult.Failure(rejectionMessage(result.problem))
            PrivateUploadResult.Failed -> return OnboardingResult.Failure(context.getString(R.string.onboarding_error_upload))
        }
        val upsert = restApi.upsertStudentProfile(
            body = mapOf("user_id" to userId, "proof_of_registration_url" to path)
        )
        if (!upsert.isSuccessful) return saveFailed(upsert.code())
        return loadStudent(userId)
    }

    // --------------------------------------------------------------- landlord

    override suspend fun getLandlordOnboarding(): OnboardingResult<LandlordOnboardingProfile> {
        val userId = tokenManager.getUserId() ?: return notSignedIn()
        return loadLandlord(userId)
    }

    private suspend fun loadLandlord(userId: String): OnboardingResult<LandlordOnboardingProfile> {
        val profileResponse = restApi.getProfile(idFilter = "eq.$userId")
        if (!profileResponse.isSuccessful) return loadFailed(profileResponse.code())
        val landlordResponse = restApi.getLandlordProfile(userIdFilter = "eq.$userId")
        if (!landlordResponse.isSuccessful) return loadFailed(landlordResponse.code())

        val profile = profileResponse.body()?.firstOrNull()
        val row = landlordResponse.body()?.firstOrNull()
        return OnboardingResult.Success(row.toDomain(profile?.fullName.orEmpty(), profile?.phone.orEmpty(), profile?.companyName, profile?.isVerified ?: false))
    }

    override suspend fun saveLandlordOnboarding(input: LandlordOnboardingInput): OnboardingResult<LandlordOnboardingProfile> {
        val invalid = input.validate()
        if (invalid.isNotEmpty()) return OnboardingResult.Invalid(invalid)
        val userId = tokenManager.getUserId() ?: return notSignedIn()

        val nameOutcome = authRepository.updateProfile(input.displayName, input.contactPhone)
        if (nameOutcome is AuthOutcome.Failure) return OnboardingResult.Failure(nameOutcome.message)

        val companyName = input.businessName.trim().takeIf { it.isNotEmpty() }
        val profilePatch = restApi.updateProfile(
            idFilter = "eq.$userId",
            body = mapOf("company_name" to companyName)
        )
        if (!profilePatch.isSuccessful) return saveFailed(profilePatch.code())

        val upsert = restApi.upsertLandlordProfile(
            body = mapOf(
                "user_id" to userId,
                "legal_name" to input.legalName.trim(),
                "id_number" to input.idNumber.trim(),
                "business_name" to companyName,
                "business_registration_number" to input.businessRegistrationNumber.trim().takeIf { it.isNotEmpty() },
                "business_address" to input.businessAddress.trim(),
                "city" to input.city.trim(),
                "province" to input.province,
                "postal_code" to input.postalCode.trim().takeIf { it.isNotEmpty() },
                "contact_phone" to input.contactPhone.trim(),
                "onboarding_complete" to true
            )
        )
        if (!upsert.isSuccessful) return saveFailed(upsert.code())
        return loadLandlord(userId)
    }

    override suspend fun uploadLandlordDocument(
        kind: LandlordDocumentKind,
        uri: Uri
    ): OnboardingResult<LandlordOnboardingProfile> {
        val userId = tokenManager.getUserId() ?: return notSignedIn()
        val path = when (val result = documents.uploadPrivate(context, uri, BUCKET, kind.column)) {
            is PrivateUploadResult.Success -> result.path
            is PrivateUploadResult.Rejected -> return OnboardingResult.Failure(rejectionMessage(result.problem))
            PrivateUploadResult.Failed -> return OnboardingResult.Failure(context.getString(R.string.onboarding_error_upload))
        }

        val body = mutableMapOf<String, Any?>("user_id" to userId, kind.column to path)

        // Decide whether this upload completes the required set. Needs the current row,
        // since flipping to `submitted` must never downgrade a verified account (the web
        // flips on EVERY upload, which would).
        val current = restApi.getLandlordProfile(userIdFilter = "eq.$userId").body()?.firstOrNull()
        val docsAfter = current.documentMap().toMutableMap().also { it[kind] = path }
        val status = current?.verificationStatus?.let(VerificationStatus::fromDb) ?: VerificationStatus.PENDING
        if (shouldMarkSubmitted(status, docsAfter)) {
            body["verification_status"] = VerificationStatus.SUBMITTED.name.lowercase()
        }

        val upsert = restApi.upsertLandlordProfile(body = body)
        if (!upsert.isSuccessful) return saveFailed(upsert.code())
        return loadLandlord(userId)
    }

    // ----------------------------------------------------------------- shared

    override suspend fun getDocumentUrl(path: String): OnboardingResult<String> {
        val url = documents.createSignedUrl(BUCKET, path)
            ?: return OnboardingResult.Failure(context.getString(R.string.onboarding_error_open_document))
        return OnboardingResult.Success(url)
    }

    private fun LandlordProfileDto?.documentMap(): Map<LandlordDocumentKind, String> {
        if (this == null) return emptyMap()
        return buildMap {
            idDocumentUrl?.takeIf { it.isNotBlank() }?.let { put(LandlordDocumentKind.ID_DOCUMENT, it) }
            proofOfOwnershipUrl?.takeIf { it.isNotBlank() }?.let { put(LandlordDocumentKind.PROOF_OF_OWNERSHIP, it) }
            proofOfAddressUrl?.takeIf { it.isNotBlank() }?.let { put(LandlordDocumentKind.PROOF_OF_ADDRESS, it) }
            businessRegistrationDocUrl?.takeIf { it.isNotBlank() }?.let { put(LandlordDocumentKind.BUSINESS_REGISTRATION, it) }
        }
    }

    private fun LandlordProfileDto?.toDomain(
        displayName: String,
        phone: String,
        companyName: String?,
        isVerified: Boolean
    ) = LandlordOnboardingProfile(
        displayName = displayName,
        contactPhone = this?.contactPhone ?: phone,
        legalName = this?.legalName.orEmpty(),
        idNumber = this?.idNumber.orEmpty(),
        businessName = this?.businessName ?: companyName.orEmpty(),
        businessRegistrationNumber = this?.businessRegistrationNumber.orEmpty(),
        businessAddress = this?.businessAddress.orEmpty(),
        city = this?.city.orEmpty(),
        province = this?.province?.takeIf { it in LocationCatalog.PROVINCES } ?: LocationCatalog.PROVINCES.first(),
        postalCode = this?.postalCode.orEmpty(),
        onboardingComplete = this?.onboardingComplete ?: false,
        verificationStatus = this?.verificationStatus?.let(VerificationStatus::fromDb) ?: VerificationStatus.PENDING,
        verificationNotes = this?.verificationNotes,
        isVerified = isVerified,
        documents = documentMap()
    )

    private fun rejectionMessage(problem: DocumentRules.Problem): String = context.getString(
        when (problem) {
            DocumentRules.Problem.EMPTY -> R.string.document_error_empty
            DocumentRules.Problem.UNSUPPORTED_TYPE -> R.string.document_error_type
            DocumentRules.Problem.TOO_LARGE -> R.string.document_error_too_large
        }
    )

    private fun notSignedIn() = OnboardingResult.Failure(context.getString(R.string.error_not_signed_in))
    private fun loadFailed(code: Int) =
        OnboardingResult.Failure("${context.getString(R.string.onboarding_error_load)}${context.httpHint(code)}")
    private fun saveFailed(code: Int) =
        OnboardingResult.Failure("${context.getString(R.string.onboarding_error_save)}${context.httpHint(code)}")

    private companion object {
        /** The web's private bucket; its RLS requires `{auth.uid()}` as the first path segment. */
        const val BUCKET = "booking-documents"
    }
}
