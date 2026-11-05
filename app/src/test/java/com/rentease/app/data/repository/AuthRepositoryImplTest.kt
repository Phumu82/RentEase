package com.rentease.app.data.repository

import com.rentease.app.data.auth.TokenManager
import com.rentease.app.data.remote.api.SupabaseAuthApi
import com.rentease.app.data.remote.api.SupabaseRestApi
import com.rentease.app.data.remote.dto.AuthSessionResponseDto
import com.rentease.app.data.remote.dto.GoTrueUserDto
import com.rentease.app.data.remote.dto.ProfileDto
import com.rentease.app.data.remote.dto.RoleDto
import com.rentease.app.data.remote.dto.LandlordProfileDto
import com.rentease.app.data.remote.dto.SignUpRequestDto
import com.rentease.app.data.remote.dto.SignUpResponseDto
import com.rentease.app.data.remote.dto.StudentProfileDto
import com.rentease.app.domain.model.AuthOutcome
import com.rentease.app.domain.model.LandlordRegistrationInput
import com.rentease.app.domain.model.RegistrationOutcome
import com.rentease.app.domain.model.StudentRegistrationInput
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

class AuthRepositoryImplTest {

    private lateinit var authApi: SupabaseAuthApi
    private lateinit var restApi: SupabaseRestApi
    private lateinit var tokenManager: TokenManager
    private lateinit var repository: AuthRepositoryImpl

    private val fakeSession = AuthSessionResponseDto(
        accessToken = "access-123",
        refreshToken = "refresh-456",
        expiresIn = 3600,
        tokenType = "bearer",
        user = GoTrueUserDto(id = "user-uuid-1", email = "student@example.com")
    )

    private val fakeProfile = ProfileDto(
        id = "user-uuid-1",
        fullName = "Thandi Nkosi",
        email = "student@example.com",
        phone = "0821234567",
        preferredLanguage = "en"
    )

    @Before
    fun setUp() {
        authApi = mockk()
        restApi = mockk()
        tokenManager = mockk(relaxUnitFun = true)
        repository = AuthRepositoryImpl(authApi, restApi, tokenManager, mockk(relaxed = true))
    }

    @Test
    fun `login succeeds and exposes the resolved AuthUser via currentUser`() = runTest {
        coEvery { authApi.signInWithPassword(body = any()) } returns Response.success(fakeSession)
        coEvery { restApi.getRoles(userIdFilter = "eq.user-uuid-1") } returns
            Response.success(listOf(RoleDto(userId = "user-uuid-1", role = "student")))
        coEvery { restApi.getProfile(idFilter = "eq.user-uuid-1") } returns
            Response.success(listOf(fakeProfile))

        val outcome = repository.login("student@example.com", "password123")

        assertTrue(outcome is AuthOutcome.Success)
        val user = (outcome as AuthOutcome.Success).user
        assertEquals("Thandi Nkosi", user.fullName)
        assertEquals("user-uuid-1", user.id)
        assertEquals(user, repository.currentUser.value)
    }

    @Test
    fun `requestPasswordReset asks GoTrue to redirect into the app and reports success`() = runTest {
        val redirect = slot<String>()
        val body = slot<com.rentease.app.data.remote.dto.RecoverRequestDto>()
        coEvery { authApi.recover(capture(redirect), capture(body)) } returns Response.success(Unit)

        val result = repository.requestPasswordReset("  student@example.com ")

        assertTrue(result is com.rentease.app.domain.model.PasswordResetResult.Success)
        assertEquals("rentease://reset-password", redirect.captured)
        assertEquals("student@example.com", body.captured.email)
    }

    @Test
    fun `completePasswordReset sends the recovery token as bearer and the new password`() = runTest {
        val bearer = slot<String>()
        val body = slot<com.rentease.app.data.remote.dto.UpdatePasswordRequestDto>()
        coEvery { authApi.updatePassword(capture(bearer), capture(body)) } returns Response.success(Unit)

        val result = repository.completePasswordReset("tok123", "NewPass#2026")

        assertTrue(result is com.rentease.app.domain.model.PasswordResetResult.Success)
        assertEquals("Bearer tok123", bearer.captured)
        assertEquals("NewPass#2026", body.captured.password)
    }

