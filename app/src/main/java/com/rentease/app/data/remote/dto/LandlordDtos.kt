package com.rentease.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RoomDto(
    val id: String? = null,
    @SerialName("property_id") val propertyId: String,
    val name: String,
    @SerialName("room_type") val roomType: String = "single",
    @SerialName("price_per_month") val pricePerMonth: Double,
    val capacity: Int = 1,
    val status: String = "available"
)

/**
 * Landlord-side booking view. Deliberately FLAT — no PostgREST embedded-resource
 * joins (`select=*,profiles(full_name)`) here, because those require exact
 * foreign-key constraint names that I can't verify against the live database
 * from this environment, and a wrong guess would silently break every landlord
 * screen rather than fail loudly. LandlordRepositoryImpl instead batch-fetches
 * student names / property titles / room names via separate `id=in.(...)`
 * queries and joins them in Kotlin. Swap to embedded joins later once you've
 * confirmed the actual constraint names in your Supabase dashboard — it'll be
 * one query instead of four, but only worth doing once it's verified.
 */
@Serializable
data class LandlordBookingDto(
    val id: String,
    val reference: String,
    @SerialName("student_id") val studentId: String,
    @SerialName("property_id") val propertyId: String,
    @SerialName("room_id") val roomId: String? = null,
    @SerialName("move_in_date") val moveInDate: String,
    @SerialName("lease_months") val leaseMonths: Int,
    @SerialName("monthly_rent") val monthlyRent: Double,
    val deposit: Double,
    val status: String,
    val message: String? = null,
    @SerialName("id_document_url") val idDocumentUrl: String? = null,
    @SerialName("proof_of_registration_url") val proofOfRegistrationUrl: String? = null
)

/** Minimal id+name projection used for the batch name-lookup joins described above. */
@Serializable
data class NameLookupDto(
    val id: String,
    @SerialName("full_name") val fullName: String? = null,
    val title: String? = null,
    val name: String? = null
)

/** Body for PATCH /bookings/:id — approve/reject/assign room. */
@Serializable
data class BookingUpdateDto(
    val status: String? = null,
    @SerialName("room_id") val roomId: String? = null,
    @SerialName("decided_at") val decidedAt: String? = null
)

@Serializable
data class MaintenanceTicketDto(
    val id: String,
    @SerialName("property_id") val propertyId: String,
    @SerialName("student_id") val studentId: String,
    val title: String,
    val description: String = "",
    val category: String = "general",
    val priority: String = "medium",
    val status: String = "open",
    @SerialName("photo_url") val photoUrl: String? = null,
    @SerialName("created_at") val createdAt: String = ""
)

@Serializable
data class MaintenanceUpdateDto(
    val status: String
)

/** POST body for a student logging a new issue. `student_id` is included
 *  explicitly (rather than relying on a DB default) because the RLS policy
 *  "Students log maintenance" checks `student_id = auth.uid()` against the
 *  submitted row, same convention as BookingDto elsewhere in this file. */
@Serializable
data class MaintenanceRequestCreateDto(
    @SerialName("student_id") val studentId: String,
    @SerialName("property_id") val propertyId: String,
    @SerialName("booking_id") val bookingId: String? = null,
    val title: String,
    val description: String,
    val category: String,
    val priority: String,
    @SerialName("photo_url") val photoUrl: String? = null
)

@Serializable
data class ConversationDto(
    val id: String? = null,
    @SerialName("student_id") val studentId: String,
    @SerialName("landlord_id") val landlordId: String,
    @SerialName("property_id") val propertyId: String? = null,
    val subject: String = "",
    @SerialName("last_message_at") val lastMessageAt: String = ""
)

@Serializable
data class MessageDto(
    val id: String? = null,
    @SerialName("conversation_id") val conversationId: String,
    @SerialName("sender_id") val senderId: String,
    val body: String,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("read_at") val readAt: String? = null
)

@Serializable
data class NotificationDto(
    val id: String,
    val title: String,
    val body: String = "",
    val category: String = "general",
    val link: String? = null,
    @SerialName("read_at") val readAt: String? = null,
    @SerialName("created_at") val createdAt: String = ""
)
