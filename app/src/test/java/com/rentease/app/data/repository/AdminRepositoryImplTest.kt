package com.rentease.app.data.repository

import com.rentease.app.data.auth.TokenManager
import com.rentease.app.data.remote.dto.AdminAccessRequestDto
import com.rentease.app.data.remote.dto.AdminAccessResponseDto
import com.rentease.app.data.remote.api.SupabaseAdminAccessApi
import com.rentease.app.data.remote.api.SupabaseAdminApi
import com.rentease.app.data.remote.dto.AuditLogInsertDto
import com.rentease.app.data.remote.dto.LandlordProfileDto
import com.rentease.app.data.remote.dto.ProfileDto
import com.rentease.app.data.remote.dto.PropertyDto
import com.rentease.app.data.remote.dto.VerificationDecisionDto
import com.rentease.app.domain.model.LandlordDocumentKind
import com.rentease.app.domain.model.VerificationDecision
import com.rentease.app.domain.model.VerificationStatus
import com.rentease.app.domain.repository.AdminResult
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

class AdminRepositoryImplTest {

    private lateinit var adminApi: SupabaseAdminApi
    private lateinit var adminAccessApi: SupabaseAdminAccessApi
    private lateinit var tokenManager: TokenManager
    private lateinit var repository: AdminRepositoryImpl

    private val adminId = "admin-1"

    @Before
    fun setUp() {
        adminApi = mockk()
        adminAccessApi = mockk()
        tokenManager = mockk()
        every { tokenManager.getUserId() } returns adminId
        repository = AdminRepositoryImpl(adminApi, adminAccessApi, tokenManager, mockk(relaxed = true))
    }

    @Test
    fun `requestAdminAccess returns Success only when the Edge Function reports success=true`() = runTest {
        coEvery { adminAccessApi.requestAdminAccess(AdminAccessRequestDto("correct-code")) } returns
            Response.success(AdminAccessResponseDto(success = true))

        val result = repository.requestAdminAccess("correct-code")

        assertTrue(result is AdminResult.Success)
    }

    @Test
    fun `requestAdminAccess fails when the Edge Function reports an error, even with HTTP 200`() = runTest {
        coEvery { adminAccessApi.requestAdminAccess(AdminAccessRequestDto("wrong-code")) } returns
            Response.success(AdminAccessResponseDto(success = false, error = "Invalid code"))

        val result = repository.requestAdminAccess("wrong-code")

        assertTrue(result is AdminResult.Failure)
        assertEquals("Invalid code", (result as AdminResult.Failure).message)
    }

    @Test
    fun `requestAdminAccess fails gracefully on a network exception rather than crashing`() = runTest {
        coEvery { adminAccessApi.requestAdminAccess(any()) } throws RuntimeException("no network")

        val result = repository.requestAdminAccess("some-code")

        assertTrue(result is AdminResult.Failure)
    }

    @Test
    fun `approveLandlord writes verification_status=verified and logs an audit event`() = runTest {
        val bodySlot = slot<VerificationDecisionDto>()
        coEvery { adminApi.setVerificationStatus(userIdFilter = "eq.landlord-1", body = capture(bodySlot)) } returns
            Response.success(listOf(LandlordProfileDto(userId = "landlord-1", verificationStatus = "verified")))
        coEvery { adminApi.setProfileVerified(idFilter = any(), body = any()) } returns Response.success(Unit)
        coEvery { adminApi.insertAuditLog(any()) } returns Response.success(Unit)

        val result = repository.approveLandlord("landlord-1")

        assertTrue(result is AdminResult.Success)
        assertEquals("verified", bodySlot.captured.status)
        coVerify {
            adminApi.insertAuditLog(
                AuditLogInsertDto(actorId = adminId, action = "admin.landlord_verified", entity = "landlord_profiles", entityId = "landlord-1")
            )
        }
    }

    @Test
    fun `rejectLandlord never writes verified status`() = runTest {
        val bodySlot = slot<VerificationDecisionDto>()
        coEvery { adminApi.setVerificationStatus(userIdFilter = "eq.landlord-2", body = capture(bodySlot)) } returns
            Response.success(listOf(LandlordProfileDto(userId = "landlord-2", verificationStatus = "rejected")))
        coEvery { adminApi.setProfileVerified(idFilter = any(), body = any()) } returns Response.success(Unit)
        coEvery { adminApi.insertAuditLog(any()) } returns Response.success(Unit)

        repository.rejectLandlord("landlord-2")

        assertEquals("rejected", bodySlot.captured.status)
    }

    @Test
    fun `approveProperty sets status=published, matching the real property_status enum`() = runTest {
        coEvery { adminApi.setPropertyModerationStatus(idFilter = "eq.prop-1", body = any()) } returns
            Response.success(listOf(PropertyDto(id = "prop-1", slug = "s", title = "t", city = "c", province = "p", pricePerMonth = 1.0, status = "published")))
        coEvery { adminApi.insertAuditLog(any()) } returns Response.success(Unit)

        val result = repository.approveProperty("prop-1")

        assertTrue(result is AdminResult.Success)
        coVerify { adminApi.insertAuditLog(match { it.action == "admin.property_approved" }) }
    }