    @Test
    fun `completePasswordReset reports failure when the token is rejected`() = runTest {
        coEvery { authApi.updatePassword(any(), any()) } returns
            Response.error(401, "{}".toResponseBody("application/json".toMediaType()))

        val result = repository.completePasswordReset("expired", "NewPass#2026")

        assertTrue(result is com.rentease.app.domain.model.PasswordResetResult.Failure)
    }

    @Test
    fun `login stores the session before reading the role so the lookup is authenticated`() = runTest {
        coEvery { authApi.signInWithPassword(body = any()) } returns Response.success(fakeSession)
        coEvery { restApi.getRoles(userIdFilter = "eq.user-uuid-1") } returns
            Response.success(listOf(RoleDto(userId = "user-uuid-1", role = "landlord")))
        coEvery { restApi.getProfile(idFilter = "eq.user-uuid-1") } returns Response.success(listOf(fakeProfile))

        repository.login("student@example.com", "password123")

        io.mockk.coVerifyOrder {
            tokenManager.saveSession(any(), any(), "user-uuid-1", any())
            restApi.getRoles(userIdFilter = "eq.user-uuid-1")
            tokenManager.saveSession(any(), any(), "user-uuid-1", "landlord")
        }
    }

    @Test
    fun `an account holding student and admin rows signs in as admin whatever the row order`() = runTest {
        coEvery { authApi.signInWithPassword(body = any()) } returns Response.success(fakeSession)
        coEvery { restApi.getRoles(userIdFilter = "eq.user-uuid-1") } returns Response.success(
            listOf(RoleDto(userId = "user-uuid-1", role = "student"), RoleDto(userId = "user-uuid-1", role = "admin"))
        )
        coEvery { restApi.getProfile(idFilter = "eq.user-uuid-1") } returns Response.success(listOf(fakeProfile))

        val outcome = repository.login("admin@rentease.com", "Admin@2026")

        assertEquals(
            com.rentease.app.domain.model.UserRole.ADMIN,
            (outcome as AuthOutcome.Success).user.role
        )
    }

    @Test
    fun `login with no role clears the stored session`() = runTest {
        coEvery { authApi.signInWithPassword(body = any()) } returns Response.success(fakeSession)
        coEvery { restApi.getRoles(userIdFilter = "eq.user-uuid-1") } returns Response.success(emptyList())

        repository.login("student@example.com", "password123")

        io.mockk.verify { tokenManager.clear() }
    }

    @Test
    fun `login fails cleanly when the account has no assigned role`() = runTest {
        coEvery { authApi.signInWithPassword(body = any()) } returns Response.success(fakeSession)
        coEvery { restApi.getRoles(userIdFilter = "eq.user-uuid-1") } returns Response.success(emptyList())

        val outcome = repository.login("student@example.com", "password123")

        assertTrue(outcome is AuthOutcome.Failure)
    }

    @Test
    fun `login fails and surfaces the server error when credentials are rejected`() = runTest {
        val errorBody = """{"error":"invalid_grant"}""".toResponseBody("application/json".toMediaType())
        coEvery { authApi.signInWithPassword(body = any()) } returns Response.error(400, errorBody)

        val outcome = repository.login("student@example.com", "wrong-password")

        assertTrue(outcome is AuthOutcome.Failure)
        assertTrue((outcome as AuthOutcome.Failure).message.contains("invalid_grant"))
    }

    private val signedUpWithSession = SignUpResponseDto(
        accessToken = "access-123", refreshToken = "refresh-456", expiresIn = 3600, tokenType = "bearer",
        user = GoTrueUserDto(id = "user-uuid-1", email = "student@example.com")
    )

    private val studentInput = StudentRegistrationInput(
        fullName = "Thandi Nkosi", email = "student@example.com", password = "password123",
        phone = "0821234567", studentNumber = "ST1001", universityId = "wits-uuid"
    )

