package com.rentease.app.data.remote.api

import com.rentease.app.data.remote.dto.AdminAccessRequestDto
import com.rentease.app.data.remote.dto.AdminAccessResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Calls the `admin-access` Supabase Edge Function (supabase/functions/admin-access),
 * which mirrors the web app's admin-access.server.ts exactly: a server-only
 * secret check that PostgREST itself cannot perform, since Postgres has no
 * access to environment secrets. See that function's doc comment for the
 * full security rationale.
 */
interface SupabaseAdminAccessApi {
    @POST("functions/v1/admin-access")
    suspend fun requestAdminAccess(@Body body: AdminAccessRequestDto): Response<AdminAccessResponseDto>
}
