package com.rentease.app.data.remote.api

import com.rentease.app.data.remote.dto.ComplaintCreateDto
import com.rentease.app.data.remote.dto.ComplaintDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Shared (role-agnostic) complaint submission — Part 1 §3.10, `/support`.
 * Kept separate from [SupabaseAdminApi]'s complaint-queue endpoints the same
 * way [SupabaseNotificationsApi] is kept separate from the landlord-specific
 * notification endpoints: same table, different audience.
 */
interface SupabaseSupportApi {

    @GET("rest/v1/complaints")
    suspend fun getMyComplaints(
        @Query("reporter_id") reporterIdFilter: String,
        @Query("order") order: String = "created_at.desc"
    ): Response<List<ComplaintDto>>

    @POST("rest/v1/complaints")
    suspend fun createComplaint(
        @Body body: ComplaintCreateDto,
        @Header("Prefer") prefer: String = "return=minimal"
    ): Response<Unit>
}
