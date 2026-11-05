package com.rentease.app.presentation.landlord.bookings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rentease.app.domain.model.BookingStatus
import com.rentease.app.domain.model.RentalRoom
import com.rentease.app.domain.model.TenantBooking
import com.rentease.app.domain.repository.LandlordRepository
import com.rentease.app.domain.repository.LandlordResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BookingRequestsUiState(
    val isLoading: Boolean = true,
    val bookings: List<TenantBooking> = emptyList(),
    val roomsByProperty: Map<String, List<RentalRoom>> = emptyMap(),
    val error: String? = null,
    val filter: BookingStatus? = null // null = show all
)

@HiltViewModel
class BookingRequestsViewModel @Inject constructor(
    private val repository: LandlordRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BookingRequestsUiState())
    val uiState: StateFlow<BookingRequestsUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val result = repository.getBookings()) {
                is LandlordResult.Success -> {
                    val bookings = result.data
                    _uiState.value = _uiState.value.copy(isLoading = false, bookings = bookings)
                    // Pre-fetch rooms for each distinct property so the "assign room"
                    // picker doesn't need a network round trip on tap.
                    bookings.map { it.propertyId }.distinct().forEach { propertyId ->
                        val roomsResult = repository.getRooms(propertyId)
                        if (roomsResult is LandlordResult.Success) {
                            _uiState.value = _uiState.value.copy(
                                roomsByProperty = _uiState.value.roomsByProperty + (propertyId to roomsResult.data)
                            )
                        }
                    }
                }
                is LandlordResult.Failure -> _uiState.value = _uiState.value.copy(isLoading = false, error = result.message)
            }
        }
    }

    fun setFilter(status: BookingStatus?) {
        _uiState.value = _uiState.value.copy(filter = status)
    }

    fun approve(booking: TenantBooking, roomId: String?) {
        viewModelScope.launch {
            val result = repository.approveBooking(booking.id, roomId)
            if (result is LandlordResult.Failure) _uiState.value = _uiState.value.copy(error = result.message) else load()
        }
    }

    fun reject(booking: TenantBooking) {
        viewModelScope.launch {
            val result = repository.rejectBooking(booking.id)
            if (result is LandlordResult.Failure) _uiState.value = _uiState.value.copy(error = result.message) else load()
        }
    }

    fun assignRoom(booking: TenantBooking, roomId: String) {
        viewModelScope.launch {
            val result = repository.assignRoom(booking.id, roomId)
            if (result is LandlordResult.Failure) _uiState.value = _uiState.value.copy(error = result.message) else load()
        }
    }
}
