package com.rentease.app.domain.repository

import com.rentease.app.domain.model.AppLanguage
import com.rentease.app.domain.model.AuthOutcome
import com.rentease.app.domain.model.NotificationPreferences
import com.rentease.app.domain.model.PasswordResetResult
import com.rentease.app.domain.model.RegistrationOutcome
import com.rentease.app.domain.model.AuthUser
import com.rentease.app.domain.model.LandlordRegistrationInput
import com.rentease.app.domain.model.StudentRegistrationInput
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {

    /** Emits the current session's resolved user, or null when logged out. Backed by
     *  the encrypted token store + a cached profile fetch, not re-hit on every read. */
    val currentUser: StateFlow<AuthUser?>

    suspend fun registerStudent(input: StudentRegistrationInput): RegistrationOutcome
    suspend fun registerLandlord(input: LandlordRegistrationInput): RegistrationOutcome
    suspend fun login(email: String, password: String): AuthOutcome
    /** [nonce] is the RAW nonce whose SHA-256 hash was given to Credential Manager. */
    suspend fun loginWithGoogleIdToken(idToken: String, nonce: String? = null): AuthOutcome
    suspend fun restoreSession(): AuthOutcome
    suspend fun logout()

    /** Emails a reset link (success is reported whether or not the address has an account). */
    suspend fun requestPasswordReset(email: String): PasswordResetResult

    /** Sets a new password with the recovery token taken from the emailed link. */
    suspend fun completePasswordReset(recoveryToken: String, newPassword: String): PasswordResetResult

    /** Re-fetches the user's role from the server (not the local cache) and
     *  updates both the encrypted token store and [currentUser]. Needed after
     *  a server-side role change — e.g. the admin-access Edge Function just
     *  granted 'admin' — that the locally cached role doesn't know about yet. */
    suspend fun refreshRole(): AuthOutcome

    /** Persists the student/landlord's language preference to their profile
     *  row server-side (so it follows them across devices/re-logins) and
     *  updates [currentUser]. Applying the change to the actual in-app
     *  locale (AppCompatDelegate.setApplicationLocales) is the caller's
     *  responsibility — see SettingsViewModel — since that's a UI-layer
     *  concern, not a data-layer one. */
    suspend fun updateLanguage(language: AppLanguage): AuthOutcome

    /** Persists notification preference toggles (booking/messages/maintenance)
     *  to the profile row server-side and updates [currentUser]. This is
     *  storage only — actually honoring these flags when deciding whether to
     *  send a push is a backend/Edge-Function concern, not something the
     *  Android app enforces client-side. */
    suspend fun updateNotificationPreferences(preferences: NotificationPreferences): AuthOutcome

    /** Persists editable profile fields (display name, phone) to the profile row
     *  and updates [currentUser]. A blank [fullName] is rejected before any
     *  network call; a blank [phone] clears the stored phone number. */
    suspend fun updateProfile(fullName: String, phone: String?): AuthOutcome
}
