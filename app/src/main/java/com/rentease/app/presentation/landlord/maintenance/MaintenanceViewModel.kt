package com.rentease.app.presentation.landlord.maintenance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rentease.app.data.repository.DocumentUploadRepository
import com.rentease.app.domain.model.MaintenanceStatus
import com.rentease.app.domain.model.MaintenanceTicket
import com.rentease.app.domain.repository.LandlordRepository
import com.rentease.app.domain.repository.LandlordResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MaintenanceUiState(
    val isLoading: Boolean = true,
    val tickets: List<MaintenanceTicket> = emptyList(),
    val error: String? = null,
    val photoUrlToOpen: String? = null,
    val photoOpenFailed: Boolean = false
)

@HiltViewModel
class MaintenanceViewModel @Inject constructor(
    private val repository: LandlordRepository,
    private val documentUploadRepository: DocumentUploadRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MaintenanceUiState())
    val uiState: StateFlow<MaintenanceUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val result = repository.getMaintenanceTickets()) {
                is LandlordResult.Success -> _uiState.value = _uiState.value.copy(isLoading = false, tickets = result.data)
                is LandlordResult.Failure -> _uiState.value = _uiState.value.copy(isLoading = false, error = result.message)
            }
        }
    }

    fun updateStatus(ticket: MaintenanceTicket, status: MaintenanceStatus) {
        viewModelScope.launch {
            val result = repository.updateMaintenanceStatus(ticket.id, status)
            if (result is LandlordResult.Failure) _uiState.value = _uiState.value.copy(error = result.message) else load()
        }
    }

    /** Resolves a tenant photo path (private `booking-documents` bucket) into a signed URL. */
    fun openPhoto(path: String) {
        viewModelScope.launch {
            val url = documentUploadRepository.createSignedUrl("booking-documents", path)
            _uiState.value = if (url != null) _uiState.value.copy(photoUrlToOpen = url, photoOpenFailed = false)
            else _uiState.value.copy(photoOpenFailed = true)
        }
    }

    fun photoOpened() { _uiState.value = _uiState.value.copy(photoUrlToOpen = null) }
}
