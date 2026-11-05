package com.rentease.app.presentation.admin.moderation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rentease.app.domain.model.PropertyModerationItem
import com.rentease.app.domain.repository.AdminRepository
import com.rentease.app.domain.repository.AdminResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PropertyModerationUiState(
    val isLoading: Boolean = true,
    val items: List<PropertyModerationItem> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class PropertyModerationViewModel @Inject constructor(
    private val repository: AdminRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PropertyModerationUiState())
    val uiState: StateFlow<PropertyModerationUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val result = repository.getPendingProperties()) {
                is AdminResult.Success -> _uiState.value = _uiState.value.copy(isLoading = false, items = result.data)
                is AdminResult.Failure -> _uiState.value = _uiState.value.copy(isLoading = false, error = result.message)
            }
        }
    }

    fun approve(item: PropertyModerationItem) {
        viewModelScope.launch {
            val result = repository.approveProperty(item.id)
            if (result is AdminResult.Failure) _uiState.value = _uiState.value.copy(error = result.message) else load()
        }
    }

    fun reject(item: PropertyModerationItem) {
        viewModelScope.launch {
            val result = repository.rejectProperty(item.id)
            if (result is AdminResult.Failure) _uiState.value = _uiState.value.copy(error = result.message) else load()
        }
    }
}
