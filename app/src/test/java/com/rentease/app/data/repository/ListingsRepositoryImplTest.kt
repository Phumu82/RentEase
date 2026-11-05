package com.rentease.app.data.repository

import com.rentease.app.data.auth.TokenManager
import com.rentease.app.data.remote.api.SupabaseListingsApi
import com.rentease.app.data.remote.dto.BookingDto
import com.rentease.app.data.remote.dto.BookingUpdateDto
import com.rentease.app.data.remote.dto.FavoriteDto
import com.rentease.app.data.remote.dto.MaintenanceRequestCreateDto
import com.rentease.app.data.remote.dto.PaymentCreateDto
import com.rentease.app.data.remote.dto.RoomOptionDto
import com.rentease.app.data.remote.dto.PaymentDto
import com.rentease.app.data.remote.dto.PaymentMarkPaidDto
import com.rentease.app.data.remote.dto.MaintenanceTicketDto
import com.rentease.app.data.remote.dto.NameLookupDto
import com.rentease.app.data.remote.dto.PropertyDto
import com.rentease.app.data.remote.dto.ReviewCreateDto
import com.rentease.app.data.remote.dto.ReviewedBookingDto
import com.rentease.app.domain.model.MaintenancePriority
import com.rentease.app.domain.model.BookingRequestInput
import com.rentease.app.domain.model.MaintenanceRequestInput
import com.rentease.app.domain.model.PaymentStatus
import com.rentease.app.domain.model.ReviewInput
import com.rentease.app.domain.model.SearchFilters
import com.rentease.app.domain.repository.ListingsResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

class ListingsRepositoryImplTest {

    private lateinit var api: SupabaseListingsApi
    private lateinit var tokenManager: TokenManager
    private lateinit var repository: ListingsRepositoryImpl

    private val sampleProperty = PropertyDto(
        id = "prop-1",
        slug = "cozy-studio-hatfield",
        title = "Cozy Studio",
        city = "Pretoria",
        province = "Gauteng",
        pricePerMonth = 4500.0
    )

    @Before
    fun setUp() {
        api = mockk()
        tokenManager = mockk()
        every { tokenManager.getUserId() } returns null // anonymous search by default
        repository = ListingsRepositoryImpl(api, tokenManager, mockk(relaxed = true))
    }

    @Test
    fun `search always constrains results to published properties`() = runTest {
        val filtersSlot = slot<Map<String, String>>()
        coEvery { api.searchProperties(filters = capture(filtersSlot), range = any()) } returns
            Response.success(listOf(sampleProperty))

        repository.search(SearchFilters(), page = 0)

        assertEquals("eq.published", filtersSlot.captured["status"])
    }

    @Test
    fun `search applies availableOnly as rooms_available greater than zero`() = runTest {
        val filtersSlot = slot<Map<String, String>>()
        coEvery { api.searchProperties(filters = capture(filtersSlot), range = any()) } returns
            Response.success(listOf(sampleProperty))

        repository.search(SearchFilters(availableOnly = true), page = 0)

        assertEquals("gt.0", filtersSlot.captured["rooms_available"])
    }

    @Test
    fun `search without availableOnly does not filter on rooms_available`() = runTest {
        val filtersSlot = slot<Map<String, String>>()
        coEvery { api.searchProperties(filters = capture(filtersSlot), range = any()) } returns
            Response.success(listOf(sampleProperty))

        repository.search(SearchFilters(), page = 0)

        assertEquals(null, filtersSlot.captured["rooms_available"])
    }

    @Test
    fun `search applies maxRent as a lte filter`() = runTest {
        val filtersSlot = slot<Map<String, String>>()
        coEvery { api.searchProperties(filters = capture(filtersSlot), range = any()) } returns
            Response.success(emptyList())

        repository.search(SearchFilters(maxRent = 5000.0), page = 0)

        assertEquals("lte.5000.0", filtersSlot.captured["price_per_month"])
    }

