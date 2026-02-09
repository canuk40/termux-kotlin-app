# 🤖 CI/CD Automation Guide

This document explains the automated workflows configured for this repository.

## 📋 Table of Contents

- [Overview](#overview)
- [Workflows](#workflows)
- [Automated Releases](#automated-releases)
- [Commit Message Convention](#commit-message-convention)
- [Dependabot](#dependabot)
- [Auto-Merge](#auto-merge)
- [Code Quality](#code-quality)

## 🎯 Overview

The repository is configured with **autonomous CI/CD pipelines** that require minimal manual intervention. Most common tasks are automated:

- ✅ Build and test on every push/PR
- ✅ Automatic versioning based on commit messages
- ✅ Automatic release creation
- ✅ Automated dependency updates
- ✅ Auto-merging of dependency PRs
- ✅ Automatic labeling of issues/PRs
- ✅ Code quality checks
- ✅ Security scanning

## 🔄 Workflows

### 1. CI Workflow (`.github/workflows/ci.yml`)

**Trigger:** Push to `main`/`develop`, Pull Requests

**What it does:**
- Builds Debug APK
- Runs unit tests
- Runs lint checks
- Uploads build artifacts
- Comments on PRs with build status
- Determines if a release should be created
- **Automatically creates releases** when conditions are met

**Skip building:** Add `[skip ci]` to commit message

**Skip documentation changes:** Pushes to `*.md`, `docs/`, or `LICENSE*` files are ignored

### 2. Auto Release Job (Part of CI)

**Trigger:** Successful build on `main` branch with feature/fix commits

**What it does:**
- Analyzes commit message to determine version bump:
  - `feat!:` or `BREAKING CHANGE` → **Major version** (v2.0.0 → v3.0.0)
  - `feat:` → **Minor version** (v2.0.0 → v2.1.0)
  - `fix:` → **Patch version** (v2.0.0 → v2.0.1)
- Generates changelog from git history
- Creates and pushes git tag
- Creates GitHub Release with APK attached
- Auto-generates release notes

**Skip release:** Add `[skip-release]` to commit message

### 3. Auto-Merge Workflow (`.github/workflows/auto-merge.yml`)

**Trigger:** Dependabot PRs

**What it does:**
- Waits for CI checks to pass
- Auto-approves the PR
- Enables auto-merge
- PR is automatically merged when all checks pass

### 4. Auto-Label Workflow (`.github/workflows/auto-label.yml`)

**Trigger:** New issues/PRs

**What it does:**
- Automatically adds labels based on:
  - Commit type (`feat`, `fix`, `docs`, etc.)
  - Component affected (`x11`, `api`, `terminal`)
  - Files changed
  - Breaking changes

### 5. Code Quality Workflow (`.github/workflows/quality.yml`)

**Trigger:** Pull Requests, Push to `main`

**What it does:**
- Security vulnerability scanning (Trivy)
- Code analysis (Detekt)
- Dependency update checks
- Comments on PRs with quality metrics

### 6. Stale Issue Management (`.github/workflows/stale.yml`)

**Trigger:** Daily schedule

**What it does:**
- Marks inactive issues/PRs as stale after 30/60 days
- Closes stale items after 7/14 days
- Exempts pinned, security, and help-wanted items

## 🚀 Automated Releases

### How It Works

1. **Make changes** and commit with conventional commit message
2. **Push to main** branch
3. **CI runs** automatically
4. **If commit is `feat:` or `fix:`** → Release is created automatically
5. **Version is bumped** based on commit type
6. **GitHub Release is created** with APK and changelog

### Example Workflow

```bash
# Feature (minor version bump)
git commit -m "feat(x11): add clipboard support"
git push origin main
# → Automatically creates v2.1.0 release

# Fix (patch version bump)
git commit -m "fix(api): resolve battery status crash"
git push origin main
# → Automatically creates v2.1.1 release

# Breaking change (major version bump)
git commit -m "feat(terminal)!: redesign terminal API"
git push origin main
# → Automatically creates v3.0.0 release
```

### Manual Release Override

If you need to create a release manually:

```bash
git tag -a v2.5.0 -m "Release v2.5.0"
git push origin v2.5.0
```

The Release workflow (`.github/workflows/release.yml`) will build and publish the release.

## 📝 Commit Message Convention

This repository follows [Conventional Commits](https://www.conventionalcommits.org/).

### Format

```
<type>(<scope>): <description>

[optional body]

[optional footer]
```

### Types

- `feat`: New feature (minor version bump)
- `fix`: Bug fix (patch version bump)
- `docs`: Documentation changes
- `style`: Code style changes (formatting)
- `refactor`: Code refactoring
- `perf`: Performance improvements
- `test`: Adding/updating tests
- `build`: Build system changes
- `ci`: CI/CD changes
- `chore`: Other changes

### Scopes (optional)

- `x11`: X11/Desktop related
- `api`: Termux API related
- `terminal`: Terminal emulator
- `ui`: User interface
- `build`: Build system
- `deps`: Dependencies

### Breaking Changes

Add `!` after type/scope or add `BREAKING CHANGE:` in footer:

```bash
feat(api)!: redesign API interface

BREAKING CHANGE: The API interface has been completely redesigned
```

### Examples

```bash
# Feature with scope
feat(x11): add VNC password support

# Fix without scope
fix: resolve memory leak in terminal

# Breaking change
feat(terminal)!: change keyboard API

# Skip CI
docs: update README [skip ci]

# Skip release
fix(ui): minor button alignment [skip-release]

# Multiple scopes
feat(x11,api): integrate desktop with API commands
```

## 🔄 Dependabot

### Configuration

Dependabot automatically:
- Checks for dependency updates weekly (Mondays at 9 AM)
- Creates PRs for:
  - Gradle dependencies (grouped by category)
  - GitHub Actions
- Limits to 5 Gradle PRs and 3 Actions PRs at a time
- Auto-assigns to `@canuk40`
- Adds appropriate labels

### Dependency Groups

- **androidx**: All AndroidX libraries
- **kotlin**: Kotlin stdlib and compiler
- **testing**: JUnit, Mockito, test frameworks

### Auto-Merge

Dependabot PRs are **automatically merged** when:
1. CI checks pass ✅
2. No conflicts ✅
3. PR is approved ✅

You don't need to manually merge dependency updates!

## 🤝 Auto-Merge

### When It Happens

- **Dependabot PRs**: Always auto-merge if checks pass
- **Regular PRs**: Never auto-merge (requires manual review)

### Process

1. Dependabot creates PR
2. CI workflow runs
3. Auto-merge workflow waits for CI
4. Once CI passes, PR is auto-approved
5. Auto-merge is enabled
6. GitHub automatically merges when ready

### Disable Auto-Merge

Add `[no-auto-merge]` label to the PR.

## 🔍 Code Quality

### What Gets Checked

- **Security vulnerabilities** (Trivy scanner)
- **Code smells** (Detekt static analysis)
- **Outdated dependencies**
- **Lint violations**

### Quality Gates

PRs will show:
- ✅ Green check: All quality checks passed
- ⚠️ Yellow warning: Non-critical issues found
- ❌ Red X: Critical issues found

### Review Quality Reports

1. Go to PR "Checks" tab
2. Click on "Code Quality" workflow
3. View artifacts for detailed reports

## 🏷️ Labels

### Automatically Added Labels

**Type labels:**
- `enhancement`, `feature`: For `feat:` commits
- `bug`, `fix`: For `fix:` commits
- `documentation`: For `docs:` commits
- `ci`: For CI-related changes
- `breaking-change`: For breaking changes

**Component labels:**
- `x11`, `desktop`: X11/VNC related
- `termux-api`: API commands
- `terminal`: Terminal emulator
- `packages`: Package repository

**Status labels:**
- `automated`: For automated PRs (Dependabot)
- `stale`: For inactive issues/PRs
- `needs-triage`: For new issues

## 🎮 Quick Actions

### Force a Release

```bash
# Make sure you're on main and up to date
git checkout main
git pull

# Create a release commit
git commit --allow-empty -m "chore: trigger release"
git push origin main
```

### Skip CI on a Commit

```bash
git commit -m "docs: update changelog [skip ci]"
```

### Skip Release Creation

```bash
git commit -m "fix: minor typo [skip-release]"
```

### Manually Trigger CI

1. Go to Actions tab
2. Select "CI" workflow
3. Click "Run workflow"
4. Select branch and run

## 📊 Monitoring

### View Workflow Status

- **Actions tab**: https://github.com/canuk40/termux-kotlin-app/actions
- **Releases**: https://github.com/canuk40/termux-kotlin-app/releases
- **Security**: https://github.com/canuk40/termux-kotlin-app/security

### Notifications

You'll be notified about:
- Failed builds
- New releases
- Dependabot PRs
- Security vulnerabilities

Configure notifications in your GitHub settings.

## 🛠️ Troubleshooting

### CI Not Triggering

1. Check `.github/workflows/ci.yml` exists
2. Verify commit isn't in ignored paths
3. Check if `[skip ci]` is in commit message
4. Ensure GitHub Actions is enabled

### Release Not Created

1. Check if commit follows conventional commit format
2. Verify you're on `main` branch
3. Check if `[skip-release]` is in message
4. Ensure commit type is `feat:` or `fix:`

### Dependabot PR Not Auto-Merging

1. Check if CI checks passed
2. Verify no merge conflicts
3. Check if `[no-auto-merge]` label is present
4. Check auto-merge workflow logs

### Build Failing

1. Check CI workflow logs
2. Verify code compiles locally
3. Check for merge conflicts
4. Review test failures

## 📚 Additional Resources

- [Conventional Commits](https://www.conventionalcommits.org/)
- [GitHub Actions Docs](https://docs.github.com/en/actions)
- [Dependabot Docs](https://docs.github.com/en/code-security/dependabot)
- [Semantic Versioning](https://semver.org/)

---

**🎉 With this setup, your workflow is now fully autonomous!**

Most common tasks happen automatically, requiring minimal manual intervention.
