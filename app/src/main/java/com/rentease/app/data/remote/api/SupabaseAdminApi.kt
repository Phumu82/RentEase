package com.rentease.app.data.remote.api

import com.rentease.app.data.remote.dto.AuditLogDto
import com.rentease.app.data.remote.dto.AuditLogInsertDto
import com.rentease.app.data.remote.dto.ComplaintDecisionDto
import com.rentease.app.data.remote.dto.ComplaintDto
import com.rentease.app.data.remote.dto.LandlordBookingDto
import com.rentease.app.data.remote.dto.LandlordProfileDto
import com.rentease.app.data.remote.dto.ProfileDto
import com.rentease.app.data.remote.dto.PropertyDto
import com.rentease.app.data.remote.dto.PropertyModerationDecisionDto
import com.rentease.app.data.remote.dto.RoleDto
import com.rentease.app.data.remote.dto.SuspendUserDto
import com.rentease.app.data.remote.dto.VerificationDecisionDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Admin-side PostgREST operations (Phase 7). All of these rely on RLS policies
 * that check `has_role(auth.uid(), 'admin')` server-side — the client sends
 * plain authenticated requests and trusts Postgres to reject anything an
 * actual admin session isn't allowed to do. Granting the admin role itself is
 * NOT done here — see SupabaseAdminAccessApi / the admin-access Edge Function.
 */
interface SupabaseAdminApi {

    // ---------- Landlord verification ----------

    @GET("rest/v1/landlord_profiles")
    suspend fun getLandlordProfiles(
        @Query("verification_status") statusFilter: String? = null, // e.g. "eq.submitted"
        @Query("order") order: String = "user_id.asc"
    ): Response<List<LandlordProfileDto>>

    @PATCH("rest/v1/landlord_profiles")
    suspend fun setVerificationStatus(
        @Query("user_id") userIdFilter: String,
        @Body body: VerificationDecisionDto,
        @Header("Prefer") prefer: String = "return=representation"
    ): Response<List<LandlordProfileDto>>

    /** Mirrors the web's decision: `profiles.is_verified` is true only while the landlord is verified. */
    @PATCH("rest/v1/profiles")
    suspend fun setProfileVerified(
        @Query("id") idFilter: String,
        @Body body: Map<String, @JvmSuppressWildcards Any?>,
        @Header("Prefer") prefer: String = "return=minimal"
    ): Response<Unit>

    // ---------- Property moderation ----------

    @GET("rest/v1/properties")
    suspend fun getPropertiesByStatus(
        @Query("status") statusFilter: String, // "eq.pending"
        @Query("order") order: String = "created_at.desc"
    ): Response<List<PropertyDto>>

    @PATCH("rest/v1/properties")
    suspend fun setPropertyModerationStatus(
        @Query("id") idFilter: String,
        @Body body: PropertyModerationDecisionDto,
        @Header("Prefer") prefer: String = "return=representation"
    ): Response<List<PropertyDto>>

    // ---------- User management ----------

    @GET("rest/v1/profiles")
    suspend fun getAllProfiles(
        @Query("order") order: String = "full_name.asc"
    ): Response<List<ProfileDto>>

    @GET("rest/v1/user_roles")
    suspend fun getAllRoles(): Response<List<RoleDto>>

    @PATCH("rest/v1/profiles")
    suspend fun setSuspended(
        @Query("id") idFilter: String,
        @Body body: SuspendUserDto,
        @Header("Prefer") prefer: String = "return=representation"
    ): Response<List<ProfileDto>>

    // ---------- Booking oversight (platform-wide, read-only) ----------

    @GET("rest/v1/bookings")
    suspend fun getAllBookings(
        @Query("order") order: String = "created_at.desc"
    ): Response<List<LandlordBookingDto>>

    // ---------- Audit log ----------

    @GET("rest/v1/audit_logs")
    suspend fun getAuditLogs(
        @Query("order") order: String = "created_at.desc",
        @Query("limit") limit: Int = 100
    ): Response<List<AuditLogDto>>

    @POST("rest/v1/audit_logs")
    suspend fun insertAuditLog(
        @Body body: AuditLogInsertDto,
        @Header("Prefer") prefer: String = "return=minimal"
    ): Response<Unit>

    // ---------- Complaints (Part 1 §3.10) ----------

    @GET("rest/v1/complaints")
    suspend fun getAllComplaints(
        @Query("order") order: String = "created_at.desc"
    ): Response<List<ComplaintDto>>

    @PATCH("rest/v1/complaints")
    suspend fun decideComplaint(
        @Query("id") idFilter: String,
        @Body body: ComplaintDecisionDto,
        @Header("Prefer") prefer: String = "return=minimal"
    ): Response<Unit>
}
