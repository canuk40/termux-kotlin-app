package com.termux.app.x11.ui

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.termux.app.x11.X11Activity
import com.termux.app.x11.models.*

@Composable
fun DesktopLauncherScreen(
    viewModel: DesktopViewModel = viewModel()
) {
    val context = LocalContext.current
    val isInstalled by viewModel.isInstalled.collectAsState()
    val sessionState by viewModel.sessionState.collectAsState()
    val installProgress by viewModel.installationProgress.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.checkInstallation()
    }

    when {
        isInstalled == null -> {
            LoadingScreen()
        }
        isInstalled == false -> {
            DesktopInstallationScreen(
                progress = installProgress,
                onInstall = { config ->
                    viewModel.installDesktop(config)
                }
            )
        }
        else -> {
            DesktopControlScreen(
                sessionState = sessionState,
                onStart = { config ->
                    viewModel.startDesktop(config)
                },
                onLaunch = { display, port ->
                    val intent = Intent(context, X11Activity::class.java).apply {
                        putExtra(X11Activity.EXTRA_DISPLAY, display)
                        putExtra(X11Activity.EXTRA_PORT, port)
                    }
                    context.startActivity(intent)
                },
                onStop = {
                    viewModel.stopDesktop()
                }
            )
        }
    }
}

@Composable
private fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DesktopInstallationScreen(
    progress: InstallationProgress?,
    onInstall: (DesktopInstallConfig) -> Unit
) {
    var selectedEnvironment by remember { mutableStateOf(DesktopEnvironment.XFCE4) }
    var selectedApps by remember { mutableStateOf(DesktopApplication.getEssentials()) }
    var installFonts by remember { mutableStateOf(true) }
    var showingDetails by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Install Desktop Environment") }
            )
        }
    ) { padding ->
        if (progress != null) {
            InstallationProgressScreen(progress, Modifier.padding(padding))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    WelcomeCard()
                }

                item {
                    EnvironmentSelectionCard(
                        selected = selectedEnvironment,
                        onSelect = { selectedEnvironment = it }
                    )
                }

                item {
                    ApplicationSelectionCard(
                        selected = selectedApps,
                        onToggle = { app ->
                            selectedApps = if (app in selectedApps) {
                                selectedApps - app
                            } else {
                                selectedApps + app
                            }
                        }
                    )
                }

                item {
                    OptionsCard(
                        installFonts = installFonts,
                        onToggleFonts = { installFonts = it }
                    )
                }

                item {
                    val config = DesktopInstallConfig(
                        environment = selectedEnvironment,
                        additionalApps = selectedApps,
                        installFonts = installFonts
                    )
                    
                    InstallButton(
                        config = config,
                        onClick = { onInstall(config) }
                    )
                }
            }
        }
    }
}

@Composable
private fun WelcomeCard() {
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Computer,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Welcome to Termux Desktop",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Install a full Linux desktop environment with GUI applications. " +
                        "Access it via VNC viewer built into the app.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EnvironmentSelectionCard(
    selected: DesktopEnvironment,
    onSelect: (DesktopEnvironment) -> Unit
) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Choose Desktop Environment",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            DesktopEnvironment.values().forEach { env ->
                EnvironmentOption(
                    environment = env,
                    selected = env == selected,
                    onSelect = { onSelect(env) }
                )
            }
        }
    }
}

@Composable
private fun EnvironmentOption(
    environment: DesktopEnvironment,
    selected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onSelect
            )
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onSelect
        )
        Column(modifier = Modifier.weight(1f)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = environment.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                if (environment.isRecommended) {
                    AssistChip(
                        onClick = { },
                        label = { Text("Recommended", style = MaterialTheme.typography.labelSmall) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }
            Text(
                text = environment.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "~${environment.estimatedSize} MB",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@Composable
private fun ApplicationSelectionCard(
    selected: List<DesktopApplication>,
    onToggle: (DesktopApplication) -> Unit
) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Additional Applications",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            AppCategory.values().forEach { category ->
                val apps = DesktopApplication.getByCategory(category)
                if (apps.isNotEmpty()) {
                    Text(
                        text = category.displayName,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    apps.forEach { app ->
                        ApplicationOption(
                            app = app,
                            checked = app in selected,
                            onToggle = { onToggle(app) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ApplicationOption(
    app: DesktopApplication,
    checked: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                onValueChange = { onToggle() }
            )
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = { onToggle() }
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = app.displayName,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "${app.description} • ~${app.estimatedSize} MB",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun OptionsCard(
    installFonts: Boolean,
    onToggleFonts: (Boolean) -> Unit
) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Options",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = installFonts,
                        onValueChange = onToggleFonts
                    )
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = installFonts,
                    onCheckedChange = onToggleFonts
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Install International Fonts",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Noto fonts with CJK support • ~30 MB",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun InstallButton(
    config: DesktopInstallConfig,
    onClick: () -> Unit
) {
    val totalSize = config.estimatedTotalSize()
    
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Total Download Size:",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "~$totalSize MB",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Download, "Install")
                Spacer(Modifier.width(8.dp))
                Text("Install Desktop Environment")
            }
        }
    }
}

@Composable
private fun InstallationProgressScreen(
    progress: InstallationProgress,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator(
                    progress = progress.progress
                )
                Text(
                    text = progress.stage.name.replace('_', ' ').lowercase().capitalize(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = progress.message,
                    style = MaterialTheme.typography.bodyMedium
                )
                LinearProgressIndicator(
                    progress = progress.progress,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "${(progress.progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DesktopControlScreen(
    sessionState: DesktopSessionState,
    onStart: (VncConfig) -> Unit,
    onLaunch: (Int, Int) -> Unit,
    onStop: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Desktop Environment") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StatusCard(sessionState)

            when (sessionState) {
                is DesktopSessionState.Idle -> {
                    StartButton(onClick = { onStart(VncConfig()) })
                }
                is DesktopSessionState.Running -> {
                    LaunchButton(
                        onClick = { onLaunch(sessionState.display, sessionState.port) }
                    )
                    StopButton(onClick = onStop)
                }
                is DesktopSessionState.Starting -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                }
                else -> {}
            }
        }
    }
}

@Composable
private fun StatusCard(state: DesktopSessionState) {
    Card {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val (icon, text, color) = when (state) {
                is DesktopSessionState.Running -> Triple(
                    Icons.Default.CheckCircle,
                    "Running on :${state.display}",
                    MaterialTheme.colorScheme.primary
                )
                is DesktopSessionState.Idle -> Triple(
                    Icons.Default.Circle,
                    "Stopped",
                    MaterialTheme.colorScheme.onSurfaceVariant
                )
                is DesktopSessionState.Starting -> Triple(
                    Icons.Default.HourglassEmpty,
                    "Starting...",
                    MaterialTheme.colorScheme.secondary
                )
                is DesktopSessionState.Error -> Triple(
                    Icons.Default.Error,
                    "Error: ${state.message}",
                    MaterialTheme.colorScheme.error
                )
                else -> Triple(
                    Icons.Default.Info,
                    "Unknown",
                    MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(icon, null, tint = color)
            Text(text = text, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun StartButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(Icons.Default.PlayArrow, "Start")
        Spacer(Modifier.width(8.dp))
        Text("Start Desktop")
    }
}

@Composable
private fun LaunchButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(Icons.Default.Launch, "Launch")
        Spacer(Modifier.width(8.dp))
        Text("Open Desktop Viewer")
    }
}

@Composable
private fun StopButton(onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(Icons.Default.Stop, "Stop")
        Spacer(Modifier.width(8.dp))
        Text("Stop Desktop")
    }
}
