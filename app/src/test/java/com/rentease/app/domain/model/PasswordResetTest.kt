package com.rentease.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordResetTest {
    @Test fun `validate checks length then match`() {
        assertEquals(PasswordReset.Problem.TOO_SHORT, PasswordReset.validate("short", "short"))
        assertEquals(PasswordReset.Problem.MISMATCH, PasswordReset.validate("longenough1", "different1"))
        assertNull(PasswordReset.validate("longenough1", "longenough1"))
    }

    @Test fun `email plausibility`() {
        assertTrue(PasswordReset.isPlausibleEmail("a@b.co"))
        assertTrue(PasswordReset.isPlausibleEmail(" phumudzo@example.com "))
        assertFalse(PasswordReset.isPlausibleEmail("no-at.example.com"))
        assertFalse(PasswordReset.isPlausibleEmail("a@b"))
        assertFalse(PasswordReset.isPlausibleEmail("a b@c.com"))
        assertFalse(PasswordReset.isPlausibleEmail("@c.com"))
    }

    @Test fun `parses the recovery token from the fragment`() {
        val link = "rentease://reset-password#access_token=abc.def.ghi&expires_in=3600&refresh_token=xyz&token_type=bearer&type=recovery"
        assertEquals("abc.def.ghi", PasswordReset.parseRecoveryToken(link))
    }

    @Test fun `parses the query form too`() {
        assertEquals("tok", PasswordReset.parseRecoveryToken("rentease://reset-password?access_token=tok&type=recovery"))
    }

    @Test fun `ignores links that are not recovery links`() {
        assertNull(PasswordReset.parseRecoveryToken("rentease://listing/some-slug"))
        assertNull(PasswordReset.parseRecoveryToken("rentease://reset-password#access_token=abc&type=signup"))
        assertNull(PasswordReset.parseRecoveryToken("rentease://reset-password"))
        assertNull(PasswordReset.parseRecoveryToken("rentease://reset-password#error=access_denied&error_code=otp_expired"))
    }
}
