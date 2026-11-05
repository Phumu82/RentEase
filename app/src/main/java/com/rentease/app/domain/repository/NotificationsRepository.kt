package com.rentease.app.domain.repository

import com.rentease.app.domain.model.AppNotification

sealed interface NotificationsResult<out T> {
    data class Success<T>(val data: T) : NotificationsResult<T>
    data class Failure(val message: String) : NotificationsResult<Nothing>
}

interface NotificationsRepository {
    suspend fun getNotifications(): NotificationsResult<List<AppNotification>>
    suspend fun markRead(notificationId: String): NotificationsResult<Unit>

    /** Registers (or refreshes) this device's FCM token against the
     *  currently signed-in user. Called on login and from
     *  FirebaseMessagingService.onNewToken. */
    suspend fun registerDeviceToken(token: String): NotificationsResult<Unit>
}
