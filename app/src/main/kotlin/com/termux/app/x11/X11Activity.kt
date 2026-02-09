package com.termux.app.x11

import android.os.Bundle
import android.view.WindowManager
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.termux.app.x11.models.DesktopSessionState
import com.termux.app.x11.ui.DesktopViewModel
import com.termux.app.ui.compose.theme.TermuxTheme
import com.termux.shared.logger.Logger
import dagger.hilt.android.AndroidEntryPoint

/**
 * Activity that displays the desktop environment via VNC in a WebView (noVNC)
 */
@AndroidEntryPoint
class X11Activity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Keep screen on while desktop is active
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val display = intent.getIntExtra(EXTRA_DISPLAY, 1)
        val port = intent.getIntExtra(EXTRA_PORT, 5901)

        setContent {
            TermuxTheme {
                DesktopScreen(
                    display = display,
                    port = port,
                    onClose = { finish() }
                )
            }
        }
    }

    companion object {
        const val EXTRA_DISPLAY = "display"
        const val EXTRA_PORT = "port"
    }
}

@Composable
private fun DesktopScreen(
    display: Int,
    port: Int,
    onClose: () -> Unit,
    viewModel: DesktopViewModel = viewModel()
) {
    val sessionState by viewModel.sessionState.collectAsState()

    Scaffold(
        topBar = {
            DesktopTopBar(
                display = display,
                sessionState = sessionState,
                onClose = onClose,
                onStop = { viewModel.stopDesktop() }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = sessionState) {
                is DesktopSessionState.Running -> {
                    VncWebView(
                        port = port,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                is DesktopSessionState.Starting -> {
                    LoadingScreen(message = state.progress)
                }
                is DesktopSessionState.Error -> {
                    ErrorScreen(
                        message = state.message,
                        onRetry = { viewModel.startDesktop() },
                        onClose = onClose
                    )
                }
                else -> {
                    LoadingScreen(message = "Initializing desktop...")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DesktopTopBar(
    display: Int,
    sessionState: DesktopSessionState,
    onClose: () -> Unit,
    onStop: () -> Unit
) {
    TopAppBar(
        title = { Text("Desktop (Display :$display)") },
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Close desktop"
                )
            }
        },
        actions = {
            if (sessionState is DesktopSessionState.Running) {
                IconButton(onClick = onStop) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Stop desktop"
                    )
                }
            }
        }
    )
}

@Composable
private fun VncWebView(
    port: Int,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    allowFileAccess = true
                    allowContentAccess = true
                    useWideViewPort = true
                    loadWithOverviewMode = true
                    builtInZoomControls = false
                    displayZoomControls = false
                    setSupportZoom(true)
                }

                webViewClient = object : WebViewClient() {
                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: WebResourceError?
                    ) {
                        Logger.logError("X11Activity", "WebView error: ${error?.description}")
                    }
                }
                
                // Load noVNC from assets
                val url = "file:///android_asset/novnc/termux_vnc.html?host=localhost&port=$port&autoconnect=true"
                loadUrl(url)
            }
        },
        modifier = modifier
    )
}

@Composable
private fun LoadingScreen(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator()
            Text(text = message)
        }
    }
}

@Composable
private fun ErrorScreen(
    message: String,
    onRetry: () -> Unit,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = "Error",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(64.dp)
            )
            Text(
                text = "Desktop Error",
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(onClick = onClose) {
                    Text("Close")
                }
                Button(onClick = onRetry) {
                    Text("Retry")
                }
            }
        }
    }
}
