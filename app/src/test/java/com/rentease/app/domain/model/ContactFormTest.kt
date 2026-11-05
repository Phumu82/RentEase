package com.rentease.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContactFormTest {
    private val goodMessage = "I would like to know how deposits work please."

    @Test fun validInputHasNoErrors() {
        assertTrue(ContactForm.validate("Thandi Nkosi", "thandi@example.com", "Deposits", goodMessage).isEmpty())
    }

    @Test fun shortFieldsAreFlagged() {
        val errors = ContactForm.validate("A", "thandi@example.com", "Hi", "too short")
        assertEquals(
            setOf(ContactForm.Field.NAME, ContactForm.Field.SUBJECT, ContactForm.Field.MESSAGE),
            errors
        )
    }

    @Test fun badEmailsAreFlagged() {
        listOf("", "plain", "a@b", "a@b.c", "a b@c.com").forEach {
            assertTrue(it, ContactForm.Field.EMAIL in ContactForm.validate("Thandi", it, "Hello", goodMessage))
        }
    }

    @Test fun surroundingWhitespaceIsIgnored() {
        assertTrue(ContactForm.validate("  Thandi  ", "  t@e.co  ", "  Hey  ", "  $goodMessage  ").isEmpty())
    }

    @Test fun overlongMessageIsFlagged() {
        val errors = ContactForm.validate("Thandi", "t@e.co", "Hello", "x".repeat(ContactForm.MESSAGE_MAX + 1))
        assertEquals(setOf(ContactForm.Field.MESSAGE), errors)
    }

    @Test fun emailBodyIncludesSender() {
        assertEquals("Hello there\n\n-- Thandi <t@e.co>", ContactForm.emailBody(" Thandi ", " t@e.co ", " Hello there "))
    }
}
