package com.termux.app.x11.models

import kotlinx.serialization.Serializable

/**
 * Desktop environment options available for installation
 */
@Serializable
enum class DesktopEnvironment(
    val displayName: String,
    val packageName: String,
    val description: String,
    val estimatedSize: Int, // in MB
    val isRecommended: Boolean = false
) {
    XFCE4(
        displayName = "XFCE4",
        packageName = "xfce4",
        description = "Full-featured desktop environment with panel, file manager, and settings. Best balance of features and performance.",
        estimatedSize = 150,
        isRecommended = true
    ),
    LXQT(
        displayName = "LXQt",
        packageName = "lxqt",
        description = "Modern, lightweight Qt-based desktop environment. Clean and efficient.",
        estimatedSize = 130
    ),
    LXDE(
        displayName = "LXDE",
        packageName = "lxde",
        description = "Older but stable GTK-based desktop. Very lightweight.",
        estimatedSize = 100
    ),
    FLUXBOX(
        displayName = "Fluxbox",
        packageName = "fluxbox",
        description = "Minimal window manager. Extremely lightweight but requires manual configuration.",
        estimatedSize = 20
    ),
    OPENBOX(
        displayName = "Openbox",
        packageName = "openbox",
        description = "Minimal window manager with more customization options.",
        estimatedSize = 25
    );

    companion object {
        fun getRecommended(): DesktopEnvironment = XFCE4
    }
}

/**
 * Additional applications that can be installed with the desktop
 */
@Serializable
enum class DesktopApplication(
    val displayName: String,
    val packageNames: List<String>,
    val description: String,
    val estimatedSize: Int, // in MB
    val category: AppCategory,
    val isEssential: Boolean = false
) {
    FIREFOX(
        displayName = "Firefox",
        packageNames = listOf("firefox"),
        description = "Web browser",
        estimatedSize = 80,
        category = AppCategory.INTERNET,
        isEssential = true
    ),
    MOUSEPAD(
        displayName = "Mousepad",
        packageNames = listOf("mousepad"),
        description = "Simple text editor",
        estimatedSize = 5,
        category = AppCategory.EDITORS,
        isEssential = true
    ),
    GEANY(
        displayName = "Geany",
        packageNames = listOf("geany"),
        description = "Lightweight IDE for programming",
        estimatedSize = 15,
        category = AppCategory.EDITORS
    ),
    THUNAR(
        displayName = "Thunar",
        packageNames = listOf("thunar", "thunar-archive-plugin"),
        description = "File manager (included with XFCE4)",
        estimatedSize = 10,
        category = AppCategory.UTILITIES,
        isEssential = true
    ),
    RISTRETTO(
        displayName = "Ristretto",
        packageNames = listOf("ristretto"),
        description = "Image viewer",
        estimatedSize = 5,
        category = AppCategory.MEDIA
    ),
    MPV(
        displayName = "MPV",
        packageNames = listOf("mpv"),
        description = "Video player",
        estimatedSize = 20,
        category = AppCategory.MEDIA
    ),
    FILE_ROLLER(
        displayName = "File Roller",
        packageNames = listOf("file-roller"),
        description = "Archive manager (zip, tar, etc.)",
        estimatedSize = 8,
        category = AppCategory.UTILITIES
    ),
    HTOP(
        displayName = "htop",
        packageNames = listOf("htop"),
        description = "System monitor",
        estimatedSize = 2,
        category = AppCategory.UTILITIES,
        isEssential = true
    ),
    GIT(
        displayName = "Git",
        packageNames = listOf("git"),
        description = "Version control system",
        estimatedSize = 10,
        category = AppCategory.DEVELOPMENT,
        isEssential = true
    );

    companion object {
        fun getEssentials(): List<DesktopApplication> = 
            values().filter { it.isEssential }
        
        fun getByCategory(category: AppCategory): List<DesktopApplication> =
            values().filter { it.category == category }
    }
}

@Serializable
enum class AppCategory(val displayName: String) {
    INTERNET("Internet"),
    EDITORS("Editors"),
    DEVELOPMENT("Development"),
    MEDIA("Media"),
    UTILITIES("Utilities")
}

/**
 * VNC connection configuration
 */
@Serializable
data class VncConfig(
    val display: Int = 1,
    val port: Int = 5901,
    val geometry: String = "1280x720",
    val colorDepth: Int = 24,
    val dpi: Int = 96,
    val localhostOnly: Boolean = true
) {
    fun toVncServerArgs(): List<String> = buildList {
        add("vncserver")
        add(":$display")
        if (localhostOnly) add("-localhost")
        add("-geometry")
        add(geometry)
        add("-depth")
        add(colorDepth.toString())
        add("-dpi")
        add(dpi.toString())
    }

    fun getConnectionString(): String = "localhost:$port"
}

/**
 * Desktop session state
 */
sealed class DesktopSessionState {
    object Idle : DesktopSessionState()
    object Installing : DesktopSessionState()
    data class Starting(val progress: String) : DesktopSessionState()
    data class Running(val display: Int, val port: Int) : DesktopSessionState()
    data class Stopping(val display: Int) : DesktopSessionState()
    data class Error(val message: String, val cause: Throwable? = null) : DesktopSessionState()
}

/**
 * Installation progress
 */
data class InstallationProgress(
    val stage: InstallStage,
    val progress: Float, // 0.0 to 1.0
    val message: String
)

enum class InstallStage {
    PREPARING,
    UPDATING_REPOS,
    INSTALLING_X11_REPO,
    INSTALLING_DESKTOP,
    INSTALLING_VNC,
    INSTALLING_APPS,
    CONFIGURING,
    COMPLETE
}

/**
 * Desktop installation configuration
 */
data class DesktopInstallConfig(
    val environment: DesktopEnvironment = DesktopEnvironment.XFCE4,
    val additionalApps: List<DesktopApplication> = DesktopApplication.getEssentials(),
    val vncConfig: VncConfig = VncConfig(),
    val installFonts: Boolean = true,
    val optimizeForTouch: Boolean = true
) {
    fun estimatedTotalSize(): Int {
        var total = environment.estimatedSize
        total += additionalApps.sumOf { it.estimatedSize }
        total += 10 // VNC server
        if (installFonts) total += 30 // Font packages
        return total
    }

    fun getPackageList(): List<String> = buildList {
        // Base packages
        add("x11-repo")
        add("tigervnc")
        add("dbus")

        // Desktop environment
        add(environment.packageName)
        
        // XFCE4 specific essentials
        if (environment == DesktopEnvironment.XFCE4) {
            addAll(listOf(
                "xfce4-terminal",
                "xfce4-settings",
                "xfce4-goodies"
            ))
        }

        // Additional applications
        additionalApps.forEach { app ->
            addAll(app.packageNames)
        }

        // Fonts
        if (installFonts) {
            addAll(listOf(
                "fonts-noto",
                "fonts-noto-cjk"
            ))
        }
    }
}
