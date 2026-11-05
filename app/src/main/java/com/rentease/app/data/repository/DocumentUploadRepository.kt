package com.rentease.app.data.repository

import com.rentease.app.data.remote.SupabaseEndpoint
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.rentease.app.data.auth.TokenManager
import com.rentease.app.domain.model.DocumentRules
import com.rentease.app.domain.model.StoragePaths
import com.rentease.app.data.remote.api.SupabaseStorageApi
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.UUID
import javax.inject.Inject

/** Outcome of a private-bucket upload. */
sealed interface PrivateUploadResult {
    /** [path] is the object path inside the bucket — store THIS (not a URL) in the database. */
    data class Success(val path: String) : PrivateUploadResult
    data class Rejected(val problem: DocumentRules.Problem) : PrivateUploadResult
    data object Failed : PrivateUploadResult
}

interface DocumentUploadRepository {
    /**
     * Uploads [uri] to a PRIVATE bucket the way the web does: validated against
     * [DocumentRules], stored at `{currentUserId}/{folder}/{timestamp}-{name}` (the
     * layout the storage RLS policy requires) with upsert, returning the object path.
     * Requires a signed-in user.
     */
    suspend fun uploadPrivate(context: Context, uri: Uri, bucket: String, folder: String): PrivateUploadResult

    /** Public URL for an object in a bucket marked PUBLIC in the Supabase dashboard (e.g. listing photos). */
    fun publicUrlFor(bucket: String, path: String): String

    /** Time-limited signed URL (default 10 min, like the web) for a private object, or null on failure. */
    suspend fun createSignedUrl(bucket: String, path: String, seconds: Int = 600): String?

    /** Uploads [uri] to the given storage [bucket] and returns its public URL, or
     *  null on failure. [pathPrefix] is typically the user id so files are namespaced
     *  per-user (e.g. "landlord-verification/{userId}/id-document.jpg"). */
    suspend fun upload(context: Context, uri: Uri, bucket: String, pathPrefix: String): String?
}

class DocumentUploadRepositoryImpl @Inject constructor(
    private val storageApi: SupabaseStorageApi,
    private val tokenManager: TokenManager
) : DocumentUploadRepository {

    override suspend fun uploadPrivate(
        context: Context,
        uri: Uri,
        bucket: String,
        folder: String
    ): PrivateUploadResult = withContext(Dispatchers.IO) {
        val userId = tokenManager.getUserId() ?: return@withContext PrivateUploadResult.Failed
        val resolver = context.contentResolver
        val mimeType = resolver.getType(uri)

        var displayName = "document"
        var declaredSize = -1L
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { c ->
            if (c.moveToFirst()) {
                c.getColumnIndex(OpenableColumns.DISPLAY_NAME).takeIf { it >= 0 }?.let { displayName = c.getString(it) ?: displayName }
                c.getColumnIndex(OpenableColumns.SIZE).takeIf { it >= 0 }?.let { if (!c.isNull(it)) declaredSize = c.getLong(it) }
            }
        }

        // Reject on the declared size BEFORE reading the file into memory.
        if (declaredSize > DocumentRules.MAX_BYTES) {
            return@withContext PrivateUploadResult.Rejected(DocumentRules.Problem.TOO_LARGE)
        }
        val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
            ?: return@withContext PrivateUploadResult.Failed
        DocumentRules.validate(mimeType, bytes.size.toLong())?.let {
            return@withContext PrivateUploadResult.Rejected(it)
        }

        val objectPath = StoragePaths.privateObjectPath(userId, folder, displayName, System.currentTimeMillis())
        val response = storageApi.uploadFile(
            bucket = bucket,
            path = objectPath,
            contentType = mimeType!!,
            file = bytes.toRequestBody(mimeType.toMediaTypeOrNull()),
            upsert = "true"
        )
        if (response.isSuccessful) PrivateUploadResult.Success(objectPath) else PrivateUploadResult.Failed
    }

    override fun publicUrlFor(bucket: String, path: String): String =
        "${SupabaseEndpoint.baseUrl}/storage/v1/object/public/$bucket/$path"

    override suspend fun createSignedUrl(bucket: String, path: String, seconds: Int): String? =
        withContext(Dispatchers.IO) {
            val response = storageApi.createSignedUrl(bucket, path, mapOf("expiresIn" to seconds))
            val relative = response.body()?.signedUrl ?: return@withContext null
            // GoTrue returns "/object/sign/{bucket}/{path}?token=..." relative to /storage/v1.
            SupabaseEndpoint.baseUrl + "/storage/v1" + relative
        }

    override suspend fun upload(context: Context, uri: Uri, bucket: String, pathPrefix: String): String? =
        withContext(Dispatchers.IO) {
            val contentResolver = context.contentResolver
            val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"
            val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return@withContext null
            val extension = mimeType.substringAfterLast("/", "bin")
            val objectPath = "$pathPrefix/${UUID.randomUUID()}.$extension"

            val body = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
            val response = storageApi.uploadFile(
                bucket = bucket,
                path = objectPath,
                contentType = mimeType,
                file = body
            )

            if (response.isSuccessful) {
                "${SupabaseEndpoint.baseUrl}/storage/v1/object/public/$bucket/$objectPath"
            } else {
                null
            }
        }
}
