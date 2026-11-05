package com.rentease.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RoomTypeGuideTest {
    @Test fun `R3400 sharing room is within the shared range`() {
        val shared = RoomTypeGuide.SHARED
        assertTrue(3_400 in shared.typicalMin..shared.typicalMax)
        assertFalse(shared.looksUnrealistic(3_400.0))
    }

    @Test fun `a R500 bachelor room is flagged as unusual but R5000 is not`() {
        assertTrue(RoomTypeGuide.STUDIO.looksUnrealistic(500.0))
        assertFalse(RoomTypeGuide.STUDIO.looksUnrealistic(5_000.0))
    }

    @Test fun `fromDb is case insensitive and matches web values`() {
        assertEquals(RoomTypeGuide.ENSUITE, RoomTypeGuide.fromDb("ensuite"))
        assertEquals(RoomTypeGuide.STUDIO, RoomTypeGuide.fromDb("Studio"))
        assertEquals(null, RoomTypeGuide.fromDb("castle"))
    }
}
