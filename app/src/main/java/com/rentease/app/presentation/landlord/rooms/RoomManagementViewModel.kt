package com.rentease.app.presentation.landlord.rooms

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rentease.app.domain.model.RentalRoom
import com.rentease.app.domain.model.RoomInput
import com.rentease.app.domain.model.RoomStatus
import com.rentease.app.domain.repository.LandlordRepository
import com.rentease.app.domain.repository.LandlordResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RoomManagementUiState(
    val isLoading: Boolean = true,
    val rooms: List<RentalRoom> = emptyList(),
    val error: String? = null,
    val isSaving: Boolean = false
)

@HiltViewModel
class RoomManagementViewModel @Inject constructor(
    private val repository: LandlordRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val propertyId: String = checkNotNull(savedStateHandle["propertyId"])

    private val _uiState = MutableStateFlow(RoomManagementUiState())
    val uiState: StateFlow<RoomManagementUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val result = repository.getRooms(propertyId)) {
                is LandlordResult.Success -> _uiState.value = _uiState.value.copy(isLoading = false, rooms = result.data)
                is LandlordResult.Failure -> _uiState.value = _uiState.value.copy(isLoading = false, error = result.message)
            }
        }
    }

    fun addRoom(name: String, roomType: String, price: Double, capacity: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            val result = repository.createRoom(
                propertyId, RoomInput(name = name, roomType = roomType, pricePerMonth = price, capacity = capacity, status = RoomStatus.AVAILABLE)
            )
            _uiState.value = _uiState.value.copy(isSaving = false)
            if (result is LandlordResult.Failure) _uiState.value = _uiState.value.copy(error = result.message) else load()
        }
    }

    fun updateRoomStatus(room: RentalRoom, status: RoomStatus) {
        viewModelScope.launch {
            val result = repository.updateRoom(
                room.id, RoomInput(name = room.name, roomType = room.roomType, pricePerMonth = room.pricePerMonth, capacity = room.capacity, status = status)
            )
            if (result is LandlordResult.Failure) _uiState.value = _uiState.value.copy(error = result.message) else load()
        }
    }

    fun updateRoomPrice(room: RentalRoom, newPrice: Double) {
        viewModelScope.launch {
            val result = repository.updateRoom(
                room.id, RoomInput(name = room.name, roomType = room.roomType, pricePerMonth = newPrice, capacity = room.capacity, status = room.status)
            )
            if (result is LandlordResult.Failure) _uiState.value = _uiState.value.copy(error = result.message) else load()
        }
    }

    fun deleteRoom(room: RentalRoom) {
        viewModelScope.launch {
            val result = repository.deleteRoom(room.id)
            if (result is LandlordResult.Failure) _uiState.value = _uiState.value.copy(error = result.message) else load()
        }
    }
}
