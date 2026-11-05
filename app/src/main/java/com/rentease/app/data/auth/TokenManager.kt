package com.rentease.app.data.auth

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stores the Supabase (GoTrue) access token and refresh token in
 * EncryptedSharedPreferences, exactly as required by Part 1 §3.1:
 * "the Android app stores [the refresh token] in the EncryptedSharedPreferences store."
 *
 * The access token is short-lived (Supabase default 1h, matching the PDF's JWT expiry);
 * the refresh token is used by [com.rentease.app.data.auth.AuthInterceptor] to obtain a
 * new access token transparently when a request comes back 401.
 */
@Singleton
class TokenManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val appContext = context

    // EncryptedSharedPreferences can throw (AEADBadTagException / KeyStoreException) when the Android
    // keystore no longer matches the stored file - e.g. after an emulator snapshot restore, a reinstall
    // or a cleared lock screen. That used to crash the app on launch with no way out; the stale file is
    // now discarded (the person just signs in again) and recreated.
    private val prefs = runCatching { openEncryptedPrefs() }.getOrElse {
        appContext.deleteSharedPreferences(PREFS_FILE)
        openEncryptedPrefs()
    }

    private fun openEncryptedPrefs() = EncryptedSharedPreferences.create(
        appContext,
        PREFS_FILE,
        MasterKey.Builder(appContext).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveSession(accessToken: String, refreshToken: String, userId: String, role: String) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .putString(KEY_USER_ID, userId)
            .putString(KEY_ROLE, role)
            .apply()
    }

    fun getAccessToken(): String? = prefs.getString(KEY_ACCESS_TOKEN, null)
    fun getRefreshToken(): String? = prefs.getString(KEY_REFRESH_TOKEN, null)
    fun getUserId(): String? = prefs.getString(KEY_USER_ID, null)
    fun getRole(): String? = prefs.getString(KEY_ROLE, null)

    fun updateAccessToken(accessToken: String) {
        prefs.edit().putString(KEY_ACCESS_TOKEN, accessToken).apply()
    }

    fun isLoggedIn(): Boolean = getAccessToken() != null && getRefreshToken() != null

    fun clear() {
        prefs.edit().clear().apply()
    }

    private companion object {
        const val PREFS_FILE = "rentease_secure_prefs"
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_REFRESH_TOKEN = "refresh_token"
        const val KEY_USER_ID = "user_id"
        const val KEY_ROLE = "role"
    }
}
