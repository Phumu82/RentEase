package com.rentease.app.data.remote

import com.rentease.app.BuildConfig
import com.rentease.app.data.auth.AuthInterceptor
import com.rentease.app.data.auth.TokenManager
import com.rentease.app.data.remote.api.SupabaseAuthApi
import com.rentease.app.data.remote.api.SupabaseRestApi
import com.rentease.app.data.remote.dto.GoogleIdTokenGrantRequestDto
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * REAL HTTP round-trips: the production Retrofit configuration (RetrofitFactory)
 * + the production AuthInterceptor + the real Retrofit service interfaces talk to
 * a local MockWebServer over a socket. Unlike the MockK-based repository tests,
 * these prove the wire contract: HTTP method, path, query, headers and JSON body
 * that the Supabase PostgREST / GoTrue endpoints actually receive.
 */
class RestApiContractTest {

    private lateinit var server: MockWebServer
    private lateinit var restApi: SupabaseRestApi
    private lateinit var authApi: SupabaseAuthApi

    @Before
    fun setUp() {
        server = MockWebServer().also { it.start() }
        val tokenManager = mockk<TokenManager>(relaxed = true)
        every { tokenManager.getAccessToken() } returns "access-token-123"
        every { tokenManager.getRefreshToken() } returns null
        val client = OkHttpClient.Builder().addInterceptor(AuthInterceptor(tokenManager)).build()
        val retrofit = RetrofitFactory.create(client, server.url("/").toString())
        restApi = retrofit.create(SupabaseRestApi::class.java)
        authApi = retrofit.create(SupabaseAuthApi::class.java)
    }

    @After
    fun tearDown() {
        runCatching { server.shutdown() }
    }

    private fun jsonResponse(body: String) =
        MockResponse().setResponseCode(200).setHeader("Content-Type", "application/json").setBody(body)

    @Test
    fun `GET profile hits the PostgREST path with apikey and bearer headers and decodes the row`() = runBlocking {
        server.enqueue(jsonResponse("""[{"id":"u1","full_name":"Thandi Nkosi","preferred_language":"zu"}]"""))

        val response = restApi.getProfile(idFilter = "eq.u1")

        val request = server.takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/rest/v1/profiles?id=eq.u1&select=*", request.path)
        assertEquals(BuildConfig.SUPABASE_ANON_KEY, request.getHeader("apikey"))
        assertEquals("Bearer access-token-123", request.getHeader("Authorization"))
        assertTrue(response.isSuccessful)
        assertEquals("Thandi Nkosi", response.body()!!.single().fullName)
        assertEquals("zu", response.body()!!.single().preferredLanguage)
    }

    @Test
    fun `PATCH profile with a mixed-type Map body serialises to JSON (regression - Any used to throw)`() = runBlocking {
        server.enqueue(jsonResponse("""[{"id":"u1","full_name":"Thandi"}]"""))

        val response = restApi.updateProfile(
            idFilter = "eq.u1",
            body = mapOf(
                "full_name" to "Thandi",
                "phone" to null,
                "notify_new_messages" to false,
                "amenities" to listOf("WiFi", "Parking"),
                "price_per_month" to 3500.5
            )
        )

        val request = server.takeRequest()
        assertEquals("PATCH", request.method)
        assertEquals("/rest/v1/profiles?id=eq.u1", request.path)
        assertEquals("return=representation", request.getHeader("Prefer"))
        assertTrue(request.getHeader("Content-Type")!!.startsWith("application/json"))

        val body = Json.parseToJsonElement(request.body.readUtf8()).jsonObject
        assertEquals("Thandi", body["full_name"]!!.jsonPrimitive.content)
        assertTrue(body["phone"] is JsonNull)
        assertFalse(body["notify_new_messages"]!!.jsonPrimitive.boolean)
        assertEquals(listOf("WiFi", "Parking"), body["amenities"]!!.jsonArray.map { it.jsonPrimitive.content })
        assertEquals(3500.5, body["price_per_month"]!!.jsonPrimitive.double, 0.0)
        assertTrue(response.isSuccessful)
    }

    @Test
    fun `Google id_token grant sends provider, id_token and the raw nonce`() = runBlocking {
        server.enqueue(
            jsonResponse(
                """{"access_token":"a","refresh_token":"r","expires_in":3600,"token_type":"bearer","user":{"id":"u1","email":"a@b.co"}}"""
            )
        )

        val response = authApi.signInWithIdToken(
            body = GoogleIdTokenGrantRequestDto(provider = "google", idToken = "google-id-token", nonce = "raw-nonce")
        )

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/auth/v1/token?grant_type=id_token", request.path)
        val body = Json.parseToJsonElement(request.body.readUtf8()).jsonObject
        // `provider` has no default value precisely so it is always serialised.
        assertEquals("google", body["provider"]!!.jsonPrimitive.content)
        assertEquals("google-id-token", body["id_token"]!!.jsonPrimitive.content)
        assertEquals("raw-nonce", body["nonce"]!!.jsonPrimitive.content)
        assertEquals("a", response.body()!!.accessToken)
    }

    @Test
    fun `server errors are surfaced as unsuccessful responses, not exceptions`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(500).setBody("boom"))
        val response = restApi.getProfile(idFilter = "eq.u1")
        assertFalse(response.isSuccessful)
        assertEquals(500, response.code())
    }

    @Test
    fun `an unreachable network becomes a handled 503 instead of an IOException crash`() = runBlocking {
        server.shutdown() // nothing is listening any more -> connection refused
        val response = restApi.getProfile(idFilter = "eq.u1")
        assertFalse(response.isSuccessful)
        assertEquals(AuthInterceptor.HTTP_NETWORK_UNAVAILABLE, response.code())
    }
}
