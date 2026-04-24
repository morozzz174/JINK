package com.example.app.data.model

data class UserData(
    val uid: String = "",
    val email: String = "",
    val phone: String = "",
    val hasConsented: Boolean = false,
    val consentTimestamp: Long = 0,
    val createdAt: Long = 0
)

sealed class AuthState {
    data object Idle : AuthState()
    data object Loading : AuthState()
    data object NeedsConsent : AuthState()
    data class Authenticated(val user: UserData) : AuthState()
    data class Error(val message: String) : AuthState()
    data object CodeSent : AuthState()
    data object CodeVerified : AuthState()
}