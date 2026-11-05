package com.rentease.app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.rentease.app.MainActivity
import com.rentease.app.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Notification channels, one per category so users can mute/configure them
 * independently in system settings (Part 1 §3.8/Phase 9 — "booking status
 * changes", "new chat message", "maintenance ticket's status changes").
 */
object NotificationChannels {
    const val BOOKINGS = "channel_bookings"
    const val MESSAGES = "channel_messages"
    const val MAINTENANCE = "channel_maintenance"
    const val SYSTEM = "channel_system"

    fun createAll(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        val channels = listOf(
            NotificationChannel(BOOKINGS, context.getString(R.string.notify_booking_updates), NotificationManager.IMPORTANCE_HIGH).apply {
                description = context.getString(R.string.channel_bookings_desc)
            },
            NotificationChannel(MESSAGES, context.getString(R.string.notify_new_messages), NotificationManager.IMPORTANCE_HIGH).apply {
                description = context.getString(R.string.channel_messages_desc)
            },
            NotificationChannel(MAINTENANCE, context.getString(R.string.notify_maintenance_updates), NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = context.getString(R.string.channel_maintenance_desc)
            },
            NotificationChannel(SYSTEM, context.getString(R.string.channel_system_name), NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = context.getString(R.string.channel_system_desc)
            }
        )
        manager.createNotificationChannels(channels)
    }
}

/**
 * Builds and posts the actual Android notification-tray notification for an
 * incoming FCM message, and wires the deep link (rentease://...) so tapping
 * it opens the relevant screen — MainActivity's intent-filter (see
 * AndroidManifest) + RentEaseNavGraph handle the actual routing once the
 * activity launches with that URI as its intent data.
 */
@Singleton
class NotificationPoster @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun post(
        notificationId: Int,
        channelId: String,
        title: String,
        body: String,
        deepLink: String?
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (deepLink != null) data = Uri.parse(deepLink)
        }
        val pendingIntent = PendingIntent.getActivity(
            context, notificationId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        // POST_NOTIFICATIONS is requested at runtime elsewhere (MainActivity);
        // NotificationManagerCompat itself checks the permission on API 33+
        // and no-ops rather than crashing if it hasn't been granted yet.
        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }
}
