package com.rentease.app.notifications

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.rentease.app.R
import com.rentease.app.domain.model.allowsCategory
import com.rentease.app.domain.repository.AuthRepository
import com.rentease.app.domain.repository.NotificationsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

/**
 * Handles both foreground and background FCM delivery:
 *  - When the app is in the foreground, FCM delivers ALL messages here
 *    (both notification and data payloads), so we always build the tray
 *    notification ourselves via NotificationPoster — this guarantees
 *    consistent behaviour whether the app is foregrounded or not, rather
 *    than relying on the OS's automatic "notification payload while
 *    backgrounded" behaviour (Part 1 §3.8: "Do not implement only local
 *    notifications" — this IS a real FCM message being received and acted
 *    on, not a fabricated local-only notification).
 *  - onNewToken fires whenever FCM (re)issues a token — e.g. first install,
 *    app data cleared, or token rotation — and re-registers it with the API.
 */
@AndroidEntryPoint
class RentEaseMessagingService : FirebaseMessagingService() {

    @Inject lateinit var notificationPoster: NotificationPoster
    @Inject lateinit var notificationsRepository: NotificationsRepository
    @Inject lateinit var authRepository: AuthRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val category = message.data["category"] ?: "system"
        val title = message.notification?.title ?: message.data["title"] ?: getString(R.string.app_name)
        val body = message.notification?.body ?: message.data["body"] ?: ""
        val deepLink = message.data["deep_link"] // e.g. "rentease://booking/<id>"

        // Honour Settings > Notification preferences on the device: if the signed-in
        // user switched this category off, drop the push (it still appears in the
        // in-app feed, which is written server-side). Unknown/system categories and
        // the no-session case are always shown. NOTE: this only applies to messages
        // delivered to onMessageReceived - i.e. data-only payloads, or any payload
        // while the app is foregrounded (see README > FCM).
        val preferences = authRepository.currentUser.value?.notificationPreferences
        if (preferences != null && !preferences.allowsCategory(category)) return

        val channelId = when (category) {
            "booking" -> NotificationChannels.BOOKINGS
            "message" -> NotificationChannels.MESSAGES
            "maintenance" -> NotificationChannels.MAINTENANCE
            else -> NotificationChannels.SYSTEM
        }

        notificationPoster.post(
            notificationId = Random.nextInt(),
            channelId = channelId,
            title = title,
            body = body,
            deepLink = deepLink
        )

        // The in-app Notifications feed (Part 1 §3.8: "also listed in an
        // in-app Notifications feed so nothing is missed if a push is
        // dropped") reads from the `notifications` table, which the backend
        // already wrote to before sending the push — nothing further to
        // persist here. This local post is purely the tray notification.
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        serviceScope.launch {
            runCatching { notificationsRepository.registerDeviceToken(token) }
        }
    }
}
