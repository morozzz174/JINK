package com.example.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.app.ads.YandexInterstitialAd
import com.example.app.billing.BillingManager
import com.example.app.data.model.ThemeMode
import com.example.app.data.repository.PasswordRepository
import com.example.app.ui.screens.MainScreen
import com.example.app.ui.screens.SettingsScreen
import com.example.app.ui.theme.SecurePasswordGeneratorTheme
import com.example.app.viewmodel.PasswordViewModel

class MainActivity : ComponentActivity() {

    private lateinit var repository: PasswordRepository
    private lateinit var billingManager: BillingManager
    private lateinit var interstitialAd: YandexInterstitialAd

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        repository = PasswordRepository(applicationContext)
        billingManager = BillingManager(applicationContext)
        interstitialAd = YandexInterstitialAd(applicationContext)

        setContent {
            val viewModel: PasswordViewModel = viewModel(
                factory = PasswordViewModel.Factory(repository, billingManager)
            )
            val appSettings by viewModel.appSettings.collectAsState()

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

                    NavHost(
                        navController = navController,
                        startDestination = "main"
                    ) {
                        composable("main") {
                            MainScreen(
                                viewModel = viewModel,
                                onNavigateToSettings = {
                                    navController.navigate("settings")
                                },
                                showInterstitialAd = {
                                    if (!appSettings.isPremium) {
                                        interstitialAd.load()
                                    }
                                }
                            )
                        }
                        composable("settings") {
                            SettingsScreen(
                                viewModel = viewModel,
                                onNavigateBack = {
                                    navController.popBackStack()
                                },
                                showInterstitialAd = {
                                    if (!appSettings.isPremium) {
                                        interstitialAd.load()
                                    }
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