package com.example.app.auth

import android.app.Activity
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.userProfileChangeRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

private val Context.authDataStore: DataStore<Preferences> by preferencesDataStore(name = "auth")

class AuthManager(private val context: Context) {

    private val auth = FirebaseAuth.getInstance()

    private object Keys {
        val USER_UID = stringPreferencesKey("user_uid")
        val USER_EMAIL = stringPreferencesKey("user_email")
        val USER_PHONE = stringPreferencesKey("user_phone")
        val HAS_CONSENTED = booleanPreferencesKey("has_consented")
        val CONSENT_TIMESTAMP = longPreferencesKey("consent_timestamp")
        val USER_CREATED_AT = longPreferencesKey("user_created_at")
    }

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private var verificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null

    init {
        checkAuthState()
    }

    private fun checkAuthState() {
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                loadUserData(user)
            } else {
                _authState.value = AuthState.Idle
            }
        }
    }

    private suspend fun loadUserData(user: FirebaseUser) {
        val userData = UserData(
            uid = user.uid,
            email = user.email ?: "",
            phone = user.phoneNumber ?: "",
            hasConsented = context.authDataStore.data.first()[Keys.HAS_CONSENTED] ?: false,
            consentTimestamp = context.authDataStore.data.first()[Keys.CONSENT_TIMESTAMP] ?: 0,
            createdAt = context.authDataStore.data.first()[Keys.USER_CREATED_AT] ?: System.currentTimeMillis()
        )
        _authState.value = AuthState.Authenticated(userData)
    }

    fun signInWithEmail(email: String, password: String) {
        _authState.value = AuthState.Loading
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                result.user?.let { loadUserData(it) }
            }
            .addOnFailureListener { e ->
                _authState.value = AuthState.Error(e.message ?: "Auth error")
            }
    }

    fun signUpWithEmail(email: String, password: String) {
        _authState.value = AuthState.Loading
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                result.user?.let { user ->
                    saveUserLocally(user.uid, email = email)
                    loadUserData(user)
                }
            }
            .addOnFailureListener { e ->
                _authState.value = AuthState.Error(e.message ?: "Registration error")
            }
    }

    fun signInWithPhone(activity: Activity) {
        val phone = _pendingPhone
        if (phone.isNullOrBlank()) {
            _authState.value = AuthState.Error("Phone number not set")
            return
        }

        _authState.value = AuthState.Loading
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phone)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    signInWithCredential(credential)
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    _authState.value = AuthState.Error(e.message ?: "Verification failed")
                }

                override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                    this@AuthManager.verificationId = verificationId
                    this@AuthManager.resendToken = token
                    _authState.value = AuthState.CodeSent
                }
            })
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private var _pendingPhone: String? = null

    fun setPendingPhone(phone: String) {
        _pendingPhone = phone
    }

    fun verifyPhoneCode(code: String) {
        val verificationId = verificationId
        if (verificationId == null) {
            _authState.value = AuthState.Error("Verification ID not found")
            return
        }

        _authState.value = AuthState.Loading
        val credential = PhoneAuthProvider.getCredential(verificationId, code)
        signInWithCredential(credential)
    }

    private fun signInWithCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnSuccessListener { result ->
                result.user?.let { user ->
                    saveUserLocally(user.uid, phone = user.phoneNumber ?: "")
                    loadUserData(user)
                }
            }
            .addOnFailureListener { e ->
                _authState.value = AuthState.Error(e.message ?: "Sign in failed")
            }
    }

    fun resendVerificationCode(activity: Activity) {
        val phone = _pendingPhone
        val token = resendToken
        if (phone.isNullOrBlank() || token == null) {
            _authState.value = AuthState.Error("Cannot resend code")
            return
        }

        _authState.value = AuthState.Loading
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phone)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setForceResendingToken(token)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    signInWithCredential(credential)
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    _authState.value = AuthState.Error(e.message ?: "Verification failed")
                }

                override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                    this@AuthManager.verificationId = verificationId
                    this@AuthManager.resendToken = token
                    _authState.value = AuthState.CodeSent
                }
            })
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private suspend fun saveUserLocally(uid: String, email: String = "", phone: String = "") {
        context.authDataStore.edit { prefs ->
            prefs[Keys.USER_UID] = uid
            prefs[Keys.USER_EMAIL] = email
            prefs[Keys.USER_PHONE] = phone
            prefs[Keys.HAS_CONSENTED] = false
            prefs[Keys.CONSENT_TIMESTAMP] = 0L
            prefs[Keys.USER_CREATED_AT] = System.currentTimeMillis()
        }
    }

    suspend fun saveConsent() {
        context.authDataStore.edit { prefs ->
            prefs[Keys.HAS_CONSENTED] = true
            prefs[Keys.CONSENT_TIMESTAMP] = System.currentTimeMillis()
        }
        val currentState = _authState.value
        if (currentState is AuthState.Authenticated) {
            _authState.value = AuthState.Authenticated(
                currentState.user.copy(hasConsented = true, consentTimestamp = System.currentTimeMillis())
            )
        }
    }

    suspend fun clearUserData() {
        context.authDataStore.edit { prefs ->
            prefs.clear()
        }
        auth.signOut()
        _authState.value = AuthState.Idle
    }

    fun signOut() {
        auth.signOut()
        _authState.value = AuthState.Idle
    }
}