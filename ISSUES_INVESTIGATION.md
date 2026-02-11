# Investigation: termux-api and chsh Issues

## User Report
Users reported that:
1. **termux-api commands** (like `termux-battery-status`) don't work
2. **chsh command** doesn't work

## Root Cause Analysis

### Issue #1: termux-api Commands Not Working

#### The Confusion
The README makes bold claims that can mislead users:
- ✅ Claims: "50+ commands built-in"
- ✅ Claims: "API commands work immediately after install"
- ✅ Claims: "No external APK needed"

#### The Reality
There are **TWO SEPARATE API SYSTEMS** in this app:

**System 1: Traditional Termux:API (Shell Scripts)**
- Commands: `termux-battery-status`, `termux-clipboard-get`, etc.
- Location: `/data/data/com.termux/files/usr/bin/termux-*`
- **Status: NOT pre-installed**
- Requires: `apt install termux-api`
- Package: `termux-api_0.59.1-1_*.deb` (exists in repo/)

**System 2: Kotlin Device API (Built-in)**
- Commands: `termuxctl device battery`, `termuxctl device location`, etc.
- Location: Built into app's Kotlin code
- **Status: BUILT-IN (only battery fully implemented)**
- No installation required
- Implementation: `app/core/deviceapi/actions/`

#### Why Users Are Confused
1. The README conflates these two systems
2. Users expect `termux-battery-status` to work "immediately"
3. But it requires manual installation: `apt install termux-api`
4. The doc doesn't clearly distinguish between `termux-*` vs `termuxctl device *`

#### Repository Status
```bash
$ ls repo/aarch64/
termux-api_0.59.1-1_aarch64.deb  # ✅ EXISTS
Packages                          # Lists 6 packages
Packages.gz
```

The `Packages` file references these packages, but only termux-api exists:
- apt (referenced but .deb missing)
- dpkg (referenced but .deb missing)
- termux-api (✅ .deb exists)
- termux-core (referenced but .deb missing)
- termux-exec (referenced but .deb missing)
- termux-tools (referenced but .deb missing)

**However, the repo/ folder is NOT bundled into the APK!**

#### Bootstrap Configuration
During bootstrap:
1. Pre-built bootstrap ZIP is extracted from `app/src/main/cpp/bootstrap-*.zip`
2. Bootstrap already contains `sources.list` pointing to: `https://packages.termux.dev/apt/termux-main`
3. Users must run `apt update` to fetch package lists from internet
4. Then `apt install termux-api` downloads the package

#### Solution Path Forward

**Quick Fix (Documentation):**
Update README.md to clarify:
```markdown
## Using Termux:API Commands

### Traditional termux-* Commands
The traditional Termux:API commands require installation:
```bash
apt update
apt install termux-api
```

Then you can use:
- `termux-battery-status` - Battery information
- `termux-clipboard-get/set` - Clipboard operations
- And 50+ more commands

### Built-in Device API (Recommended)
Use the new Kotlin-based API for better integration:
```bash
termuxctl device battery        # Get battery status
termuxctl device list          # List available APIs
```
```

**Proper Fix (Bootstrap):**
Pre-install termux-api during bootstrap:
1. Include `termux-api_*.deb` in bootstrap ZIP
2. Auto-install with `dpkg -i` after extraction
3. Match README claims of "works immediately"

---

### Issue #2: chsh Command Not Working

#### Root Cause
The `chsh` command is part of the `util-linux` package, which:
- ❌ Is NOT in your custom repo
- ❌ Is NOT in the bootstrap ZIP
- ✅ Available from upstream Termux mirrors

#### Why It Fails
1. User tries: `chsh -s zsh`
2. Command not found (util-linux not installed)
3. User needs: `apt install util-linux`
4. If `apt update` wasn't run, package lists are missing
5. Installation fails

#### Expected Workflow
```bash
# After first launch
$ apt update                    # Fetch package lists from mirrors
$ apt install util-linux        # Install chsh, lsblk, etc.
$ apt install zsh              # Install desired shell
$ chsh -s /data/data/com.termux/files/usr/bin/zsh
```

#### Sources Configuration
The bootstrap includes `sources.list`:
```
deb https://packages.termux.dev/apt/termux-main stable main
```

This mirror has thousands of packages including util-linux.

#### Solution

**Option A: Documentation (Recommended)**
Add to README.md:
```markdown
## First Time Setup

After installing the app, run:
```bash
apt update                      # Update package lists
apt upgrade                     # Upgrade base packages
apt install util-linux zsh      # Install additional tools
```

**Common packages:**
- `util-linux` - chsh, lsblk, mount tools
- `zsh` / `fish` - Alternative shells
- `vim` / `neovim` - Text editors
- `git` - Version control
```

**Option B: Pre-install in Bootstrap**
Include util-linux in the bootstrap ZIP so chsh is available immediately.

---

## Recommendations

### 1. Fix Documentation (Immediate)
- Remove misleading claims about "built-in" API commands
- Clarify the difference between `termux-*` and `termuxctl device *`
- Add clear installation instructions for both termux-api and util-linux
- Create a "First Time Setup" section

### 2. Add User Guide (Short Term)
Create `docs/USER_GUIDE.md`:
```markdown
# Termux Kotlin - User Guide

## First Launch Checklist
1. Open the app (bootstrap installs automatically)
2. Run `apt update` to fetch package lists
3. Install essential packages:
   ```bash
   apt install termux-api util-linux
   ```

## Device API Commands

### Option 1: Traditional (requires installation)
```bash
apt install termux-api
termux-battery-status
termux-clipboard-get
```

### Option 2: Built-in (no installation)
```bash
termuxctl device battery
termuxctl device list
```

## Changing Your Shell
```bash
apt install util-linux zsh
chsh -s /data/data/com.termux/files/usr/bin/zsh
```
```

### 3. Improve Bootstrap (Long Term)
Options:
- Pre-install termux-api and util-linux
- Bundle a minimal local repo in the APK
- Show first-launch tutorial with setup commands

### 4. Error Messages (UX Improvement)
When users try missing commands, show helpful errors:
```bash
$ termux-battery-status
Command not found: termux-battery-status

Tip: Install termux-api package:
  apt update && apt install termux-api

Or use the built-in API:
  termuxctl device battery
```

---

## Testing Checklist

To verify fixes, test this scenario:
```bash
# 1. Clean install (uninstall, clear data, reinstall)
# 2. Wait for bootstrap to complete
# 3. Try problematic commands
termux-battery-status           # Should show clear error
chsh -s zsh                      # Should show clear error

# 4. Install packages
apt update
apt install termux-api util-linux

# 5. Retry commands
termux-battery-status           # Should work now
chsh -s zsh                      # Should work now (if zsh installed)

# 6. Test built-in API
termuxctl device battery        # Should work without installation
```

---

## Files to Modify

1. **README.md**
   - Section: "Complete Termux:API Integration"
   - Clarify installation requirements
   - Add first-time setup instructions

2. **docs/USER_GUIDE.md** (NEW)
   - Create comprehensive user guide
   - Include troubleshooting section

3. **app/TermuxActivity.kt or welcome screen**
   - Show first-launch tips
   - Suggest running `apt update && apt install termux-api util-linux`

4. **Command not found handler** (if exists)
   - Detect missing common commands
   - Show installation hints
