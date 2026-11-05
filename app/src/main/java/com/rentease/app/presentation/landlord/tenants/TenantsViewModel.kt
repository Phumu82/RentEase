package com.rentease.app.presentation.landlord.tenants

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rentease.app.domain.model.BookingStatus
import com.rentease.app.domain.model.TenantBooking
import com.rentease.app.domain.repository.LandlordRepository
import com.rentease.app.domain.repository.LandlordResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TenantsUiState(
    val isLoading: Boolean = true,
    val tenants: List<TenantBooking> = emptyList(),
    val error: String? = null
)

/**
 * "Tenant management" (Phase 6) — a current tenant is simply a booking that has
 * moved past approval (APPROVED or ACTIVE), viewed from the landlord's side.
 * There is no separate `tenants` table in the schema; this is a derived view
 * over `bookings`, which is the correct normalized approach rather than
 * duplicating tenant data into a second table.
 */
@HiltViewModel
class TenantsViewModel @Inject constructor(
    private val repository: LandlordRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TenantsUiState())
    val uiState: StateFlow<TenantsUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val result = repository.getBookings()) {
                is LandlordResult.Success -> {
                    val tenants = result.data.filter { it.status == BookingStatus.APPROVED || it.status == BookingStatus.ACTIVE }
                    _uiState.value = _uiState.value.copy(isLoading = false, tenants = tenants)
                }
                is LandlordResult.Failure -> _uiState.value = _uiState.value.copy(isLoading = false, error = result.message)
            }
        }
    }
}
