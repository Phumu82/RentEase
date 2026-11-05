package com.rentease.app.presentation.student.payments

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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rentease.app.R
import com.rentease.app.domain.model.Payment
import com.rentease.app.domain.model.PaymentStatus
import com.rentease.app.presentation.common.label

@Composable
fun PaymentsScreen(
    onBack: () -> Unit,
    viewModel: PaymentsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.payments_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = null) }
                }
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            else -> LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                item {
                    Text(stringResource(R.string.payments_description), style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(12.dp))
                    uiState.error?.let { Text(it, color = MaterialTheme.colorScheme.error); Spacer(Modifier.height(8.dp)) }

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatCard(
                            stringResource(R.string.payments_stat_paid),
                            "R${uiState.totalPaid.toInt()}",
                            Modifier.weight(1f)
                        )
                        StatCard(
                            stringResource(R.string.payments_stat_outstanding),
                            "R${uiState.totalOutstanding.toInt()}",
                            Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                }
                if (uiState.payments.isEmpty()) {
                    item { Text(stringResource(R.string.no_payments_yet)) }
                } else {
                    items(uiState.payments, key = { it.id }) { payment ->
                        PaymentRow(
                            payment = payment,
                            isPaying = uiState.payingId == payment.id,
                            onPayNow = { viewModel.markPaid(payment) }
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.padding(12.dp)) {
            Text(value, style = MaterialTheme.typography.headlineMedium)
            Text(label, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun PaymentRow(
    payment: Payment,
    isPaying: Boolean,
    onPayNow: () -> Unit
) {
    Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            val amountLabel = "R${payment.amount.toInt()}"
            Row(Modifier.fillMaxWidth()) {
                Text(payment.propertyTitle, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text(amountLabel, style = MaterialTheme.typography.titleMedium)
            }
            Text(
                "${payment.kind.label()} \u00b7 ${payment.reference}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(payment.status.label(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            when {
                payment.paidAt != null -> Text(
                    stringResource(R.string.payment_paid_on_label, payment.paidAt),
                    style = MaterialTheme.typography.bodyMedium
                )
                payment.dueDate != null -> Text(
                    stringResource(R.string.payment_due_label, payment.dueDate),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (payment.status == PaymentStatus.PENDING) {
                Row(Modifier.fillMaxWidth()) {
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onPayNow, enabled = !isPaying) {
                        Text(stringResource(if (isPaying) R.string.paying_button else R.string.pay_now_button))
                    }
                }
            }
        }
    }
}
