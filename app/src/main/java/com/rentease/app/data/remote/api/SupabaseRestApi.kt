package com.rentease.app.data.remote.api

import com.rentease.app.data.remote.dto.LandlordProfileDto
import com.rentease.app.data.remote.dto.ProfileDto
import com.rentease.app.data.remote.dto.RoleDto
import com.rentease.app.data.remote.dto.StudentProfileDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Supabase's auto-generated PostgREST API at {SUPABASE_URL}/rest/v1/ (all table endpoints).
 * This is the genuine REST-over-Postgres layer described conceptually in
 * Part 1 §5.4 (Android → HTTPS/JSON → REST API → Postgres) — see the audit
 * note on architecture (§0) for why we're calling it directly rather than
 * through a hand-written Express layer.
 *
 * Row Level Security on the server enforces who can read/write what; these
 * calls will simply fail with 401/403 if the bearer token's role doesn't
 * satisfy the relevant policy (e.g. only the row owner or an admin can
 * update a profile — see "Users update own profile" policy).
 */
interface SupabaseRestApi {

    @GET("rest/v1/profiles")
    suspend fun getProfile(
        @Query("id") idFilter: String, // e.g. "eq.<uuid>"
        @Query("select") select: String = "*"
    ): Response<List<ProfileDto>>

    @PATCH("rest/v1/profiles")
    suspend fun updateProfile(
        @Query("id") idFilter: String,
        @Body body: Map<String, @JvmSuppressWildcards Any?>,
        @Header("Prefer") prefer: String = "return=representation"
    ): Response<List<ProfileDto>>

    @GET("rest/v1/user_roles")
    suspend fun getRoles(
        @Query("user_id") userIdFilter: String, // e.g. "eq.<uuid>"
        @Query("select") select: String = "role"
    ): Response<List<RoleDto>>

    /**
     * Writes the child student_profiles row right after Student sign-up.
     *
     * This is an UPSERT (`resolution=merge-duplicates`), not a plain insert: the
     * `handle_new_user()` trigger already creates a student_profiles row for the new
     * auth user, so a plain POST would fail with a primary-key conflict on user_id.
     */
    @POST("rest/v1/student_profiles")
    suspend fun createStudentProfile(
        @Body body: StudentProfileDto,
        @Header("Prefer") prefer: String = "resolution=merge-duplicates,return=representation"
    ): Response<List<StudentProfileDto>>

    /** Same upsert semantics as [createStudentProfile], for landlord_profiles. */
    @POST("rest/v1/landlord_profiles")
    suspend fun createLandlordProfile(
        @Body body: LandlordProfileDto,
        @Header("Prefer") prefer: String = "resolution=merge-duplicates,return=representation"
    ): Response<List<LandlordProfileDto>>

    /** Reads the signed-in student's onboarding row (RLS: own row only). */
    @GET("rest/v1/student_profiles")
    suspend fun getStudentProfile(
        @Query("user_id") userIdFilter: String, // "eq.<uuid>"
        @Query("select") select: String = "*"
    ): Response<List<StudentProfileDto>>

    /**
     * Partial upsert keyed on user_id. A Map body is used on purpose: with a DTO,
     * kotlinx.serialization omits default values (e.g. needs_furnished=false), so
     * un-ticking "furnished" would silently never be written.
     */
    @POST("rest/v1/student_profiles")
    suspend fun upsertStudentProfile(
        @Body body: Map<String, @JvmSuppressWildcards Any?>,
        @Query("on_conflict") onConflict: String = "user_id",
        @Header("Prefer") prefer: String = "resolution=merge-duplicates,return=representation"
    ): Response<List<StudentProfileDto>>

    /** Reads the signed-in landlord's onboarding/verification row (RLS: own row only). */
    @GET("rest/v1/landlord_profiles")
    suspend fun getLandlordProfile(
        @Query("user_id") userIdFilter: String, // "eq.<uuid>"
        @Query("select") select: String = "*"
    ): Response<List<LandlordProfileDto>>

    /** Partial upsert keyed on user_id; see [upsertStudentProfile] for why this is a Map. */
    @POST("rest/v1/landlord_profiles")
    suspend fun upsertLandlordProfile(
        @Body body: Map<String, @JvmSuppressWildcards Any?>,
        @Query("on_conflict") onConflict: String = "user_id",
        @Header("Prefer") prefer: String = "resolution=merge-duplicates,return=representation"
    ): Response<List<LandlordProfileDto>>
}
