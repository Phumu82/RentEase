package com.rentease.app.presentation.student.maintenance

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rentease.app.R
import com.rentease.app.domain.model.MaintenancePriority
import com.rentease.app.domain.model.MaintenanceTicket
import com.rentease.app.domain.model.StudentBookingSummary
import com.rentease.app.presentation.common.OpenUrlEffect
import com.rentease.app.presentation.common.label

private val CATEGORIES = listOf("Plumbing", "Electrical", "Appliance", "Internet", "Security", "Structural", "Other")

@Composable
fun StudentMaintenanceScreen(
    onBack: () -> Unit,
    viewModel: StudentMaintenanceViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    OpenUrlEffect(uiState.photoUrlToOpen, viewModel::photoOpened)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.student_maintenance_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = null) }
                }
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            else -> LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                item {
                    Text(
                        stringResource(R.string.student_maintenance_description),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(12.dp))
                }
                uiState.error?.let { error ->
                    item {
                        Text(error, color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(8.dp))
                    }
                }
                if (uiState.photoOpenFailed) {
                    item {
                        Text(stringResource(R.string.onboarding_error_open_document), color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(8.dp))
                    }
                }
                item {
                    NewRequestForm(
                        eligibleBookings = uiState.eligibleBookings,
                        isSubmitting = uiState.isSubmitting,
                        submitError = uiState.submitError,
                        submitSuccess = uiState.submitSuccess,
                        onSubmit = { bookingId, title, description, category, priority, photoUri ->
                            viewModel.submit(context, bookingId, title, description, category, priority, photoUri)
                        },
                        onDismissSuccess = viewModel::clearSubmitState
                    )
                    Spacer(Modifier.height(20.dp))
                    Text(stringResource(R.string.maintenance_your_requests_heading), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                }
                if (uiState.requests.isEmpty()) {
                    item { Text(stringResource(R.string.no_maintenance_tickets)) }
                } else {
                    items(uiState.requests, key = { it.id }) { ticket ->
                        RequestRow(ticket, onViewPhoto = viewModel::openPhoto)
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun NewRequestForm(
    eligibleBookings: List<StudentBookingSummary>,
    isSubmitting: Boolean,
    submitError: String?,
    submitSuccess: Boolean,
    onSubmit: (bookingId: String, title: String, description: String, category: String, priority: MaintenancePriority, photoUri: Uri?) -> Unit,
    onDismissSuccess: () -> Unit
) {
    var selectedBookingId by remember { mutableStateOf<String?>(null) }
    var bookingMenuOpen by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(CATEGORIES.first()) }
    var categoryMenuOpen by remember { mutableStateOf(false) }
    var priority by remember { mutableStateOf(MaintenancePriority.MEDIUM) }
    var priorityMenuOpen by remember { mutableStateOf(false) }
    var photoUri by remember { mutableStateOf<Uri?>(null) }

    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { photoUri = it }

    Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(stringResource(R.string.maintenance_new_request_heading), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))

            if (eligibleBookings.isEmpty()) {
                Text(stringResource(R.string.maintenance_no_eligible_bookings), style = MaterialTheme.typography.bodyMedium)
            } else {
                Box {
                    val selectedLabel = eligibleBookings.firstOrNull { it.id == selectedBookingId }
                        ?.let { "${it.propertyTitle} \u2014 ${it.reference}" }
                        ?: stringResource(R.string.maintenance_select_booking_label)
                    OutlinedButton(onClick = { bookingMenuOpen = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(selectedLabel)
                    }
                    DropdownMenu(expanded = bookingMenuOpen, onDismissRequest = { bookingMenuOpen = false }) {
                        eligibleBookings.forEach { booking ->
                            DropdownMenuItem(
                                text = { Text("${booking.propertyTitle} \u2014 ${booking.reference}") },
                                onClick = { selectedBookingId = booking.id; bookingMenuOpen = false }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = title, onValueChange = { title = it },
                    label = { Text(stringResource(R.string.maintenance_issue_title_label)) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = description, onValueChange = { description = it },
                    label = { Text(stringResource(R.string.maintenance_issue_description_label)) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Box {
                    OutlinedButton(onClick = { categoryMenuOpen = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("${stringResource(R.string.maintenance_category_label)}: $category")
                    }
                    DropdownMenu(expanded = categoryMenuOpen, onDismissRequest = { categoryMenuOpen = false }) {
                        CATEGORIES.forEach { option ->
                            DropdownMenuItem(text = { Text(option) }, onClick = { category = option; categoryMenuOpen = false })
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Box {
                    OutlinedButton(onClick = { priorityMenuOpen = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("${stringResource(R.string.maintenance_priority_label)}: ${priority.label()}")
                    }
                    DropdownMenu(expanded = priorityMenuOpen, onDismissRequest = { priorityMenuOpen = false }) {
                        MaintenancePriority.entries.forEach { option ->
                            DropdownMenuItem(text = { Text(option.label()) }, onClick = { priority = option; priorityMenuOpen = false })
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { pickPhoto.launch("image/*") }, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        if (photoUri == null) stringResource(R.string.maintenance_attach_photo_button)
                        else stringResource(R.string.maintenance_photo_attached)
                    )
                }

                if (submitError != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(submitError, color = MaterialTheme.colorScheme.error)
                }
                if (submitSuccess) {
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.maintenance_request_submitted), color = MaterialTheme.colorScheme.primary)
                }

                Spacer(Modifier.height(12.dp))
                Button(
                    enabled = !isSubmitting && selectedBookingId != null && title.isNotBlank() && description.isNotBlank(),
                    onClick = {
                        onDismissSuccess()
                        onSubmit(selectedBookingId!!, title, description, category, priority, photoUri)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(modifier = Modifier.height(18.dp))
                    } else {
                        Text(stringResource(R.string.maintenance_submit_button))
                    }
                }
            }
        }
    }
}

@Composable
private fun RequestRow(ticket: MaintenanceTicket, onViewPhoto: (String) -> Unit) {
    Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text(ticket.title, style = MaterialTheme.typography.titleMedium)
            Text(ticket.propertyTitle, style = MaterialTheme.typography.bodyMedium)
            Text(ticket.description, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.maintenance_priority_category, ticket.priority.label(), ticket.category),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(ticket.status.label(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            ticket.photoUrl?.let { path ->
                TextButton(onClick = { onViewPhoto(path) }) { Text(stringResource(R.string.maintenance_view_photo)) }
            }
        }
    }
}
