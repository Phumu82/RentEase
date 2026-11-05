package com.rentease.app.presentation.admin.access

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rentease.app.R
import com.rentease.app.domain.model.AuthOutcome
import com.rentease.app.domain.repository.AdminRepository
import com.rentease.app.domain.repository.AdminResult
import com.rentease.app.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface AdminAccessUiState {
    data object Idle : AdminAccessUiState
    data object Submitting : AdminAccessUiState
    data object Granted : AdminAccessUiState
    data class Error(val message: String) : AdminAccessUiState
}

/**
 * Deliberately NOT reachable from registration or any main nav — only from a
 * low-key link on the Login screen or the `rentease://admin-access` deep link.
 * The code check itself happens entirely server-side (see the admin-access
 * Edge Function); this ViewModel never sees or compares the secret.
 *
 * The Edge Function grants the admin role to "whoever is currently
 * authenticated", so this screen has two stages: sign in (if not already
 * signed in as SOME account), then submit the code. [isAuthenticated]
 * drives which stage the Composable shows.
 */
@HiltViewModel
class AdminAccessViewModel @Inject constructor(
    private val adminRepository: AdminRepository,
    private val authRepository: AuthRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow<AdminAccessUiState>(AdminAccessUiState.Idle)
    val uiState: StateFlow<AdminAccessUiState> = _uiState.asStateFlow()

    val isAuthenticated: StateFlow<Boolean> = authRepository.currentUser
        .combine(_uiState) { user, _ -> user != null }
        .stateIn(viewModelScope, SharingStarted.Eagerly, authRepository.currentUser.value != null)

    fun signIn(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = AdminAccessUiState.Error(context.getString(R.string.error_enter_email_password))
            return
        }
        _uiState.value = AdminAccessUiState.Submitting
        viewModelScope.launch {
            when (val outcome = authRepository.login(email.trim(), password)) {
                is AuthOutcome.Success -> _uiState.value = AdminAccessUiState.Idle
                is AuthOutcome.Failure -> _uiState.value = AdminAccessUiState.Error(outcome.message)
            }
        }
    }

    fun submitCode(code: String) {
        if (code.isBlank()) {
            _uiState.value = AdminAccessUiState.Error(context.getString(R.string.error_enter_admin_code))
            return
        }
        _uiState.value = AdminAccessUiState.Submitting
        viewModelScope.launch {
            when (val result = adminRepository.requestAdminAccess(code)) {
                is AdminResult.Success -> {
                    // Re-fetch the role from the server so the locally-cached
                    // role catches up to what was just granted, without
                    // trusting the client to self-report "I am now admin".
                    when (authRepository.refreshRole()) {
                        is AuthOutcome.Success -> _uiState.value = AdminAccessUiState.Granted
                        is AuthOutcome.Failure -> _uiState.value = AdminAccessUiState.Error(
                            context.getString(R.string.error_admin_session_refresh_failed)
                        )
                    }
                }
                is AdminResult.Failure -> _uiState.value = AdminAccessUiState.Error(result.message)
            }
        }
    }
}
