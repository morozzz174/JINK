package com.example.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.FrameLayout
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.app.R
import com.example.app.ads.YandexBannerAd
import com.example.app.viewmodel.PasswordViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: PasswordViewModel,
    onNavigateToSettings: () -> Unit,
    onNavigateToLegal: (String) -> Unit,
    showInterstitialAd: () -> Unit
) {
    val context = LocalContext.current
    val passwordOptions by viewModel.passwordOptions.collectAsState()
    val currentPassword by viewModel.currentPassword.collectAsState()
    val savedPasswords by viewModel.savedPasswords.collectAsState()
    val appSettings by viewModel.appSettings.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val copiedMessage = stringResource(R.string.copied_toast)
    val savedMessage = stringResource(R.string.saved_toast)

    LaunchedEffect(toastMessage) {
        toastMessage?.let { message ->
            val text = when (message) {
                "copied" -> copiedMessage
                "saved" -> savedMessage
                else -> message
            }
            snackbarHostState.showSnackbar(text)
            viewModel.clearToast()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.main_title)) },
                actions = {
                    IconButton(onClick = {
                        if (!appSettings.isPremium) {
                            showInterstitialAd()
                        }
                        onNavigateToSettings()
                    }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.settings_title)
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
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
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
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.password_label),
                            style = MaterialTheme.typography.labelMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        if (currentPassword.isNotEmpty()) {
                            Text(
                                text = currentPassword,
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontFamily = FontFamily.Monospace
                                ),
                                textAlign = TextAlign.Center
                            )
                        } else {
                            Text(
                                text = "—",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { viewModel.generatePassword() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.generate_button))
                }

                if (currentPassword.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                copyToClipboard(context, currentPassword)
                                viewModel.showToast("copied")
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.ContentCopy, null)
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.copy_button))
                        }
                        OutlinedButton(
                            onClick = { viewModel.saveCurrentPassword() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.save_button))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = stringResource(R.string.options_title),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.password_length_value, passwordOptions.length)
                )
                Slider(
                    value = passwordOptions.length.toFloat(),
                    onValueChange = { viewModel.updateLength(it.toInt()) },
                    valueRange = 8f..32f,
                    steps = 23,
                    modifier = Modifier.fillMaxWidth()
                )

                SwitchSetting(
                    label = stringResource(R.string.include_uppercase),
                    checked = passwordOptions.includeUppercase,
                    onCheckedChange = { viewModel.updateUppercase(it) }
                )
                SwitchSetting(
                    label = stringResource(R.string.include_lowercase),
                    checked = passwordOptions.includeLowercase,
                    onCheckedChange = { viewModel.updateLowercase(it) }
                )
                SwitchSetting(
                    label = stringResource(R.string.include_numbers),
                    checked = passwordOptions.includeNumbers,
                    onCheckedChange = { viewModel.updateNumbers(it) }
                )
                SwitchSetting(
                    label = stringResource(R.string.include_symbols),
                    checked = passwordOptions.includeSymbols,
                    onCheckedChange = { viewModel.updateSymbols(it) }
                )

                if (savedPasswords.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = stringResource(R.string.saved_passwords_title),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Spacer(modifier = Modifier.height(24.dp))

                TextButton(
                    onClick = { onNavigateToLegal("privacy") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.legal_privacy_link))
                }
            }

            if (!appSettings.isPremium) {
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

@Composable
fun SwitchSetting(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("password", text)
    clipboard.setPrimaryClip(clip)
}