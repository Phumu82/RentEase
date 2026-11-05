package com.rentease.app.domain.model

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** The failures people actually hit on sign-up / sign-in, so they get a sentence instead of raw JSON. */
enum class AuthErrorKind {
    EMAIL_TAKEN,
    WEAK_PASSWORD,
    RATE_LIMITED,
    EMAIL_INVALID,
    NO_CONNECTION,
    INVALID_CREDENTIALS,
    EMAIL_NOT_CONFIRMED,
    UNKNOWN
}

/** Turns a GoTrue error response (status + JSON body) into a kind and, failing that, its plain message text. */
object AuthErrorParser {

    private val FIELDS = listOf("error_code", "msg", "message", "error_description", "error")

    fun classify(httpCode: Int, body: String?): AuthErrorKind {
        if (httpCode == 503 && (body.isNullOrBlank() || "network_unreachable" in body)) {
            return AuthErrorKind.NO_CONNECTION
        }
        val text = fieldValues(body).joinToString(" ").lowercase()
        return when {
            "user_already_exists" in text || "already registered" in text || "already been registered" in text ->
                AuthErrorKind.EMAIL_TAKEN
            "weak_password" in text || "password should" in text -> AuthErrorKind.WEAK_PASSWORD
            "rate_limit" in text || "rate limit" in text || httpCode == 429 -> AuthErrorKind.RATE_LIMITED
            "invalid_credentials" in text || "invalid login credentials" in text -> AuthErrorKind.INVALID_CREDENTIALS
            "email_not_confirmed" in text || "email not confirmed" in text -> AuthErrorKind.EMAIL_NOT_CONFIRMED
            "email_address_invalid" in text || "unable to validate email" in text ||
                ("email" in text && "invalid" in text) -> AuthErrorKind.EMAIL_INVALID
            else -> AuthErrorKind.UNKNOWN
        }
    }

    /** The server's own message (msg / message / error_description / error), or the raw body when it is not JSON. */
    fun messageText(body: String?): String? {
        if (body.isNullOrBlank()) return null
        val obj = parse(body) ?: return body.trim()
        for (key in listOf("msg", "message", "error_description", "error")) {
            obj[key]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }?.let { return it }
        }
        return null
    }

    private fun fieldValues(body: String?): List<String> {
        if (body.isNullOrBlank()) return emptyList()
        val obj = parse(body) ?: return listOf(body)
        return FIELDS.mapNotNull { obj[it]?.jsonPrimitive?.contentOrNull }
    }

    private fun parse(body: String): JsonObject? =
        runCatching { Json.parseToJsonElement(body).jsonObject }.getOrNull()
}

/** Client-side checks for the short sign-up forms, so mistakes are caught before a network call. */
object RegistrationValidation {

    enum class Problem { MISSING_FIELDS, EMAIL_INVALID, PHONE_INVALID, PASSWORD_RULES }

    private val PHONE = Regex("^[+0-9][0-9 ()-]{8,19}$")

    /** Same password rule as the website: 8+ characters with at least one letter and one number. */
    fun passwordMeetsRules(password: String): Boolean =
        password.length in 8..72 && password.any { it.isLetter() } && password.any { it.isDigit() }

    fun validate(fullName: String, email: String, phone: String, password: String, extraRequired: List<String> = emptyList()): Problem? = when {
        (listOf(fullName, email, phone, password) + extraRequired).any { it.isBlank() } -> Problem.MISSING_FIELDS
        !PasswordReset.isPlausibleEmail(email) -> Problem.EMAIL_INVALID
        !PHONE.matches(phone.trim()) -> Problem.PHONE_INVALID
        !passwordMeetsRules(password) -> Problem.PASSWORD_RULES
        else -> null
    }
}
