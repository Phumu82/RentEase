package com.rentease.app.presentation.student.messages

import com.rentease.app.data.remote.httpHint
import dagger.hilt.android.qualifiers.ApplicationContext
import com.rentease.app.R
import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rentease.app.data.auth.TokenManager
import com.rentease.app.data.remote.api.SupabaseListingsApi
import com.rentease.app.data.remote.dto.MessageDto
import com.rentease.app.domain.model.ChatMessage
import com.rentease.app.domain.model.ConversationSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

data class StudentConversationListUiState(
    val isLoading: Boolean = true,
    val conversations: List<ConversationSummary> = emptyList(),
    val error: String? = null
)

/**
 * Student-side messaging, using the same `conversations`/`messages` tables
 * as the Landlord side but filtered by student_id — see
 * SupabaseListingsApi's student-messaging endpoints (kept separate from
 * SupabaseLandlordApi so Student screens never depend on a landlord-named
 * API surface, even though the underlying tables are shared).
 */
@HiltViewModel
class StudentConversationListViewModel @Inject constructor(
    private val api: SupabaseListingsApi,
    private val tokenManager: TokenManager,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(StudentConversationListUiState())
    val uiState: StateFlow<StudentConversationListUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() = fetch(showSpinner = true)

    /** Quiet re-fetch for the live-updating list: no spinner, and a failed poll keeps what is on screen. */
    fun refresh() = fetch(showSpinner = false)

    private fun fetch(showSpinner: Boolean) {
        val userId = tokenManager.getUserId()
        if (userId == null) {
            if (showSpinner) {
                _uiState.value = StudentConversationListUiState(isLoading = false, error = appContext.getString(R.string.error_not_signed_in))
            }
            return
        }
        viewModelScope.launch {
            if (showSpinner) _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val response = api.getConversationsForStudent(studentIdFilter = "eq.$userId")
            if (!response.isSuccessful) {
                if (showSpinner) _uiState.value = _uiState.value.copy(isLoading = false, error = "${appContext.getString(R.string.error_could_not_load_conversations)}${appContext.httpHint(response.code())}")
                return@launch
            }
            val conversations = response.body().orEmpty()
            val summaries = conversations.map { dto ->
                val unread = api.getMessagesForConversation(conversationIdFilter = "eq.${dto.id}").body().orEmpty()
                    .count { it.senderId != userId && it.readAt == null }
                ConversationSummary(
                    id = dto.id.orEmpty(),
                    studentId = dto.studentId,
                    landlordId = dto.landlordId,
                    propertyId = dto.propertyId,
                    // Resolving the landlord's real name would need a batch
                    // profile lookup identical to the landlord-side pattern;
                    // left as a generic label here since Student's API
                    // surface doesn't yet expose a name-lookup endpoint — a
                    // follow-up, not a functional blocker for viewing/sending
                    // messages themselves.
                    counterpartName = appContext.getString(R.string.fallback_landlord_name),
                    subject = dto.subject,
                    lastMessageAt = dto.lastMessageAt,
                    unreadCount = unread
                )
            }
            if (showSpinner || summaries != _uiState.value.conversations) {
                _uiState.value = _uiState.value.copy(isLoading = false, conversations = summaries)
            }
        }
    }
}

data class StudentThreadUiState(
    val isLoading: Boolean = true,
    val messages: List<ChatMessage> = emptyList(),
    val error: String? = null,
    val isSending: Boolean = false,
    val currentUserId: String? = null
)

@HiltViewModel
class StudentThreadViewModel @Inject constructor(
    private val api: SupabaseListingsApi,
    private val tokenManager: TokenManager,
    @ApplicationContext private val appContext: Context,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val conversationId: String = checkNotNull(savedStateHandle["conversationId"])

    private val _uiState = MutableStateFlow(StudentThreadUiState(currentUserId = tokenManager.getUserId()))
    val uiState: StateFlow<StudentThreadUiState> = _uiState.asStateFlow()

    init {
        load()
        val userId = tokenManager.getUserId()
        if (userId != null) {
            viewModelScope.launch {
                runCatching {
                    api.markMessagesReadAsStudent(
                        conversationIdFilter = "eq.$conversationId",
                        notSenderFilter = "neq.$userId",
                        body = mapOf("read_at" to Instant.now().toString())
                    )
                }
            }
        }
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val response = api.getMessagesForConversation(conversationIdFilter = "eq.$conversationId")
            if (response.isSuccessful) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    messages = response.body().orEmpty().map {
                        ChatMessage(
                            id = it.id.orEmpty(), conversationId = it.conversationId, senderId = it.senderId,
                            body = it.body, createdAt = it.createdAt, readAt = it.readAt
                        )
                    }
                )
            } else {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "${appContext.getString(R.string.error_could_not_load_messages)}${appContext.httpHint(response.code())}")
            }
        }
    }

    /** Quiet re-fetch for the live-updating chat: no spinner, and a failed poll keeps what is on screen. */
    fun refresh() {
        viewModelScope.launch {
            val response = runCatching { api.getMessagesForConversation(conversationIdFilter = "eq.$conversationId") }.getOrNull() ?: return@launch
            if (!response.isSuccessful) return@launch
            val fresh = response.body().orEmpty().map {
                ChatMessage(
                    id = it.id.orEmpty(), conversationId = it.conversationId, senderId = it.senderId,
                    body = it.body, createdAt = it.createdAt, readAt = it.readAt
                )
            }
            if (fresh != _uiState.value.messages) _uiState.value = _uiState.value.copy(messages = fresh)
        }
    }

    fun sendMessage(body: String) {
        if (body.isBlank()) return
        val userId = tokenManager.getUserId() ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSending = true)
            val response = api.sendMessageAsStudent(
                MessageDto(conversationId = conversationId, senderId = userId, body = body)
            )
            _uiState.value = _uiState.value.copy(isSending = false)
            if (response.isSuccessful) load() else {
                _uiState.value = _uiState.value.copy(error = "${appContext.getString(R.string.error_could_not_send_message)}${appContext.httpHint(response.code())}")
            }
        }
    }
}
