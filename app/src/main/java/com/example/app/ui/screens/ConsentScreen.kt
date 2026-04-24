package com.example.app.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.FrameLayout
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.app.viewmodel.PasswordViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsentScreen(
    authManager: AuthManager,
    viewModel: PasswordViewModel,
    onConsentComplete: () -> Unit,
    showInterstitialAd: () -> Unit
) {
    val context = LocalContext.current
    val authState by authManager.authState.collectAsState()
    val appSettings by viewModel.appSettings.collectAsState()
    val scope = rememberCoroutineScope()

    var consentPersonalData by remember { mutableStateOf(false) }
    var consentTerms by remember { mutableStateOf(false) }
    var consentMarketing by remember { mutableStateOf(false) }

    val userData = (authState as? AuthState.Authenticated)?.user
    val hasAlreadyConsented = userData?.hasConsented == true

    if (hasAlreadyConsented) {
        onConsentComplete()
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.consent_title)) }
            )
        }
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
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.consent_intro_title),
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.consent_intro_text),
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = consentPersonalData,
                            onCheckedChange = { consentPersonalData = it }
                        )
                        Column(modifier = Modifier.padding(start = 8.dp)) {
                            Text(
                                text = stringResource(R.string.consent_personal_data_title),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = stringResource(R.string.consent_personal_data_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = consentTerms,
                            onCheckedChange = { consentTerms = it }
                        )
                        Column(modifier = Modifier.padding(start = 8.dp)) {
                            Text(
                                text = stringResource(R.string.consent_terms_title),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            TextButton(
                                onClick = { openOffer(context) },
                                modifier = Modifier.padding(start = 0.dp)
                            ) {
                                Text(stringResource(R.string.consent_view_offer))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = consentMarketing,
                            onCheckedChange = { consentMarketing = it }
                        )
                        Column(modifier = Modifier.padding(start = 8.dp)) {
                            Text(
                                text = stringResource(R.string.consent_marketing_title),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = stringResource(R.string.consent_marketing_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    scope.launch {
                        authManager.saveConsent()
                        if (!appSettings.isPremium) {
                            showInterstitialAd()
                        }
                        onConsentComplete()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = consentPersonalData && consentTerms
            ) {
                Text(stringResource(R.string.consent_accept))
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(
                onClick = {
                    scope.launch {
                        authManager.signOut()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.consent_decline))
            }

            if (!BuildConfig.DEBUG && !appSettings.isPremium) {
                Spacer(modifier = Modifier.height(24.dp))
                var bannerContainer by remember { mutableStateOf<FrameLayout?>(null) }

                AndroidView(
                    factory = { ctx ->
                        FrameLayout(ctx).also { bannerContainer = it }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                )

                DisposableEffect(Unit) {
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

private fun openOffer(context: Context) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/morozzz174/JINK/offer"))
    context.startActivity(intent)
}