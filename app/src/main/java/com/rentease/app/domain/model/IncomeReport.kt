package com.rentease.app.domain.model

/** Contracted monthly rent of one property's approved/active bookings. */
data class PropertyIncome(
    val propertyId: String,
    val title: String,
    val monthlyRent: Double,
    val tenants: Int
)

/**
 * Landlord income figures. "Contracted" is the monthly rent of bookings that are approved or active;
 * "collected" (computed from the payments table elsewhere) is money actually paid. Pure and unit-testable.
 */
object IncomeReport {
    private fun counts(status: BookingStatus) = status == BookingStatus.APPROVED || status == BookingStatus.ACTIVE

    fun contractedMonthly(bookings: List<TenantBooking>): Double =
        bookings.filter { counts(it.status) }.sumOf { it.monthlyRent }

    /** Highest income first. Properties without an approved/active booking are not listed. */
    fun byProperty(bookings: List<TenantBooking>): List<PropertyIncome> =
        bookings.filter { counts(it.status) }
            .groupBy { it.propertyId }
            .map { (id, rows) -> PropertyIncome(id, rows.first().propertyTitle, rows.sumOf { it.monthlyRent }, rows.size) }
            .sortedByDescending { it.monthlyRent }
}