    @Test
    fun `a failed audit log insert does not fail the underlying admin action`() = runTest {
        coEvery { adminApi.setVerificationStatus(userIdFilter = any(), body = any()) } returns
            Response.success(listOf(LandlordProfileDto(userId = "landlord-3", verificationStatus = "verified")))
        coEvery { adminApi.setProfileVerified(idFilter = any(), body = any()) } returns Response.success(Unit)
        coEvery { adminApi.insertAuditLog(any()) } throws RuntimeException("audit table unreachable")

        val result = repository.approveLandlord("landlord-3")

        assertTrue(result is AdminResult.Success)
    }

    @Test
    fun `approving also sets profiles is_verified true`() = runTest {
        coEvery { adminApi.setVerificationStatus(userIdFilter = "eq.l-4", body = any()) } returns
            Response.success(listOf(LandlordProfileDto(userId = "l-4", verificationStatus = "verified")))
        val badge = slot<Map<String, Any?>>()
        coEvery { adminApi.setProfileVerified(idFilter = "eq.l-4", body = capture(badge)) } returns Response.success(Unit)
        coEvery { adminApi.insertAuditLog(any()) } returns Response.success(Unit)

        repository.decideLandlord("l-4", VerificationDecision.VERIFIED)

        assertEquals(true, badge.captured["is_verified"])
    }

    @Test
    fun `requesting documents writes the notes, clears the badge and audits the decision`() = runTest {
        val bodySlot = slot<VerificationDecisionDto>()
        coEvery { adminApi.setVerificationStatus(userIdFilter = "eq.l-5", body = capture(bodySlot)) } returns
            Response.success(listOf(LandlordProfileDto(userId = "l-5", verificationStatus = "documents_required")))
        val badge = slot<Map<String, Any?>>()
        coEvery { adminApi.setProfileVerified(idFilter = "eq.l-5", body = capture(badge)) } returns Response.success(Unit)
        coEvery { adminApi.insertAuditLog(any()) } returns Response.success(Unit)

        val result = repository.decideLandlord("l-5", VerificationDecision.DOCUMENTS_REQUIRED, "  Please upload a clearer ID  ")

        assertTrue(result is AdminResult.Success)
        assertEquals("documents_required", bodySlot.captured.status)
        assertEquals("Please upload a clearer ID", bodySlot.captured.notes)
        assertEquals(false, badge.captured["is_verified"])
        coVerify { adminApi.insertAuditLog(match { it.action == "admin.landlord_documents_required" }) }
    }

    @Test
    fun `a failed badge update is reported instead of silently ignored`() = runTest {
        coEvery { adminApi.setVerificationStatus(userIdFilter = any(), body = any()) } returns
            Response.success(listOf(LandlordProfileDto(userId = "l-6", verificationStatus = "verified")))
        coEvery { adminApi.setProfileVerified(idFilter = any(), body = any()) } returns
            Response.error(403, "denied".toResponseBody("text/plain".toMediaType()))

        val result = repository.decideLandlord("l-6", VerificationDecision.VERIFIED)

        assertTrue(result is AdminResult.Failure)
        coVerify(exactly = 0) { adminApi.insertAuditLog(any()) }
    }

    @Test
    fun `getLandlordVerifications maps every document slot and the notes`() = runTest {
        coEvery { adminApi.getLandlordProfiles() } returns Response.success(
            listOf(
                LandlordProfileDto(
                    userId = "l-7", legalName = "Thandi N", businessName = "TN Rentals", contactPhone = "0820000000",
                    businessAddress = "1 Main Rd", city = "Johannesburg", province = "Gauteng",
                    idDocumentUrl = "l-7/landlord-id/a.pdf", proofOfOwnershipUrl = "l-7/landlord-ownership/b.pdf",
                    businessRegistrationDocUrl = "l-7/landlord-business/c.pdf",
                    verificationStatus = "documents_required", verificationNotes = "Need proof of address"
                )
            )
        )
        coEvery { adminApi.getAllProfiles() } returns Response.success(
            listOf(ProfileDto(id = "l-7", fullName = "Thandi Nkosi", email = "t@example.com"))
        )

        val item = (repository.getLandlordVerifications() as AdminResult.Success).data.single()

        assertEquals("Thandi Nkosi", item.fullName)
        assertEquals("1 Main Rd, Johannesburg, Gauteng", item.address)
        assertEquals("Need proof of address", item.notes)
        assertEquals(VerificationStatus.DOCUMENTS_REQUIRED, item.status)
        assertEquals(
            listOf(LandlordDocumentKind.ID_DOCUMENT, LandlordDocumentKind.PROOF_OF_OWNERSHIP, LandlordDocumentKind.BUSINESS_REGISTRATION),
            item.documents.map { it.kind }
        )
    }
}
