package com.rentease.app.presentation.auth

import com.rentease.app.domain.model.AuthUser

sealed interface AuthUiState {
    data object Idle : AuthUiState
    data object Loading : AuthUiState
    data class Success(val user: AuthUser) : AuthUiState
    data class Error(val message: String) : AuthUiState
}
