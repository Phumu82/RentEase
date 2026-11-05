package com.rentease.app.presentation.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SamplePhotosTest {
    @Test fun indexIsStableAndInRange() {
        listOf("", "a", "campus-view-house", "zzzzzzzzzzzzzzzzzzzzzzz", "Ünïcode", "x".repeat(500)).forEach { key ->
            val i = SamplePhotos.indexFor(key, 4)
            assertTrue(key, i in 0..3)
            assertEquals(i, SamplePhotos.indexFor(key, 4))
        }
    }

    @Test fun handlesNegativeHashCodes() {
        // "polygenelubricants" has Integer.MIN_VALUE as its hashCode.
        assertEquals(0, SamplePhotos.indexFor("polygenelubricants", 4))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsEmptyPool() { SamplePhotos.indexFor("a", 0) }
}
