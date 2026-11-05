package com.rentease.app.presentation.landlord.onboarding

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
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
import com.rentease.app.domain.model.LandlordDocumentKind
import com.rentease.app.domain.model.LandlordOnboardingInput
import com.rentease.app.domain.model.LocationCatalog
import com.rentease.app.domain.model.OnboardingField
import com.rentease.app.domain.model.REQUIRED_DOCUMENT_COUNT
import com.rentease.app.domain.model.VerificationStatus
import com.rentease.app.domain.model.requiredDocumentsUploaded
import com.rentease.app.presentation.common.DropdownField

/**
 * Landlord onboarding / verification (web `landlord.onboarding.tsx`): contact, identity
 * and business details, a live verification status with admin notes, and four document
 * slots (ID + proof of ownership required; proof of address + company registration optional).
 */
@Composable
fun LandlordOnboardingScreen(
    onDone: () -> Unit,
    onBack: () -> Unit,
    viewModel: LandlordOnboardingViewModel = hiltViewModel()
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
    var pendingKind by remember { mutableStateOf<LandlordDocumentKind?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        val kind = pendingKind
        if (uri != null && kind != null) viewModel.upload(kind, uri)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.onboarding_landlord_title)) },
                navigationIcon = { TextButton(onClick = onBack) { Text(stringResource(R.string.back_button)) } }
            )
        }
    ) { padding ->
        val initial = ui.initial
        val current = ui.current
        if (ui.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        if (initial == null || current == null) {
            Column(Modifier.fillMaxSize().padding(padding).padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(ui.error ?: stringResource(R.string.onboarding_error_load), color = MaterialTheme.colorScheme.error)
                Button(onClick = viewModel::load) { Text(stringResource(R.string.retry_button)) }
            }
            return@Scaffold
        }

        var displayName by remember { mutableStateOf(initial.displayName) }
        var contactPhone by remember { mutableStateOf(initial.contactPhone) }
        var legalName by remember { mutableStateOf(initial.legalName) }
        var idNumber by remember { mutableStateOf(initial.idNumber) }
        var businessName by remember { mutableStateOf(initial.businessName) }
        var regNumber by remember { mutableStateOf(initial.businessRegistrationNumber) }
        var address by remember { mutableStateOf(initial.businessAddress) }
        var city by remember { mutableStateOf(initial.city) }
        var province by remember { mutableStateOf(initial.province) }
        var postalCode by remember { mutableStateOf(initial.postalCode) }
        val requiredMsg = stringResource(R.string.onboarding_field_required)
        fun err(f: OnboardingField): String? = if (f in ui.invalidFields) requiredMsg else null

        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            VerificationStatusCard(current.verificationStatus, current.isVerified, current.verificationNotes, current.requiredDocumentsUploaded())

            Text(stringResource(R.string.onboarding_section_contact), style = MaterialTheme.typography.titleMedium)
            Field(displayName, { displayName = it }, R.string.onboarding_display_name_label, err(OnboardingField.FULL_NAME))
            Field(contactPhone, { contactPhone = it }, R.string.onboarding_contact_phone_label, err(OnboardingField.CONTACT_PHONE), KeyboardType.Phone)

            Text(stringResource(R.string.onboarding_section_identity), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.onboarding_identity_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Field(legalName, { legalName = it }, R.string.onboarding_legal_name_label, err(OnboardingField.LEGAL_NAME))
            Field(idNumber, { idNumber = it }, R.string.onboarding_id_number_label, err(OnboardingField.ID_NUMBER))

            Text(stringResource(R.string.onboarding_section_business), style = MaterialTheme.typography.titleMedium)
            Field(businessName, { businessName = it }, R.string.onboarding_company_name_label, null)
            Field(regNumber, { regNumber = it }, R.string.onboarding_registration_number_label, null)
            Field(address, { address = it }, R.string.onboarding_address_label, err(OnboardingField.BUSINESS_ADDRESS))
            Field(city, { city = it }, R.string.onboarding_city_label, err(OnboardingField.CITY))
            DropdownField(
                label = stringResource(R.string.onboarding_province_label),
                options = LocationCatalog.PROVINCES.map { it to it },
                selectedKey = province,
                onSelected = { province = it },
                isError = err(OnboardingField.PROVINCE) != null
            )
            Field(postalCode, { postalCode = it }, R.string.onboarding_postal_code_label, null, KeyboardType.Number)

            Text(stringResource(R.string.onboarding_documents_title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.onboarding_documents_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            LandlordDocumentKind.entries.forEach { kind ->
                DocumentSlot(
                    kind = kind,
                    hasFile = !current.documents[kind].isNullOrBlank(),
                    busy = ui.uploading == kind,
                    anyBusy = ui.uploading != null,
                    onPick = { pendingKind = kind; picker.launch("*/*") },
                    onView = { viewModel.open(kind) }
                )
            }

            ui.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            Button(
                onClick = {
                    viewModel.save(
                        LandlordOnboardingInput(
                            displayName = displayName, contactPhone = contactPhone, legalName = legalName,
                            idNumber = idNumber, businessName = businessName, businessRegistrationNumber = regNumber,
                            businessAddress = address, city = city, province = province, postalCode = postalCode
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

@Composable
private fun Field(value: String, onChange: (String) -> Unit, labelRes: Int, error: String?, keyboard: KeyboardType = KeyboardType.Text) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(stringResource(labelRes)) },
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun VerificationStatusCard(status: VerificationStatus, isVerified: Boolean, notes: String?, requiredUploaded: Int) {
    val effective = if (isVerified) VerificationStatus.VERIFIED else status
    val statusLabel = stringResource(
        when (effective) {
            VerificationStatus.PENDING -> R.string.verification_status_pending
            VerificationStatus.SUBMITTED -> R.string.verification_status_submitted
            VerificationStatus.VERIFIED -> R.string.verification_status_verified
            VerificationStatus.REJECTED -> R.string.verification_status_rejected
            VerificationStatus.DOCUMENTS_REQUIRED -> R.string.verification_status_documents_required
        }
    )
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(R.string.onboarding_verification_status_title), style = MaterialTheme.typography.titleMedium)
            Text(statusLabel, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            LinearProgressIndicator(progress = { requiredUploaded.toFloat() / REQUIRED_DOCUMENT_COUNT }, modifier = Modifier.fillMaxWidth())
            Text(stringResource(R.string.onboarding_required_docs_progress, requiredUploaded, REQUIRED_DOCUMENT_COUNT), style = MaterialTheme.typography.bodySmall)
            if (!notes.isNullOrBlank() && effective != VerificationStatus.VERIFIED) {
                Text(stringResource(R.string.onboarding_admin_notes_label), style = MaterialTheme.typography.labelLarge)
                Text(notes, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun DocumentSlot(
    kind: LandlordDocumentKind,
    hasFile: Boolean,
    busy: Boolean,
    anyBusy: Boolean,
    onPick: () -> Unit,
    onView: () -> Unit
) {
    val title = stringResource(
        when (kind) {
            LandlordDocumentKind.ID_DOCUMENT -> R.string.doc_id_document
            LandlordDocumentKind.PROOF_OF_OWNERSHIP -> R.string.doc_proof_of_ownership
            LandlordDocumentKind.PROOF_OF_ADDRESS -> R.string.doc_proof_of_address
            LandlordDocumentKind.BUSINESS_REGISTRATION -> R.string.doc_business_registration
        }
    )
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                if (kind.required) stringResource(R.string.doc_required_suffix, title) else stringResource(R.string.doc_optional_suffix, title),
                style = MaterialTheme.typography.titleSmall
            )
            if (hasFile) TextButton(onClick = onView) { Text(stringResource(R.string.onboarding_view_document)) }
            OutlinedButton(onClick = onPick, enabled = !anyBusy, modifier = Modifier.fillMaxWidth()) {
                if (busy) CircularProgressIndicator(Modifier.height(18.dp), strokeWidth = 2.dp)
                else Text(stringResource(if (hasFile) R.string.onboarding_replace_document else R.string.onboarding_upload_document))
            }
        }
    }
}
