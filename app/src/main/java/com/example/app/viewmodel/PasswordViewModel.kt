package com.example.app.viewmodel

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.app.billing.BillingManager
import com.example.app.data.model.AppSettings
import com.example.app.data.model.PasswordOptions
import com.example.app.data.model.SavedPassword
import com.example.app.data.model.ThemeMode
import com.example.app.data.repository.PasswordRepository
import com.example.app.util.PasswordGenerator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PasswordViewModel(
    private val repository: PasswordRepository,
    private val billingManager: BillingManager
) : ViewModel() {

    val passwordOptions: StateFlow<PasswordOptions> = repository.passwordOptions
        .stateIn(viewModelScope, SharingStarted.Eagerly, PasswordOptions())

    val appSettings: StateFlow<AppSettings> = repository.appSettings
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    val savedPasswords: StateFlow<List<SavedPassword>> = repository.savedPasswords
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _currentPassword = MutableStateFlow("")
    val currentPassword: StateFlow<String> = _currentPassword.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    init {
        billingManager.connect()
    }

    fun generatePassword() {
        val options = passwordOptions.value
        _currentPassword.value = PasswordGenerator.generate(options)
    }

    fun updateLength(length: Int) {
        viewModelScope.launch {
            repository.savePasswordOptions(passwordOptions.value.copy(length = length))
        }
    }

    fun updateUppercase(include: Boolean) {
        viewModelScope.launch {
            repository.savePasswordOptions(passwordOptions.value.copy(includeUppercase = include))
        }
    }

    fun updateLowercase(include: Boolean) {
        viewModelScope.launch {
            repository.savePasswordOptions(passwordOptions.value.copy(includeLowercase = include))
        }
    }

    fun updateNumbers(include: Boolean) {
        viewModelScope.launch {
            repository.savePasswordOptions(passwordOptions.value.copy(includeNumbers = include))
        }
    }

    fun updateSymbols(include: Boolean) {
        viewModelScope.launch {
            repository.savePasswordOptions(passwordOptions.value.copy(includeSymbols = include))
        }
    }

    fun updateThemeMode(themeMode: ThemeMode) {
        viewModelScope.launch {
            repository.saveThemeMode(themeMode)
        }
    }

    fun setPremium(isPremium: Boolean) {
        viewModelScope.launch {
            repository.setPremium(isPremium)
        }
    }

    fun saveCurrentPassword() {
        val password = _currentPassword.value
        if (password.isNotEmpty()) {
            viewModelScope.launch {
                repository.savePassword(password)
                showToast("saved")
            }
        }
    }

    fun deletePassword(id: Long) {
        viewModelScope.launch {
            repository.deletePassword(id)
        }
    }

    fun showToast(message: String) {
        _toastMessage.value = message
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun purchasePremium(activity: Activity) {
        billingManager.purchase(activity)
    }

    fun restorePurchases() {
        billingManager.queryPurchases()
    }

    class Factory(
        private val repository: PasswordRepository,
        private val billingManager: BillingManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PasswordViewModel(repository, billingManager) as T
        }
    }
}