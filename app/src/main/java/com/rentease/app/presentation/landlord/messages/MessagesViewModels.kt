package com.rentease.app.presentation.landlord.messages

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rentease.app.data.auth.TokenManager
import com.rentease.app.domain.model.ChatMessage
import com.rentease.app.domain.model.ConversationSummary
import com.rentease.app.domain.repository.LandlordRepository
import com.rentease.app.domain.repository.LandlordResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ConversationListUiState(
    val isLoading: Boolean = true,
    val conversations: List<ConversationSummary> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class ConversationListViewModel @Inject constructor(
    private val repository: LandlordRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConversationListUiState())
    val uiState: StateFlow<ConversationListUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val result = repository.getConversations()) {
                is LandlordResult.Success -> _uiState.value = _uiState.value.copy(isLoading = false, conversations = result.data)
                is LandlordResult.Failure -> _uiState.value = _uiState.value.copy(isLoading = false, error = result.message)
            }
        }
    }
    /** Quiet re-fetch for the live-updating list: no spinner, and a failed poll keeps what is on screen. */
    fun refresh() {
        viewModelScope.launch {
            val result = repository.getConversations()
            if (result is LandlordResult.Success && result.data != _uiState.value.conversations) {
                _uiState.value = _uiState.value.copy(conversations = result.data)
            }
        }
    }
}

data class ThreadUiState(
    val isLoading: Boolean = true,
    val messages: List<ChatMessage> = emptyList(),
    val error: String? = null,
    val isSending: Boolean = false,
    val currentUserId: String? = null
)

@HiltViewModel
class ThreadViewModel @Inject constructor(
    private val repository: LandlordRepository,
    private val tokenManager: TokenManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val conversationId: String = checkNotNull(savedStateHandle["conversationId"])

    private val _uiState = MutableStateFlow(ThreadUiState(currentUserId = tokenManager.getUserId()))
    val uiState: StateFlow<ThreadUiState> = _uiState.asStateFlow()

    init {
        load()
        viewModelScope.launch { repository.markConversationRead(conversationId) }
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val result = repository.getMessages(conversationId)) {
                is LandlordResult.Success -> _uiState.value = _uiState.value.copy(isLoading = false, messages = result.data)
                is LandlordResult.Failure -> _uiState.value = _uiState.value.copy(isLoading = false, error = result.message)
            }
        }
    }

    /** Quiet re-fetch for the live-updating chat: no spinner, and a failed poll keeps what is on screen. */
    fun refresh() {
        viewModelScope.launch {
            val result = repository.getMessages(conversationId)
            if (result is LandlordResult.Success && result.data != _uiState.value.messages) {
                _uiState.value = _uiState.value.copy(messages = result.data)
                repository.markConversationRead(conversationId)
            }
        }
    }

    fun sendMessage(body: String) {
        if (body.isBlank()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSending = true)
            val result = repository.sendMessage(conversationId, body)
            _uiState.value = _uiState.value.copy(isSending = false)
            if (result is LandlordResult.Failure) _uiState.value = _uiState.value.copy(error = result.message) else load()
        }
    }
}
