package com.rentease.app.data.remote.api

import com.rentease.app.data.remote.dto.BookingDto
import com.rentease.app.data.remote.dto.BookingUpdateDto
import com.rentease.app.data.remote.dto.ConversationDto
import com.rentease.app.data.remote.dto.FavoriteDto
import com.rentease.app.data.remote.dto.MaintenanceRequestCreateDto
import com.rentease.app.data.remote.dto.MaintenanceTicketDto
import com.rentease.app.data.remote.dto.MessageDto
import com.rentease.app.data.remote.dto.PaymentCreateDto
import com.rentease.app.data.remote.dto.PaymentDto
import com.rentease.app.data.remote.dto.PaymentMarkPaidDto
import com.rentease.app.data.remote.dto.PropertyDto
import com.rentease.app.data.remote.dto.PropertyImageDto
import com.rentease.app.data.remote.dto.ReviewCreateDto
import com.rentease.app.data.remote.dto.ReviewDto
import com.rentease.app.data.remote.dto.ReviewedBookingDto
import com.rentease.app.data.remote.dto.RoomOptionDto
import com.rentease.app.data.remote.dto.UniversityDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.QueryMap

/**
 * Listings/booking slice of the PostgREST API (Part 1 §3.4/§3.5/§5.5). Query
 * params follow PostgREST filter syntax, e.g. price_per_month=lte.5000,
 * amenities=cs.{WiFi,Parking} (array "contains"), city=ilike.*durban*.
 * status=eq.published is applied on every listings query so unpublished/
 * pending properties never appear to students, matching the admin-moderation
 * workflow in §3.10.
 */
interface SupabaseListingsApi {

    @GET("rest/v1/properties")
    suspend fun searchProperties(
        @QueryMap filters: Map<String, String>,
        @Header("Range") range: String, // PostgREST pagination, e.g. "0-11" for 12/page
        @Query("select") select: String = "*",
        @Query("order") order: String = "created_at.desc"
    ): Response<List<PropertyDto>>

    @GET("rest/v1/properties")
    suspend fun getPropertyBySlug(
        @Query("slug") slugFilter: String, // "eq.<slug>"
        @Query("select") select: String = "*"
    ): Response<List<PropertyDto>>

    @GET("rest/v1/property_images")
    suspend fun getPropertyImages(
        @Query("property_id") propertyIdFilter: String,
        @Query("order") order: String = "position.asc"
    ): Response<List<PropertyImageDto>>

    /** Rooms a student can pick when booking: only `status = available`, cheapest first (web booking-dialog). */
    @GET("rest/v1/rooms")
    suspend fun getAvailableRooms(
        @Query("property_id") propertyIdFilter: String, // "eq.<uuid>"
        @Query("status") statusFilter: String = "eq.available",
        @Query("select") select: String = "id,name,room_type,price_per_month,status",
        @Query("order") order: String = "price_per_month.asc"
    ): Response<List<RoomOptionDto>>

    @GET("rest/v1/universities")
    suspend fun getUniversities(
        @Query("order") order: String = "name.asc"
    ): Response<List<UniversityDto>>

    @GET("rest/v1/reviews")
    suspend fun getReviews(
        @Query("property_id") propertyIdFilter: String,
        @Query("order") order: String = "created_at.desc"
    ): Response<List<ReviewDto>>

    /** Student writes a review of a property they booked. Returns 201 on
     *  success, or 409 if this booking was already reviewed (unique index on
     *  `reviews.booking_id`). `return=minimal` since the caller only needs
     *  success/failure, same as [addFavorite]. */
    @POST("rest/v1/reviews")
    suspend fun createReview(
        @Body body: ReviewCreateDto,
        @Header("Prefer") prefer: String = "return=minimal"
    ): Response<Unit>

    /** Which of this student's bookings already have a review — used to hide
     *  the "Write a review" button once a booking has been reviewed. */
    @GET("rest/v1/reviews")
    suspend fun getMyReviewedBookings(
        @Query("student_id") studentIdFilter: String,
        @Query("select") select: String = "booking_id"
    ): Response<List<ReviewedBookingDto>>

    @GET("rest/v1/favorites")
    suspend fun getFavoriteIds(
        @Query("student_id") studentIdFilter: String,
        @Query("select") select: String = "property_id"
    ): Response<List<FavoriteDto>>

    @POST("rest/v1/favorites")
    suspend fun addFavorite(
        @Body body: FavoriteDto,
        @Header("Prefer") prefer: String = "return=minimal"
    ): Response<Unit>

