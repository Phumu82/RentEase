package com.rentease.app.presentation.admin.verification

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.rentease.app.domain.model.LandlordDocumentKind
import com.rentease.app.domain.model.LandlordVerificationItem
import com.rentease.app.domain.model.VerificationDecision
import com.rentease.app.domain.model.VerificationFilter
import com.rentease.app.domain.model.VerificationReview
import com.rentease.app.domain.model.VerificationStatus
import com.rentease.app.presentation.common.OpenUrlEffect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LandlordVerificationScreen(
    onBack: () -> Unit,
    viewModel: LandlordVerificationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    OpenUrlEffect(uiState.documentUrlToOpen, viewModel::documentOpened)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.landlord_verification_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back_button))
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            LazyRow(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(VerificationFilter.entries) { filter ->
                    FilterChip(
                        selected = uiState.filter == filter,
                        onClick = { viewModel.setFilter(filter) },
                        label = { Text(stringResource(filterLabel(filter))) }
                    )
                }
            }
            uiState.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 16.dp)) }
            uiState.messageRes?.let {
                Text(stringResource(it), color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 16.dp))
            }
            when {
                uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                uiState.visible.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.no_landlords_in_filter))
                }
                else -> LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(uiState.visible, key = { it.userId }) { item ->
                        VerificationRow(
                            item = item,
                            notes = uiState.notes[item.userId].orEmpty(),
                            busy = uiState.busyUserId == item.userId,
                            onNotes = { viewModel.setNotes(item.userId, it) },
                            onOpenDocument = viewModel::openDocument,
                            onDecide = { viewModel.decide(item, it) }
                        )
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }
}

@StringRes
private fun filterLabel(filter: VerificationFilter): Int = when (filter) {
    VerificationFilter.AWAITING_REVIEW -> R.string.verification_filter_awaiting
    VerificationFilter.DOCUMENTS_REQUIRED -> R.string.verification_filter_documents_required
    VerificationFilter.REJECTED -> R.string.verification_filter_rejected
    VerificationFilter.VERIFIED -> R.string.verification_filter_verified
    VerificationFilter.ALL -> R.string.verification_filter_all
}

@StringRes
private fun statusLabel(status: VerificationStatus): Int = when (status) {
    VerificationStatus.PENDING -> R.string.verification_status_pending
    VerificationStatus.SUBMITTED -> R.string.verification_status_submitted
    VerificationStatus.VERIFIED -> R.string.verification_status_verified
    VerificationStatus.REJECTED -> R.string.verification_status_rejected
    VerificationStatus.DOCUMENTS_REQUIRED -> R.string.verification_status_documents_required
}

@StringRes
private fun documentLabel(kind: LandlordDocumentKind): Int = when (kind) {
    LandlordDocumentKind.ID_DOCUMENT -> R.string.doc_id_document
    LandlordDocumentKind.PROOF_OF_OWNERSHIP -> R.string.doc_proof_of_ownership
    LandlordDocumentKind.PROOF_OF_ADDRESS -> R.string.doc_proof_of_address
    LandlordDocumentKind.BUSINESS_REGISTRATION -> R.string.doc_business_registration
}

@Composable
private fun VerificationRow(
    item: LandlordVerificationItem,
    notes: String,
    busy: Boolean,
    onNotes: (String) -> Unit,
    onOpenDocument: (String) -> Unit,
    onDecide: (VerificationDecision) -> Unit
) {
    Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(item.fullName, style = MaterialTheme.typography.titleMedium)
            Text(stringResource(statusLabel(item.status)), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            item.email?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            item.phone?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            item.businessName?.let { Text(stringResource(R.string.business_label, it), style = MaterialTheme.typography.bodyMedium) }
            item.registrationNumber?.takeIf { it.isNotBlank() }?.let {
                Text(stringResource(R.string.admin_registration_number, it), style = MaterialTheme.typography.bodyMedium)
            }
            item.address?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }

            if (item.documents.isEmpty()) {
                Text(stringResource(R.string.admin_no_documents), style = MaterialTheme.typography.bodyMedium)
            } else {
                item.documents.forEach { doc ->
                    TextButton(onClick = { onOpenDocument(doc.path) }) {
                        Text(stringResource(R.string.admin_view_document, stringResource(documentLabel(doc.kind))))
                    }
                }
            }
            if (!VerificationReview.hasRequiredDocuments(item)) {
                Text(stringResource(R.string.admin_required_documents_missing), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }

            item.notes?.takeIf { it.isNotBlank() }?.let {
                Text(stringResource(R.string.admin_previous_notes, it), style = MaterialTheme.typography.bodySmall)
            }
            OutlinedTextField(
                value = notes,
                onValueChange = onNotes,
                label = { Text(stringResource(R.string.admin_notes_label)) },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onDecide(VerificationDecision.VERIFIED) }, enabled = !busy, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.verify_button))
                }
                OutlinedButton(onClick = { onDecide(VerificationDecision.DOCUMENTS_REQUIRED) }, enabled = !busy, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.admin_request_documents_button))
                }
                OutlinedButton(onClick = { onDecide(VerificationDecision.REJECTED) }, enabled = !busy, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.reject_button))
                }
            }
        }
    }
}
