package com.rentease.app.presentation.admin.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rentease.app.domain.model.AdminDashboardStats
import com.rentease.app.domain.repository.AdminRepository
import com.rentease.app.domain.repository.AdminResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AdminDashboardUiState(
    val isLoading: Boolean = true,
    val stats: AdminDashboardStats? = null,
    val error: String? = null
)

@HiltViewModel
class AdminDashboardViewModel @Inject constructor(
    private val repository: AdminRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminDashboardUiState())
    val uiState: StateFlow<AdminDashboardUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val result = repository.getDashboardStats()) {
                is AdminResult.Success -> _uiState.value = _uiState.value.copy(isLoading = false, stats = result.data)
                is AdminResult.Failure -> _uiState.value = _uiState.value.copy(isLoading = false, error = result.message)
            }
        }
    }
}
