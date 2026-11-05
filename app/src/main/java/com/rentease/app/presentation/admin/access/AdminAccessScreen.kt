package com.rentease.app.presentation.admin.access

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rentease.app.R

@Composable
fun AdminAccessScreen(
    onAccessGranted: () -> Unit,
    viewModel: AdminAccessViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isAuthenticated by viewModel.isAuthenticated.collectAsState()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }

    LaunchedEffect(uiState) {
        if (uiState is AdminAccessUiState.Granted) onAccessGranted()
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(stringResource(R.string.admin_access_title), style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(8.dp))

            if (!isAuthenticated) {
                Text(
                    stringResource(R.string.admin_access_signin_prompt),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(20.dp))
                OutlinedTextField(
                    value = email, onValueChange = { email = it },
                    label = { Text(stringResource(R.string.email_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = password, onValueChange = { password = it },
                    label = { Text(stringResource(R.string.password_label)) },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )

                if (uiState is AdminAccessUiState.Error) {
                    Spacer(Modifier.height(8.dp))
                    Text((uiState as AdminAccessUiState.Error).message, color = MaterialTheme.colorScheme.error)
                }

                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = { viewModel.signIn(email, password) },
                    enabled = uiState !is AdminAccessUiState.Submitting,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (uiState is AdminAccessUiState.Submitting) {
                        CircularProgressIndicator(modifier = Modifier.height(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text(stringResource(R.string.sign_in_button))
                    }
                }
            } else {
                Text(
                    stringResource(R.string.admin_access_code_prompt),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(20.dp))

                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text(stringResource(R.string.access_code_label)) },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )

                if (uiState is AdminAccessUiState.Error) {
                    Spacer(Modifier.height(8.dp))
                    Text((uiState as AdminAccessUiState.Error).message, color = MaterialTheme.colorScheme.error)
                }

                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = { viewModel.submitCode(code) },
                    enabled = uiState !is AdminAccessUiState.Submitting,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (uiState is AdminAccessUiState.Submitting) {
                        CircularProgressIndicator(modifier = Modifier.height(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text(stringResource(R.string.verify_code_button))
                    }
                }
            }
        }
    }
}
