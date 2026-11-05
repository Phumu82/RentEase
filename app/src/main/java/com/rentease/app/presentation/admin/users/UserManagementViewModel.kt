package com.rentease.app.presentation.admin.users

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rentease.app.domain.model.AdminUserItem
import com.rentease.app.domain.repository.AdminRepository
import com.rentease.app.domain.repository.AdminResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UserManagementUiState(
    val isLoading: Boolean = true,
    val users: List<AdminUserItem> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class UserManagementViewModel @Inject constructor(
    private val repository: AdminRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(UserManagementUiState())
    val uiState: StateFlow<UserManagementUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val result = repository.getAllUsers()) {
                is AdminResult.Success -> _uiState.value = _uiState.value.copy(isLoading = false, users = result.data)
                is AdminResult.Failure -> _uiState.value = _uiState.value.copy(isLoading = false, error = result.message)
            }
        }
    }

    fun toggleSuspended(user: AdminUserItem) {
        viewModelScope.launch {
            val result = repository.setUserSuspended(user.id, !user.isSuspended)
            if (result is AdminResult.Failure) _uiState.value = _uiState.value.copy(error = result.message) else load()
        }
    }
}
