package com.rentease.app.data.remote.api

import com.rentease.app.data.remote.dto.AuthSessionResponseDto
import com.rentease.app.data.remote.dto.GoogleIdTokenGrantRequestDto
import com.rentease.app.data.remote.dto.PasswordGrantRequestDto
import com.rentease.app.data.remote.dto.RecoverRequestDto
import com.rentease.app.data.remote.dto.RefreshTokenRequestDto
import com.rentease.app.data.remote.dto.SignUpRequestDto
import com.rentease.app.data.remote.dto.SignUpResponseDto
import com.rentease.app.data.remote.dto.UpdatePasswordRequestDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Query

/**
 * Supabase's GoTrue auth service, exposed at {SUPABASE_URL}/auth/v1/ (all auth endpoints).
 * This *is* the "POST /auth/register" / "POST /auth/login" contract from
 * Part 1 §5.5 — Supabase just names the paths differently. Password hashing
 * (bcrypt, per §3.1) happens inside GoTrue; the client never sees a hash.
 */
interface SupabaseAuthApi {

    @POST("auth/v1/signup")
    suspend fun signUp(@Body body: SignUpRequestDto): Response<SignUpResponseDto>

    @POST("auth/v1/token")
    suspend fun signInWithPassword(
        @Query("grant_type") grantType: String = "password",
        @Body body: PasswordGrantRequestDto
    ): Response<AuthSessionResponseDto>

    /** Google SSO — Part 1 §3.2. The Android app gets an ID token via Credential
     *  Manager and hands it to GoTrue, which verifies it against Google's public
     *  keys and returns the same JWT/refresh-token pair as password login. */
    @POST("auth/v1/token")
    suspend fun signInWithIdToken(
        @Query("grant_type") grantType: String = "id_token",
        @Body body: GoogleIdTokenGrantRequestDto
    ): Response<AuthSessionResponseDto>

    @POST("auth/v1/token")
    suspend fun refreshSession(
        @Query("grant_type") grantType: String = "refresh_token",
        @Body body: RefreshTokenRequestDto
    ): Response<AuthSessionResponseDto>

    /** Emails a password-reset link. GoTrue answers 200 whether or not the address has an account. */
    @POST("auth/v1/recover")
    suspend fun recover(
        @Query("redirect_to") redirectTo: String,
        @Body body: RecoverRequestDto
    ): Response<Unit>

    /** Sets a new password using the short-lived recovery token from the emailed link. */
    @PUT("auth/v1/user")
    suspend fun updatePassword(
        @Header("Authorization") bearer: String,
        @Body body: UpdatePasswordRequestDto
    ): Response<Unit>

    @POST("auth/v1/logout")
    suspend fun signOut(): Response<Unit>
}
