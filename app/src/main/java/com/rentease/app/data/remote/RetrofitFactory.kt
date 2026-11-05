package com.rentease.app.data.remote

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * Single place where the app's Retrofit instance is configured, shared by the
 * Hilt NetworkModule (production) and the MockWebServer contract tests, so the
 * tests exercise exactly the converters and JSON settings the app ships with.
 */
object RetrofitFactory {

    val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    fun create(client: OkHttpClient, baseUrl: String): Retrofit =
        Retrofit.Builder()
            .baseUrl(baseUrl.ensureTrailingSlash())
            .client(client)
            // Order matters: the Map<String, Any?> converter must come first.
            .addConverterFactory(JsonMapConverterFactory(json))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    private fun String.ensureTrailingSlash() = if (endsWith("/")) this else "$this/"
}