    @Test
    fun `search applies amenities as a PostgREST array contains filter`() = runTest {
        val filtersSlot = slot<Map<String, String>>()
        coEvery { api.searchProperties(filters = capture(filtersSlot), range = any()) } returns
            Response.success(emptyList())

        repository.search(SearchFilters(amenities = setOf("WiFi", "Parking")), page = 0)

        assertEquals("cs.{WiFi,Parking}", filtersSlot.captured["amenities"])
    }

    @Test
    fun `search paginates using a PostgREST Range header derived from page and pageSize`() = runTest {
        val rangeSlot = slot<String>()
        coEvery { api.searchProperties(filters = any(), range = capture(rangeSlot)) } returns
            Response.success(emptyList())

        repository.search(SearchFilters(), page = 2, pageSize = 12)

        assertEquals("24-35", rangeSlot.captured)
    }

    @Test
    fun `search returns Failure on a non-2xx response instead of throwing`() = runTest {
        coEvery { api.searchProperties(filters = any(), range = any()) } returns
            Response.error(500, "".toResponseBody("application/json".toMediaType()))

        val result = repository.search(SearchFilters(), page = 0)

        assertTrue(result is ListingsResult.Failure)
    }

    @Test
    fun `toggleFavourite fails clearly when no user is signed in`() = runTest {
        every { tokenManager.getUserId() } returns null

        val result = repository.toggleFavourite(propertyId = "prop-1", isCurrentlyFavourited = false)

        assertTrue(result is ListingsResult.Failure)
    }

    @Test
    fun `toggleFavourite adds a favorite row when not currently favourited`() = runTest {
        every { tokenManager.getUserId() } returns "user-1"
        coEvery { api.addFavorite(any()) } returns Response.success(Unit)

        val result = repository.toggleFavourite(propertyId = "prop-1", isCurrentlyFavourited = false)

        assertTrue(result is ListingsResult.Success)
        assertTrue((result as ListingsResult.Success).data) // now favourited = true
    }

    @Test
    fun `getMyMaintenanceRequests fails clearly when no user is signed in`() = runTest {
        every { tokenManager.getUserId() } returns null

        val result = repository.getMyMaintenanceRequests()

        assertTrue(result is ListingsResult.Failure)
    }

    @Test
    fun `getMyMaintenanceRequests maps DTOs and resolves property titles`() = runTest {
        every { tokenManager.getUserId() } returns "student-1"
        coEvery { api.getMaintenanceForStudent(studentIdFilter = "eq.student-1") } returns
            Response.success(
                listOf(
                    MaintenanceTicketDto(
                        id = "mr-1", propertyId = "prop-1", studentId = "student-1",
                        title = "Leaking tap", description = "Bathroom tap won't stop dripping",
                        category = "plumbing", priority = "medium", status = "open"
                    )
                )
            )
        coEvery { api.getPropertyTitlesById(idInFilter = "in.(prop-1)") } returns
            Response.success(listOf(NameLookupDto(id = "prop-1", title = "Cozy Studio")))

        val result = repository.getMyMaintenanceRequests()

        assertTrue(result is ListingsResult.Success)
        val tickets = (result as ListingsResult.Success).data
        assertEquals(1, tickets.size)
        assertEquals("Cozy Studio", tickets.first().propertyTitle)
        assertEquals(MaintenancePriority.MEDIUM, tickets.first().priority)
    }

    @Test
    fun `createMaintenanceRequest fails clearly when no user is signed in`() = runTest {
        every { tokenManager.getUserId() } returns null

        val result = repository.createMaintenanceRequest(
            MaintenanceRequestInput(
                propertyId = "prop-1", bookingId = "booking-1", title = "Leaking tap",
                description = "Bathroom tap won't stop dripping", category = "plumbing",
                priority = MaintenancePriority.MEDIUM, photoUrl = null
            )
        )

        assertTrue(result is ListingsResult.Failure)
    }

