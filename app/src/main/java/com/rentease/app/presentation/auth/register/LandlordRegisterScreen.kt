package com.rentease.app.presentation.auth.register

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rentease.app.R
import com.rentease.app.domain.model.AuthUser
import com.rentease.app.presentation.auth.AuthUiState

/**
 * Landlord sign-up, like the website: name, email, phone and password only. Business details and
 * verification documents are collected afterwards from the landlord dashboard, because the private
 * storage buckets only accept uploads from a signed-in user. Deliberately a SEPARATE route from the
 * student form.
 */
@Composable
fun LandlordRegisterScreen(
    onRegistered: (AuthUser) -> Unit,
    onGoToLogin: () -> Unit,
    viewModel: RegisterViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val confirmEmail by viewModel.confirmEmail.collectAsState()

    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    LaunchedEffect(uiState) {
        val state = uiState
        if (state is AuthUiState.Success) onRegistered(state.user)
    }

    val awaitingConfirmation = confirmEmail
    if (awaitingConfirmation != null) {
        Scaffold { padding -> Column(Modifier.padding(padding)) { ConfirmEmailPanel(awaitingConfirmation, onGoToLogin) } }
        return
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(stringResource(R.string.landlord_register_title), style = MaterialTheme.typography.headlineMedium)
            Text(
                stringResource(R.string.landlord_register_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(fullName, { fullName = it }, label = { Text(stringResource(R.string.full_name_label)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(email, { email = it }, label = { Text(stringResource(R.string.email_label)) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth())
            OutlinedTextField(phone, { phone = it }, label = { Text(stringResource(R.string.phone_label)) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth())
            PasswordField(password, { password = it })

            Text(
                stringResource(R.string.landlord_register_documents_later),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (uiState is AuthUiState.Error) {
                Text((uiState as AuthUiState.Error).message, color = MaterialTheme.colorScheme.error)
            }

            Button(
                onClick = { viewModel.registerLandlord(fullName, email, password, phone) },
                enabled = uiState !is AuthUiState.Loading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState is AuthUiState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.height(20.dp), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(R.string.create_account_button))
                }
            }
        }
    }
}
