package com.rentease.app.data.remote.api

import com.rentease.app.data.remote.dto.BookingUpdateDto
import com.rentease.app.data.remote.dto.ConversationDto
import com.rentease.app.data.remote.dto.LandlordBookingDto
import com.rentease.app.data.remote.dto.LandlordProfileDto
import com.rentease.app.data.remote.dto.MaintenanceTicketDto
import com.rentease.app.data.remote.dto.MaintenanceUpdateDto
import com.rentease.app.data.remote.dto.MessageDto
import com.rentease.app.data.remote.dto.NameLookupDto
import com.rentease.app.data.remote.dto.NotificationDto
import com.rentease.app.data.remote.dto.PaymentDto
import com.rentease.app.data.remote.dto.PropertyDto
import com.rentease.app.data.remote.dto.PropertyImageDto
import com.rentease.app.data.remote.dto.RoomDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Landlord-side PostgREST operations (Phase 6). RLS already restricts writes to
 * rows the landlord owns (`landlord_id = auth.uid()` / via property ownership),
 * so these calls rely on the server to reject anything out of scope — the
 * client doesn't need to re-check ownership before calling, only handle the
 * 401/403 it gets back if it tries something it shouldn't.
 */
interface SupabaseLandlordApi {

    // ---------- Verification ----------

    @GET("rest/v1/landlord_profiles")
    suspend fun getLandlordProfile(
        @Query("user_id") userIdFilter: String
    ): Response<List<LandlordProfileDto>>

    // ---------- Properties ----------

    @GET("rest/v1/properties")
    suspend fun getMyProperties(
        @Query("landlord_id") landlordIdFilter: String,
        @Query("order") order: String = "created_at.desc"
    ): Response<List<PropertyDto>>

    @GET("rest/v1/properties")
    suspend fun getPropertyById(
        @Query("id") idFilter: String
    ): Response<List<PropertyDto>>

    @POST("rest/v1/properties")
    suspend fun createProperty(
        @Body body: Map<String, @JvmSuppressWildcards Any?>,
        @Header("Prefer") prefer: String = "return=representation"
    ): Response<List<PropertyDto>>

    @PATCH("rest/v1/properties")
    suspend fun updateProperty(
        @Query("id") idFilter: String,
        @Body body: Map<String, @JvmSuppressWildcards Any?>,
        @Header("Prefer") prefer: String = "return=representation"
    ): Response<List<PropertyDto>>

    @DELETE("rest/v1/properties")
    suspend fun deleteProperty(@Query("id") idFilter: String): Response<Unit>

    @GET("rest/v1/property_images")
    suspend fun getPropertyImages(@Query("property_id") propertyIdFilter: String): Response<List<PropertyImageDto>>

    @POST("rest/v1/property_images")
    suspend fun addPropertyImage(
        @Body body: PropertyImageDto,
        @Header("Prefer") prefer: String = "return=representation"
    ): Response<List<PropertyImageDto>>

    @DELETE("rest/v1/property_images")
    suspend fun deletePropertyImage(@Query("id") idFilter: String): Response<Unit>

    @DELETE("rest/v1/property_images")
    suspend fun deleteAllPropertyImages(@Query("property_id") propertyIdFilter: String): Response<Unit>

    // ---------- Rooms ----------

    @GET("rest/v1/rooms")
    suspend fun getRooms(@Query("property_id") propertyIdFilter: String): Response<List<RoomDto>>

    @GET("rest/v1/rooms")
    suspend fun getRoomById(@Query("id") idFilter: String): Response<List<RoomDto>>

    @POST("rest/v1/rooms")
    suspend fun createRoom(
        @Body body: RoomDto,
        @Header("Prefer") prefer: String = "return=representation"
    ): Response<List<RoomDto>>

    @PATCH("rest/v1/rooms")
    suspend fun updateRoom(
        @Query("id") idFilter: String,
        @Body body: Map<String, @JvmSuppressWildcards Any?>,
        @Header("Prefer") prefer: String = "return=representation"
    ): Response<List<RoomDto>>

    @DELETE("rest/v1/rooms")
    suspend fun deleteRoom(@Query("id") idFilter: String): Response<Unit>

