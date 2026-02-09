# Copilot Instructions for Termux Kotlin App

## Project Overview

**Termux Kotlin App** - A modern Android terminal emulator with X11 desktop environment support.

See full documentation in `docs/` directory.

### Key Features
- 🖥️ **Terminal Emulator** with kitty keyboard protocol
- 🪟 **X11 Desktop** via VNC (XFCE4)
- 📱 **Termux API** with 50+ commands
- 📦 **APT Repository** with .deb packages
- 🤖 **Autonomous CI/CD** for releases

## Important Conventions

### Commit Messages (CRITICAL!)
**Use Conventional Commits for automatic releases:**

```
feat(scope): description     → Minor version (v2.0.0 → v2.1.0)
fix(scope): description      → Patch version (v2.0.0 → v2.0.1)
feat(scope)!: description    → Major version (v3.0.0)
docs: description            → No release
[skip ci] in message         → Skip CI
[skip-release] in message    → No release
```

### Code Style
- **Kotlin only** (no Java)
- **Compose** for UI
- **Hilt** for DI
- **ProcessBuilder** for shell (ShellUtils doesn't exist!)

## Quick Links
- **Architecture**: `ARCHITECTURE.md`
- **CI/CD Guide**: `docs/CI_CD_AUTOMATION.md`
- **X11 Docs**: `docs/X11_*.md`
- **Actions**: https://github.com/canuk40/termux-kotlin-app/actions

## Key Directories
```
app/src/main/kotlin/com/termux/app/x11/  # X11 integration
app/src/main/assets/novnc/               # VNC client
repo/{arch}/                             # Package repository
.github/workflows/                       # CI/CD automation
```

---
**Last Updated**: February 9, 2026 | **Version**: v2.1.0
