package com.rentease.app.presentation.auth.login

import android.util.Log
import com.rentease.app.BuildConfig
import kotlin.coroutines.cancellation.CancellationException
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rentease.app.R
import com.rentease.app.domain.model.AuthOutcome
import com.rentease.app.domain.repository.AuthRepository
import com.rentease.app.notifications.DeviceTokenRegistrar
import com.rentease.app.presentation.auth.AuthUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val deviceTokenRegistrar: DeviceTokenRegistrar,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = AuthUiState.Error(context.getString(R.string.error_enter_email_password))
            return
        }
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            when (val outcome = guarded { authRepository.login(email.trim(), password) }) {
                is AuthOutcome.Success -> {
                    // Signed in is signed in: push registration runs on its own and never blocks navigation.
                    _uiState.value = AuthUiState.Success(outcome.user)
                    launch { deviceTokenRegistrar.registerCurrentToken() }
                }
                is AuthOutcome.Failure -> _uiState.value = AuthUiState.Error(outcome.message)
            }
        }
    }

    /** Called once Credential Manager returns a Google ID token — see
     *  LoginScreen's GoogleSignInButton wiring. */
    fun loginWithGoogle(idToken: String, rawNonce: String) {
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            when (val outcome = guarded { authRepository.loginWithGoogleIdToken(idToken, rawNonce) }) {
                is AuthOutcome.Success -> {
                    // Signed in is signed in: push registration runs on its own and never blocks navigation.
                    _uiState.value = AuthUiState.Success(outcome.user)
                    launch { deviceTokenRegistrar.registerCurrentToken() }
                }
                is AuthOutcome.Failure -> _uiState.value = AuthUiState.Error(outcome.message)
            }
        }
    }

    /** A malformed or unexpected server answer must show a message, never crash the app. */
    private suspend fun guarded(block: suspend () -> AuthOutcome): AuthOutcome =
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Logged so the real cause is visible in Logcat; the user only sees a friendly message.
            Log.e("LoginViewModel", "Login failed unexpectedly", e)
            // Debug builds show the exception type so a tester can report it; release builds stay friendly.
            val detail = if (BuildConfig.DEBUG) " (${e.javaClass.simpleName}: ${e.message.orEmpty().take(90)})" else ""
            AuthOutcome.Failure(context.getString(R.string.error_request_failed) + detail)
        }

    /** Surfaces a UI-level failure (e.g. Google sign-in cancelled/unavailable). */
    fun showError(message: String) {
        _uiState.value = AuthUiState.Error(message)
    }

    fun resetError() {
        if (_uiState.value is AuthUiState.Error) _uiState.value = AuthUiState.Idle
    }
}