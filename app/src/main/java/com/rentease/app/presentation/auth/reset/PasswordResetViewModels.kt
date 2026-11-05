package com.rentease.app.presentation.auth.reset

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rentease.app.domain.model.PasswordReset
import com.rentease.app.domain.model.PasswordResetResult
import com.rentease.app.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ForgotPasswordUiState(
    val isSending: Boolean = false,
    val sent: Boolean = false,
    val invalidEmail: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(ForgotPasswordUiState())
    val uiState: StateFlow<ForgotPasswordUiState> = _uiState.asStateFlow()

    fun send(email: String) {
        if (!PasswordReset.isPlausibleEmail(email)) {
            _uiState.value = ForgotPasswordUiState(invalidEmail = true)
            return
        }
        viewModelScope.launch {
            _uiState.value = ForgotPasswordUiState(isSending = true)
            _uiState.value = when (val result = authRepository.requestPasswordReset(email)) {
                PasswordResetResult.Success -> ForgotPasswordUiState(sent = true)
                is PasswordResetResult.Failure -> ForgotPasswordUiState(error = result.message)
            }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(invalidEmail = false, error = null)
    }
}

data class ResetPasswordUiState(
    val isSaving: Boolean = false,
    val done: Boolean = false,
    val problem: PasswordReset.Problem? = null,
    val error: String? = null
)

@HiltViewModel
class ResetPasswordViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    /** Recovery token from the emailed link (route argument); blank means the link was not valid. */
    val token: String = savedStateHandle.get<String>("token").orEmpty()

    private val _uiState = MutableStateFlow(ResetPasswordUiState())
    val uiState: StateFlow<ResetPasswordUiState> = _uiState.asStateFlow()

    fun submit(password: String, confirm: String) {
        val problem = PasswordReset.validate(password, confirm)
        if (problem != null) {
            _uiState.value = ResetPasswordUiState(problem = problem)
            return
        }
        viewModelScope.launch {
            _uiState.value = ResetPasswordUiState(isSaving = true)
            _uiState.value = when (val result = authRepository.completePasswordReset(token, password)) {
                PasswordResetResult.Success -> ResetPasswordUiState(done = true)
                is PasswordResetResult.Failure -> ResetPasswordUiState(error = result.message)
            }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(problem = null, error = null)
    }
}
