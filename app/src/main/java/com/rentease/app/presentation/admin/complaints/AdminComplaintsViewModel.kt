package com.rentease.app.presentation.admin.complaints

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rentease.app.domain.model.Complaint
import com.rentease.app.domain.model.MaintenanceStatus
import com.rentease.app.domain.repository.AdminRepository
import com.rentease.app.domain.repository.AdminResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AdminComplaintsUiState(
    val isLoading: Boolean = true,
    val complaints: List<Complaint> = emptyList(),
    val error: String? = null,
    val updatingId: String? = null
)

@HiltViewModel
class AdminComplaintsViewModel @Inject constructor(
    private val repository: AdminRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminComplaintsUiState())
    val uiState: StateFlow<AdminComplaintsUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val result = repository.getAllComplaints()) {
                is AdminResult.Success -> _uiState.value = _uiState.value.copy(isLoading = false, complaints = result.data)
                is AdminResult.Failure -> _uiState.value = _uiState.value.copy(isLoading = false, error = result.message)
            }
        }
    }

    /** [response] is only ever sent on this one call (there's no separate
     *  "save response" action) — matches the web app's admin panel, where the
     *  response textarea and the status buttons submit together. */
    fun decide(complaint: Complaint, status: MaintenanceStatus, response: String) {
        _uiState.value = _uiState.value.copy(updatingId = complaint.id)
        viewModelScope.launch {
            val result = repository.decideComplaint(complaint.id, status, response.ifBlank { null })
            _uiState.value = _uiState.value.copy(updatingId = null)
            if (result is AdminResult.Failure) {
                _uiState.value = _uiState.value.copy(error = result.message)
            } else {
                load()
            }
        }
    }
}
