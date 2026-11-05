package com.rentease.app.data.remote.api

import com.rentease.app.data.remote.dto.SignedUrlDto
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Supabase Storage REST endpoints at {SUPABASE_URL}/storage/v1/object/{bucket}/{path}.
 * Used for registration documents (student proof-of-registration, landlord ID +
 * proof-of-ownership) and later for property photos / maintenance photos.
 *
 * Two bucket families exist:
 *  - the web app's private buckets ("booking-documents", "property-images",
 *    "avatars"), whose RLS requires the first path segment to be the caller's user id;
 *  - the legacy Android "verification-documents" / "maintenance-photos" buckets,
 *    which are NOT created by the web migrations and must be created manually.
 */
interface SupabaseStorageApi {

    @POST("storage/v1/object/{bucket}/{path}")
    suspend fun uploadFile(
        @Path("bucket") bucket: String,
        @Path(value = "path", encoded = true) path: String,
        @Header("Content-Type") contentType: String,
        @Body file: RequestBody,
        @Header("x-upsert") upsert: String = "false"
    ): Response<Unit>

    /** Creates a time-limited signed URL for an object in a PRIVATE bucket. */
    @POST("storage/v1/object/sign/{bucket}/{path}")
    suspend fun createSignedUrl(
        @Path("bucket") bucket: String,
        @Path(value = "path", encoded = true) path: String,
        @Body body: Map<String, @JvmSuppressWildcards Any?>
    ): Response<SignedUrlDto>
}
