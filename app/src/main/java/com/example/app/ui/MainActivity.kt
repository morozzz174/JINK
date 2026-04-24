package com.example.app.ui

import android.content.Intent
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.app.ads.YandexInterstitialAd
import com.example.app.auth.AuthManager
import com.example.app.auth.AuthState
import com.example.app.billing.BillingManager
import com.example.app.data.model.ThemeMode
import com.example.app.data.repository.PasswordRepository
import com.example.app.ui.screens.AuthScreen
import com.example.app.ui.screens.ConsentScreen
import com.example.app.ui.screens.LegalScreen
import com.example.app.ui.screens.MainScreen
import com.example.app.ui.screens.SettingsScreen
import com.example.app.ui.theme.SecurePasswordGeneratorTheme
import com.example.app.viewmodel.PasswordViewModel

class MainActivity : ComponentActivity() {

    private lateinit var repository: PasswordRepository
    private lateinit var billingManager: BillingManager
    private lateinit var interstitialAd: YandexInterstitialAd
    private lateinit var authManager: AuthManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        repository = PasswordRepository(applicationContext)
        billingManager = BillingManager(applicationContext)
        interstitialAd = YandexInterstitialAd(applicationContext)
        authManager = AuthManager(applicationContext)

        setContent {
            val viewModel: PasswordViewModel = viewModel(
                factory = PasswordViewModel.Factory(repository, billingManager, authManager)
            )
            val appSettings by viewModel.appSettings.collectAsState()
            val authState by authManager.authState.collectAsState()

            val isDarkTheme = when (appSettings.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            SecurePasswordGeneratorTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val context = LocalContext.current

                    var showInterstitial by remember { mutableStateOf(false) }

                    val showInterstitialAd: () -> Unit = {
                        if (!appSettings.isPremium) {
                            showInterstitial = true
                        }
                    }

                    LaunchedEffect(showInterstitial) {
                        if (showInterstitial) {
                            interstitialAd.load()
                            showInterstitial = false
                        }
                    }

                    NavHost(
                        navController = navController,
                        startDestination = when {
                            authState is AuthState.Idle -> "auth"
                            authState is AuthState.Authenticated && !(authState as AuthState.Authenticated).user.hasConsented -> "consent"
                            else -> "main"
                        }
                    ) {
                        composable("auth") {
                            AuthScreen(
                                authManager = authManager,
                                onAuthSuccess = {
                                    navController.navigate("consent") {
                                        popUpTo("auth") { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable("consent") {
                            ConsentScreen(
                                authManager = authManager,
                                viewModel = viewModel,
                                onConsentComplete = {
                                    navController.navigate("main") {
                                        popUpTo("consent") { inclusive = true }
                                    }
                                },
                                showInterstitialAd = showInterstitialAd
                            )
                        }
                        composable("main") {
                            MainScreen(
                                viewModel = viewModel,
                                onNavigateToSettings = {
                                    navController.navigate("settings")
                                },
                                onNavigateToLegal = { type ->
                                    navController.navigate("legal/$type")
                                },
                                showInterstitialAd = showInterstitialAd
                            )
                        }
                        composable("settings") {
                            SettingsScreen(
                                viewModel = viewModel,
                                authManager = authManager,
                                onNavigateBack = {
                                    navController.popBackStack()
                                },
                                onNavigateToLegal = { type ->
                                    navController.navigate("legal/$type")
                                },
                                showInterstitialAd = showInterstitialAd
                            )
                        }
                        composable("legal/{type}") { backStackEntry ->
                            val type = backStackEntry.arguments?.getString("type") ?: "privacy"
                            LegalScreen(
                                type = type,
                                onNavigateBack = {
                                    navController.popBackStack()
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        billingManager.disconnect()
    }
}