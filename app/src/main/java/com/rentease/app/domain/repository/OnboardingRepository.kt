package com.rentease.app.domain.repository

import android.net.Uri
import com.rentease.app.domain.model.LandlordDocumentKind
import com.rentease.app.domain.model.LandlordOnboardingInput
import com.rentease.app.domain.model.LandlordOnboardingProfile
import com.rentease.app.domain.model.OnboardingField
import com.rentease.app.domain.model.StudentOnboardingInput
import com.rentease.app.domain.model.StudentOnboardingProfile

sealed interface OnboardingResult<out T> {
    data class Success<T>(val data: T) : OnboardingResult<T>
    data class Failure(val message: String) : OnboardingResult<Nothing>
    /** Client-side validation failed; nothing was sent to the server. */
    data class Invalid(val fields: Set<OnboardingField>) : OnboardingResult<Nothing>
}

/**
 * Student + landlord onboarding, mirroring the web's `student.onboarding.tsx` and
 * `landlord.onboarding.tsx`: reads/writes `profiles` + `student_profiles` /
 * `landlord_profiles`, and stores documents in the private `booking-documents` bucket.
 * Role-agnostic storage helpers live here too so both flows share one implementation.
 */
interface OnboardingRepository {
    suspend fun getStudentOnboarding(): OnboardingResult<StudentOnboardingProfile>
    suspend fun saveStudentOnboarding(input: StudentOnboardingInput): OnboardingResult<StudentOnboardingProfile>
    suspend fun uploadProofOfRegistration(uri: Uri): OnboardingResult<StudentOnboardingProfile>

    suspend fun getLandlordOnboarding(): OnboardingResult<LandlordOnboardingProfile>
    suspend fun saveLandlordOnboarding(input: LandlordOnboardingInput): OnboardingResult<LandlordOnboardingProfile>
    suspend fun uploadLandlordDocument(kind: LandlordDocumentKind, uri: Uri): OnboardingResult<LandlordOnboardingProfile>

    /** Signed URL (10 min) for a stored document path, or a Failure. */
    suspend fun getDocumentUrl(path: String): OnboardingResult<String>
}
