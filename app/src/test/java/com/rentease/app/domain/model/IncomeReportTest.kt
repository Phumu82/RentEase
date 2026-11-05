package com.rentease.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class IncomeReportTest {
    private fun booking(id: String, property: String, title: String, rent: Double, status: BookingStatus) = TenantBooking(
        id = id, reference = "RE-$id", studentId = "s$id", studentName = "Student $id", propertyId = property,
        propertyTitle = title, roomId = null, roomName = null, moveInDate = "2027-02-01", leaseMonths = 12,
        monthlyRent = rent, deposit = rent, status = status, message = null, idDocumentUrl = null, proofOfRegistrationUrl = null
    )

    private val bookings = listOf(
        booking("1", "p1", "Hatfield Rooms", 3400.0, BookingStatus.APPROVED),
        booking("2", "p1", "Hatfield Rooms", 5000.0, BookingStatus.ACTIVE),
        booking("3", "p2", "Glenwood Suites", 2800.0, BookingStatus.APPROVED),
        booking("4", "p2", "Glenwood Suites", 9999.0, BookingStatus.PENDING),
        booking("5", "p2", "Glenwood Suites", 9999.0, BookingStatus.REJECTED),
        booking("6", "p1", "Hatfield Rooms", 9999.0, BookingStatus.CANCELLED)
    )

    @Test fun `contracted income counts only approved and active bookings`() {
        assertEquals(11_200.0, IncomeReport.contractedMonthly(bookings), 0.001)
    }

    @Test fun `income by property is grouped and sorted highest first`() {
        val rows = IncomeReport.byProperty(bookings)
        assertEquals(listOf("p1", "p2"), rows.map { it.propertyId })
        assertEquals(8_400.0, rows[0].monthlyRent, 0.001)
        assertEquals(2, rows[0].tenants)
        assertEquals(1, rows[1].tenants)
    }

    @Test fun `no bookings gives an empty report`() {
        assertEquals(0.0, IncomeReport.contractedMonthly(emptyList()), 0.001)
        assertEquals(emptyList<PropertyIncome>(), IncomeReport.byProperty(emptyList()))
    }
}
