package com.rentease.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class PropertyPhotosTest {
    @Test fun `initial keeps legacy cover first without duplicates`() {
        assertEquals(listOf("c", "a", "b"), PropertyPhotos.initial("c", listOf("a", "c", "b")))
    }

    @Test fun `initial ignores blank cover`() {
        assertEquals(listOf("a"), PropertyPhotos.initial(" ", listOf("a")))
    }

    @Test fun `add skips duplicates and respects the limit`() {
        assertEquals(listOf("a"), PropertyPhotos.add(listOf("a"), "a"))
        val full = (1..PropertyPhotos.MAX_PHOTOS).map { "p$it" }
        assertEquals(full, PropertyPhotos.add(full, "extra"))
        assertEquals(0, PropertyPhotos.remainingSlots(full))
    }

    @Test fun `makeCover moves photo to front`() {
        assertEquals(listOf("c", "a", "b"), PropertyPhotos.makeCover(listOf("a", "b", "c"), "c"))
        assertEquals(listOf("a", "b"), PropertyPhotos.makeCover(listOf("a", "b"), "zzz"))
    }

    @Test fun `remove drops the photo`() {
        assertEquals(listOf("b"), PropertyPhotos.remove(listOf("a", "b"), "a"))
    }
}
