package com.rentease.app.data.repository

import com.rentease.app.data.auth.TokenManager
import com.rentease.app.data.remote.api.SupabaseAuthApi
import com.rentease.app.data.remote.api.SupabaseRestApi
import com.rentease.app.data.remote.dto.AuthSessionResponseDto
import com.rentease.app.data.remote.dto.GoTrueUserDto
import com.rentease.app.data.remote.dto.ProfileDto
import com.rentease.app.domain.model.AppLanguage
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

class LanguagePersistenceTest {

    private lateinit var authApi: SupabaseAuthApi
    private lateinit var restApi: SupabaseRestApi
    private lateinit var tokenManager: TokenManager
    private lateinit var repository: AuthRepositoryImpl

    @Before
    fun setUp() {
        authApi = mockk()
        restApi = mockk()
        tokenManager = mockk(relaxUnitFun = true)
        every { tokenManager.getUserId() } returns "user-1"
        every { tokenManager.getRole() } returns "student"
        every { tokenManager.getAccessToken() } returns "token"
        every { tokenManager.getRefreshToken() } returns "refresh"
        repository = AuthRepositoryImpl(authApi, restApi, tokenManager, mockk(relaxed = true))
    }

    @Test
    fun `updateLanguage writes the correct db code for isiZulu`() = runTest {
        val bodySlot = slot<Map<String, Any?>>()
        coEvery { restApi.updateProfile(idFilter = "eq.user-1", body = capture(bodySlot)) } returns
            Response.success(listOf(ProfileDto(id = "user-1", fullName = "Test User", preferredLanguage = "zu")))
        coEvery { restApi.getProfile(idFilter = "eq.user-1") } returns
            Response.success(listOf(ProfileDto(id = "user-1", fullName = "Test User", preferredLanguage = "zu")))

        val outcome = repository.updateLanguage(AppLanguage.ISIZULU)

        assertTrue(outcome is AuthOutcome.Success)
        assertEquals("zu", bodySlot.captured["preferred_language"])
    }

    @Test
    fun `updateLanguage writes the correct db code for Tshivenda`() = runTest {
        val bodySlot = slot<Map<String, Any?>>()
        coEvery { restApi.updateProfile(idFilter = "eq.user-1", body = capture(bodySlot)) } returns
            Response.success(listOf(ProfileDto(id = "user-1", fullName = "Test User", preferredLanguage = "ve")))
        coEvery { restApi.getProfile(idFilter = "eq.user-1") } returns
            Response.success(listOf(ProfileDto(id = "user-1", fullName = "Test User", preferredLanguage = "ve")))

        repository.updateLanguage(AppLanguage.TSHIVENDA)

        assertEquals("ve", bodySlot.captured["preferred_language"])
    }

    @Test
    fun `updateLanguage updates currentUser so the Settings screen reflects the new choice immediately`() = runTest {
        coEvery { restApi.updateProfile(idFilter = any(), body = any()) } returns
            Response.success(listOf(ProfileDto(id = "user-1", fullName = "Test User", preferredLanguage = "zu")))
        coEvery { restApi.getProfile(idFilter = "eq.user-1") } returns
            Response.success(listOf(ProfileDto(id = "user-1", fullName = "Test User", preferredLanguage = "zu")))

        repository.updateLanguage(AppLanguage.ISIZULU)

        assertEquals(AppLanguage.ISIZULU, repository.currentUser.value?.preferredLanguage)
    }

    @Test
    fun `updateLanguage fails cleanly when not signed in`() = runTest {
        every { tokenManager.getUserId() } returns null

        val outcome = repository.updateLanguage(AppLanguage.ENGLISH)

        assertTrue(outcome is AuthOutcome.Failure)
        coVerify(exactly = 0) { restApi.updateProfile(idFilter = any(), body = any()) }
    }

    @Test
    fun `updateLanguage surfaces a failure when the server rejects the write`() = runTest {
        coEvery { restApi.updateProfile(idFilter = any(), body = any()) } returns
            Response.error(500, "".toResponseBody("application/json".toMediaType()))

        val outcome = repository.updateLanguage(AppLanguage.ENGLISH)

        assertTrue(outcome is AuthOutcome.Failure)
    }

    @Test
    fun `updateNotificationPreferences writes all three flags to the profile row`() = runTest {
        val bodySlot = slot<Map<String, Any?>>()
        coEvery { restApi.updateProfile(idFilter = "eq.user-1", body = capture(bodySlot)) } returns
            Response.success(listOf(ProfileDto(id = "user-1", fullName = "Test User")))
        coEvery { restApi.getProfile(idFilter = "eq.user-1") } returns
            Response.success(listOf(ProfileDto(id = "user-1", fullName = "Test User")))

        val prefs = com.rentease.app.domain.model.NotificationPreferences(
            bookingUpdates = false, newMessages = true, maintenanceUpdates = false
        )
        val outcome = repository.updateNotificationPreferences(prefs)

        assertTrue(outcome is AuthOutcome.Success)
        assertEquals(false, bodySlot.captured["notify_booking_updates"])
        assertEquals(true, bodySlot.captured["notify_new_messages"])
        assertEquals(false, bodySlot.captured["notify_maintenance_updates"])
    }

    @Test
    fun `updateNotificationPreferences fails cleanly when not signed in`() = runTest {
        every { tokenManager.getUserId() } returns null

        val outcome = repository.updateNotificationPreferences(com.rentease.app.domain.model.NotificationPreferences())

        assertTrue(outcome is AuthOutcome.Failure)
        coVerify(exactly = 0) { restApi.updateProfile(idFilter = any(), body = any()) }
    }

    @Test
    fun `resolveAndCacheUser carries notification preferences through from the profile row`() = runTest {
        coEvery { authApi.signInWithPassword(body = any()) } returns Response.success(
            AuthSessionResponseDto(
                accessToken = "t", refreshToken = "r", expiresIn = 3600, tokenType = "bearer",
                user = GoTrueUserDto(id = "user-1", email = "test@example.com")
            )
        )
        coEvery { restApi.getRoles(userIdFilter = "eq.user-1") } returns
            Response.success(listOf(com.rentease.app.data.remote.dto.RoleDto(userId = "user-1", role = "student")))
        coEvery { restApi.getProfile(idFilter = "eq.user-1") } returns Response.success(
            listOf(
                ProfileDto(
                    id = "user-1", fullName = "Test User",
                    notifyBookingUpdates = false, notifyNewMessages = true, notifyMaintenanceUpdates = false
                )
            )
        )

        val outcome = repository.login("test@example.com", "password")

        assertTrue(outcome is AuthOutcome.Success)
        val user = (outcome as AuthOutcome.Success).user
        assertEquals(false, user.notificationPreferences.bookingUpdates)
        assertEquals(true, user.notificationPreferences.newMessages)
        assertEquals(false, user.notificationPreferences.maintenanceUpdates)
    }
}
