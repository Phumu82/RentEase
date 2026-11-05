package com.rentease.app.presentation.session

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.rentease.app.presentation.common.RentEaseLogo
import androidx.hilt.navigation.compose.hiltViewModel
import com.rentease.app.domain.model.AuthUser

/** Start destination: tries to restore a stored session, then reports the result. */
@Composable
fun SplashScreen(
    onResolved: (AuthUser?) -> Unit,
    viewModel: SessionViewModel = hiltViewModel()
) {
    LaunchedEffect(Unit) { viewModel.restore(onResolved) }
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        RentEaseLogo()
        Spacer(Modifier.height(16.dp))
        Text("RentEase", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(24.dp))
        CircularProgressIndicator()
    }
}
