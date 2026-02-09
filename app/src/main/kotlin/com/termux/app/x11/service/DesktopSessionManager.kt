package com.termux.app.x11.service

import android.content.Context
import com.termux.app.x11.models.DesktopInstallConfig
import com.termux.app.x11.models.DesktopSessionState
import com.termux.app.x11.models.InstallStage
import com.termux.app.x11.models.InstallationProgress
import com.termux.app.x11.models.VncConfig
import com.termux.shared.logger.Logger
import com.termux.shared.termux.TermuxConstants
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DesktopSessionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _sessionState = MutableStateFlow<DesktopSessionState>(DesktopSessionState.Idle)
    val sessionState: StateFlow<DesktopSessionState> = _sessionState.asStateFlow()

    private val _installationProgress = MutableStateFlow<InstallationProgress?>(null)
    val installationProgress: StateFlow<InstallationProgress?> = _installationProgress.asStateFlow()

    private var currentDisplay: Int? = null

    companion object {
        private const val TAG = "DesktopSessionManager"
        private const val DESKTOP_INSTALLED_MARKER = ".desktop_installed"
    }

    /**
     * Check if desktop environment is already installed
     */
    suspend fun isDesktopInstalled(): Boolean = withContext(Dispatchers.IO) {
        val markerFile = File(TermuxConstants.TERMUX_HOME_DIR_PATH, DESKTOP_INSTALLED_MARKER)
        val vncServerExists = File(TermuxConstants.TERMUX_PREFIX_DIR_PATH + "/bin/vncserver").exists()
        markerFile.exists() && vncServerExists
    }

    /**
     * Install desktop environment using bundled script
     */
    suspend fun installDesktop(config: DesktopInstallConfig): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            _sessionState.value = DesktopSessionState.Installing
            updateProgress(InstallStage.PREPARING, 0.05f, "Preparing installation...")

            // Copy installation script from assets to Termux home
            val scriptFile = copyInstallScriptToTermux()

            // Build command arguments
            val desktopEnv = config.environment.packageName
            val installApps = if (config.additionalApps.isNotEmpty()) "true" else "false"
            val installFonts = if (config.installFonts) "true" else "false"

            // Execute installation script
            updateProgress(InstallStage.UPDATING_REPOS, 0.10f, "Running installation script...")
            
            val result = executeTermuxCommand(
                command = arrayOf(
                    "/data/data/com.termux/files/usr/bin/bash",
                    scriptFile.absolutePath,
                    desktopEnv,
                    installApps,
                    installFonts
                ),
                workingDir = File(TermuxConstants.TERMUX_HOME_DIR_PATH),
                progressCallback = { line ->
                    // Parse progress from script output
                    when {
                        line.contains("Updating package lists") -> 
                            updateProgress(InstallStage.UPDATING_REPOS, 0.15f, line)
                        line.contains("Installing X11 repository") -> 
                            updateProgress(InstallStage.INSTALLING_X11_REPO, 0.25f, line)
                        line.contains("Installing VNC") -> 
                            updateProgress(InstallStage.INSTALLING_VNC, 0.35f, line)
                        line.contains("Installing desktop") -> 
                            updateProgress(InstallStage.INSTALLING_DESKTOP, 0.50f, line)
                        line.contains("Installing applications") -> 
                            updateProgress(InstallStage.INSTALLING_APPS, 0.70f, line)
                        line.contains("Creating launcher") -> 
                            updateProgress(InstallStage.CONFIGURING, 0.90f, line)
                        line.contains("Installation complete") -> 
                            updateProgress(InstallStage.COMPLETE, 1.0f, line)
                    }
                }
            )

            if (result.exitCode != 0) {
                throw Exception("Installation failed with exit code ${result.exitCode}: ${result.stderr}")
            }

            updateProgress(InstallStage.COMPLETE, 1.0f, "Installation complete!")
            delay(1000)
            _installationProgress.value = null
            _sessionState.value = DesktopSessionState.Idle

            Result.success(Unit)
        } catch (e: Exception) {
            Logger.logError(TAG, "Desktop installation failed: ${e.message}")
            _sessionState.value = DesktopSessionState.Error("Installation failed: ${e.message}", e)
            _installationProgress.value = null
            Result.failure(e)
        }
    }

    /**
     * Start desktop session
     */
    suspend fun startDesktop(config: VncConfig = VncConfig()): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            _sessionState.value = DesktopSessionState.Starting("Stopping existing sessions...")
            
            // Kill any existing VNC server on this display
            stopDesktop(config.display)
            delay(500)

            _sessionState.value = DesktopSessionState.Starting("Starting VNC server...")
            
            val result = executeTermuxCommand(
                command = arrayOf(
                    "/data/data/com.termux/files/usr/bin/bash",
                    "-c",
                    config.toVncServerArgs().joinToString(" ")
                ),
                workingDir = File(TermuxConstants.TERMUX_HOME_DIR_PATH)
            )

            if (result.exitCode != 0) {
                throw Exception("VNC server failed: ${result.stderr}")
            }

            // Verify VNC server started
            delay(1000)
            val pidFile = File(TermuxConstants.TERMUX_HOME_DIR_PATH, ".vnc/localhost:${config.display}.pid")
            if (!pidFile.exists()) {
                throw Exception("VNC server failed to start - no PID file found")
            }

            currentDisplay = config.display
            _sessionState.value = DesktopSessionState.Running(config.display, config.port)
            
            Logger.logInfo(TAG, "Desktop started on display :${config.display}, port ${config.port}")
            Result.success(Unit)
        } catch (e: Exception) {
            Logger.logError(TAG, "Failed to start desktop: ${e.message}")
            _sessionState.value = DesktopSessionState.Error("Failed to start: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Stop desktop session
     */
    suspend fun stopDesktop(display: Int? = null): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val targetDisplay = display ?: currentDisplay ?: 1
            _sessionState.value = DesktopSessionState.Stopping(targetDisplay)

            executeTermuxCommand(
                command = arrayOf(
                    "/data/data/com.termux/files/usr/bin/bash",
                    "-c",
                    "vncserver -kill :$targetDisplay"
                ),
                workingDir = File(TermuxConstants.TERMUX_HOME_DIR_PATH),
                ignoreErrors = true
            )
            
            delay(500)
            currentDisplay = null
            _sessionState.value = DesktopSessionState.Idle

            Logger.logInfo(TAG, "Desktop stopped on display :$targetDisplay")
            Result.success(Unit)
        } catch (e: Exception) {
            Logger.logError(TAG, "Failed to stop desktop: ${e.message}")
            _sessionState.value = DesktopSessionState.Error("Failed to stop: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Check if VNC server is running
     */
    suspend fun isRunning(display: Int = 1): Boolean = withContext(Dispatchers.IO) {
        val pidFile = File(TermuxConstants.TERMUX_HOME_DIR_PATH, ".vnc/localhost:$display.pid")
        if (!pidFile.exists()) return@withContext false

        try {
            val pid = pidFile.readText().trim().toIntOrNull() ?: return@withContext false
            val procFile = File("/proc/$pid")
            procFile.exists()
        } catch (e: Exception) {
            false
        }
    }

    private fun updateProgress(stage: InstallStage, progress: Float, message: String) {
        _installationProgress.value = InstallationProgress(stage, progress, message)
        Logger.logInfo(TAG, "[$stage] $message")
    }

    private data class CommandResult(
        val exitCode: Int,
        val stdout: String,
        val stderr: String
    )

    private fun executeTermuxCommand(
        command: Array<String>,
        workingDir: File,
        progressCallback: ((String) -> Unit)? = null,
        ignoreErrors: Boolean = false
    ): CommandResult {
        try {
            val processBuilder = ProcessBuilder(*command)
                .directory(workingDir)
                .redirectErrorStream(false)

            // Set Termux environment
            val env = processBuilder.environment()
            env["HOME"] = TermuxConstants.TERMUX_HOME_DIR_PATH
            env["PREFIX"] = TermuxConstants.TERMUX_PREFIX_DIR_PATH
            env["PATH"] = "${TermuxConstants.TERMUX_PREFIX_DIR_PATH}/bin:${env["PATH"]}"
            env["LD_LIBRARY_PATH"] = "${TermuxConstants.TERMUX_PREFIX_DIR_PATH}/lib"
            env["TMPDIR"] = "${TermuxConstants.TERMUX_PREFIX_DIR_PATH}/tmp"

            val process = processBuilder.start()

            val stdout = StringBuilder()
            val stderr = StringBuilder()

            // Read stdout
            Thread {
                BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                    reader.lineSequence().forEach { line ->
                        stdout.appendLine(line)
                        progressCallback?.invoke(line)
                        Logger.logVerbose(TAG, "STDOUT: $line")
                    }
                }
            }.start()

            // Read stderr
            Thread {
                BufferedReader(InputStreamReader(process.errorStream)).use { reader ->
                    reader.lineSequence().forEach { line ->
                        stderr.appendLine(line)
                        Logger.logWarn(TAG, "STDERR: $line")
                    }
                }
            }.start()

            val exitCode = process.waitFor()

            return CommandResult(
                exitCode = if (ignoreErrors) 0 else exitCode,
                stdout = stdout.toString(),
                stderr = stderr.toString()
            )
        } catch (e: Exception) {
            Logger.logError(TAG, "Command execution failed")
            if (ignoreErrors) {
                return CommandResult(0, "", e.message ?: "")
            }
            throw e
        }
    }

    private fun copyInstallScriptToTermux(): File {
        val scriptFile = File(TermuxConstants.TERMUX_HOME_DIR_PATH, "install-desktop.sh")
        
        // Copy from assets
        context.assets.open("desktop-scripts/install-desktop.sh").use { input ->
            scriptFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        
        // Make executable
        scriptFile.setExecutable(true, true)
        scriptFile.setReadable(true, false)
        scriptFile.setWritable(true, true)
        
        return scriptFile
    }
}
