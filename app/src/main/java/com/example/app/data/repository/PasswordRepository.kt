package com.example.app.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.app.data.model.AppSettings
import com.example.app.data.model.PasswordOptions
import com.example.app.data.model.SavedPassword
import com.example.app.data.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class PasswordRepository(private val context: Context) {

    private object Keys {
        val PASSWORD_LENGTH = intPreferencesKey("password_length")
        val INCLUDE_UPPERCASE = booleanPreferencesKey("include_uppercase")
        val INCLUDE_LOWERCASE = booleanPreferencesKey("include_lowercase")
        val INCLUDE_NUMBERS = booleanPreferencesKey("include_numbers")
        val INCLUDE_SYMBOLS = booleanPreferencesKey("include_symbols")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val IS_PREMIUM = booleanPreferencesKey("is_premium")
        val SAVED_PASSWORDS = stringPreferencesKey("saved_passwords")
    }

    val passwordOptions: Flow<PasswordOptions> = context.dataStore.data.map { prefs ->
        PasswordOptions(
            length = prefs[Keys.PASSWORD_LENGTH] ?: 16,
            includeUppercase = prefs[Keys.INCLUDE_UPPERCASE] ?: true,
            includeLowercase = prefs[Keys.INCLUDE_LOWERCASE] ?: true,
            includeNumbers = prefs[Keys.INCLUDE_NUMBERS] ?: true,
            includeSymbols = prefs[Keys.INCLUDE_SYMBOLS] ?: true
        )
    }

    val appSettings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            themeMode = prefs[Keys.THEME_MODE]?.let { ThemeMode.valueOf(it) } ?: ThemeMode.SYSTEM,
            isPremium = prefs[Keys.IS_PREMIUM] ?: false
        )
    }

    val savedPasswords: Flow<List<SavedPassword>> = context.dataStore.data.map { prefs ->
        val json = prefs[Keys.SAVED_PASSWORDS] ?: "[]"
        try {
            val array = JSONArray(json)
            (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                SavedPassword(
                    id = obj.getLong("id"),
                    password = obj.getString("password"),
                    createdAt = obj.getLong("createdAt")
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun savePasswordOptions(options: PasswordOptions) {
        context.dataStore.edit { prefs ->
            prefs[Keys.PASSWORD_LENGTH] = options.length
            prefs[Keys.INCLUDE_UPPERCASE] = options.includeUppercase
            prefs[Keys.INCLUDE_LOWERCASE] = options.includeLowercase
            prefs[Keys.INCLUDE_NUMBERS] = options.includeNumbers
            prefs[Keys.INCLUDE_SYMBOLS] = options.includeSymbols
        }
    }

    suspend fun saveThemeMode(themeMode: ThemeMode) {
        context.dataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = themeMode.name
        }
    }

    suspend fun setPremium(isPremium: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.IS_PREMIUM] = isPremium
        }
    }

    suspend fun savePassword(password: String) {
        context.dataStore.edit { prefs ->
            val currentJson = prefs[Keys.SAVED_PASSWORDS] ?: "[]"
            val array = try {
                JSONArray(currentJson)
            } catch (e: Exception) {
                JSONArray()
            }
            val newPassword = JSONObject().apply {
                put("id", System.currentTimeMillis())
                put("password", password)
                put("createdAt", System.currentTimeMillis())
            }
            array.put(newPassword)

            val passwords = (0 until array.length()).map { i ->
                array.getJSONObject(i)
            }.sortedByDescending { it.getLong("createdAt") }.take(50)

            val newArray = JSONArray()
            passwords.forEach { newArray.put(it) }

            prefs[Keys.SAVED_PASSWORDS] = newArray.toString()
        }
    }

    suspend fun deletePassword(id: Long) {
        context.dataStore.edit { prefs ->
            val currentJson = prefs[Keys.SAVED_PASSWORDS] ?: "[]"
            val array = try {
                JSONArray(currentJson)
            } catch (e: Exception) {
                JSONArray()
            }
            val newArray = JSONArray()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                if (obj.getLong("id") != id) {
                    newArray.put(obj)
                }
            }
            prefs[Keys.SAVED_PASSWORDS] = newArray.toString()
        }
    }
}