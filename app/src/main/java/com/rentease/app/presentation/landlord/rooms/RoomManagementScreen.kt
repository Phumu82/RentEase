package com.rentease.app.presentation.landlord.rooms

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rentease.app.R
import com.rentease.app.presentation.common.DropdownField
import com.rentease.app.domain.model.RoomTypeGuide
import com.rentease.app.domain.model.RentalRoom
import com.rentease.app.domain.model.RoomStatus
import com.rentease.app.presentation.common.label

@Composable
fun RoomManagementScreen(
    onBack: () -> Unit,
    viewModel: RoomManagementViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.rooms_title)) }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) { Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_room_title)) }
        }
    ) { padding ->
        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            uiState.rooms.isEmpty() -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.no_rooms_yet))
            }
            else -> LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                uiState.error?.let { error -> item { Text(error, color = MaterialTheme.colorScheme.error); Spacer(Modifier.height(8.dp)) } }
                items(uiState.rooms, key = { it.id }) { room ->
                    RoomRow(
                        room = room,
                        onStatusChange = { newStatus -> viewModel.updateRoomStatus(room, newStatus) },
                        onPriceChange = { newPrice -> viewModel.updateRoomPrice(room, newPrice) },
                        onDelete = { viewModel.deleteRoom(room) }
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }

    if (showAddDialog) {
        AddRoomDialog(
            isSaving = uiState.isSaving,
            onDismiss = { showAddDialog = false },
            onConfirm = { name, type, price, capacity ->
                viewModel.addRoom(name, type, price, capacity)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun RoomRow(room: RentalRoom, onStatusChange: (RoomStatus) -> Unit, onPriceChange: (Double) -> Unit, onDelete: () -> Unit) {
    var priceText by remember(room.id) { mutableStateOf(room.pricePerMonth.toString()) }
    var statusMenuOpen by remember { mutableStateOf(false) }

    Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(room.name + " · " + stringResource(R.string.room_type_capacity, room.roomType, room.capacity), style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = onDelete) { Text(stringResource(R.string.delete_button)) }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    label = { Text(stringResource(R.string.price_month_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = { priceText.toDoubleOrNull()?.let(onPriceChange) }) { Text(stringResource(R.string.save_button)) }
            }
            Box {
                TextButton(onClick = { statusMenuOpen = true }) { Text(stringResource(R.string.status_label, room.status.label())) }
                DropdownMenu(expanded = statusMenuOpen, onDismissRequest = { statusMenuOpen = false }) {
                    RoomStatus.entries.forEach { status ->
                        DropdownMenuItem(text = { Text(status.label()) }, onClick = { onStatusChange(status); statusMenuOpen = false })
                    }
                }
            }
        }
    }
}

@Composable
private fun AddRoomDialog(isSaving: Boolean, onDismiss: () -> Unit, onConfirm: (String, String, Double, Int) -> Unit) {
    var name by remember { mutableStateOf("") }
    var typeGuide by remember { mutableStateOf(RoomTypeGuide.SINGLE) }
    var price by remember { mutableStateOf("") }
    var capacity by remember { mutableStateOf("1") }
    val priceValue = price.toDoubleOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_room_title)) },
        text = {
            Column {
                OutlinedTextField(name, { name = it }, label = { Text(stringResource(R.string.room_name_label)) })
                Spacer(Modifier.height(8.dp))
                DropdownField(
                    label = stringResource(R.string.room_type_label),
                    options = RoomTypeGuide.entries.map { it.dbValue to stringResource(it.labelRes()) },
                    selectedKey = typeGuide.dbValue,
                    onSelected = { key ->
                        RoomTypeGuide.fromDb(key)?.let {
                            typeGuide = it
                            capacity = it.defaultCapacity.toString()
                        }
                    }
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    price, { price = it },
                    label = { Text(stringResource(R.string.price_month_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = priceValue != null && typeGuide.looksUnrealistic(priceValue),
                    supportingText = {
                        Text(
                            if (priceValue != null && typeGuide.looksUnrealistic(priceValue)) stringResource(R.string.room_price_unusual, typeGuide.typicalMin, typeGuide.typicalMax)
                            else stringResource(R.string.room_price_typical, typeGuide.typicalMin, typeGuide.typicalMax)
                        )
                    }
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(capacity, { capacity = it }, label = { Text(stringResource(R.string.capacity_label)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            }
        },
        confirmButton = {
            Button(
                enabled = !isSaving && name.isNotBlank() && price.toDoubleOrNull() != null,
                onClick = { onConfirm(name, typeGuide.dbValue, price.toDoubleOrNull() ?: 0.0, capacity.toIntOrNull() ?: 1) }
            ) {
                if (isSaving) CircularProgressIndicator(modifier = Modifier.height(18.dp)) else Text(stringResource(R.string.add_button))
            }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text(stringResource(R.string.cancel_button)) } }
    )
}

@androidx.annotation.StringRes
private fun RoomTypeGuide.labelRes(): Int = when (this) {
    RoomTypeGuide.SINGLE -> R.string.room_type_single
    RoomTypeGuide.SHARED -> R.string.room_type_shared
    RoomTypeGuide.ENSUITE -> R.string.room_type_ensuite
    RoomTypeGuide.STUDIO -> R.string.room_type_studio
}
