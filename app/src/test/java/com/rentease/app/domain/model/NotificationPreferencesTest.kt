package com.rentease.app.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationPreferencesTest {

    @Test
    fun `each category follows its own toggle`() {
        val prefs = NotificationPreferences(bookingUpdates = false, newMessages = true, maintenanceUpdates = false)
        assertFalse(prefs.allowsCategory("booking"))
        assertTrue(prefs.allowsCategory("message"))
        assertFalse(prefs.allowsCategory("maintenance"))
    }

    @Test
    fun `system and unknown categories are never suppressed`() {
        val allOff = NotificationPreferences(bookingUpdates = false, newMessages = false, maintenanceUpdates = false)
        assertTrue(allOff.allowsCategory("system"))
        assertTrue(allOff.allowsCategory("something-new"))
    }
}
