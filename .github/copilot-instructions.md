# Termux Kotlin App - Copilot Instructions

This is a **Kotlin conversion** of the official [Termux](https://github.com/termux/termux-app) Android terminal emulator. All Java code has been converted to idiomatic Kotlin while maintaining full compatibility with the original app.

## Build, Test, and Lint

### Build Commands
```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK (requires signing configuration)
./gradlew assembleRelease

# APKs are output to: app/build/outputs/apk/{debug,release}/*.apk
```

### Test Commands
```bash
# Run all unit tests
./gradlew test

# Run tests for specific module
./gradlew :app:testDebugUnitTest
./gradlew :terminal-emulator:test
./gradlew :terminal-view:test
./gradlew :termux-shared:test

# Test with coverage
./gradlew testDebugUnitTest jacocoTestReport
```

### Lint and Static Analysis
```bash
# Android Lint
./gradlew lint

# Detekt (Kotlin static analysis)
./gradlew detekt

# Lint specific module
./gradlew :app:lint
```

### Clean Build
```bash
# Clean all build outputs
./gradlew clean

# Clean and rebuild
./gradlew clean assembleDebug
```

## Architecture Overview

### Module Structure
Four Gradle modules with clear separation of concerns:

```
┌─────────────────┐
│       app       │  ← Main app, UI, services, activities, plugins
└────────┬────────┘
         │
    ┌────┴────┐
    ▼         ▼
┌───────┐ ┌─────────────┐
│terminal│ │terminal-view│  ← Terminal rendering (Android View)
│emulator│ └──────┬──────┘
└───┬───┘        │
    │            │
    └─────┬──────┘
          ▼
   ┌─────────────┐
   │termux-shared│  ← Shared utilities, models, file ops
   └─────────────┘
```

**Module responsibilities:**
- `app/`: Main application, TermuxActivity, TermuxService, modern features (Compose UI, plugins, agents)
- `terminal-emulator/`: VT100/ANSI terminal emulation logic (TerminalEmulator, TerminalSession, TerminalBuffer)
- `terminal-view/`: Android View for rendering terminal (TerminalView, TerminalRenderer)
- `termux-shared/`: Shared utilities across modules (file operations, shell execution, settings)

### Core Architecture Patterns

**Service + Activity Pattern:**
- `TermuxActivity`: Main terminal UI, manages sessions and keyboard input
- `TermuxService`: Background service keeping shell processes alive
- Sessions survive configuration changes and run in background

**Data Flow:**
```
User Input → TerminalView → TerminalSession → Shell Process
                                   ↓
Shell Output ← TerminalEmulator ← TerminalSession
                   ↓
             TerminalView (render)
```

**Modern Android Stack:**
- Jetpack Compose UI for settings, dialogs, and new features
- Hilt for dependency injection (`@AndroidEntryPoint`, `@Inject`)
- Kotlin Coroutines + Flow for async operations
- DataStore for reactive preferences (replacing SharedPreferences)

### Key Package Structure

```
app/src/main/kotlin/com/termux/app/
├── core/                           # Core infrastructure
│   ├── api/                        # Result<T,E> sealed types
│   ├── logging/                    # TermuxLogger with file output
│   ├── permissions/                # Permission manager
│   ├── terminal/                   # Terminal event bus (Flow)
│   ├── plugin/                     # Plugin API with versioning
│   └── deviceapi/                  # Integrated device APIs (battery, location, etc.)
├── agents/                         # Agent framework (v2.0.5+)
│   ├── daemon/                     # AgentDaemon supervisor
│   ├── skills/                     # Pure Kotlin skills (pkg, fs, git, diagnostic)
│   ├── swarm/                      # Stigmergy-based coordination
│   └── runtime/                    # Sandbox, memory, execution
├── boot/                           # Termux:Boot plugin (built-in)
├── styling/                        # Termux:Styling plugin (built-in)
├── widget/                         # Termux:Widget plugin (built-in)
├── x11/                            # X11 desktop environment support
├── pkg/                            # Package management
│   ├── backup/                     # Backup/restore system
│   ├── doctor/                     # Health diagnostics
│   └── cli/                        # termuxctl CLI
├── activities/                     # Activities (TermuxActivity, HelpActivity, etc.)
├── fragments/                      # UI fragments
└── terminal/                       # Terminal session clients
```

### Integrated Plugins (v2.0.5+)

Unlike official Termux which uses separate plugin APKs, these are **built into the main app**:
- **Termux:Boot** - Auto-run scripts on device boot
- **Termux:Styling** - 11 color schemes with Compose UI
- **Termux:Widget** - Home screen widgets for script shortcuts
- **Termux:API** - 20+ hardware APIs (battery, sensors, location, camera)

All plugin code lives in `app/src/main/kotlin/com/termux/app/{boot,styling,widget,x11}/`.

### Agent Framework (Kotlin-Native)

Pure Kotlin agent system with zero Python dependency:
- **AgentDaemon**: Runs as singleton, auto-starts with app
- **Skills**: pkg, fs, git, diagnostic (all in pure Kotlin)
- **Swarm Intelligence**: Stigmergy-based multi-agent coordination
- **Capabilities**: 45+ fine-grained permissions system

Access via: `agent list`, `agent run <agent> <skill.function>`, `agent swarm`

## Key Conventions

### Kotlin Style

**Immutability First:**
```kotlin
val constantValue = "immutable"     // Prefer val
var mutableValue = "changeable"      // Only when mutation needed
```

**Null Safety:**
```kotlin
val nullableString: String? = mayReturnNull()
val length = nullableString?.length ?: 0        // Safe call with elvis
val definitelyNotNull = nullableString!!        // Only when absolutely certain
```

**Extension Functions:**
This codebase heavily uses Kotlin extensions. Check `termux-shared/src/main/kotlin/com/termux/shared/` for utility extensions before implementing new helpers.

**Sealed Classes for State:**
```kotlin
sealed class Result<out T, out E> {
    data class Success<T>(val value: T) : Result<T, Nothing>()
    data class Error<E>(val error: E) : Result<Nothing, E>()
}
```

Used throughout for type-safe error handling. See `app/core/api/Result.kt`.

### File Paths and Environment

**Package name:** `com.termux.kotlin` (not `com.termux`)
- Uses custom PREFIX: `/data/data/com.termux.kotlin/files/usr`
- Uses custom HOME: `/data/data/com.termux.kotlin/files/home`

**Critical environment variables** (set in `TermuxService`):
- `PREFIX`, `HOME`, `PATH`, `TMPDIR` - Basic terminal operation
- `LD_LIBRARY_PATH` - Overrides hardcoded RUNPATH in binaries
- `TERMINFO`, `TERM`, `COLORTERM` - Full terminal capability
- `SSL_CERT_FILE`, `CURL_CA_BUNDLE` - HTTPS support
- `DPKG_ADMINDIR`, `DPKG_DATADIR` - Package manager paths

See `ARCHITECTURE.md#environment-variables` for complete list.

### Terminal Emulation

**VT100/ANSI Escape Sequences:**
- Implemented in `terminal-emulator/` module
- DO NOT modify escape sequence parsing without understanding VT100 spec
- Test changes with: `cat`, `vim`, `htop`, `tmux`

**Kitty Keyboard Protocol:**
- Enhanced keyboard input (CSI u encoding) for modern TUI apps
- Enable with: `printf '\033[>1;2017h'`
- Test with: `test-kitty-keyboard` command

### Native Code (C/C++)

**Bootstrap loader** in `app/src/main/cpp/`:
- Unpacks bootstrap archive on first run
- Links to `libandroid-support.so` for bionic compatibility
- Minimal changes here - stable interface

**Building native code requires Android NDK:**
```bash
# NDK version specified in CI workflow
sdkmanager --install "ndk;29.0.14206865"
```

### Dependency Injection (Hilt)

All major components use Hilt:
```kotlin
@AndroidEntryPoint
class MyActivity : AppCompatActivity() {
    @Inject lateinit var repository: MyRepository
}

@Singleton
class MyRepository @Inject constructor(
    @ApplicationContext private val context: Context
) { }
```

DI modules in `app/di/` - add new modules for feature-specific dependencies.

### Error Handling Pattern

**Use sealed Result types** instead of exceptions for expected errors:
```kotlin
sealed class TermuxError {
    data class FileNotFound(val path: String) : TermuxError()
    data class PermissionDenied(val permission: String) : TermuxError()
    // ... more specific errors
}

fun readFile(path: String): Result<String, TermuxError> {
    return try {
        Result.Success(File(path).readText())
    } catch (e: FileNotFoundException) {
        Result.Error(TermuxError.FileNotFound(path))
    }
}
```

### Logging

**Use TermuxLogger** (not Android Log directly):
```kotlin
@Inject lateinit var logger: TermuxLogger

logger.logInfo("MyTag", "Info message")
logger.logError("MyTag", "Error message", exception)
logger.logVerbose("MyTag", "Debug details")
```

Logs go to both logcat and file (`$PREFIX/var/log/termux/app.log`).

### Settings and Preferences

**Modern DataStore** (not SharedPreferences):
```kotlin
val mySettingFlow: Flow<String> = dataStore.data.map { prefs ->
    prefs[stringPreferencesKey("my_setting")] ?: "default"
}
```

Settings UI uses **Jetpack Compose** - see `app/fragments/settings/`.

### Package Name Compatibility

**Uses `com.termux` package name** for upstream compatibility:
- All Termux packages install without modification
- Cannot coexist with official Termux (same package name, different signature)
- Bootstrap and environment paths use `com.termux` internally

This differs from the Kotlin package structure (`com.termux.kotlin` in code).

## CI/CD Automation

**Fully autonomous release pipeline:**
- Commit with `feat:` → Minor version bump + release
- Commit with `fix:` → Patch version bump + release  
- Commit with `feat!:` or `BREAKING CHANGE` → Major version bump
- Add `[skip-release]` to commit message to skip auto-release

**PR workflow:**
1. Runs lint, Detekt, and tests
2. Builds debug APK
3. Comments on PR with build status and APK download link

**Security scanning:**
- Trivy for vulnerability scanning
- Detekt for static analysis
- Automatic Dependabot updates with auto-merge

See `.github/workflows/` for all automation.

## Testing Notes

**Unit tests are minimal** - this is a UI-heavy terminal app:
- Test logic, not Android framework components
- Use `@RunWith(RobolectricTestRunner::class)` for Android-dependent tests
- Mock `Context`, `SharedPreferences`, etc. sparingly

**Manual testing critical:**
- Install APK on real device or emulator
- Test terminal interactions: `vim`, `tmux`, `htop`
- Verify keyboard input (hardware keyboard, soft keyboard, extra keys)
- Check session persistence (rotate device, switch apps)

## Version Configuration

**SDK levels** (in `gradle/libs.versions.toml`):
```toml
minSdk = "24"      # Android 7.0
targetSdk = "28"   # Intentionally lower for compatibility
compileSdk = "36"  # Latest
```

**AGP and Kotlin:**
```toml
agp = "8.7.3"
kotlin = "2.0.21"
```

Stay on tested versions - AGP/Gradle upgrades can break NDK builds.

## Common Pitfalls

1. **Don't break terminal emulation** - Changes to `terminal-emulator/` need extensive testing
2. **Path assumptions** - Always use `TermuxConstants.TERMUX_PREFIX_DIR_PATH`, never hardcode paths
3. **Background restrictions** - TermuxService uses foreground service and wake locks intentionally
4. **NDK paths** - Native code paths must match environment variables or app breaks
5. **Module dependencies** - `app` depends on others, but others should NOT depend on `app`

## Useful Commands

```bash
# Find Kotlin files with specific pattern
find . -name "*.kt" -type f | xargs grep -l "PatternHere"

# Check module dependencies
./gradlew :app:dependencies

# Generate dependency graph
./gradlew generateDependencyGraph

# Check for dependency updates
./gradlew dependencyUpdates
```

## Documentation

- `README.md` - Features, installation, overview
- `ARCHITECTURE.md` - Detailed architecture, environment vars, modules
- `CONTRIBUTING.md` - Development setup, CI/CD, code style
- `docs/AGENTS.md` - Agent framework documentation
- `docs/KITTY_KEYBOARD_PROTOCOL.md` - Keyboard protocol details
- `docs/X11_COMPLETE_IMPLEMENTATION.md` - X11 desktop environment
- `docs/CI_CD_AUTOMATION.md` - CI/CD pipeline details
