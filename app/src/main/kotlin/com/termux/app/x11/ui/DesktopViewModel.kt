package com.termux.app.x11.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.termux.app.x11.models.*
import com.termux.app.x11.service.DesktopSessionManager
import com.termux.shared.logger.Logger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DesktopViewModel @Inject constructor(
    private val sessionManager: DesktopSessionManager
) : ViewModel() {

    val sessionState: StateFlow<DesktopSessionState> = sessionManager.sessionState
    val installationProgress: StateFlow<InstallationProgress?> = sessionManager.installationProgress

    private val _isInstalled = MutableStateFlow<Boolean?>(null)
    val isInstalled: StateFlow<Boolean?> = _isInstalled.asStateFlow()

    companion object {
        private const val TAG = "DesktopViewModel"
    }

    init {
        checkInstallation()
    }

    fun checkInstallation() {
        viewModelScope.launch {
            try {
                _isInstalled.value = sessionManager.isDesktopInstalled()
            } catch (e: Exception) {
                Logger.logError(TAG, "Failed to check installation")
                _isInstalled.value = false
            }
        }
    }

    fun installDesktop(config: DesktopInstallConfig) {
        viewModelScope.launch {
            sessionManager.installDesktop(config).onSuccess {
                _isInstalled.value = true
            }.onFailure { e ->
                Logger.logError(TAG, "Installation failed")
            }
        }
    }

    fun startDesktop(config: VncConfig = VncConfig()) {
        viewModelScope.launch {
            sessionManager.startDesktop(config).onFailure { e ->
                Logger.logError(TAG, "Failed to start desktop")
            }
        }
    }

    fun stopDesktop() {
        viewModelScope.launch {
            sessionManager.stopDesktop().onFailure { e ->
                Logger.logError(TAG, "Failed to stop desktop")
            }
        }
    }
}