    @Test
    fun `registerStudent stores the session BEFORE writing the child row, so the write is not anonymous`() = runTest {
        val order = mutableListOf<String>()
        coEvery { authApi.signUp(any()) } returns Response.success(signedUpWithSession)
        every { tokenManager.saveSession(any(), any(), any(), any()) } answers { order += "session" }
        coEvery { restApi.createStudentProfile(any()) } answers {
            order += "child"
            Response.success(listOf(StudentProfileDto(userId = "user-uuid-1")))
        }
        coEvery { restApi.getProfile(idFilter = "eq.user-uuid-1") } returns Response.success(listOf(fakeProfile))

        val outcome = repository.registerStudent(studentInput)

        assertTrue(outcome is RegistrationOutcome.Success)
        assertEquals(listOf("session", "child"), order)
    }

    @Test
    fun `registerStudent sends student number and university in the sign-up metadata`() = runTest {
        val request = slot<SignUpRequestDto>()
        coEvery { authApi.signUp(capture(request)) } returns Response.success(signedUpWithSession)
        coEvery { restApi.createStudentProfile(any()) } returns Response.success(listOf(StudentProfileDto(userId = "user-uuid-1")))
        coEvery { restApi.getProfile(idFilter = "eq.user-uuid-1") } returns Response.success(listOf(fakeProfile))

        repository.registerStudent(studentInput)

        assertEquals("student", request.captured.data.role)
        assertEquals("ST1001", request.captured.data.studentNumber)
        assertEquals("wits-uuid", request.captured.data.universityId)
    }

    @Test
    fun `registerStudent still succeeds when the optional child-row write is rejected`() = runTest {
        val errorBody = "".toResponseBody("application/json".toMediaType())
        coEvery { authApi.signUp(any()) } returns Response.success(signedUpWithSession)
        coEvery { restApi.createStudentProfile(any()) } returns Response.error(403, errorBody)
        coEvery { restApi.getProfile(idFilter = "eq.user-uuid-1") } returns Response.success(listOf(fakeProfile))

        val outcome = repository.registerStudent(studentInput)

        assertTrue(outcome is RegistrationOutcome.Success)
    }

    @Test
    fun `registerStudent reports ConfirmEmail when the project requires email confirmation and issues no session`() = runTest {
        coEvery { authApi.signUp(any()) } returns Response.success(SignUpResponseDto(id = "user-uuid-1", email = "student@example.com"))

        val outcome = repository.registerStudent(studentInput)

        assertEquals(RegistrationOutcome.ConfirmEmail("student@example.com"), outcome)
    }

    @Test
    fun `registerStudent turns a duplicate-email response into a readable failure instead of raw JSON`() = runTest {
        val ctx = mockk<android.content.Context>()
        every { ctx.getString(com.rentease.app.R.string.error_email_taken) } returns "EMAIL_TAKEN_TEXT"
        val repo = AuthRepositoryImpl(authApi, restApi, tokenManager, ctx)
        val body = """{"code":422,"error_code":"user_already_exists","msg":"User already registered"}"""
            .toResponseBody("application/json".toMediaType())
        coEvery { authApi.signUp(any()) } returns Response.error(422, body)

        val outcome = repo.registerStudent(studentInput)

        assertEquals(RegistrationOutcome.Failure("EMAIL_TAKEN_TEXT"), outcome)
    }

    @Test
    fun `registerLandlord needs only name email phone and password and stores the session first`() = runTest {
        val order = mutableListOf<String>()
        val request = slot<SignUpRequestDto>()
        coEvery { authApi.signUp(capture(request)) } returns Response.success(signedUpWithSession)
        every { tokenManager.saveSession(any(), any(), any(), any()) } answers { order += "session" }
        coEvery { restApi.createLandlordProfile(any()) } answers {
            order += "child"
            Response.success(listOf(LandlordProfileDto(userId = "user-uuid-1")))
        }
        coEvery { restApi.getProfile(idFilter = "eq.user-uuid-1") } returns Response.success(listOf(fakeProfile))

        val outcome = repository.registerLandlord(
            LandlordRegistrationInput(fullName = "Sipho Dlamini", email = "l@example.com", password = "password123", phone = "0821234567")
        )

        assertTrue(outcome is RegistrationOutcome.Success)
        assertEquals("landlord", request.captured.data.role)
        assertEquals("Sipho Dlamini", request.captured.data.legalName)
        assertEquals(listOf("session", "child"), order)
    }

