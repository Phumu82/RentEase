package com.rentease.app.presentation.student.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rentease.app.domain.model.AppNotification
import com.rentease.app.domain.repository.NotificationsRepository
import com.rentease.app.domain.repository.NotificationsResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StudentNotificationsUiState(
    val isLoading: Boolean = true,
    val notifications: List<AppNotification> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class StudentNotificationsViewModel @Inject constructor(
    private val repository: NotificationsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StudentNotificationsUiState())
    val uiState: StateFlow<StudentNotificationsUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val result = repository.getNotifications()) {
                is NotificationsResult.Success -> _uiState.value = _uiState.value.copy(isLoading = false, notifications = result.data)
                is NotificationsResult.Failure -> _uiState.value = _uiState.value.copy(isLoading = false, error = result.message)
            }
        }
    }

    fun markRead(notification: AppNotification) {
        if (notification.readAt != null) return
        viewModelScope.launch {
            val result = repository.markRead(notification.id)
            if (result is NotificationsResult.Failure) {
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