    @Test
    fun `createMaintenanceRequest posts the signed-in student's id and lowercased priority`() = runTest {
        every { tokenManager.getUserId() } returns "student-1"
        val bodySlot = slot<MaintenanceRequestCreateDto>()
        coEvery { api.createMaintenanceRequest(body = capture(bodySlot), prefer = any()) } returns
            Response.success(
                listOf(
                    MaintenanceTicketDto(
                        id = "mr-1", propertyId = "prop-1", studentId = "student-1",
                        title = "Leaking tap", priority = "high", status = "open"
                    )
                )
            )

        val result = repository.createMaintenanceRequest(
            MaintenanceRequestInput(
                propertyId = "prop-1", bookingId = "booking-1", title = "Leaking tap",
                description = "Bathroom tap won't stop dripping", category = "plumbing",
                priority = MaintenancePriority.HIGH, photoUrl = null
            )
        )

        assertTrue(result is ListingsResult.Success)
        assertEquals("student-1", bodySlot.captured.studentId)
        assertEquals("high", bodySlot.captured.priority)
    }

    @Test
    fun `createMaintenanceRequest returns Failure on a non-2xx response`() = runTest {
        every { tokenManager.getUserId() } returns "student-1"
        coEvery { api.createMaintenanceRequest(body = any(), prefer = any()) } returns
            Response.error(500, "".toResponseBody("application/json".toMediaType()))

        val result = repository.createMaintenanceRequest(
            MaintenanceRequestInput(
                propertyId = "prop-1", bookingId = null, title = "Leaking tap",
                description = "Bathroom tap won't stop dripping", category = "plumbing",
                priority = MaintenancePriority.LOW, photoUrl = null
            )
        )

        assertTrue(result is ListingsResult.Failure)
    }

    // ---------- cancelBooking ----------

    @Test
    fun `cancelBooking fails clearly when no user is signed in`() = runTest {
        every { tokenManager.getUserId() } returns null

        val result = repository.cancelBooking("booking-1")

        assertTrue(result is ListingsResult.Failure)
    }

    @Test
    fun `cancelBooking sends a cancelled status for the given booking id`() = runTest {
        every { tokenManager.getUserId() } returns "student-1"
        val bodySlot = slot<BookingUpdateDto>()
        coEvery {
            api.cancelBooking(idFilter = "eq.booking-1", body = capture(bodySlot), prefer = any())
        } returns Response.success<List<BookingDto>>(emptyList())

        val result = repository.cancelBooking("booking-1")

        assertTrue(result is ListingsResult.Success)
        assertEquals("cancelled", bodySlot.captured.status)
    }

    @Test
    fun `cancelBooking returns Failure on a non-2xx response`() = runTest {
        every { tokenManager.getUserId() } returns "student-1"
        coEvery {
            api.cancelBooking(idFilter = any(), body = any(), prefer = any())
        } returns Response.error<List<BookingDto>>(403, "".toResponseBody("application/json".toMediaType()))

        val result = repository.cancelBooking("booking-1")

        assertTrue(result is ListingsResult.Failure)
        assertTrue((result as ListingsResult.Failure).message.contains("HTTP 403"))
    }

    // ---------- reviews ----------

    private fun reviewInput() = ReviewInput(
        propertyId = "prop-1", bookingId = "booking-1", authorName = "Naledi",
        rating = 5, title = "Great", body = "Lovely place close to campus"
    )

    @Test
    fun `createReview fails clearly when no user is signed in`() = runTest {
        every { tokenManager.getUserId() } returns null

        val result = repository.createReview(reviewInput())

        assertTrue(result is ListingsResult.Failure)
    }

    @Test
    fun `createReview posts the signed-in student's id with the booking and rating`() = runTest {
        every { tokenManager.getUserId() } returns "student-1"
        val bodySlot = slot<ReviewCreateDto>()
        coEvery { api.createReview(body = capture(bodySlot), prefer = any()) } returns Response.success(Unit)

        val result = repository.createReview(reviewInput())

        assertTrue(result is ListingsResult.Success)
        assertEquals("student-1", bodySlot.captured.studentId)
        assertEquals("booking-1", bodySlot.captured.bookingId)
        assertEquals("prop-1", bodySlot.captured.propertyId)
        assertEquals(5, bodySlot.captured.rating)
    }

