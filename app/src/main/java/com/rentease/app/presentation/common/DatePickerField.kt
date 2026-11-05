package com.rentease.app.presentation.common

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.rentease.app.R
import java.time.Instant
import java.time.ZoneOffset

/**
 * Read-only text field that opens the platform date picker. Value is ISO `yyyy-MM-dd`
 * (what Postgres `date` columns and the booking API expect). [minDateMillis] (UTC
 * midnight millis) disables earlier days, e.g. "today" for move-in dates.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    minDateMillis: Long? = null
) {
    var open by remember { mutableStateOf(false) }
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    LaunchedEffect(pressed) { if (pressed) open = true }

    OutlinedTextField(
        value = value,
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        trailingIcon = { Icon(Icons.Filled.DateRange, contentDescription = stringResource(R.string.pick_date_description)) },
        interactionSource = interaction,
        modifier = modifier.fillMaxWidth()
    )

    if (open) {
        val initial = runCatching {
            java.time.LocalDate.parse(value).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        }.getOrNull()
        val state = rememberDatePickerState(
            initialSelectedDateMillis = initial,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = minDateMillis == null || utcTimeMillis >= minDateMillis
            }
        )
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        onValueChange(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toString())
                    }
                    open = false
                }) { Text(stringResource(R.string.ok_button)) }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text(stringResource(R.string.cancel_button)) } }
        ) { DatePicker(state = state) }
    }
}
