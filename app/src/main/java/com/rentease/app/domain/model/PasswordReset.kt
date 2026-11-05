package com.rentease.app.domain.model

import java.net.URLDecoder

sealed interface PasswordResetResult {
    data object Success : PasswordResetResult
    data class Failure(val message: String) : PasswordResetResult
}

/**
 * Rules for the forgot-password flow. Pure so they are unit-testable.
 *
 * Flow: the app asks Supabase to email a link whose redirect is [REDIRECT_URL]. Tapping the link
 * opens the app at `rentease://reset-password#access_token=...&type=recovery`; the short-lived
 * recovery token is then used (only) to set the new password.
 */
object PasswordReset {
    const val REDIRECT_URL = "rentease://reset-password"
    const val MIN_LENGTH = 8

    enum class Problem { TOO_SHORT, MISMATCH }

    fun validate(password: String, confirm: String): Problem? = when {
        password.length < MIN_LENGTH -> Problem.TOO_SHORT
        password != confirm -> Problem.MISMATCH
        else -> null
    }

    fun isPlausibleEmail(email: String): Boolean {
        val e = email.trim()
        val at = e.indexOf('@')
        return at > 0 && at == e.lastIndexOf('@') && e.indexOf('.', at) > at + 1 && !e.endsWith(".") && !e.contains(' ')
    }

    /**
     * Returns the recovery access token from a `rentease://reset-password` link, or null when the
     * link is not a recovery link. Supabase puts the values in the URL fragment; the query form is
     * accepted too.
     */
    fun parseRecoveryToken(rawUri: String): String? {
        if (!rawUri.startsWith("rentease://reset-password")) return null
        val rest = rawUri.removePrefix("rentease://reset-password")
        val params = rest.trimStart('#', '?', '/').let { if (it.contains('#')) it.substringAfter('#') else it }
        val map = params.split('&').filter { it.contains('=') }.associate {
            val (k, v) = it.split('=', limit = 2)
            k to runCatching { URLDecoder.decode(v, "UTF-8") }.getOrDefault(v)
        }
        if (map["type"] != "recovery") return null
        return map["access_token"]?.takeIf { it.isNotBlank() }
    }
}
