package com.rentease.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BookingModelsTest {

    private val room = BookableRoom(id = "r1", name = "Room 1", roomType = "en-suite", pricePerMonth = 3900.0)

    @Test fun `quote uses the property rent when no room is chosen`() {
        val quote = BookingQuote.of(propertyRent = 4200.0, deposit = 4200.0, room = null)
        assertEquals(4200.0, quote.monthlyRent, 0.0)
        assertEquals(4200.0, quote.deposit, 0.0)
    }

    @Test fun `quote uses the chosen room price but keeps the property deposit`() {
        val quote = BookingQuote.of(propertyRent = 4200.0, deposit = 3000.0, room = room)
        assertEquals(3900.0, quote.monthlyRent, 0.0)
        assertEquals(3000.0, quote.deposit, 0.0)
    }

    @Test fun `lease options match the web dialog and include the default`() {
        assertEquals(listOf(6, 9, 12, 24), LeaseOptions.MONTHS)
        assertTrue(LeaseOptions.DEFAULT_MONTHS in LeaseOptions.MONTHS)
    }

    @Test fun `only pending approved and active bookings are open`() {
        assertTrue(isOpenBookingStatus("pending"))
        assertTrue(isOpenBookingStatus("APPROVED"))
        assertTrue(isOpenBookingStatus("active"))
        assertFalse(isOpenBookingStatus("cancelled"))
        assertFalse(isOpenBookingStatus("rejected"))
        assertFalse(isOpenBookingStatus("completed"))
    }
}
