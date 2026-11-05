package com.rentease.app.data.repository

import com.rentease.app.data.auth.TokenManager
import com.rentease.app.data.remote.api.SupabaseLandlordApi
import com.rentease.app.data.remote.dto.LandlordBookingDto
import com.rentease.app.data.remote.dto.NameLookupDto
import com.rentease.app.data.remote.dto.PropertyDto
import com.rentease.app.data.remote.dto.RoomDto
import com.rentease.app.domain.repository.LandlordResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

class LandlordRepositoryImplTest {

    private lateinit var api: SupabaseLandlordApi
    private lateinit var tokenManager: TokenManager
    private lateinit var repository: LandlordRepositoryImpl

    private val landlordId = "landlord-1"

    @Before
    fun setUp() {
        api = mockk()
        tokenManager = mockk()
        every { tokenManager.getUserId() } returns landlordId
        repository = LandlordRepositoryImpl(api, tokenManager, mockk(relaxed = true))
    }

    @Test
    fun `approveBooking with a room assigns the room and marks it occupied`() = runTest {
        coEvery { api.updateBooking(idFilter = "eq.booking-1", body = any()) } returns
            Response.success(listOf(bookingDto(id = "booking-1", status = "approved", roomId = "room-1")))
        coEvery { api.updateRoom(idFilter = "eq.room-1", body = any()) } returns
            Response.success(listOf(RoomDto(id = "room-1", propertyId = "prop-1", name = "A1", pricePerMonth = 2000.0, status = "occupied")))

        val result = repository.approveBooking(bookingId = "booking-1", roomId = "room-1")

        assertTrue(result is LandlordResult.Success)
        coVerify { api.updateRoom(idFilter = "eq.room-1", body = mapOf("status" to "occupied")) }
    }

    @Test
    fun `approveBooking without a room does not attempt to update any room`() = runTest {
        coEvery { api.updateBooking(idFilter = "eq.booking-1", body = any()) } returns
            Response.success(listOf(bookingDto(id = "booking-1", status = "approved", roomId = null)))

        val result = repository.approveBooking(bookingId = "booking-1", roomId = null)

        assertTrue(result is LandlordResult.Success)
        coVerify(exactly = 0) { api.updateRoom(idFilter = any(), body = any()) }
    }

    @Test
    fun `rejectBooking sends status=rejected and never touches room status`() = runTest {
        val bodySlot = slot<com.rentease.app.data.remote.dto.BookingUpdateDto>()
        coEvery { api.updateBooking(idFilter = "eq.booking-2", body = capture(bodySlot)) } returns
            Response.success(listOf(bookingDto(id = "booking-2", status = "rejected")))

        val result = repository.rejectBooking("booking-2")

        assertTrue(result is LandlordResult.Success)
        assertEquals("rejected", bodySlot.captured.status)
        coVerify(exactly = 0) { api.updateRoom(idFilter = any(), body = any()) }
    }

    @Test
    fun `createProperty always starts a new listing as pending, never published`() = runTest {
        val bodySlot = slot<Map<String, Any?>>()
        stubLandlordName(business = "Naledi Student Homes")
        coEvery { api.createProperty(capture(bodySlot)) } returns
            Response.success(listOf(propertyDto(status = "pending")))

        repository.createProperty(sampleInput())

        assertEquals("pending", bodySlot.captured["status"])
    }

    @Test
    fun `createProperty sends landlord name, university and distance`() = runTest {
        val bodySlot = slot<Map<String, Any?>>()
        stubLandlordName(business = "Naledi Student Homes")
        coEvery { api.createProperty(capture(bodySlot)) } returns
            Response.success(listOf(propertyDto(status = "pending")))

        repository.createProperty(sampleInput())

        assertEquals("Naledi Student Homes", bodySlot.captured["landlord_name"])
        assertEquals("uni-1", bodySlot.captured["university_id"])
        assertEquals(1.5, bodySlot.captured["distance_km"])
    }

    @Test
    fun `createProperty falls back to the legal name when there is no business name`() = runTest {
        val bodySlot = slot<Map<String, Any?>>()
        stubLandlordName(business = null, legal = "Sipho Ndlovu")
        coEvery { api.createProperty(capture(bodySlot)) } returns
            Response.success(listOf(propertyDto(status = "pending")))

        repository.createProperty(sampleInput())

        assertEquals("Sipho Ndlovu", bodySlot.captured["landlord_name"])
    }

    @Test
    fun `setPropertyImages clears the gallery then inserts photos in order`() = runTest {
        coEvery { api.deleteAllPropertyImages("eq.prop-1") } returns Response.success(Unit)
        val saved = mutableListOf<com.rentease.app.data.remote.dto.PropertyImageDto>()
        coEvery { api.addPropertyImage(capture(saved), any()) } returns Response.success(emptyList())

        val result = repository.setPropertyImages("prop-1", listOf("u1", "u2"))

        assertTrue(result is LandlordResult.Success)
        assertEquals(listOf("u1" to 0, "u2" to 1), saved.map { it.url to it.position })
        coVerify(exactly = 1) { api.deleteAllPropertyImages("eq.prop-1") }
    }

