package com.rentease.app.presentation.student.payments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rentease.app.domain.model.Payment
import com.rentease.app.domain.model.PaymentStatus
import com.rentease.app.domain.repository.ListingsRepository
import com.rentease.app.domain.repository.ListingsResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PaymentsUiState(
    val isLoading: Boolean = true,
    val payments: List<Payment> = emptyList(),
    val error: String? = null,
    val payingId: String? = null
) {
    val totalPaid: Double get() = payments.filter { it.status == PaymentStatus.PAID }.sumOf { it.amount }
    val totalOutstanding: Double get() = payments.filter { it.status == PaymentStatus.PENDING }.sumOf { it.amount }
}

@HiltViewModel
class PaymentsViewModel @Inject constructor(
    private val repository: ListingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PaymentsUiState())
    val uiState: StateFlow<PaymentsUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val result = repository.getMyPayments()) {
                is ListingsResult.Success -> _uiState.value = _uiState.value.copy(isLoading = false, payments = result.data)
                is ListingsResult.Failure -> _uiState.value = _uiState.value.copy(isLoading = false, error = result.message)
            }
        }
    }

    /** Self-reported "I've paid this" — see [Payment]'s doc comment: there is
     *  no payment gateway behind this button, on either platform. */
    fun markPaid(payment: Payment) {
        _uiState.value = _uiState.value.copy(payingId = payment.id)
        viewModelScope.launch {
            val result = repository.markPaymentPaid(payment.id)
            _uiState.value = _uiState.value.copy(payingId = null)
            if (result is ListingsResult.Failure) {
                _uiState.value = _uiState.value.copy(error = result.message)
            } else {
                load()
            }
        }
    }
}
