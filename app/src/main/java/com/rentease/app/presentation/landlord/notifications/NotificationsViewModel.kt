package com.rentease.app.presentation.landlord.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rentease.app.domain.model.AppNotification
import com.rentease.app.domain.repository.LandlordRepository
import com.rentease.app.domain.repository.LandlordResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotificationsUiState(
    val isLoading: Boolean = true,
    val notifications: List<AppNotification> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val repository: LandlordRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val result = repository.getNotifications()) {
                is LandlordResult.Success -> _uiState.value = _uiState.value.copy(isLoading = false, notifications = result.data)
                is LandlordResult.Failure -> _uiState.value = _uiState.value.copy(isLoading = false, error = result.message)
            }
        }
    }

    fun markRead(notification: AppNotification) {
        if (notification.readAt != null) return
        viewModelScope.launch {
            val result = repository.markNotificationRead(notification.id)
            if (result is LandlordResult.Failure) {
                _uiState.value = _uiState.value.copy(error = result.message)
            } else {
                _uiState.value = _uiState.value.copy(
                    notifications = _uiState.value.notifications.map {
                        if (it.id == notification.id) it.copy(readAt = "now") else it
                    }
                )
            }
        }
    }
}
