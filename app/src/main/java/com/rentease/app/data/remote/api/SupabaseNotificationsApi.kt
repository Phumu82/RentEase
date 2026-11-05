package com.rentease.app.data.remote.api

import com.rentease.app.data.remote.dto.DeviceTokenDto
import com.rentease.app.data.remote.dto.NotificationDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Shared (role-agnostic) notification feed + device token registration.
 * Deliberately separate from SupabaseLandlordApi's notification endpoints —
 * both hit the same `notifications` table (it's not role-specific), but
 * keeping this as its own interface means the Landlord screens already
 * built against SupabaseLandlordApi are untouched while Student and the FCM
 * service share this one instead of duplicating query logic differently.
 */
interface SupabaseNotificationsApi {

    @GET("rest/v1/notifications")
    suspend fun getNotifications(
        @Query("user_id") userIdFilter: String,
        @Query("order") order: String = "created_at.desc"
    ): Response<List<NotificationDto>>

    @PATCH("rest/v1/notifications")
    suspend fun markRead(
        @Query("id") idFilter: String,
        @Body body: Map<String, String>
    ): Response<Unit>

    /** Upsert — POST /devices per Part 1 §3.8/§5.5, backed by device_tokens
     *  with a UNIQUE(user_id, fcm_token) constraint so re-registering the
     *  same token (e.g. app restart) doesn't create duplicate rows. */
    @POST("rest/v1/device_tokens")
    suspend fun registerDeviceToken(
        @Body body: DeviceTokenDto,
        @Header("Prefer") prefer: String = "resolution=merge-duplicates,return=minimal"
    ): Response<Unit>
}
