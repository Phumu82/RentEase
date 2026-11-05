package com.rentease.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class AppLanguageTest {

    @Test
    fun `fromDb resolves known language codes`() {
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromDb("en"))
        assertEquals(AppLanguage.ISIZULU, AppLanguage.fromDb("zu"))
        assertEquals(AppLanguage.TSHIVENDA, AppLanguage.fromDb("ve"))
    }

    @Test
    fun `fromDb falls back to English for null or unsupported codes`() {
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromDb(null))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromDb("fr"))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromDb(""))
    }
}
