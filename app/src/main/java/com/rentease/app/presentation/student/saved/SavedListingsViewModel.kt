package com.rentease.app.presentation.student.saved

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rentease.app.domain.model.PropertySummary
import com.rentease.app.domain.repository.SavedListingsRepository
import com.rentease.app.domain.repository.SyncState
import com.rentease.app.util.ConnectivityObserver
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SavedListingsUiState(
    val listings: List<PropertySummary> = emptyList(),
    val isOnline: Boolean = true,
    val syncState: SyncState = SyncState.Idle
)

@HiltViewModel
class SavedListingsViewModel @Inject constructor(
    private val repository: SavedListingsRepository,
    connectivityObserver: ConnectivityObserver
) : ViewModel() {

    val uiState: StateFlow<SavedListingsUiState> = combine(
        repository.observeSavedListings(),
        connectivityObserver.observe(),
        repository.observeSyncState()
    ) { listings, online, sync ->
        SavedListingsUiState(listings = listings, isOnline = online, syncState = sync)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SavedListingsUiState())

    init {
        viewModelScope.launch { repository.refreshFromServer() }
    }

    fun toggleSaved(property: PropertySummary) {
        viewModelScope.launch { repository.toggleSaved(property) }
    }

    fun retrySync() {
        repository.requestImmediateSync()
    }
}