    @Test
    fun `createReview maps HTTP 409 to the already-reviewed failure, not a generic HTTP error`() = runTest {
        every { tokenManager.getUserId() } returns "student-1"
        coEvery { api.createReview(body = any(), prefer = any()) } returns
            Response.error(409, "".toResponseBody("application/json".toMediaType()))

        val result = repository.createReview(reviewInput())

        assertTrue(result is ListingsResult.Failure)
        // The generic branch appends " (HTTP <code>)"; the 409 branch must not.
        assertFalse((result as ListingsResult.Failure).message.contains("HTTP"))
    }

    @Test
    fun `createReview reports the HTTP code for other failures`() = runTest {
        every { tokenManager.getUserId() } returns "student-1"
        coEvery { api.createReview(body = any(), prefer = any()) } returns
            Response.error(500, "".toResponseBody("application/json".toMediaType()))

        val result = repository.createReview(reviewInput())

        assertTrue(result is ListingsResult.Failure)
        assertTrue((result as ListingsResult.Failure).message.contains("HTTP 500"))
    }

    @Test
    fun `getMyReviewedBookingIds returns the set of reviewed booking ids and ignores nulls`() = runTest {
        every { tokenManager.getUserId() } returns "student-1"
        coEvery { api.getMyReviewedBookings(studentIdFilter = "eq.student-1", select = any()) } returns
            Response.success(
                listOf(
                    ReviewedBookingDto(bookingId = "booking-1"),
                    ReviewedBookingDto(bookingId = null),
                    ReviewedBookingDto(bookingId = "booking-2")
                )
            )

        val result = repository.getMyReviewedBookingIds()

        assertTrue(result is ListingsResult.Success)
        assertEquals(setOf("booking-1", "booking-2"), (result as ListingsResult.Success).data)
    }

    // ---------- createBookingRequest (now also auto-creates a deposit payment) ----------

    private fun bookingInput() = BookingRequestInput(
        propertyId = "prop-1", moveInDate = "2026-02-01", leaseMonths = 10,
        monthlyRent = 4200.0, deposit = 4200.0, message = null,
        idDocumentUrl = "https://example.com/id.pdf", proofOfRegistrationUrl = "https://example.com/reg.pdf"
    )

    @Test
    fun `createBookingRequest also creates a deposit payment on success`() = runTest {
        every { tokenManager.getUserId() } returns "student-1"
        coEvery { api.getBookingsForStudent(studentIdFilter = "eq.student-1") } returns Response.success(emptyList())
        coEvery { api.createBooking(body = any(), prefer = any()) } returns
            Response.success(listOf(BookingDto(id = "booking-1", reference = "RE-ABC123", studentId = "student-1", propertyId = "prop-1", moveInDate = "2026-02-01", leaseMonths = 10, monthlyRent = 4200.0, deposit = 4200.0, idDocumentUrl = "x", proofOfRegistrationUrl = "x")))
        val paymentBodySlot = slot<PaymentCreateDto>()
        coEvery { api.createPayment(body = capture(paymentBodySlot), prefer = any()) } returns Response.success(Unit)

        val result = repository.createBookingRequest(bookingInput())

        assertTrue(result is ListingsResult.Success)
        assertEquals("RE-ABC123", (result as ListingsResult.Success).data)
        assertEquals("booking-1", paymentBodySlot.captured.bookingId)
        assertEquals("student-1", paymentBodySlot.captured.studentId)
        assertEquals(4200.0, paymentBodySlot.captured.amount, 0.0)
    }

