package com.rentease.app.data.repository

import android.net.Uri
import com.rentease.app.data.auth.TokenManager
import com.rentease.app.data.remote.api.SupabaseRestApi
import com.rentease.app.data.remote.dto.LandlordProfileDto
import com.rentease.app.data.remote.dto.ProfileDto
import com.rentease.app.data.remote.dto.StudentProfileDto
import com.rentease.app.domain.model.AuthOutcome
import com.rentease.app.domain.model.DocumentRules
import com.rentease.app.domain.model.LandlordDocumentKind
import com.rentease.app.domain.model.LandlordOnboardingInput
import com.rentease.app.domain.model.OnboardingField
import com.rentease.app.domain.model.StudentOnboardingInput
import com.rentease.app.domain.model.VerificationStatus
import com.rentease.app.domain.repository.AuthRepository
import com.rentease.app.domain.repository.OnboardingResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

class OnboardingRepositoryImplTest {

    private lateinit var restApi: SupabaseRestApi
    private lateinit var documents: DocumentUploadRepository
    private lateinit var authRepository: AuthRepository
    private lateinit var tokenManager: TokenManager
    private lateinit var repository: OnboardingRepositoryImpl
    private val uri = mockk<Uri>()

    @Before
    fun setUp() {
        restApi = mockk()
        documents = mockk()
        authRepository = mockk()
        tokenManager = mockk()
        every { tokenManager.getUserId() } returns "user-1"
        repository = OnboardingRepositoryImpl(restApi, documents, authRepository, tokenManager, mockk(relaxed = true))
    }

    private fun error(code: Int) = "".toResponseBody("application/json".toMediaType())

    private fun studentInput() = StudentOnboardingInput(
        fullName = "Thandi Nkosi", phone = "0821234567", studentNumber = "2024001", universityId = "uni-1",
        course = "BSc IT", yearOfStudy = 2, budgetMax = 4500.0, preferredRoomType = "Single room",
        earliestMoveIn = "2027-01-15", needsFurnished = false
    )

    private fun stubStudentReads(row: StudentProfileDto? = StudentProfileDto(userId = "user-1", onboardingComplete = true)) {
        coEvery { restApi.getProfile(idFilter = any(), select = any()) } returns
            Response.success(listOf(ProfileDto(id = "user-1", fullName = "Thandi Nkosi", phone = "0821234567")))
        coEvery { restApi.getStudentProfile(userIdFilter = any(), select = any()) } returns
            Response.success(listOfNotNull(row))
    }

    // ---------------------------------------------------------------- student

    @Test
    fun `invalid student input never reaches the network`() = runTest {
        val result = repository.saveStudentOnboarding(studentInput().copy(course = "", budgetMax = null))

        assertTrue(result is OnboardingResult.Invalid)
        assertEquals(setOf(OnboardingField.COURSE, OnboardingField.BUDGET), (result as OnboardingResult.Invalid).fields)
        coVerify(exactly = 0) { restApi.upsertStudentProfile(any(), any(), any()) }
        coVerify(exactly = 0) { authRepository.updateProfile(any(), any()) }
    }

    @Test
    fun `saving the student profile upserts onboarding_complete and sends needs_furnished explicitly`() = runTest {
        coEvery { authRepository.updateProfile(any(), any()) } returns mockk<AuthOutcome.Success>()
        coEvery { restApi.updateProfile(idFilter = any(), body = any(), prefer = any()) } returns Response.success(emptyList())
        val body = slot<Map<String, Any?>>()
        coEvery { restApi.upsertStudentProfile(capture(body), any(), any()) } returns Response.success(emptyList())
        stubStudentReads()

        val result = repository.saveStudentOnboarding(studentInput())

        assertTrue(result is OnboardingResult.Success)
        assertEquals("user-1", body.captured["user_id"])
        assertEquals(true, body.captured["onboarding_complete"])
        // Must be present even when false, otherwise un-ticking "furnished" would never persist.
        assertTrue(body.captured.containsKey("needs_furnished"))
        assertEquals(false, body.captured["needs_furnished"])
        assertEquals(4500.0, body.captured["budget_max"])
        assertEquals("2027-01-15", body.captured["earliest_move_in"])
    }