    @Test
    fun `changing a room re-counts available rooms on the property`() = runTest {
        val roomBody = com.rentease.app.data.remote.dto.RoomDto(id = "r1", propertyId = "prop-1", name = "A", pricePerMonth = 3400.0, status = "occupied")
        coEvery { api.updateRoom(idFilter = "eq.r1", body = any()) } returns Response.success(listOf(roomBody))
        coEvery { api.getRooms(propertyIdFilter = "eq.prop-1") } returns Response.success(
            listOf(roomBody, roomBody.copy(id = "r2", status = "available"), roomBody.copy(id = "r3", status = "available"))
        )
        val patch = slot<Map<String, Any?>>()
        coEvery { api.updateProperty(idFilter = "eq.prop-1", body = capture(patch)) } returns Response.success(emptyList())

        repository.updateRoom("r1", com.rentease.app.domain.model.RoomInput("A", "Shared", 3400.0, 2, com.rentease.app.domain.model.RoomStatus.OCCUPIED))

        assertEquals(2, patch.captured["rooms_available"])
    }

    @Test
    fun `getAnalytics computes occupancy rate from occupied over total rooms`() = runTest {
        coEvery { api.getMyProperties(landlordIdFilter = "eq.$landlordId") } returns
            Response.success(listOf(propertyDto(id = "prop-1", status = "published")))
        coEvery { api.getPropertyImages(propertyIdFilter = any()) } returns Response.success(emptyList())
        coEvery { api.getRooms(propertyIdFilter = "eq.prop-1") } returns Response.success(
            listOf(
                RoomDto(id = "r1", propertyId = "prop-1", name = "A", pricePerMonth = 1000.0, status = "occupied"),
                RoomDto(id = "r2", propertyId = "prop-1", name = "B", pricePerMonth = 1000.0, status = "available")
            )
        )
        coEvery { api.getBookingsForLandlord(landlordIdFilter = "eq.$landlordId") } returns Response.success(emptyList())
        coEvery { api.getMaintenanceForProperties(propertyIdInFilter = any()) } returns Response.success(emptyList())
        coEvery { api.getProfileNames(any()) } returns Response.success(emptyList())

        val result = repository.getAnalytics()

        assertTrue(result is LandlordResult.Success)
        val analytics = (result as LandlordResult.Success).data
        assertEquals(2, analytics.totalRoomsAvailable)
        assertEquals(1, analytics.occupiedRoomsEstimate)
        assertEquals(50.0, analytics.occupancyRatePercent, 0.001)
    }

    @Test
    fun `getBookings resolves student and property names via batch lookup`() = runTest {
        coEvery { api.getBookingsForLandlord(landlordIdFilter = "eq.$landlordId") } returns
            Response.success(listOf(bookingDto(id = "booking-3", status = "pending")))
        coEvery { api.getProfileNames(idInFilter = "in.(student-1)") } returns
            Response.success(listOf(NameLookupDto(id = "student-1", fullName = "Sipho Dlamini")))
        coEvery { api.getPropertyTitles(idInFilter = "in.(prop-1)") } returns
            Response.success(listOf(NameLookupDto(id = "prop-1", title = "Sunny Studio")))

        val result = repository.getBookings()

        assertTrue(result is LandlordResult.Success)
        val booking = (result as LandlordResult.Success).data.first()
        assertEquals("Sipho Dlamini", booking.studentName)
        assertEquals("Sunny Studio", booking.propertyTitle)
    }

    private fun bookingDto(id: String, status: String, roomId: String? = null) = LandlordBookingDto(
        id = id, reference = "REF-$id", studentId = "student-1", propertyId = "prop-1", roomId = roomId,
        moveInDate = "2026-09-01", leaseMonths = 12, monthlyRent = 3000.0, deposit = 3000.0, status = status
    )

    private fun propertyDto(id: String = "prop-1", status: String = "pending") = PropertyDto(
        id = id, slug = "slug-$id", title = "Test Property", city = "Pretoria", province = "Gauteng",
        pricePerMonth = 3000.0, status = status
    )

    private fun stubLandlordName(business: String?, legal: String? = null) {
        coEvery { api.getLandlordProfile(any()) } returns Response.success(
            listOf(
                com.rentease.app.data.remote.dto.LandlordProfileDto(
                    userId = "landlord-1", legalName = legal, businessName = business
                )
            )
        )
    }

    private fun sampleInput() = com.rentease.app.domain.model.PropertyInput(
        title = "Test Property", description = "desc", address = "123 Main Rd", city = "Pretoria",
        province = "Gauteng", universityId = "uni-1", distanceKm = 1.5, pricePerMonth = 3000.0, deposit = 3000.0,
        bedrooms = 1, bathrooms = 1, furnished = true, amenities = listOf("WiFi"), coverImageUrl = null
    )
}
