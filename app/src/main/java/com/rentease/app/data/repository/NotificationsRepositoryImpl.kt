package com.rentease.app.data.repository

import com.rentease.app.data.remote.httpHint
import android.content.Context
import com.rentease.app.R
import com.rentease.app.data.auth.TokenManager
import com.rentease.app.data.remote.api.SupabaseNotificationsApi
import com.rentease.app.data.remote.dto.DeviceTokenDto
import com.rentease.app.domain.model.AppNotification
import com.rentease.app.domain.repository.NotificationsRepository
import com.rentease.app.domain.repository.NotificationsResult
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationsRepositoryImpl @Inject constructor(
    private val api: SupabaseNotificationsApi,
    private val tokenManager: TokenManager,
    @ApplicationContext private val context: Context
) : NotificationsRepository {

    override suspend fun getNotifications(): NotificationsResult<List<AppNotification>> {
        val userId = tokenManager.getUserId() ?: return NotificationsResult.Failure(context.getString(R.string.error_not_signed_in))
        val response = api.getNotifications(userIdFilter = "eq.$userId")
        return if (response.isSuccessful) {
            NotificationsResult.Success(
                response.body().orEmpty().map {
                    AppNotification(
                        id = it.id, title = it.title, body = it.body, category = it.category,
                        link = it.link, readAt = it.readAt, createdAt = it.createdAt
                    )
                }
            )
        } else {
            NotificationsResult.Failure("${context.getString(R.string.error_could_not_load_notifications)}${context.httpHint(response.code())}")
        }
    }

    override suspend fun markRead(notificationId: String): NotificationsResult<Unit> {
        val response = api.markRead(idFilter = "eq.$notificationId", body = mapOf("read_at" to Instant.now().toString()))
        return if (response.isSuccessful) NotificationsResult.Success(Unit)
        else NotificationsResult.Failure("${context.getString(R.string.error_could_not_update_notification)}${context.httpHint(response.code())}")
    }

    override suspend fun registerDeviceToken(token: String): NotificationsResult<Unit> {
        val userId = tokenManager.getUserId() ?: return NotificationsResult.Failure(context.getString(R.string.error_not_signed_in))
        val response = api.registerDeviceToken(DeviceTokenDto(userId = userId, fcmToken = token))
        return if (response.isSuccessful) NotificationsResult.Success(Unit)
        else NotificationsResult.Failure("${context.getString(R.string.error_could_not_register_device)}${context.httpHint(response.code())}")
    }
}
