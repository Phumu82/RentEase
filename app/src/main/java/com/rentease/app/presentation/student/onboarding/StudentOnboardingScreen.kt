package com.rentease.app.presentation.student.onboarding

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rentease.app.R
import com.rentease.app.domain.model.OnboardingField
import com.rentease.app.domain.model.RoomTypeCatalog
import com.rentease.app.domain.model.StudentOnboardingInput
import com.rentease.app.presentation.common.DatePickerField
import com.rentease.app.presentation.common.DropdownField
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Authenticated student onboarding (web `student.onboarding.tsx`): contact, academic
 * and accommodation-preference sections plus a proof-of-registration upload. Pre-fills
 * from the stored profile, so an unfinished onboarding is resumed where it was left.
 */
@Composable
fun StudentOnboardingScreen(
    onDone: () -> Unit,
    onBack: () -> Unit,
    viewModel: StudentOnboardingViewModel = hiltViewModel()
) {
    val ui by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(ui.saved) { if (ui.saved) onDone() }
    LaunchedEffect(ui.documentUrlToOpen) {
        ui.documentUrlToOpen?.let {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            viewModel.documentOpened()
        }
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) viewModel.uploadProof(uri)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.onboarding_student_title)) },
                navigationIcon = { TextButton(onClick = onBack) { Text(stringResource(R.string.back_button)) } }
            )
        }
    ) { padding ->
        val initial = ui.initial
        if (ui.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        if (initial == null) {
            Column(Modifier.fillMaxSize().padding(padding).padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(ui.error ?: stringResource(R.string.onboarding_error_load), color = MaterialTheme.colorScheme.error)
                Button(onClick = viewModel::load) { Text(stringResource(R.string.retry_button)) }
            }
            return@Scaffold
        }

        var fullName by remember { mutableStateOf(initial.fullName) }
        var phone by remember { mutableStateOf(initial.phone) }
        var studentNumber by remember { mutableStateOf(initial.studentNumber) }
        var universityId by remember { mutableStateOf(initial.universityId) }
        var course by remember { mutableStateOf(initial.course) }
        var year by remember { mutableStateOf(initial.yearOfStudy) }
        var budget by remember { mutableStateOf(initial.budgetMax?.toInt()?.toString().orEmpty()) }
        var roomType by remember { mutableStateOf(initial.preferredRoomType) }
        var moveIn by remember { mutableStateOf(initial.earliestMoveIn.orEmpty()) }
        var furnished by remember { mutableStateOf(initial.needsFurnished) }
        val todayMillis = remember { LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() }
        val requiredMsg = stringResource(R.string.onboarding_field_required)
        fun err(f: OnboardingField): String? = if (f in ui.invalidFields) requiredMsg else null

        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(stringResource(R.string.onboarding_student_subtitle), style = MaterialTheme.typography.bodyMedium)

            Text(stringResource(R.string.onboarding_section_contact), style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(fullName, { fullName = it }, label = { Text(stringResource(R.string.full_name_label)) },
                isError = err(OnboardingField.FULL_NAME) != null, supportingText = err(OnboardingField.FULL_NAME)?.let { { Text(it) } }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(phone, { phone = it }, label = { Text(stringResource(R.string.phone_label)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth())

            Text(stringResource(R.string.onboarding_section_academic), style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(studentNumber, { studentNumber = it }, label = { Text(stringResource(R.string.student_number_label)) },
                isError = err(OnboardingField.STUDENT_NUMBER) != null, supportingText = err(OnboardingField.STUDENT_NUMBER)?.let { { Text(it) } }, modifier = Modifier.fillMaxWidth())
            DropdownField(
                label = stringResource(R.string.university_label),
                options = ui.universities.map { it.id to "${it.name} — ${it.city}" },
                selectedKey = universityId,
                onSelected = { universityId = it },
                isError = err(OnboardingField.UNIVERSITY) != null
            )
            OutlinedTextField(course, { course = it }, label = { Text(stringResource(R.string.course_label)) },
                isError = err(OnboardingField.COURSE) != null, supportingText = err(OnboardingField.COURSE)?.let { { Text(it) } }, modifier = Modifier.fillMaxWidth())
            DropdownField(
                label = stringResource(R.string.year_of_study_label),
                options = (1..6).map { it.toString() to stringResource(R.string.year_of_study_value, it) },
                selectedKey = year.toString(),
                onSelected = { year = it.toInt() }
            )

            Text(stringResource(R.string.onboarding_section_preferences), style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(budget, { budget = it.filter { c -> c.isDigit() } }, label = { Text(stringResource(R.string.budget_max_label)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = err(OnboardingField.BUDGET) != null, supportingText = err(OnboardingField.BUDGET)?.let { { Text(it) } }, modifier = Modifier.fillMaxWidth())
            DropdownField(
                label = stringResource(R.string.preferred_room_type_label),
                options = RoomTypeCatalog.ALL.map { it to it },
                selectedKey = roomType,
                onSelected = { roomType = it }
            )
            DatePickerField(value = moveIn, onValueChange = { moveIn = it }, label = stringResource(R.string.earliest_move_in_label), minDateMillis = todayMillis)
            err(OnboardingField.EARLIEST_MOVE_IN)?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = furnished, onCheckedChange = { furnished = it })
                Text(stringResource(R.string.needs_furnished_label))
            }

            Text(stringResource(R.string.onboarding_proof_title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.onboarding_proof_body), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (ui.proofPath != null) {
                TextButton(onClick = viewModel::openProof) { Text(stringResource(R.string.onboarding_view_document)) }
            }
            OutlinedButton(
                onClick = { picker.launch("*/*") },
                enabled = !ui.isUploading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (ui.isUploading) CircularProgressIndicator(Modifier.height(18.dp), strokeWidth = 2.dp)
                else Text(stringResource(if (ui.proofPath != null) R.string.onboarding_replace_document else R.string.onboarding_upload_document))
            }
            Text(stringResource(R.string.document_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            ui.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            Button(
                onClick = {
                    viewModel.save(
                        StudentOnboardingInput(
                            fullName = fullName, phone = phone, studentNumber = studentNumber,
                            universityId = universityId, course = course, yearOfStudy = year,
                            budgetMax = budget.toDoubleOrNull(), preferredRoomType = roomType,
                            earliestMoveIn = moveIn.ifBlank { null }, needsFurnished = furnished
                        )
                    )
                },
                enabled = !ui.isSaving,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (ui.isSaving) CircularProgressIndicator(Modifier.height(20.dp), strokeWidth = 2.dp)
                else Text(stringResource(R.string.onboarding_save_continue))
            }
        }
    }
}
