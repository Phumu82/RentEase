package com.rentease.app.data.repository

import com.rentease.app.data.auth.TokenManager
import com.rentease.app.data.remote.api.SupabaseAuthApi
import com.rentease.app.data.remote.api.SupabaseRestApi
import com.rentease.app.data.remote.dto.ProfileDto
import com.rentease.app.domain.model.AuthOutcome
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

class ProfileUpdateTest {

    private lateinit var restApi: SupabaseRestApi
    private lateinit var tokenManager: TokenManager
    private lateinit var repository: AuthRepositoryImpl

    @Before
    fun setUp() {
        restApi = mockk()
        tokenManager = mockk(relaxUnitFun = true)
        every { tokenManager.getUserId() } returns "user-1"
        every { tokenManager.getRole() } returns "student"
        every { tokenManager.getAccessToken() } returns "token"
        every { tokenManager.getRefreshToken() } returns "refresh"
        repository = AuthRepositoryImpl(mockk<SupabaseAuthApi>(), restApi, tokenManager, mockk(relaxed = true))
    }

    @Test
    fun `updateProfile trims the name and writes full_name and phone to the profile row`() = runTest {
        val bodySlot = slot<Map<String, Any?>>()
        coEvery { restApi.updateProfile(idFilter = "eq.user-1", body = capture(bodySlot)) } returns
            Response.success(listOf(ProfileDto(id = "user-1", fullName = "Thandi Nkosi")))
        coEvery { restApi.getProfile(idFilter = "eq.user-1") } returns
            Response.success(listOf(ProfileDto(id = "user-1", fullName = "Thandi Nkosi", phone = "0821234567")))

        val outcome = repository.updateProfile("  Thandi Nkosi  ", " 0821234567 ")

        assertTrue(outcome is AuthOutcome.Success)
        assertEquals("Thandi Nkosi", bodySlot.captured["full_name"])
        assertEquals("0821234567", bodySlot.captured["phone"])
        assertEquals("Thandi Nkosi", repository.currentUser.value?.fullName)
    }

    @Test
    fun `a blank phone clears the stored number by sending null`() = runTest {
        val bodySlot = slot<Map<String, Any?>>()
        coEvery { restApi.updateProfile(idFilter = "eq.user-1", body = capture(bodySlot)) } returns
            Response.success(listOf(ProfileDto(id = "user-1", fullName = "T")))
        coEvery { restApi.getProfile(idFilter = "eq.user-1") } returns
            Response.success(listOf(ProfileDto(id = "user-1", fullName = "T")))

        repository.updateProfile("T", "   ")

        assertTrue(bodySlot.captured.containsKey("phone"))
        assertNull(bodySlot.captured["phone"])
    }

    @Test
    fun `a blank name is rejected without any network call`() = runTest {
        val outcome = repository.updateProfile("   ", "0821234567")
        assertTrue(outcome is AuthOutcome.Failure)
        coVerify(exactly = 0) { restApi.updateProfile(any(), any(), any()) }
    }

    @Test
    fun `a server error becomes a Failure`() = runTest {
        coEvery { restApi.updateProfile(idFilter = any(), body = any()) } returns
            Response.error(500, "boom".toResponseBody("text/plain".toMediaType()))

        val outcome = repository.updateProfile("Thandi", null)

        assertTrue(outcome is AuthOutcome.Failure)
    }
}
