package com.rentease.app.data.auth

import com.rentease.app.data.remote.SupabaseEndpoint
import com.rentease.app.BuildConfig
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import java.io.IOException
import javax.inject.Inject

/**
 * Attaches the Supabase anon key (required on every PostgREST/GoTrue call) and, when
 * present, the user's bearer access token. On a 401 it attempts exactly one silent
 * refresh via the GoTrue `grant_type=refresh_token` endpoint before giving up — this
 * satisfies Part 1 §3.1's "token refresh" requirement without looping forever on a
 * genuinely expired session.
 */
class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager
) : Interceptor {

    // Only visible ASCII is legal in an HTTP header value; a stray smart quote or newline pasted along with
    // the key would otherwise make OkHttp throw IllegalArgumentException on every single request.
    private val anonKey: String = BuildConfig.SUPABASE_ANON_KEY.filter { it.code in 33..126 }

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val accessToken = tokenManager.getAccessToken()

        val authedRequest = original.newBuilder()
            .header("apikey", anonKey)
            .apply {
                // An explicit Authorization header (the password-recovery token) must win over the session token.
                if (accessToken != null && original.header("Authorization") == null) {
                    header("Authorization", "Bearer $accessToken")
                }
            }
            .build()

        val response = chain.proceedSafely(authedRequest)

        if (response.code == 401 && tokenManager.getRefreshToken() != null) {
            response.close()
            val refreshed = refreshAccessToken(chain)
            if (refreshed != null) {
                tokenManager.updateAccessToken(refreshed)
                val retryRequest = original.newBuilder()
                    .header("apikey", anonKey)
                    .header("Authorization", "Bearer $refreshed")
                    .build()
                return chain.proceedSafely(retryRequest)
            }
        }

        return response
    }

    /**
     * Offline guard. With no connectivity (or a timeout) OkHttp throws an
     * IOException, which Retrofit would rethrow into the calling coroutine - and
     * most ViewModels launch repository calls without a try/catch, so the app
     * would crash whenever it is used offline. Converting the failure into a
     * synthetic HTTP 503 makes every repository take its normal "request
     * failed" path (they all check `isSuccessful`) instead of crashing.
     */
    private fun Interceptor.Chain.proceedSafely(request: Request): Response =
        try {
            proceed(request)
        } catch (e: IOException) {
            Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(HTTP_NETWORK_UNAVAILABLE)
                .message("Network unavailable")
                // Marker body so the UI can tell "cannot reach the server" from a real server 503.
                .body(
                    """{"error":"$NETWORK_UNREACHABLE","detail":"${e.javaClass.simpleName}"}"""
                        .toResponseBody("application/json".toMediaType())
                )
                .build()
        }

    /**
     * Performs a synchronous refresh-token exchange on the same OkHttp client.
     * Returns null (caller then routes the user back to login) if the refresh
     * token itself has expired or been revoked.
     */
    private fun refreshAccessToken(chain: Interceptor.Chain): String? {
        val refreshToken = tokenManager.getRefreshToken() ?: return null
        val url = "${SupabaseEndpoint.baseUrl}/auth/v1/token?grant_type=refresh_token"
        val body = """{"refresh_token":"$refreshToken"}""".toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(url)
            .header("apikey", anonKey)
            .post(body)
            .build()

        return try {
            chain.proceed(request).use { resp ->
                if (!resp.isSuccessful) return null
                val json = resp.body?.string() ?: return null
                Regex("\"access_token\"\\s*:\\s*\"([^\"]+)\"").find(json)?.groupValues?.get(1)
                    ?.also { newAccess ->
                        Regex("\"refresh_token\"\\s*:\\s*\"([^\"]+)\"").find(json)?.groupValues?.get(1)
                            ?.let { newRefresh ->
                                tokenManager.saveSession(
                                    newAccess, newRefresh,
                                    tokenManager.getUserId().orEmpty(),
                                    tokenManager.getRole().orEmpty()
                                )
                            }
                    }
            }
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        /** Status used for the synthetic response produced when the network is unreachable. */
        const val HTTP_NETWORK_UNAVAILABLE = 503

        /** Value of the `error` field in the synthetic response body. */
        const val NETWORK_UNREACHABLE = "network_unreachable"
    }
}
