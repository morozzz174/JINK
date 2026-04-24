package com.example.app.data.model

data class SavedPassword(
    val id: Long = System.currentTimeMillis(),
    val password: String,
    val createdAt: Long = System.currentTimeMillis()
)

data class PasswordOptions(
    val length: Int = 16,
    val includeUppercase: Boolean = true,
    val includeLowercase: Boolean = true,
    val includeNumbers: Boolean = true,
    val includeSymbols: Boolean = true
)

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val isPremium: Boolean = false
)