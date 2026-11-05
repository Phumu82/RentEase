package com.rentease.app.data.repository

import com.rentease.app.data.auth.TokenManager
import com.rentease.app.data.remote.api.SupabaseSupportApi
import com.rentease.app.data.remote.dto.ComplaintCreateDto
import com.rentease.app.data.remote.dto.ComplaintDto
import com.rentease.app.domain.model.ComplaintInput
import com.rentease.app.domain.model.MaintenanceStatus
import com.rentease.app.domain.repository.SupportResult
import io.mockk.coEvery
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

class SupportRepositoryImplTest {

    private lateinit var api: SupabaseSupportApi
    private lateinit var tokenManager: TokenManager
    private lateinit var repository: SupportRepositoryImpl

    @Before
    fun setUp() {
        api = mockk()
        tokenManager = mockk()
        repository = SupportRepositoryImpl(api, tokenManager, mockk(relaxed = true))
    }

    private fun complaintInput() = ComplaintInput(
        subject = "Noisy neighbours", category = "Property", description = "Loud music every night past midnight"
    )

    @Test
    fun `getMyComplaints fails clearly when no user is signed in`() = runTest {
        every { tokenManager.getUserId() } returns null

        val result = repository.getMyComplaints()

        assertTrue(result is SupportResult.Failure)
    }

    @Test
    fun `getMyComplaints maps DTOs including the status enum`() = runTest {
        every { tokenManager.getUserId() } returns "user-1"
        coEvery { api.getMyComplaints(reporterIdFilter = "eq.user-1") } returns
            Response.success(
                listOf(
                    ComplaintDto(
                        id = "c-1", subject = "Noisy neighbours", category = "Property",
                        description = "Loud music", status = "in_progress", adminResponse = "Looking into it"
                    )
                )
            )

        val result = repository.getMyComplaints()

        assertTrue(result is SupportResult.Success)
        val complaints = (result as SupportResult.Success).data
        assertEquals(1, complaints.size)
        assertEquals(MaintenanceStatus.IN_PROGRESS, complaints.first().status)
        assertEquals("Looking into it", complaints.first().adminResponse)
    }

    @Test
    fun `getMyComplaints returns Failure on a non-2xx response`() = runTest {
        every { tokenManager.getUserId() } returns "user-1"
        coEvery { api.getMyComplaints(reporterIdFilter = any()) } returns
            Response.error(500, "".toResponseBody("application/json".toMediaType()))

        val result = repository.getMyComplaints()

        assertTrue(result is SupportResult.Failure)
        assertTrue((result as SupportResult.Failure).message.contains("HTTP 500"))
    }

    @Test
    fun `submitComplaint fails clearly when no user is signed in`() = runTest {
        every { tokenManager.getUserId() } returns null

        val result = repository.submitComplaint(complaintInput())

        assertTrue(result is SupportResult.Failure)
    }

    @Test
    fun `submitComplaint posts the signed-in user's id with the complaint fields`() = runTest {
        every { tokenManager.getUserId() } returns "user-1"
        val bodySlot = slot<ComplaintCreateDto>()
        coEvery { api.createComplaint(body = capture(bodySlot), prefer = any()) } returns Response.success(Unit)

        val result = repository.submitComplaint(complaintInput())

        assertTrue(result is SupportResult.Success)
        assertEquals("user-1", bodySlot.captured.reporterId)
        assertEquals("Noisy neighbours", bodySlot.captured.subject)
        assertEquals("Property", bodySlot.captured.category)
    }

    @Test
    fun `submitComplaint returns Failure on a non-2xx response`() = runTest {
        every { tokenManager.getUserId() } returns "user-1"
        coEvery { api.createComplaint(body = any(), prefer = any()) } returns
            Response.error(403, "".toResponseBody("application/json".toMediaType()))

        val result = repository.submitComplaint(complaintInput())

        assertTrue(result is SupportResult.Failure)
        assertTrue((result as SupportResult.Failure).message.contains("HTTP 403"))
    }
}
