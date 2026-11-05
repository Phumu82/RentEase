package com.rentease.app.presentation.landlord.bookings

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
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rentease.app.R
import com.rentease.app.domain.model.BookingStatus
import com.rentease.app.presentation.common.label
import com.rentease.app.domain.model.TenantBooking

@Composable
fun BookingRequestsScreen(
    onBack: () -> Unit,
    viewModel: BookingRequestsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val visibleBookings = uiState.bookings.filter { uiState.filter == null || it.status == uiState.filter }

    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.booking_requests_title)) }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {

            LazyRow(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                item {
                    FilterChip(selected = uiState.filter == null, onClick = { viewModel.setFilter(null) }, label = { Text(stringResource(R.string.filter_all)) })
                    Spacer(Modifier.height(0.dp))
                }
                items(BookingStatus.entries) { status ->
                    Row {
                        Spacer(Modifier.height(0.dp))
                        FilterChip(
                            selected = uiState.filter == status,
                            onClick = { viewModel.setFilter(status) },
                            label = { Text(status.label()) },
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }

            when {
                uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                visibleBookings.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(stringResource(R.string.no_booking_requests)) }
                else -> LazyColumn(Modifier.fillMaxSize().padding(16.dp)) {
                    uiState.error?.let { error -> item { Text(error, color = MaterialTheme.colorScheme.error); Spacer(Modifier.height(8.dp)) } }
                    items(visibleBookings, key = { it.id }) { booking ->
                        BookingRow(
                            booking = booking,
                            availableRooms = uiState.roomsByProperty[booking.propertyId].orEmpty(),
                            onApprove = { roomId -> viewModel.approve(booking, roomId) },
                            onReject = { viewModel.reject(booking) },
                            onAssignRoom = { roomId -> viewModel.assignRoom(booking, roomId) }
                        )
                        Spacer(Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun BookingRow(
    booking: TenantBooking,
    availableRooms: List<com.rentease.app.domain.model.RentalRoom>,
    onApprove: (roomId: String?) -> Unit,
    onReject: () -> Unit,
    onAssignRoom: (String) -> Unit
) {
    var roomMenuOpen by remember { mutableStateOf(false) }
    var selectedRoomId by remember { mutableStateOf(booking.roomId) }

    Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text("${booking.studentName} — ${booking.propertyTitle}", style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.booking_request_summary, booking.reference, booking.moveInDate, booking.leaseMonths), style = MaterialTheme.typography.bodyMedium)
            Text(stringResource(R.string.price_per_month, booking.monthlyRent.toInt()) + " · " + stringResource(R.string.deposit_label, booking.deposit.toInt()), style = MaterialTheme.typography.bodyMedium)
            booking.message?.takeIf { it.isNotBlank() }?.let { Text("\"$it\"", style = MaterialTheme.typography.bodyMedium) }
            Text(stringResource(R.string.status_label, booking.status.label()), style = MaterialTheme.typography.bodyMedium)

            if (booking.status == BookingStatus.PENDING || booking.status == BookingStatus.APPROVED) {
                Spacer(Modifier.height(8.dp))
                Box {
                    TextButton(onClick = { roomMenuOpen = true }) {
                        Text(
                            booking.roomName?.let { stringResource(R.string.room_assigned_label, it) }
                                ?: selectedRoomId?.let { id -> availableRooms.find { it.id == id }?.name }
                                    ?.let { stringResource(R.string.room_assigned_label, it) }
                                ?: stringResource(R.string.assign_room_action)
                        )
                    }
                    DropdownMenu(expanded = roomMenuOpen, onDismissRequest = { roomMenuOpen = false }) {
                        availableRooms.forEach { room ->
                            DropdownMenuItem(
                                text = { Text("${room.name} (${room.status.label()})") },
                                onClick = {
                                    selectedRoomId = room.id
                                    roomMenuOpen = false
                                    if (booking.status == BookingStatus.APPROVED) onAssignRoom(room.id)
                                }
                            )
                        }
                    }
                }
            }

            if (booking.status == BookingStatus.PENDING) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onReject) { Text(stringResource(R.string.reject_button)) }
                    TextButton(onClick = { onApprove(selectedRoomId) }) { Text(stringResource(R.string.approve_button)) }
                }
            }
        }
    }
}