    // ---------- Bookings ----------

    @GET("rest/v1/bookings")
    suspend fun getBookingsForLandlord(
        @Query("landlord_id") landlordIdFilter: String,
        @Query("order") order: String = "created_at.desc"
    ): Response<List<LandlordBookingDto>>

    @PATCH("rest/v1/bookings")
    suspend fun updateBooking(
        @Query("id") idFilter: String,
        @Body body: BookingUpdateDto,
        @Header("Prefer") prefer: String = "return=representation"
    ): Response<List<LandlordBookingDto>>

    // ---------- Maintenance ----------

    @GET("rest/v1/maintenance_requests")
    suspend fun getMaintenanceForProperties(
        @Query("property_id") propertyIdInFilter: String, // "in.(id1,id2,...)"
        @Query("order") order: String = "created_at.desc"
    ): Response<List<MaintenanceTicketDto>>

    @PATCH("rest/v1/maintenance_requests")
    suspend fun updateMaintenanceStatus(
        @Query("id") idFilter: String,
        @Body body: MaintenanceUpdateDto,
        @Header("Prefer") prefer: String = "return=representation"
    ): Response<List<MaintenanceTicketDto>>

    // ---------- Messaging ----------

    @GET("rest/v1/conversations")
    suspend fun getConversations(
        @Query("landlord_id") landlordIdFilter: String,
        @Query("order") order: String = "last_message_at.desc"
    ): Response<List<ConversationDto>>

    @POST("rest/v1/conversations")
    suspend fun createConversation(
        @Body body: ConversationDto,
        @Header("Prefer") prefer: String = "return=representation"
    ): Response<List<ConversationDto>>

    @GET("rest/v1/messages")
    suspend fun getMessages(
        @Query("conversation_id") conversationIdFilter: String,
        @Query("order") order: String = "created_at.asc"
    ): Response<List<MessageDto>>

    @POST("rest/v1/messages")
    suspend fun sendMessage(
        @Body body: MessageDto,
        @Header("Prefer") prefer: String = "return=representation"
    ): Response<List<MessageDto>>

    @PATCH("rest/v1/messages")
    suspend fun markMessagesRead(
        @Query("conversation_id") conversationIdFilter: String,
        @Query("sender_id") notSenderFilter: String, // "neq.<myId>" — only mark the OTHER party's messages read
        @Body body: Map<String, String> // {"read_at": "<iso timestamp>"}
    ): Response<Unit>

    // ---------- Notifications ----------

    @GET("rest/v1/notifications")
    suspend fun getNotifications(
        @Query("user_id") userIdFilter: String,
        @Query("order") order: String = "created_at.desc"
    ): Response<List<NotificationDto>>

    @PATCH("rest/v1/notifications")
    suspend fun markNotificationRead(
        @Query("id") idFilter: String,
        @Body body: Map<String, String>
    ): Response<Unit>

    // ---------- Batch name lookups (see NameLookupDto doc comment) ----------

    @GET("rest/v1/profiles")
    suspend fun getProfileNames(
        @Query("id") idInFilter: String, // "in.(id1,id2)"
        @Query("select") select: String = "id,full_name"
    ): Response<List<NameLookupDto>>

    @GET("rest/v1/properties")
    suspend fun getPropertyTitles(
        @Query("id") idInFilter: String,
        @Query("select") select: String = "id,title"
    ): Response<List<NameLookupDto>>

    @GET("rest/v1/rooms")
    suspend fun getRoomNames(
        @Query("id") idInFilter: String,
        @Query("select") select: String = "id,name"
    ): Response<List<NameLookupDto>>

    /** Read-only for landlords: RLS's "Payment parties read" policy allows
     *  SELECT via `owns_property`, but "Parties update payments" only allows
     *  the student (or an admin) to change a payment's status — a landlord
     *  can see what's been paid for their own revenue reporting, never edit
     *  it. Used to compute real revenue instead of a rent-based estimate. */
    @GET("rest/v1/payments")
    suspend fun getPaymentsForBookings(
        @Query("booking_id") bookingIdInFilter: String, // "in.(id1,id2)"
        @Query("status") statusFilter: String = "eq.paid"
    ): Response<List<PaymentDto>>
}
