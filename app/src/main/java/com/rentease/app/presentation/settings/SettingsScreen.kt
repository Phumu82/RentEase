package com.rentease.app.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import com.rentease.app.data.local.ThemeMode
import com.rentease.app.domain.model.AppLanguage

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onLoggedOut: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var fullName by remember(uiState.fullName) { mutableStateOf(uiState.fullName) }
    var phone by remember(uiState.phone) { mutableStateOf(uiState.phone) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back_button))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp)
        ) {
            // ---- Appearance ----
            Text(stringResource(R.string.appearance_heading), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            SegmentedChoice(
                options = listOf(
                    ThemeMode.SYSTEM to stringResource(R.string.theme_system),
                    ThemeMode.LIGHT to stringResource(R.string.theme_light),
                    ThemeMode.DARK to stringResource(R.string.theme_dark)
                ),
                selected = uiState.themeMode,
                onSelect = { viewModel.setThemeMode(it) }
            )

            // ---- Language ----
            Spacer(Modifier.height(24.dp))
            Text(stringResource(R.string.language_heading), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            SegmentedChoice(
                options = listOf(
                    AppLanguage.ENGLISH to stringResource(R.string.language_english),
                    AppLanguage.ISIZULU to stringResource(R.string.language_zulu),
                    AppLanguage.TSHIVENDA to stringResource(R.string.language_venda)
                ),
                selected = uiState.currentLanguage,
                onSelect = { viewModel.selectLanguage(it) }
            )

            // ---- Notification preferences ----
            Spacer(Modifier.height(24.dp))
            Text(stringResource(R.string.notification_preferences_heading), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            NotificationToggleRow(
                label = stringResource(R.string.notify_booking_updates),
                checked = uiState.notificationPreferences.bookingUpdates,
                onCheckedChange = { viewModel.setBookingUpdatesEnabled(it) }
            )
            NotificationToggleRow(
                label = stringResource(R.string.notify_new_messages),
                checked = uiState.notificationPreferences.newMessages,
                onCheckedChange = { viewModel.setNewMessagesEnabled(it) }
            )
            NotificationToggleRow(
                label = stringResource(R.string.notify_maintenance_updates),
                checked = uiState.notificationPreferences.maintenanceUpdates,
                onCheckedChange = { viewModel.setMaintenanceUpdatesEnabled(it) }
            )

            if (uiState.isSaving) {
                Spacer(Modifier.height(8.dp))
                CircularProgressIndicator(modifier = Modifier.height(20.dp), strokeWidth = 2.dp)
            }
            uiState.error?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }

            // ---- Profile (editable) ----
            Spacer(Modifier.height(24.dp))
            Text(stringResource(R.string.profile_heading), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(stringResource(R.string.signed_in_as, uiState.email), style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it },
                label = { Text(stringResource(R.string.full_name_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text(stringResource(R.string.phone_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { viewModel.saveProfile(fullName, phone) },
                enabled = !uiState.isSaving,
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.save_button)) }
            if (uiState.profileSaved) {
                Text(stringResource(R.string.profile_saved_message), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
            }

            // ---- Logout ----
            Spacer(Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))
            OutlinedButton(
                onClick = { viewModel.logout(onLoggedOut) },
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.logout_button)) }
        }
    }
}

/** Pill-shaped single-choice switch (like the website's Light / Dark / System toggle). */
@Composable
private fun <T> SegmentedChoice(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(1.dp, MaterialTheme.colorScheme.outline, shape)
    ) {
        options.forEach { (value, label) ->
            val isSelected = value == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
                    .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onSelect(value) })
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun NotificationToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