    @Test
    fun `createBookingRequest fails overall when the deposit payment insert fails`() = runTest {
        every { tokenManager.getUserId() } returns "student-1"
        coEvery { api.getBookingsForStudent(studentIdFilter = "eq.student-1") } returns Response.success(emptyList())
        coEvery { api.createBooking(body = any(), prefer = any()) } returns
            Response.success(listOf(BookingDto(id = "booking-1", reference = "RE-ABC123", studentId = "student-1", propertyId = "prop-1", moveInDate = "2026-02-01", leaseMonths = 10, monthlyRent = 4200.0, deposit = 4200.0, idDocumentUrl = "x", proofOfRegistrationUrl = "x")))
        coEvery { api.createPayment(body = any(), prefer = any()) } returns
            Response.error(500, "".toResponseBody("application/json".toMediaType()))

        val result = repository.createBookingRequest(bookingInput())

        // Mirrors the web app's `if (paymentError) throw paymentError` — the booking row
        // already exists server-side, but the overall call still reports failure.
        assertTrue(result is ListingsResult.Failure)
    }

    private fun existingBooking(status: String, id: String = "booking-9") = BookingDto(
        id = id, reference = "RE-OLD", studentId = "student-1", propertyId = "prop-1", moveInDate = "2026-02-01",
        leaseMonths = 10, monthlyRent = 4200.0, deposit = 4200.0, status = status,
        idDocumentUrl = "x", proofOfRegistrationUrl = "x"
    )

    @Test
    fun `createBookingRequest forwards the chosen room and the landlord id`() = runTest {
        every { tokenManager.getUserId() } returns "student-1"
        coEvery { api.getBookingsForStudent(studentIdFilter = "eq.student-1") } returns Response.success(emptyList())
        val bookingSlot = slot<BookingDto>()
        coEvery { api.createBooking(body = capture(bookingSlot), prefer = any()) } returns
            Response.success(listOf(existingBooking("pending", id = "booking-1")))
        coEvery { api.createPayment(body = any(), prefer = any()) } returns Response.success(Unit)

        repository.createBookingRequest(bookingInput().copy(roomId = "room-7", landlordId = "landlord-3"))

        assertEquals("room-7", bookingSlot.captured.roomId)
        assertEquals("landlord-3", bookingSlot.captured.landlordId)
    }

    @Test
    fun `createBookingRequest refuses a second open request for the same property`() = runTest {
        every { tokenManager.getUserId() } returns "student-1"
        coEvery { api.getBookingsForStudent(studentIdFilter = "eq.student-1") } returns
            Response.success(listOf(existingBooking("pending")))
        coEvery { api.getPaymentsForStudent(studentIdFilter = "eq.student-1") } returns
            Response.success(listOf(PaymentDto(id = "p1", amount = 4200.0, kind = "deposit", bookingId = "booking-9")))

        val result = repository.createBookingRequest(bookingInput())

        assertTrue(result is ListingsResult.Failure)
        coVerify(exactly = 0) { api.createBooking(body = any(), prefer = any()) }
        coVerify(exactly = 0) { api.createPayment(body = any(), prefer = any()) }
    }

    @Test
    fun `createBookingRequest ignores cancelled and rejected bookings for the duplicate check`() = runTest {
        every { tokenManager.getUserId() } returns "student-1"
        coEvery { api.getBookingsForStudent(studentIdFilter = "eq.student-1") } returns
            Response.success(listOf(existingBooking("cancelled"), existingBooking("rejected", id = "booking-8")))
        coEvery { api.createBooking(body = any(), prefer = any()) } returns
            Response.success(listOf(existingBooking("pending", id = "booking-1")))
        coEvery { api.createPayment(body = any(), prefer = any()) } returns Response.success(Unit)

        val result = repository.createBookingRequest(bookingInput())

        assertTrue(result is ListingsResult.Success)
        coVerify(exactly = 1) { api.createBooking(body = any(), prefer = any()) }
    }

