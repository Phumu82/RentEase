package com.rentease.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UniversityDto(
    val id: String,
    val name: String,
    @SerialName("short_name") val shortName: String,
    val city: String,
    val province: String,
    val students: Int = 0,
    @SerialName("average_rent") val averageRent: Double = 0.0
)

@Serializable
data class PropertyDto(
    val id: String,
    val slug: String,
    val title: String,
    val description: String = "",
    val address: String = "",
    val city: String,
    val province: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    @SerialName("distance_km") val distanceKm: Double = 0.0,
    @SerialName("price_per_month") val pricePerMonth: Double,
    val deposit: Double = 0.0,
    val bedrooms: Int = 1,
    val bathrooms: Int = 1,
    @SerialName("rooms_available") val roomsAvailable: Int = 1,
    val furnished: Boolean = false,
    val verified: Boolean = false,
    val amenities: List<String> = emptyList(),
    @SerialName("cover_image") val coverImage: String? = null,
    val rating: Double = 0.0,
    @SerialName("review_count") val reviewCount: Int = 0,
    @SerialName("landlord_name") val landlordName: String = "",
    @SerialName("landlord_id") val landlordId: String? = null,
    @SerialName("university_id") val universityId: String? = null,
    val status: String = "published"
)

/** Bookable room option (public.rooms) shown in the booking sheet. */
@Serializable
data class RoomOptionDto(
    val id: String,
    val name: String,
    @SerialName("room_type") val roomType: String = "single",
    @SerialName("price_per_month") val pricePerMonth: Double,
    val status: String = "available"
)

@Serializable
data class PropertyImageDto(
    val id: String,
    @SerialName("property_id") val propertyId: String,
    val url: String,
    val position: Int = 0
)

@Serializable
data class FavoriteDto(
    val id: String? = null,
    @SerialName("student_id") val studentId: String,
    @SerialName("property_id") val propertyId: String
)

@Serializable
data class ReviewDto(
    val id: String,
    @SerialName("author_name") val authorName: String = "Student",
    val rating: Int,
    val title: String = "",
    val body: String = ""
)

/** POST body for a student writing a review. `student_id` is sent explicitly
 *  because the RLS policy "Students write reviews" checks
 *  `student_id = auth.uid()` against the submitted row. */
@Serializable
data class ReviewCreateDto(
    @SerialName("property_id") val propertyId: String,
    @SerialName("booking_id") val bookingId: String? = null,
    @SerialName("student_id") val studentId: String,
    @SerialName("author_name") val authorName: String,
    val rating: Int,
    val title: String,
    val body: String
)

/** Projection used to find which of a student's bookings already have a
 *  review (`select=booking_id`); older reviews may have a null booking_id. */
@Serializable
data class ReviewedBookingDto(
    @SerialName("booking_id") val bookingId: String? = null
)

@Serializable
data class BookingDto(
    val id: String? = null,
    val reference: String? = null,
    @SerialName("student_id") val studentId: String,
    @SerialName("property_id") val propertyId: String,
    @SerialName("room_id") val roomId: String? = null,
    @SerialName("landlord_id") val landlordId: String? = null,
    @SerialName("move_in_date") val moveInDate: String,
    @SerialName("lease_months") val leaseMonths: Int,
    @SerialName("monthly_rent") val monthlyRent: Double,
    val deposit: Double,
    val status: String = "pending",
    val message: String? = null,
    @SerialName("id_document_url") val idDocumentUrl: String,
    @SerialName("proof_of_registration_url") val proofOfRegistrationUrl: String
)

@Serializable
data class PaymentDto(
    val id: String,
    val reference: String = "",
    val amount: Double,
    val kind: String = "rent",
    val status: String = "pending",
    @SerialName("booking_id") val bookingId: String,
    @SerialName("due_date") val dueDate: String? = null,
    @SerialName("paid_at") val paidAt: String? = null,
    @SerialName("created_at") val createdAt: String = ""
)

/**
 * POST body for the deposit row auto-created alongside a new booking —
 * mirrors `booking-dialog.tsx`'s second insert (same field set, same
 * "deposit"/"pending" defaults). `reference` is deliberately omitted here,
 * same as this app's own [BookingDto] insert already does for
 * `bookings.reference`: the `payments` table has an identical
 * `DEFAULT ('PAY-' || ...)` generator, so the DB assigns one automatically.
 */
@Serializable
data class PaymentCreateDto(
    @SerialName("booking_id") val bookingId: String,
    @SerialName("student_id") val studentId: String,
    val amount: Double,
    val kind: String = "deposit",
    val status: String = "pending",
    @SerialName("due_date") val dueDate: String? = null
)

/** PATCH body for the student's own "Pay now" action. */
@Serializable
data class PaymentMarkPaidDto(
    val status: String = "paid",
    @SerialName("paid_at") val paidAt: String
)
