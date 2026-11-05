package com.rentease.app.presentation.auth.reset

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rentease.app.R
import com.rentease.app.domain.model.PasswordReset

@Composable
fun ForgotPasswordScreen(
    onBack: () -> Unit,
    viewModel: ForgotPasswordViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var email by remember { mutableStateOf("") }

    Scaffold { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(stringResource(R.string.forgot_title), style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.forgot_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(20.dp))

            if (state.sent) {
                Text(stringResource(R.string.forgot_sent_message), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
            } else {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; viewModel.clearMessages() },
                    label = { Text(stringResource(R.string.email_label)) },
                    singleLine = true,
                    isError = state.invalidEmail,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth()
                )
                if (state.invalidEmail) {
                    Text(stringResource(R.string.forgot_invalid_email), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                state.error?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(Modifier.height(20.dp))
                Button(onClick = { viewModel.send(email) }, enabled = !state.isSending, modifier = Modifier.fillMaxWidth()) {
                    if (state.isSending) CircularProgressIndicator(modifier = Modifier.height(20.dp), strokeWidth = 2.dp)
                    else Text(stringResource(R.string.forgot_send_button))
                }
            }

            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.back_to_login)) }
        }
    }
}

@Composable
fun ResetPasswordScreen(
    onDone: () -> Unit,
    viewModel: ResetPasswordViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }

    Scaffold { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(stringResource(R.string.reset_title), style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(8.dp))

            when {
                viewModel.token.isBlank() -> {
                    Text(stringResource(R.string.reset_invalid_link), color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.back_to_login)) }
                }
                state.done -> {
                    Text(stringResource(R.string.reset_success), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.sign_in_button)) }
                }
                else -> {
                    Text(
                        stringResource(R.string.reset_intro, PasswordReset.MIN_LENGTH),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(20.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; viewModel.clearMessages() },
                        label = { Text(stringResource(R.string.new_password_label)) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        isError = state.problem == PasswordReset.Problem.TOO_SHORT,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = confirm,
                        onValueChange = { confirm = it; viewModel.clearMessages() },
                        label = { Text(stringResource(R.string.confirm_password_label)) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        isError = state.problem == PasswordReset.Problem.MISMATCH,
                        modifier = Modifier.fillMaxWidth()
                    )
                    state.problem?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            when (it) {
                                PasswordReset.Problem.TOO_SHORT -> stringResource(R.string.reset_too_short, PasswordReset.MIN_LENGTH)
                                PasswordReset.Problem.MISMATCH -> stringResource(R.string.reset_mismatch)
                            },
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    state.error?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(Modifier.height(20.dp))
                    Button(onClick = { viewModel.submit(password, confirm) }, enabled = !state.isSaving, modifier = Modifier.fillMaxWidth()) {
                        if (state.isSaving) CircularProgressIndicator(modifier = Modifier.height(20.dp), strokeWidth = 2.dp)
                        else Text(stringResource(R.string.reset_button))
                    }
                }
            }
        }
    }
}
