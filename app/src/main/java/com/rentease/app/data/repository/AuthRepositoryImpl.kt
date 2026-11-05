package com.rentease.app.data.repository

import com.rentease.app.domain.model.SupabaseConfig
import com.rentease.app.BuildConfig
import com.rentease.app.data.remote.httpHint
import android.content.Context
import com.rentease.app.R
import com.rentease.app.domain.model.PasswordResetResult
import com.rentease.app.domain.model.PasswordReset
import com.rentease.app.data.remote.dto.UpdatePasswordRequestDto
import com.rentease.app.data.remote.dto.RecoverRequestDto
import com.rentease.app.data.auth.TokenManager
import com.rentease.app.data.remote.api.SupabaseAuthApi
import com.rentease.app.data.remote.api.SupabaseRestApi
import com.rentease.app.data.remote.dto.AuthSessionResponseDto
import com.rentease.app.data.remote.dto.GoogleIdTokenGrantRequestDto
import com.rentease.app.data.remote.dto.LandlordProfileDto
import com.rentease.app.data.remote.dto.PasswordGrantRequestDto
import com.rentease.app.data.remote.dto.ProfileDto
import com.rentease.app.data.remote.dto.SignUpMetadataDto
import com.rentease.app.data.remote.dto.SignUpRequestDto
import com.rentease.app.data.remote.dto.StudentProfileDto
import com.rentease.app.domain.model.AppLanguage
import com.rentease.app.domain.model.AuthErrorKind
import com.rentease.app.domain.model.AuthErrorParser
import com.rentease.app.domain.model.AuthOutcome
import com.rentease.app.domain.model.RegistrationOutcome
import com.rentease.app.domain.model.AuthUser
import com.rentease.app.domain.model.NotificationPreferences
import com.rentease.app.domain.model.LandlordRegistrationInput
import com.rentease.app.domain.model.StudentRegistrationInput
import com.rentease.app.domain.model.UserRole
import com.rentease.app.domain.repository.AuthRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authApi: SupabaseAuthApi,
    private val restApi: SupabaseRestApi,
    private val tokenManager: TokenManager,
    @ApplicationContext private val context: Context
) : AuthRepository {

    private val _currentUser = MutableStateFlow<AuthUser?>(null)
    override val currentUser: StateFlow<AuthUser?> = _currentUser.asStateFlow()

    override suspend fun registerStudent(input: StudentRegistrationInput): RegistrationOutcome {
        val signUpResponse = authApi.signUp(
            SignUpRequestDto(
                email = input.email,
                password = input.password,
                data = SignUpMetadataDto(
                    fullName = input.fullName,
                    role = "student",
                    phone = input.phone,
                    studentNumber = input.studentNumber,
                    universityId = input.universityId.takeIf { it.isNotBlank() }
                )
            )
        )
        val body = signUpResponse.bodyOrNull() ?: return RegistrationOutcome.Failure(signUpResponse.friendlyMessage())
        // No session means the project requires email confirmation: the account exists, sign-in comes after the link.
        val session = body.toSession() ?: return RegistrationOutcome.ConfirmEmail(input.email)

        // The session must be stored BEFORE any further call, otherwise those calls go out as
        // "anon" and row-level security rejects them.
        persistSession(session, UserRole.STUDENT)

        // Best effort: the DB trigger normally creates this row from the metadata above. This upsert
        // fills it in (or creates it on older databases). A failure here must not undo a created
        // account - the onboarding screen lets the student complete these details later.
        runCatching {
            restApi.createStudentProfile(
                StudentProfileDto(
                    userId = session.user.id,
                    studentNumber = input.studentNumber,
                    universityId = input.universityId.takeIf { it.isNotBlank() },
                    course = input.course,
                    faculty = input.faculty,
                    yearOfStudy = input.yearOfStudy,
                    budgetMax = input.budgetMax,
                    preferredRoomType = input.preferredRoomType,
                    earliestMoveIn = input.earliestMoveIn,
                    needsFurnished = input.needsFurnished,
                    preferredProvince = input.preferredProvince,
                    preferredCity = input.preferredCity,
                    preferredUniversityId = input.preferredUniversityId
                )
            )
        }
        return resolveAndCacheUser(session.user.id, session.user.email, UserRole.STUDENT).toRegistration()
    }

    override suspend fun registerLandlord(input: LandlordRegistrationInput): RegistrationOutcome {
        val signUpResponse = authApi.signUp(
            SignUpRequestDto(
                email = input.email,
                password = input.password,
                data = SignUpMetadataDto(
                    fullName = input.fullName,
                    role = "landlord",
                    phone = input.phone,
                    companyName = input.businessName,
                    legalName = input.fullName
                )
            )
        )
        val body = signUpResponse.bodyOrNull() ?: return RegistrationOutcome.Failure(signUpResponse.friendlyMessage())
        val session = body.toSession() ?: return RegistrationOutcome.ConfirmEmail(input.email)

        persistSession(session, UserRole.LANDLORD)

        // Same reasoning as the student path: token first, then a non-fatal upsert of the child row.
        runCatching {
            restApi.createLandlordProfile(
                LandlordProfileDto(
                    userId = session.user.id,
                    legalName = input.fullName,
                    businessName = input.businessName,
                    idNumber = input.idNumber,
                    contactPhone = input.phone
                )
            )
        }
        return resolveAndCacheUser(session.user.id, session.user.email, UserRole.LANDLORD).toRegistration()
    }

    override suspend fun login(email: String, password: String): AuthOutcome {
        val response = authApi.signInWithPassword(body = PasswordGrantRequestDto(email, password))
        val session = response.bodyOrNull() ?: return response.toFailure()
        // user_roles / profiles are only readable by the "authenticated" role, so the new token must be
        // stored BEFORE they are queried; otherwise the lookup goes out as anon, comes back empty and a
        // perfectly valid login ends in "signed in but no role". A provisional role is stored first and
        // corrected once the real one is known.
        persistSession(session, UserRole.STUDENT)
        val role = fetchRole(session.user.id)
        if (role == null) {
            tokenManager.clear()
            return AuthOutcome.Failure(context.getString(R.string.error_signed_in_no_role))
        }
        persistSession(session, role)
        val outcome = resolveAndCacheUser(session.user.id, session.user.email, role)
        if (outcome is AuthOutcome.Failure) tokenManager.clear()
        return outcome
    }

    override suspend fun loginWithGoogleIdToken(idToken: String, nonce: String?): AuthOutcome {
        val response = authApi.signInWithIdToken(
            body = GoogleIdTokenGrantRequestDto(provider = "google", idToken = idToken, nonce = nonce)
        )
        val session = response.bodyOrNull() ?: return response.toFailure()
        // New Google sign-ins default to 'student' via the same DB trigger used for
        // email sign-up (§3.2 of the spec) — role is guaranteed to exist by the time
        // GoTrue returns a session for a first-time user.
        persistSession(session, UserRole.STUDENT) // token first - see login()
        val role = fetchRole(session.user.id) ?: UserRole.STUDENT
        persistSession(session, role)
        val outcome = resolveAndCacheUser(session.user.id, session.user.email, role)
        if (outcome is AuthOutcome.Failure) tokenManager.clear()
        return outcome
    }

    override suspend fun requestPasswordReset(email: String): PasswordResetResult {
        val response = authApi.recover(PasswordReset.REDIRECT_URL, RecoverRequestDto(email.trim()))
        return if (response.isSuccessful) PasswordResetResult.Success
        else PasswordResetResult.Failure(context.getString(R.string.reset_request_failed))
    }

    override suspend fun completePasswordReset(recoveryToken: String, newPassword: String): PasswordResetResult {
        val response = authApi.updatePassword("Bearer $recoveryToken", UpdatePasswordRequestDto(newPassword))
        return if (response.isSuccessful) PasswordResetResult.Success
        else PasswordResetResult.Failure(context.getString(R.string.reset_failed))
    }

    override suspend fun restoreSession(): AuthOutcome {
        val userId = tokenManager.getUserId()
        val roleStr = tokenManager.getRole()
        if (!tokenManager.isLoggedIn() || userId == null || roleStr == null) {
            return AuthOutcome.Failure(context.getString(R.string.error_no_stored_session))
        }
        return resolveAndCacheUser(userId, email = null, role = UserRole.fromDb(roleStr))
    }

    override suspend fun logout() {
        runCatching { authApi.signOut() }
        tokenManager.clear()
        _currentUser.value = null
    }

    override suspend fun refreshRole(): AuthOutcome {
        val userId = tokenManager.getUserId() ?: return AuthOutcome.Failure(context.getString(R.string.error_no_stored_session))
        val role = fetchRole(userId) ?: return AuthOutcome.Failure(context.getString(R.string.error_could_not_resolve_role_server))
        // Update the cached role too, so future restoreSession() calls (e.g.
        // after a cold app restart) see the up-to-date role without another
        // network round trip.
        tokenManager.saveSession(
            accessToken = tokenManager.getAccessToken().orEmpty(),
            refreshToken = tokenManager.getRefreshToken().orEmpty(),
            userId = userId,
            role = role.toDb()
        )
        return resolveAndCacheUser(userId, email = null, role = role)
    }

    override suspend fun updateLanguage(language: AppLanguage): AuthOutcome {
        val userId = tokenManager.getUserId() ?: return AuthOutcome.Failure(context.getString(R.string.error_not_signed_in))
        val role = tokenManager.getRole()?.let { UserRole.fromDb(it) }
            ?: return AuthOutcome.Failure(context.getString(R.string.error_could_not_resolve_role))

        val response = restApi.updateProfile(
            idFilter = "eq.$userId",
            body = mapOf("preferred_language" to language.dbCode)
        )
        if (!response.isSuccessful) {
            return AuthOutcome.Failure("${context.getString(R.string.error_could_not_save_language)}${context.httpHint(response.code())}")
        }
        return resolveAndCacheUser(userId, email = _currentUser.value?.email, role = role)
    }

    override suspend fun updateNotificationPreferences(preferences: NotificationPreferences): AuthOutcome {
        val userId = tokenManager.getUserId() ?: return AuthOutcome.Failure(context.getString(R.string.error_not_signed_in))
        val role = tokenManager.getRole()?.let { UserRole.fromDb(it) }
            ?: return AuthOutcome.Failure(context.getString(R.string.error_could_not_resolve_role))

        val response = restApi.updateProfile(
            idFilter = "eq.$userId",
            body = mapOf(
                "notify_booking_updates" to preferences.bookingUpdates,
                "notify_new_messages" to preferences.newMessages,
                "notify_maintenance_updates" to preferences.maintenanceUpdates
            )
        )
        if (!response.isSuccessful) {
            return AuthOutcome.Failure("${context.getString(R.string.error_could_not_save_notification_preferences)}${context.httpHint(response.code())}")
        }
        return resolveAndCacheUser(userId, email = _currentUser.value?.email, role = role)
    }

    override suspend fun updateProfile(fullName: String, phone: String?): AuthOutcome {
        val trimmedName = fullName.trim()
        if (trimmedName.isEmpty()) return AuthOutcome.Failure(context.getString(R.string.error_name_required))
        val userId = tokenManager.getUserId() ?: return AuthOutcome.Failure(context.getString(R.string.error_not_signed_in))
        val role = tokenManager.getRole()?.let { UserRole.fromDb(it) }
            ?: return AuthOutcome.Failure(context.getString(R.string.error_could_not_resolve_role))

        val response = restApi.updateProfile(
            idFilter = "eq.$userId",
            body = mapOf(
                "full_name" to trimmedName,
                "phone" to phone?.trim()?.takeIf { it.isNotEmpty() }
            )
        )
        if (!response.isSuccessful) {
            return AuthOutcome.Failure("${context.getString(R.string.error_could_not_save_profile)}${context.httpHint(response.code())}")
        }
        return resolveAndCacheUser(userId, email = _currentUser.value?.email, role = role)
    }

    private suspend fun fetchRole(userId: String): UserRole? {
        val response = restApi.getRoles(userIdFilter = "eq.$userId")
        // An account can hold several rows (an admin created in the dashboard also gets the default
        // 'student' row from the sign-up trigger), and PostgREST returns them in no guaranteed order,
        // so the most privileged role wins instead of whichever row happens to come first.
        val roles = response.bodyOrNull()?.mapNotNull { runCatching { UserRole.fromDb(it.role) }.getOrNull() } ?: return null
        return roles.maxByOrNull { it.priority() }

    }

    private fun UserRole.priority(): Int = when (this) {
        UserRole.ADMIN -> 3
        UserRole.LANDLORD -> 2
        UserRole.STUDENT -> 1
    }

    private suspend fun resolveAndCacheUser(userId: String, email: String?, role: UserRole): AuthOutcome {
        val profileResponse = restApi.getProfile(idFilter = "eq.$userId")
        val profile: ProfileDto = profileResponse.bodyOrNull()?.firstOrNull()
            ?: return AuthOutcome.Failure(context.getString(R.string.error_could_not_load_profile_after_signin))

        val user = AuthUser(
            id = userId,
            email = email ?: profile.email.orEmpty(),
            fullName = profile.fullName.ifBlank { email ?: profile.email.orEmpty() },
            role = role,
            phone = profile.phone,
            profilePhotoUrl = profile.avatarUrl,
            preferredLanguage = AppLanguage.fromDb(profile.preferredLanguage),
            isVerified = profile.isVerified,
            notificationPreferences = NotificationPreferences(
                bookingUpdates = profile.notifyBookingUpdates,
                newMessages = profile.notifyNewMessages,
                maintenanceUpdates = profile.notifyMaintenanceUpdates
            )
        )
        _currentUser.value = user
        return AuthOutcome.Success(user)
    }

    private fun persistSession(session: AuthSessionResponseDto, role: UserRole) {
        tokenManager.saveSession(
            accessToken = session.accessToken,
            refreshToken = session.refreshToken,
            userId = session.user.id,
            role = role.toDb()
        )
    }

    private fun <T> Response<T>.bodyOrNull(): T? = if (isSuccessful) body() else null

    private fun <T> Response<T>.toFailure(): AuthOutcome.Failure = AuthOutcome.Failure(friendlyMessage())

    private fun AuthOutcome.toRegistration(): RegistrationOutcome = when (this) {
        is AuthOutcome.Success -> RegistrationOutcome.Success(user)
        is AuthOutcome.Failure -> RegistrationOutcome.Failure(message)
    }

    /** A readable sentence for the common auth failures; otherwise the server's own message, never raw JSON. */
    private fun <T> Response<T>.friendlyMessage(): String {
        val raw = errorBody()?.string()
        return when (AuthErrorParser.classify(code(), raw)) {
            AuthErrorKind.EMAIL_TAKEN -> context.getString(R.string.error_email_taken)
            AuthErrorKind.WEAK_PASSWORD -> context.getString(R.string.error_password_rules)
            AuthErrorKind.RATE_LIMITED -> context.getString(R.string.error_rate_limited)
            AuthErrorKind.EMAIL_INVALID -> context.getString(R.string.error_email_invalid)
            AuthErrorKind.NO_CONNECTION ->
                if (SupabaseConfig.isConfigured(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_ANON_KEY)) {
                    context.getString(R.string.error_no_connection)
                } else {
                    context.getString(R.string.error_not_configured)
                }
            AuthErrorKind.INVALID_CREDENTIALS -> context.getString(R.string.error_invalid_credentials)
            AuthErrorKind.EMAIL_NOT_CONFIRMED -> context.getString(R.string.error_email_not_confirmed)
            AuthErrorKind.UNKNOWN ->
                AuthErrorParser.messageText(raw)?.takeIf { code() != 503 }
                    ?: "${context.getString(R.string.error_request_failed)}${context.httpHint(code())}"
        }
    }
}