    @Test
    fun `a failed student upsert surfaces the HTTP code`() = runTest {
        coEvery { authRepository.updateProfile(any(), any()) } returns mockk<AuthOutcome.Success>()
        coEvery { restApi.updateProfile(idFilter = any(), body = any(), prefer = any()) } returns Response.success(emptyList())
        coEvery { restApi.upsertStudentProfile(any(), any(), any()) } returns Response.error(403, error(403))

        val result = repository.saveStudentOnboarding(studentInput())

        assertTrue(result is OnboardingResult.Failure)
        assertTrue((result as OnboardingResult.Failure).message.contains("HTTP 403"))
    }

    @Test
    fun `uploading proof of registration stores the object path not a URL`() = runTest {
        coEvery { documents.uploadPrivate(any(), uri, "booking-documents", "proof-of-registration") } returns
            PrivateUploadResult.Success("user-1/proof-of-registration/1-doc.pdf")
        val body = slot<Map<String, Any?>>()
        coEvery { restApi.upsertStudentProfile(capture(body), any(), any()) } returns Response.success(emptyList())
        stubStudentReads(StudentProfileDto(userId = "user-1", proofOfRegistrationUrl = "user-1/proof-of-registration/1-doc.pdf"))

        val result = repository.uploadProofOfRegistration(uri)

        assertEquals("user-1/proof-of-registration/1-doc.pdf", body.captured["proof_of_registration_url"])
        assertEquals(
            "user-1/proof-of-registration/1-doc.pdf",
            (result as OnboardingResult.Success).data.proofOfRegistrationPath
        )
    }

    @Test
    fun `a rejected file is never written to the profile`() = runTest {
        coEvery { documents.uploadPrivate(any(), uri, any(), any()) } returns
            PrivateUploadResult.Rejected(DocumentRules.Problem.TOO_LARGE)

        val result = repository.uploadProofOfRegistration(uri)

        assertTrue(result is OnboardingResult.Failure)
        coVerify(exactly = 0) { restApi.upsertStudentProfile(any(), any(), any()) }
    }

    @Test
    fun `student read maps defaults when no student_profiles row exists yet`() = runTest {
        stubStudentReads(row = null)

        val profile = (repository.getStudentOnboarding() as OnboardingResult.Success).data

        assertEquals("Thandi Nkosi", profile.fullName)
        assertEquals(false, profile.onboardingComplete)
        assertEquals(1, profile.yearOfStudy)
        assertEquals("Single room", profile.preferredRoomType)
    }

    @Test
    fun `no signed-in user fails clearly`() = runTest {
        every { tokenManager.getUserId() } returns null

        assertTrue(repository.getStudentOnboarding() is OnboardingResult.Failure)
        assertTrue(repository.getLandlordOnboarding() is OnboardingResult.Failure)
    }

    // --------------------------------------------------------------- landlord

    private fun landlordRow(
        status: String = "pending",
        id: String? = null,
        ownership: String? = null
    ) = LandlordProfileDto(
        userId = "user-1", idDocumentUrl = id, proofOfOwnershipUrl = ownership, verificationStatus = status
    )

    private fun stubLandlordRead(row: LandlordProfileDto?) {
        coEvery { restApi.getProfile(idFilter = any(), select = any()) } returns
            Response.success(listOf(ProfileDto(id = "user-1", fullName = "Sipho")))
        coEvery { restApi.getLandlordProfile(userIdFilter = any(), select = any()) } returns
            Response.success(listOfNotNull(row))
    }

    private fun landlordInput() = LandlordOnboardingInput(
        displayName = "Sipho", contactPhone = "0831112222", legalName = "Sipho Dlamini", idNumber = "8001015009087",
        businessName = "", businessRegistrationNumber = "", businessAddress = "12 Main Rd", city = "Johannesburg",
        province = "Gauteng", postalCode = ""
    )

