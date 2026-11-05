package com.rentease.app.domain.repository

import com.rentease.app.domain.model.BookableRoom
import com.rentease.app.domain.model.BookingRequestInput
import com.rentease.app.domain.model.MaintenanceRequestInput
import com.rentease.app.domain.model.MaintenanceTicket
import com.rentease.app.domain.model.Payment
import com.rentease.app.domain.model.PropertyDetail
import com.rentease.app.domain.model.PropertySummary
import com.rentease.app.domain.model.ReviewInput
import com.rentease.app.domain.model.SearchFilters
import com.rentease.app.domain.model.StudentBookingSummary
import com.rentease.app.domain.model.University

sealed interface ListingsResult<out T> {
    data class Success<T>(val data: T) : ListingsResult<T>
    data class Failure(val message: String) : ListingsResult<Nothing>
}

interface ListingsRepository {
    suspend fun getUniversities(): ListingsResult<List<University>>

    /** Available rooms of a property for the booking sheet, cheapest first. */
    suspend fun getAvailableRooms(propertyId: String): ListingsResult<List<BookableRoom>>
    suspend fun search(filters: SearchFilters, page: Int, pageSize: Int = 12): ListingsResult<List<PropertySummary>>
    suspend fun getPropertyDetail(slug: String): ListingsResult<PropertyDetail>

    /** Other published properties in the same city, closest in rent first (detail page "Similar properties"). */
    suspend fun getSimilar(property: PropertySummary): ListingsResult<List<PropertySummary>>
    suspend fun toggleFavourite(propertyId: String, isCurrentlyFavourited: Boolean): ListingsResult<Boolean>
    suspend fun createBookingRequest(input: BookingRequestInput): ListingsResult<String>

    /** Student's own booking history, most recent first — Part 1 §3.5:
     *  "Students track booking status in real time on their dashboard." */
    suspend fun getMyBookings(): ListingsResult<List<StudentBookingSummary>>

    /** Cancels one of the student's own bookings. Mirrors the web app's rule:
     *  only a "pending" booking can be cancelled this way. */
    suspend fun cancelBooking(bookingId: String): ListingsResult<Unit>

    /** Posts a review for a property the student booked. Fails with a
     *  friendly message if that booking was already reviewed (one review per
     *  booking, enforced by a unique index server-side). */
    suspend fun createReview(input: ReviewInput): ListingsResult<Unit>

    /** IDs of the student's bookings that already have a review, so the UI can
     *  stop offering "Write a review" for them. */
    suspend fun getMyReviewedBookingIds(): ListingsResult<Set<String>>

    /** Student's own maintenance requests, most recent first — Part 1 §3.9
     *  (student/maintenance): "Report an issue at your accommodation and
     *  follow the landlord's progress." */
    suspend fun getMyMaintenanceRequests(): ListingsResult<List<MaintenanceTicket>>

    /** Logs a new maintenance issue against one of the student's own
     *  bookings/properties. Mirrors the web app's insert into
     *  `maintenance_requests` (status defaults to "open" server-side). */
    suspend fun createMaintenanceRequest(input: MaintenanceRequestInput): ListingsResult<Unit>

    /** Student's own payment records (deposits/rent/fees), most recent first —
     *  see [Payment]'s doc comment: this is a self-reported ledger, not a
     *  gateway. */
    suspend fun getMyPayments(): ListingsResult<List<Payment>>

    /** The student self-reporting that they've actually paid, by whatever
     *  means (EFT, in person, etc.) — mirrors `student.payments.tsx`'s "Pay
     *  now" button exactly: a plain status + paid_at update, no gateway call
     *  of any kind. */
    suspend fun markPaymentPaid(paymentId: String): ListingsResult<Unit>
}
