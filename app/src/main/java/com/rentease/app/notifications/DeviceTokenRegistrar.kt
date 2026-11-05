package com.rentease.app.notifications

import kotlinx.coroutines.withTimeoutOrNull
import com.google.firebase.messaging.FirebaseMessaging
import com.rentease.app.domain.repository.NotificationsRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fetches the current FCM token and registers it via NotificationsRepository.
 * Called right after any successful sign-in/registration (see LoginViewModel,
 * RegisterViewModel, AdminAccessViewModel) so a device is always associated
 * with whichever account is currently signed in — matching Part 1 §3.8's
 * "the Android app registers an FCM device token with the API on login."
 *
 * Deliberately a small standalone class rather than baked into
 * AuthRepositoryImpl, so the already-working, already-tested auth flow
 * doesn't need to change to pick this up.
 */
@Singleton
class DeviceTokenRegistrar @Inject constructor(
    private val notificationsRepository: NotificationsRepository
) {
    private companion object {
        const val TOKEN_TIMEOUT_MS = 8_000L
    }

    suspend fun registerCurrentToken() {
        // Bounded wait: with a placeholder google-services.json or an emulator without Play Services the
        // token task can stay pending forever, which must never hold up sign-in.
        val token = withTimeoutOrNull(TOKEN_TIMEOUT_MS) {
            runCatching { FirebaseMessaging.getInstance().token.await() }.getOrNull()
        } ?: return
        runCatching { notificationsRepository.registerDeviceToken(token) }
    }
}