    @Test
    fun `saving landlord details upserts blank optional fields as null`() = runTest {
        coEvery { authRepository.updateProfile(any(), any()) } returns mockk<AuthOutcome.Success>()
        coEvery { restApi.updateProfile(idFilter = any(), body = any(), prefer = any()) } returns Response.success(emptyList())
        val body = slot<Map<String, Any?>>()
        coEvery { restApi.upsertLandlordProfile(capture(body), any(), any()) } returns Response.success(emptyList())
        stubLandlordRead(landlordRow())

        val result = repository.saveLandlordOnboarding(landlordInput())

        assertTrue(result is OnboardingResult.Success)
        assertEquals(true, body.captured["onboarding_complete"])
        assertEquals(null, body.captured["business_name"])
        assertEquals(null, body.captured["postal_code"])
        assertEquals("Gauteng", body.captured["province"])
        assertTrue(!body.captured.containsKey("verification_status"))
    }

    @Test
    fun `first required document does not flip status to submitted`() = runTest {
        coEvery { documents.uploadPrivate(any(), uri, "booking-documents", "id_document_url") } returns
            PrivateUploadResult.Success("user-1/id_document_url/1-id.pdf")
        stubLandlordRead(landlordRow())
        val body = slot<Map<String, Any?>>()
        coEvery { restApi.upsertLandlordProfile(capture(body), any(), any()) } returns Response.success(emptyList())

        repository.uploadLandlordDocument(LandlordDocumentKind.ID_DOCUMENT, uri)

        assertEquals("user-1/id_document_url/1-id.pdf", body.captured["id_document_url"])
        assertTrue(!body.captured.containsKey("verification_status"))
    }

    @Test
    fun `completing the required set submits for review`() = runTest {
        coEvery { documents.uploadPrivate(any(), uri, any(), "proof_of_ownership_url") } returns
            PrivateUploadResult.Success("user-1/proof_of_ownership_url/2-own.pdf")
        stubLandlordRead(landlordRow(id = "user-1/id_document_url/1-id.pdf"))
        val body = slot<Map<String, Any?>>()
        coEvery { restApi.upsertLandlordProfile(capture(body), any(), any()) } returns Response.success(emptyList())

        repository.uploadLandlordDocument(LandlordDocumentKind.PROOF_OF_OWNERSHIP, uri)

        assertEquals("submitted", body.captured["verification_status"])
    }

    @Test
    fun `re-uploading on a verified account never downgrades the status`() = runTest {
        coEvery { documents.uploadPrivate(any(), uri, any(), any()) } returns PrivateUploadResult.Success("user-1/x/3-new.pdf")
        stubLandlordRead(landlordRow(status = "verified", id = "a", ownership = "b"))
        val body = slot<Map<String, Any?>>()
        coEvery { restApi.upsertLandlordProfile(capture(body), any(), any()) } returns Response.success(emptyList())

        repository.uploadLandlordDocument(LandlordDocumentKind.ID_DOCUMENT, uri)

        assertTrue(!body.captured.containsKey("verification_status"))
    }

    @Test
    fun `documents_required plus a complete set resubmits`() = runTest {
        coEvery { documents.uploadPrivate(any(), uri, any(), any()) } returns PrivateUploadResult.Success("user-1/x/4-new.pdf")
        stubLandlordRead(landlordRow(status = "documents_required", id = "a", ownership = "b"))
        val body = slot<Map<String, Any?>>()
        coEvery { restApi.upsertLandlordProfile(capture(body), any(), any()) } returns Response.success(emptyList())

        repository.uploadLandlordDocument(LandlordDocumentKind.PROOF_OF_ADDRESS, uri)

        assertEquals("submitted", body.captured["verification_status"])
    }

    @Test
    fun `landlord read exposes status notes and documents`() = runTest {
        stubLandlordRead(
            LandlordProfileDto(
                userId = "user-1", verificationStatus = "documents_required", verificationNotes = "Please add a lease",
                idDocumentUrl = "user-1/id.pdf", proofOfAddressUrl = "user-1/addr.pdf", province = "Atlantis"
            )
        )

        val profile = (repository.getLandlordOnboarding() as OnboardingResult.Success).data

        assertEquals(VerificationStatus.DOCUMENTS_REQUIRED, profile.verificationStatus)
        assertEquals("Please add a lease", profile.verificationNotes)
        assertEquals(setOf(LandlordDocumentKind.ID_DOCUMENT, LandlordDocumentKind.PROOF_OF_ADDRESS), profile.documents.keys)
        assertEquals("Gauteng", profile.province) // unknown stored province falls back to a valid one
    }
}
