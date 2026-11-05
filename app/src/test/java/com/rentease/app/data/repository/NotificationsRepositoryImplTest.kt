package com.rentease.app.data.repository

import com.rentease.app.data.auth.TokenManager
import com.rentease.app.data.remote.api.SupabaseNotificationsApi
import com.rentease.app.data.remote.dto.DeviceTokenDto
import com.rentease.app.data.remote.dto.NotificationDto
import com.rentease.app.domain.repository.NotificationsResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

class NotificationsRepositoryImplTest {

    private lateinit var api: SupabaseNotificationsApi
    private lateinit var tokenManager: TokenManager
    private lateinit var repository: NotificationsRepositoryImpl

    @Before
    fun setUp() {
        api = mockk()
        tokenManager = mockk()
        every { tokenManager.getUserId() } returns "user-1"
        repository = NotificationsRepositoryImpl(api, tokenManager, mockk(relaxed = true))
    }

    @Test
    fun `getNotifications maps DTOs to domain models for the signed-in user`() = runTest {
        coEvery { api.getNotifications(userIdFilter = "eq.user-1") } returns Response.success(
            listOf(NotificationDto(id = "n1", title = "Booking approved", body = "Your booking was approved", category = "booking"))
        )

        val result = repository.getNotifications()

        assertTrue(result is NotificationsResult.Success)
        val notifications = (result as NotificationsResult.Success).data
        assertEquals(1, notifications.size)
        assertEquals("Booking approved", notifications.first().title)
        assertEquals("booking", notifications.first().category)
    }

    @Test
    fun `getNotifications fails cleanly when not signed in`() = runTest {
        every { tokenManager.getUserId() } returns null

        val result = repository.getNotifications()

        assertTrue(result is NotificationsResult.Failure)
    }

    @Test
    fun `markRead sends a read_at timestamp`() = runTest {
        coEvery { api.markRead(idFilter = "eq.notif-1", body = any()) } returns Response.success(Unit)

        val result = repository.markRead("notif-1")

        assertTrue(result is NotificationsResult.Success)
        coVerify { api.markRead(idFilter = "eq.notif-1", body = match { it.containsKey("read_at") }) }
    }

    @Test
    fun `registerDeviceToken associates the FCM token with the signed-in user`() = runTest {
        coEvery { api.registerDeviceToken(DeviceTokenDto(userId = "user-1", fcmToken = "fcm-token-abc")) } returns Response.success(Unit)

        val result = repository.registerDeviceToken("fcm-token-abc")

        assertTrue(result is NotificationsResult.Success)
    }

    @Test
    fun `registerDeviceToken fails cleanly when not signed in rather than sending a null user id`() = runTest {
        every { tokenManager.getUserId() } returns null

        val result = repository.registerDeviceToken("fcm-token-abc")

        assertTrue(result is NotificationsResult.Failure)
        coVerify(exactly = 0) { api.registerDeviceToken(any()) }
    }

    @Test
    fun `registerDeviceToken surfaces a failure on a non-2xx response`() = runTest {
        coEvery { api.registerDeviceToken(any()) } returns Response.error(500, "".toResponseBody("application/json".toMediaType()))

        val result = repository.registerDeviceToken("fcm-token-abc")

        assertTrue(result is NotificationsResult.Failure)
    }
}