    @Test
    fun `retry after a failed deposit repairs the existing booking instead of duplicating it`() = runTest {
        every { tokenManager.getUserId() } returns "student-1"
        coEvery { api.getBookingsForStudent(studentIdFilter = "eq.student-1") } returns
            Response.success(listOf(existingBooking("pending")))
        coEvery { api.getPaymentsForStudent(studentIdFilter = "eq.student-1") } returns Response.success(emptyList())
        val paymentSlot = slot<PaymentCreateDto>()
        coEvery { api.createPayment(body = capture(paymentSlot), prefer = any()) } returns Response.success(Unit)

        val result = repository.createBookingRequest(bookingInput())

        assertEquals("RE-OLD", (result as ListingsResult.Success).data)
        assertEquals("booking-9", paymentSlot.captured.bookingId)
        coVerify(exactly = 0) { api.createBooking(body = any(), prefer = any()) }
    }

    @Test
    fun `getAvailableRooms maps room options`() = runTest {
        coEvery { api.getAvailableRooms(propertyIdFilter = "eq.prop-1") } returns
            Response.success(listOf(RoomOptionDto(id = "r1", name = "Room 1", roomType = "en-suite", pricePerMonth = 3900.0)))

        val rooms = (repository.getAvailableRooms("prop-1") as ListingsResult.Success).data

        assertEquals(1, rooms.size)
        assertEquals("en-suite", rooms.first().roomType)
        assertEquals(3900.0, rooms.first().pricePerMonth, 0.0)
    }

    @Test
    fun `getAvailableRooms fails on a non-2xx response`() = runTest {
        coEvery { api.getAvailableRooms(propertyIdFilter = any()) } returns
            Response.error(500, "".toResponseBody("application/json".toMediaType()))

        assertTrue(repository.getAvailableRooms("prop-1") is ListingsResult.Failure)
    }

    // ---------- payments ----------

    @Test
    fun `getMyPayments fails clearly when no user is signed in`() = runTest {
        every { tokenManager.getUserId() } returns null

        val result = repository.getMyPayments()

        assertTrue(result is ListingsResult.Failure)
    }

    @Test
    fun `getMyPayments maps DTOs and resolves property titles via getMyBookings`() = runTest {
        every { tokenManager.getUserId() } returns "student-1"
        coEvery { api.getPaymentsForStudent(studentIdFilter = "eq.student-1") } returns
            Response.success(
                listOf(PaymentDto(id = "pay-1", reference = "PAY-XYZ", amount = 4200.0, kind = "deposit", status = "pending", bookingId = "booking-1"))
            )
        coEvery { api.getBookingsForStudent(studentIdFilter = "eq.student-1") } returns
            Response.success(listOf(BookingDto(id = "booking-1", reference = "RE-1", studentId = "student-1", propertyId = "prop-1", moveInDate = "2026-02-01", leaseMonths = 10, monthlyRent = 4200.0, deposit = 4200.0, idDocumentUrl = "x", proofOfRegistrationUrl = "x")))
        coEvery { api.getPropertyTitlesById(idInFilter = any()) } returns Response.success(listOf(NameLookupDto(id = "prop-1", title = "Cozy Studio")))

        val result = repository.getMyPayments()

        assertTrue(result is ListingsResult.Success)
        val payments = (result as ListingsResult.Success).data
        assertEquals(1, payments.size)
        assertEquals("Cozy Studio", payments.first().propertyTitle)
        assertEquals(PaymentStatus.PENDING, payments.first().status)
    }

    @Test
    fun `markPaymentPaid fails clearly when no user is signed in`() = runTest {
        every { tokenManager.getUserId() } returns null

        val result = repository.markPaymentPaid("pay-1")

        assertTrue(result is ListingsResult.Failure)
    }

    @Test
    fun `markPaymentPaid sends a paid status with a paid_at timestamp`() = runTest {
        every { tokenManager.getUserId() } returns "student-1"
        val bodySlot = slot<PaymentMarkPaidDto>()
        coEvery { api.markPaymentPaid(idFilter = "eq.pay-1", body = capture(bodySlot), prefer = any()) } returns Response.success(Unit)

        val result = repository.markPaymentPaid("pay-1")

        assertTrue(result is ListingsResult.Success)
        assertEquals("paid", bodySlot.captured.status)
        assertTrue(bodySlot.captured.paidAt.isNotBlank())
    }
}