    @DELETE("rest/v1/favorites")
    suspend fun removeFavorite(
        @Query("student_id") studentIdFilter: String,
        @Query("property_id") propertyIdFilter: String
    ): Response<Unit>

    @POST("rest/v1/bookings")
    suspend fun createBooking(
        @Body body: BookingDto,
        @Header("Prefer") prefer: String = "return=representation"
    ): Response<List<BookingDto>>

    // ---------- Payments (Part 1 §3.11) — a self-reported ledger, not a
    // payment gateway: see PaymentModels.kt's doc comment. ----------

    @POST("rest/v1/payments")
    suspend fun createPayment(
        @Body body: PaymentCreateDto,
        @Header("Prefer") prefer: String = "return=minimal"
    ): Response<Unit>

    @GET("rest/v1/payments")
    suspend fun getPaymentsForStudent(
        @Query("student_id") studentIdFilter: String,
        @Query("order") order: String = "created_at.desc"
    ): Response<List<PaymentDto>>

    @PATCH("rest/v1/payments")
    suspend fun markPaymentPaid(
        @Query("id") idFilter: String,
        @Body body: PaymentMarkPaidDto,
        @Header("Prefer") prefer: String = "return=minimal"
    ): Response<Unit>

    // ---------- Student booking history (Part 1 §3.5: "students track booking
    // status in real time on their dashboard") ----------

    @GET("rest/v1/bookings")
    suspend fun getBookingsForStudent(
        @Query("student_id") studentIdFilter: String,
        @Query("order") order: String = "created_at.desc"
    ): Response<List<BookingDto>>

    /** Student cancels their own pending booking request — mirrors the web
     *  app's `student.bookings.tsx` cancel button exactly (a plain
     *  `status = "cancelled"` update; RLS's "Parties update bookings" policy
     *  is what actually restricts this to the booking's own student). */
    @PATCH("rest/v1/bookings")
    suspend fun cancelBooking(
        @Query("id") idFilter: String,
        @Body body: BookingUpdateDto,
        @Header("Prefer") prefer: String = "return=representation"
    ): Response<List<BookingDto>>

    /** Batch title lookup for booking history — avoids one GET per booking
     *  when rendering a student's booking list. */
    @GET("rest/v1/properties")
    suspend fun getPropertyTitlesById(
        @Query("id") idInFilter: String, // "in.(id1,id2)"
        @Query("select") select: String = "id,title"
    ): Response<List<com.rentease.app.data.remote.dto.NameLookupDto>>

    // ---------- Student messaging (Part 1 §3.6) — mirrors the landlord-side
    // conversation/message endpoints in SupabaseLandlordApi, filtered by
    // student_id instead of landlord_id. Kept in this interface (already
    // injected into every Student repository/ViewModel) rather than reusing
    // SupabaseLandlordApi, so Student screens never depend on a
    // landlord-named API surface. ----------

    @GET("rest/v1/conversations")
    suspend fun getConversationsForStudent(
        @Query("student_id") studentIdFilter: String,
        @Query("order") order: String = "last_message_at.desc"
    ): Response<List<ConversationDto>>

    @GET("rest/v1/messages")
    suspend fun getMessagesForConversation(
        @Query("conversation_id") conversationIdFilter: String,
        @Query("order") order: String = "created_at.asc"
    ): Response<List<MessageDto>>

    @POST("rest/v1/messages")
    suspend fun sendMessageAsStudent(
        @Body body: MessageDto,
        @Header("Prefer") prefer: String = "return=representation"
    ): Response<List<MessageDto>>

    @PATCH("rest/v1/messages")
    suspend fun markMessagesReadAsStudent(
        @Query("conversation_id") conversationIdFilter: String,
        @Query("sender_id") notSenderFilter: String,
        @Body body: Map<String, String>
    ): Response<Unit>

    // ---------- Student maintenance (Part 1 §3.9, student/maintenance) ----------

    @GET("rest/v1/maintenance_requests")
    suspend fun getMaintenanceForStudent(
        @Query("student_id") studentIdFilter: String,
        @Query("order") order: String = "created_at.desc"
    ): Response<List<MaintenanceTicketDto>>

    @POST("rest/v1/maintenance_requests")
    suspend fun createMaintenanceRequest(
        @Body body: MaintenanceRequestCreateDto,
        @Header("Prefer") prefer: String = "return=representation"
    ): Response<List<MaintenanceTicketDto>>
}
