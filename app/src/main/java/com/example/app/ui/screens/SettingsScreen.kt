package com.example.app.ui.screens

import android.app.Activity
import android.widget.FrameLayout
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.app.BuildConfig
import com.example.app.R
import com.example.app.ads.YandexBannerAd
import com.example.app.auth.AuthManager
import com.example.app.auth.AuthState
import com.example.app.data.model.ThemeMode
import com.example.app.viewmodel.PasswordViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: PasswordViewModel,
    authManager: AuthManager,
    onNavigateBack: () -> Unit,
    onNavigateToLegal: (String) -> Unit,
    showInterstitialAd: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as Activity
    val appSettings by viewModel.appSettings.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()
    val authState by authManager.authState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val premiumSuccessMessage = stringResource(R.string.premium_success)
    val premiumErrorMessage = stringResource(R.string.premium_error)
    val premiumRestoreSuccess = stringResource(R.string.premium_restore_success)
    val premiumRestoreError = stringResource(R.string.premium_restore_error)

    LaunchedEffect(toastMessage) {
        toastMessage?.let { message ->
            val text = when (message) {
                "premium_success" -> premiumSuccessMessage
                "premium_error" -> premiumErrorMessage
                "restore_success" -> premiumRestoreSuccess
                "restore_error" -> premiumRestoreError
                else -> message
            }
            snackbarHostState.showSnackbar(text)
            viewModel.clearToast()
        }
    }

    LaunchedEffect(appSettings.isPremium) {
        if (appSettings.isPremium) {
            viewModel.showToast("premium_success")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(R.string.settings_title))
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (!appSettings.isPremium) {
                            showInterstitialAd()
                        }
                        onNavigateBack()
                    }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Назад"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.settings_theme),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    ThemeMode.entries.forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.updateThemeMode(mode) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = appSettings.themeMode == mode,
                                onClick = { viewModel.updateThemeMode(mode) }
                            )
                            Text(
                                text = when (mode) {
                                    ThemeMode.SYSTEM -> stringResource(R.string.theme_system)
                                    ThemeMode.LIGHT -> stringResource(R.string.theme_light)
                                    ThemeMode.DARK -> stringResource(R.string.theme_dark)
                                },
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.settings_premium),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (appSettings.isPremium) {
                        Text(
                            text = stringResource(R.string.settings_premium_purchased),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.premium_description),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedButton(
                            onClick = { viewModel.purchasePremium(activity) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.settings_buy_premium))
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = { viewModel.restorePurchases() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.settings_restore_premium))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.settings_about),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = stringResource(R.string.about_version, BuildConfig.VERSION_NAME),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = stringResource(R.string.about_description),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.settings_legal),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(
                        onClick = { onNavigateToLegal("privacy") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(R.string.legal_privacy_link),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    TextButton(
                        onClick = { onNavigateToLegal("offer") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(R.string.legal_offer_link),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (authState is AuthState.Authenticated) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        val userData = (authState as AuthState.Authenticated).user
                        val userInfo = userData.email.ifEmpty { userData.phone }
                        Text(
                            text = stringResource(R.string.settings_account, userInfo),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = {
                                authManager.signOut()
                                onNavigateBack()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.auth_logout))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (!BuildConfig.DEBUG && !appSettings.isPremium) {
                var bannerContainer by remember { mutableStateOf<FrameLayout?>(null) }

                AndroidView(
                    factory = { ctx ->
                        FrameLayout(ctx).also { bannerContainer = it }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                )

                DisposableEffect(appSettings.isPremium) {
                    bannerContainer?.let {
                        val bannerAd = YandexBannerAd(context, it)
                        bannerAd.load()
                        onDispose { bannerAd.destroy() }
                    }
                    Unit
                }
            }
        }
    }
}