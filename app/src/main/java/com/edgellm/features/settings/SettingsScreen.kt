package com.edgellm.features.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.edgellm.domain.model.ProviderType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Theme Section
            item {
                SettingsSection(title = "Appearance") {
                    ThemeSelector(
                        currentTheme = uiState.theme,
                        onThemeChange = { viewModel.setTheme(it) }
                    )
                }
            }

            // Engine Section
            item {
                SettingsSection(title = "Local Model") {
                    EngineSelector(
                        currentEngine = uiState.selectedEngine,
                        onEngineChange = { viewModel.setEngine(it) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SliderSetting(
                        label = "Context Length",
                        value = uiState.contextLength.toFloat(),
                        valueRange = 512f..8192f,
                        steps = 14,
                        onValueChange = { viewModel.setContextLength(it.toInt()) },
                        valueLabel = { "${it.toInt()} tokens" }
                    )
                    SliderSetting(
                        label = "Temperature",
                        value = uiState.temperature,
                        valueRange = 0f..1f,
                        steps = 9,
                        onValueChange = { viewModel.setTemperature(it) },
                        valueLabel = { String.format("%.1f", it) }
                    )
                }
            }

            // Cloud API Section
            item {
                SettingsSection(title = "Cloud API Keys") {
                    Text(
                        text = "Add API keys to enable cloud model fallback",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    ProviderType.entries.filter { it != ProviderType.XAI }.forEach { provider ->
                        CloudProviderRow(
                            provider = provider,
                            hasApiKey = viewModel.hasApiKey(provider),
                            onConfigure = { viewModel.showApiKeyDialog(provider) }
                        )
                    }
                }
            }

            // About Section
            item {
                SettingsSection(title = "About") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Version")
                        Text("1.0.0", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }

    // API Key Dialog
    uiState.showApiKeyDialog?.let { provider ->
        ApiKeyDialog(
            provider = provider,
            onDismiss = { viewModel.hideApiKeyDialog() },
            onSave = { apiKey -> viewModel.saveApiKey(provider, apiKey) }
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun ThemeSelector(
    currentTheme: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        ThemeMode.entries.forEach { mode ->
            FilterChip(
                selected = currentTheme == mode,
                onClick = { onThemeChange(mode) },
                label = { Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }) },
                leadingIcon = {
                    Icon(
                        imageVector = when (mode) {
                            ThemeMode.LIGHT -> Icons.Default.LightMode
                            ThemeMode.DARK -> Icons.Default.DarkMode
                            ThemeMode.SYSTEM -> Icons.Default.SettingsBrightness
                        },
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            )
        }
    }
}

@Composable
private fun EngineSelector(
    currentEngine: EngineType,
    onEngineChange: (EngineType) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        EngineType.entries.forEach { engine ->
            FilterChip(
                selected = currentEngine == engine,
                onClick = { onEngineChange(engine) },
                label = { Text(engine.name) }
            )
        }
    }
}

@Composable
private fun SliderSetting(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Float) -> Unit,
    valueLabel: (Float) -> String
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label)
            Text(
                text = valueLabel(value),
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps
        )
    }
}

@Composable
private fun CloudProviderRow(
    provider: ProviderType,
    hasApiKey: Boolean,
    onConfigure: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onConfigure)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = when (provider) {
                    ProviderType.OPENAI -> Icons.Default.Cloud
                    ProviderType.ANTHROPIC -> Icons.Default.Psychology
                    ProviderType.GEMINI -> Icons.Default.AutoAwesome
                    ProviderType.XAI -> Icons.Default.Bolt
                },
                contentDescription = null
            )
            Column {
                Text(
                    text = provider.name,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = if (hasApiKey) "Configured" else "Tap to add API key",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (hasApiKey)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null
        )
    }
}

@Composable
private fun ApiKeyDialog(
    provider: ProviderType,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var apiKey by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configure ${provider.name}") },
        text = {
            Column {
                Text(
                    text = when (provider) {
                        ProviderType.OPENAI -> "Enter your OpenAI API key"
                        ProviderType.ANTHROPIC -> "Enter your Anthropic API key"
                        ProviderType.GEMINI -> "Enter your Gemini API key"
                        else -> "Enter API key"
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("API Key") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(apiKey) },
                enabled = apiKey.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}