package com.rentease.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthErrorsTest {

    @Test
    fun `duplicate email is recognised by error code or by message`() {
        assertEquals(AuthErrorKind.EMAIL_TAKEN, AuthErrorParser.classify(422, """{"error_code":"user_already_exists","msg":"x"}"""))
        assertEquals(AuthErrorKind.EMAIL_TAKEN, AuthErrorParser.classify(400, """{"msg":"User already registered"}"""))
    }

    @Test
    fun `weak password, rate limit and invalid email are recognised`() {
        assertEquals(AuthErrorKind.WEAK_PASSWORD, AuthErrorParser.classify(422, """{"error_code":"weak_password","msg":"Password should be at least 6 characters."}"""))
        assertEquals(AuthErrorKind.RATE_LIMITED, AuthErrorParser.classify(429, """{"error_code":"over_email_send_rate_limit","msg":"email rate limit exceeded"}"""))
        assertEquals(AuthErrorKind.EMAIL_INVALID, AuthErrorParser.classify(400, """{"error_code":"email_address_invalid","msg":"Email address is invalid"}"""))
    }

    @Test
    fun `wrong sign-in details are not mistaken for an invalid email`() {
        assertEquals(AuthErrorKind.INVALID_CREDENTIALS, AuthErrorParser.classify(400, """{"error_code":"invalid_credentials","msg":"Invalid login credentials"}"""))
        assertEquals(AuthErrorKind.EMAIL_NOT_CONFIRMED, AuthErrorParser.classify(400, """{"error_code":"email_not_confirmed","msg":"Email not confirmed"}"""))
    }

    @Test
    fun `the synthetic offline response is reported as no connection`() {
        assertEquals(AuthErrorKind.NO_CONNECTION, AuthErrorParser.classify(503, ""))
        assertEquals(AuthErrorKind.NO_CONNECTION, AuthErrorParser.classify(503, null))
    }

    @Test
    fun `unknown errors fall back to the servers own message, never the raw JSON`() {
        val body = """{"code":500,"msg":"Database error saving new user"}"""
        assertEquals(AuthErrorKind.UNKNOWN, AuthErrorParser.classify(500, body))
        assertEquals("Database error saving new user", AuthErrorParser.messageText(body))
        assertEquals("invalid_grant", AuthErrorParser.messageText("""{"error":"invalid_grant"}"""))
        assertNull(AuthErrorParser.messageText(""))
        assertEquals("plain text", AuthErrorParser.messageText("plain text"))
    }

    @Test
    fun `password rule matches the website - 8 or more with a letter and a number`() {
        assertTrue(RegistrationValidation.passwordMeetsRules("password1"))
        assertFalse(RegistrationValidation.passwordMeetsRules("short1"))
        assertFalse(RegistrationValidation.passwordMeetsRules("onlyletters"))
        assertFalse(RegistrationValidation.passwordMeetsRules("12345678"))
    }

    @Test
    fun `validate returns the first problem in a sensible order`() {
        val ok = { n: String, e: String, ph: String, pw: String -> RegistrationValidation.validate(n, e, ph, pw) }
        assertNull(ok("Thandi", "t@example.com", "0821234567", "password1"))
        assertEquals(RegistrationValidation.Problem.MISSING_FIELDS, ok("", "t@example.com", "0821234567", "password1"))
        assertEquals(RegistrationValidation.Problem.EMAIL_INVALID, ok("Thandi", "not-an-email", "0821234567", "password1"))
        assertEquals(RegistrationValidation.Problem.PHONE_INVALID, ok("Thandi", "t@example.com", "abc", "password1"))
        assertEquals(RegistrationValidation.Problem.PASSWORD_RULES, ok("Thandi", "t@example.com", "0821234567", "short"))
        assertEquals(
            RegistrationValidation.Problem.MISSING_FIELDS,
            RegistrationValidation.validate("Thandi", "t@example.com", "0821234567", "password1", listOf("ST1", ""))
        )
    }

    @Test
    fun `interceptor marker body is classified as no connection`() {
        assertEquals(
            AuthErrorKind.NO_CONNECTION,
            AuthErrorParser.classify(503, """{"error":"network_unreachable","detail":"UnknownHostException"}""")
        )
    }

    @Test
    fun `placeholder or blank supabase config is not configured`() {
        assertEquals(false, SupabaseConfig.isConfigured("https://not-configured.invalid", "key"))
        assertEquals(false, SupabaseConfig.isConfigured("https://abc.supabase.co", ""))
        assertEquals(false, SupabaseConfig.isConfigured("", "key"))
        assertEquals(true, SupabaseConfig.isConfigured("https://abc.supabase.co", "anon"))
    }

    @Test
    fun `normalizeUrl repairs common local-properties slips and never returns something Retrofit rejects`() {
        assertEquals("https://abc.supabase.co", SupabaseConfig.normalizeUrl("abc.supabase.co"))
        assertEquals("https://abc.supabase.co", SupabaseConfig.normalizeUrl("  \"https://abc.supabase.co/\" "))
        assertEquals("https://abc.supabase.co", SupabaseConfig.normalizeUrl("'https://abc.supabase.co'"))
        assertEquals("https://not-configured.invalid", SupabaseConfig.normalizeUrl(""))
        assertEquals("https://not-configured.invalid", SupabaseConfig.normalizeUrl("https://"))
        assertEquals("https://not-configured.invalid", SupabaseConfig.normalizeUrl("my project"))
    }
}
