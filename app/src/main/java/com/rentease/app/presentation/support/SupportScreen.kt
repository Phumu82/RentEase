package com.rentease.app.presentation.support

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rentease.app.R
import com.rentease.app.domain.model.Complaint
import com.rentease.app.domain.model.ComplaintCategoryCatalog
import com.rentease.app.presentation.common.label

@Composable
fun SupportScreen(
    onBack: () -> Unit,
    viewModel: SupportViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.support_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = null) }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }

        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            item {
                Text(stringResource(R.string.support_description), style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(12.dp))
                uiState.error?.let { Text(it, color = MaterialTheme.colorScheme.error); Spacer(Modifier.height(8.dp)) }
                NewComplaintForm(
                    isSubmitting = uiState.isSubmitting,
                    submitError = uiState.submitError,
                    submitSuccess = uiState.submitSuccess,
                    onSubmit = { subject, category, description -> viewModel.submit(subject, category, description) }
                )
                Spacer(Modifier.height(8.dp))
            }
            if (uiState.complaints.isEmpty()) {
                item {
                    Text(stringResource(R.string.no_complaints_logged), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.no_complaints_logged_subtitle), style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                items(uiState.complaints, key = { it.id }) { complaint ->
                    ComplaintRow(complaint)
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun NewComplaintForm(
    isSubmitting: Boolean,
    submitError: String?,
    submitSuccess: Boolean,
    onSubmit: (subject: String, category: String, description: String) -> Unit
) {
    var subject by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(ComplaintCategoryCatalog.ALL.first()) }
    var categoryMenuOpen by remember { mutableStateOf(false) }
    var description by remember { mutableStateOf("") }

    Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(stringResource(R.string.new_complaint_heading), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = subject, onValueChange = { subject = it },
                label = { Text(stringResource(R.string.complaint_subject_label)) },
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))

            Box {
                OutlinedButton(onClick = { categoryMenuOpen = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("${stringResource(R.string.complaint_category_label)}: $category")
                }
                DropdownMenu(expanded = categoryMenuOpen, onDismissRequest = { categoryMenuOpen = false }) {
                    ComplaintCategoryCatalog.ALL.forEach { option ->
                        DropdownMenuItem(text = { Text(option) }, onClick = { category = option; categoryMenuOpen = false })
                    }
                }
            }
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = description, onValueChange = { description = it },
                label = { Text(stringResource(R.string.complaint_description_label)) },
                modifier = Modifier.fillMaxWidth()
            )

            if (submitError != null) {
                Spacer(Modifier.height(8.dp))
                Text(submitError, color = MaterialTheme.colorScheme.error)
            }
            if (submitSuccess) {
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.complaint_submitted_confirmation), color = MaterialTheme.colorScheme.primary)
            }

            Spacer(Modifier.height(12.dp))
            Button(
                enabled = !isSubmitting && subject.trim().length >= 4 && description.trim().length >= 15,
                onClick = { onSubmit(subject.trim(), category, description.trim()) },
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.height(18.dp))
                } else {
                    Text(stringResource(R.string.submit_complaint_button))
                }
            }
        }
    }
}

@Composable
private fun ComplaintRow(complaint: Complaint) {
    Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text(complaint.subject, style = MaterialTheme.typography.titleMedium)
            Text(complaint.category, style = MaterialTheme.typography.bodyMedium)
            Text(complaint.status.label(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(4.dp))
            Text(complaint.description, style = MaterialTheme.typography.bodyMedium)
            complaint.adminResponse?.let { response ->
                Spacer(Modifier.height(8.dp))
                Text("${stringResource(R.string.admin_response_label)} $response", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
