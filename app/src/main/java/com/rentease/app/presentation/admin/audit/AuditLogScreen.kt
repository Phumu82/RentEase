package com.rentease.app.presentation.admin.audit

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rentease.app.R
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rentease.app.domain.model.AuditLogEntry
import com.rentease.app.domain.repository.AdminRepository
import com.rentease.app.domain.repository.AdminResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuditLogUiState(
    val isLoading: Boolean = true,
    val entries: List<AuditLogEntry> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class AuditLogViewModel @Inject constructor(
    private val repository: AdminRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuditLogUiState())
    val uiState: StateFlow<AuditLogUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            when (val result = repository.getAuditLog()) {
                is AdminResult.Success -> _uiState.value = AuditLogUiState(isLoading = false, entries = result.data)
                is AdminResult.Failure -> _uiState.value = AuditLogUiState(isLoading = false, error = result.message)
            }
        }
    }
}

@Composable
fun AuditLogScreen(
    onBack: () -> Unit,
    viewModel: AuditLogViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.audit_log_title)) }) }) { padding ->
        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            uiState.entries.isEmpty() -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { Text(stringResource(R.string.no_audit_events)) }
            else -> LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                uiState.error?.let { error -> item { Text(error, color = MaterialTheme.colorScheme.error); Spacer(Modifier.height(8.dp)) } }
                items(uiState.entries, key = { it.id }) { entry ->
                    Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text(entry.action, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${entry.actorName ?: stringResource(R.string.fallback_unknown_actor)} · ${entry.entity}${entry.entityId?.let { " ($it)" } ?: ""}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(entry.createdAt, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}