    @Test
    fun `registerLandlord reports ConfirmEmail when no session is issued`() = runTest {
        coEvery { authApi.signUp(any()) } returns Response.success(SignUpResponseDto(id = "u", email = "l@example.com"))

        val outcome = repository.registerLandlord(
            LandlordRegistrationInput(fullName = "Sipho Dlamini", email = "l@example.com", password = "password123", phone = "0821234567")
        )

        assertEquals(RegistrationOutcome.ConfirmEmail("l@example.com"), outcome)
    }

    @Test
    fun `loginWithGoogleIdToken resolves the existing role for a returning Google user`() = runTest {
        coEvery { authApi.signInWithIdToken(body = any()) } returns Response.success(fakeSession)
        coEvery { restApi.getRoles(userIdFilter = "eq.user-uuid-1") } returns
            Response.success(listOf(RoleDto(userId = "user-uuid-1", role = "landlord")))
        coEvery { restApi.getProfile(idFilter = "eq.user-uuid-1") } returns Response.success(listOf(fakeProfile))

        val outcome = repository.loginWithGoogleIdToken("fake-google-id-token")

        assertTrue(outcome is AuthOutcome.Success)
        assertEquals(com.rentease.app.domain.model.UserRole.LANDLORD, (outcome as AuthOutcome.Success).user.role)
    }

    @Test
    fun `loginWithGoogleIdToken defaults a brand-new Google user to Student, never Admin`() = runTest {
        // Simulates the DB trigger's behaviour for a first-time Google sign-in:
        // no user_roles row exists yet at the moment the client asks, so the
        // repository must fall back to STUDENT rather than fail or (worse)
        // default to an elevated role.
        coEvery { authApi.signInWithIdToken(body = any()) } returns Response.success(fakeSession)
        coEvery { restApi.getRoles(userIdFilter = "eq.user-uuid-1") } returns Response.success(emptyList())
        coEvery { restApi.getProfile(idFilter = "eq.user-uuid-1") } returns Response.success(listOf(fakeProfile))

        val outcome = repository.loginWithGoogleIdToken("fake-google-id-token")

        assertTrue(outcome is AuthOutcome.Success)
        assertEquals(com.rentease.app.domain.model.UserRole.STUDENT, (outcome as AuthOutcome.Success).user.role)
    }

    @Test
    fun `loginWithGoogleIdToken fails cleanly when GoTrue rejects the id token`() = runTest {
        val errorBody = """{"error":"invalid_token"}""".toResponseBody("application/json".toMediaType())
        coEvery { authApi.signInWithIdToken(body = any()) } returns Response.error(401, errorBody)

        val outcome = repository.loginWithGoogleIdToken("bad-token")

        assertTrue(outcome is AuthOutcome.Failure)
    }

    @Test
    fun `login failure messages are resolved via Context, not hardcoded English, so the language toggle actually covers them`() = runTest {
        // A distinct fake string proves the repository genuinely calls
        // context.getString(...) for its fallback error text, rather than
        // using a hardcoded English literal that would never change when
        // the user picks isiZulu/Tshivenda in Settings.
        val fakeLocalizedContext = mockk<android.content.Context>()
        every { fakeLocalizedContext.getString(com.rentease.app.R.string.error_no_stored_session) } returns "TEST_LOCALIZED_STRING"
        val localizedRepository = AuthRepositoryImpl(authApi, restApi, tokenManager, fakeLocalizedContext)
        every { tokenManager.getUserId() } returns null

        val outcome = localizedRepository.refreshRole()

        assertTrue(outcome is AuthOutcome.Failure)
        assertEquals("TEST_LOCALIZED_STRING", (outcome as AuthOutcome.Failure).message)
    }
}
