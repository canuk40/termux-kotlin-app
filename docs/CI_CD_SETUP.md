# CI/CD Setup Guide

This document explains the CI/CD workflows configured for this repository.

## 🤖 Automated Workflows

### CI Workflow (`.github/workflows/ci.yml`)

**Triggers:**
- Push to `main` or `develop` branches
- Pull requests to `main` or `develop`

**Jobs:**

1. **Build & Test**
   - Compiles debug APK
   - Runs unit tests
   - Runs Android lint
   - Uploads build artifacts and test results

2. **Kotlin Lint**
   - Runs ktlint for code style checking
   - Uploads lint reports

3. **Security Scan**
   - Scans for vulnerabilities with Trivy
   - Uploads results to GitHub Security tab

### Release Workflow (`.github/workflows/release.yml`)

**Triggers:**
- Git tags matching `v*` (e.g., `v2.1.0`)
- Manual workflow dispatch

**Jobs:**

1. **Build Release APKs** (Matrix: 4 architectures)
   - arm64-v8a
   - armeabi-v7a
   - x86_64
   - x86
   - Supports signed or unsigned builds

2. **Build Universal APK**
   - Single APK for all architectures

3. **Create GitHub Release**
   - Generates changelog from commits
   - Uploads all APKs as release assets
   - Auto-publishes release

### PR Workflow (`.github/workflows/pr.yml`)

**Triggers:**
- Pull request opened, synchronized, or reopened

**Jobs:**

1. **Validate PR**
   - Validates PR title follows [Conventional Commits](https://www.conventionalcommits.org/)
   - Checks for merge conflicts

2. **Size Label**
   - Auto-labels PRs by size:
     - `size/xs` (< 10 lines)
     - `size/s` (< 100 lines)
     - `size/m` (< 500 lines)
     - `size/l` (< 1000 lines)
     - `size/xl` (1000+ lines)

3. **Comment**
   - Posts welcome message on new PRs

## 📦 Creating a Release

### Automatic Release (Recommended)

1. Create a tag:
   ```bash
   git tag v2.1.0
   git push origin v2.1.0
   ```

2. GitHub Actions will automatically:
   - Build APKs for all architectures
   - Create a GitHub release
   - Upload APKs as release assets
   - Generate changelog from commits since last tag

### Manual Release

1. Go to **Actions** → **Release** → **Run workflow**
2. Enter version (e.g., `v2.1.0`)
3. APKs will be available as workflow artifacts

## 🔒 Signing APKs

To build signed release APKs, add these secrets to your repository:

1. **KEYSTORE_BASE64** - Base64-encoded keystore file:
   ```bash
   base64 -w 0 your-keystore.jks
   ```

2. **KEYSTORE_PASSWORD** - Keystore password
3. **KEY_ALIAS** - Key alias
4. **KEY_PASSWORD** - Key password

**To add secrets:**
1. Go to **Settings** → **Secrets and variables** → **Actions**
2. Click **New repository secret**
3. Add each secret

Without these secrets, unsigned debug APKs will be built.

## 🔄 Automated Dependency Updates

Dependabot is configured to create PRs for:

- **Gradle dependencies** - Weekly on Mondays at 9am
- **GitHub Actions** - Weekly on Mondays at 9am

PRs are auto-labeled with:
- `dependencies`
- `gradle` or `github-actions`

## 📋 Issue Templates

Two issue templates are available:

1. **Bug Report** (`.github/ISSUE_TEMPLATE/bug_report.yml`)
   - Required: description, steps, version, Android version
   - Optional: logs, device architecture

2. **Feature Request** (`.github/ISSUE_TEMPLATE/feature_request.yml`)
   - Required: problem statement, solution, priority
   - Optional: contribution willingness

## 🎯 Conventional Commits

PR titles must follow [Conventional Commits](https://www.conventionalcommits.org/):

```
feat: add new feature
fix: fix a bug
docs: update documentation
style: code style changes
refactor: refactor code
perf: performance improvements
test: add tests
build: build system changes
ci: CI/CD changes
chore: other changes
revert: revert previous changes
```

Examples:
- ✅ `feat: add kitty keyboard protocol support`
- ✅ `fix: resolve bootstrap welcome screen issue`
- ✅ `docs: update README with new features`
- ❌ `Updated documentation` (no type prefix)
- ❌ `added feature` (not imperative mood)

## 🔍 Monitoring

**View Workflow Runs:**
https://github.com/canuk40/termux-kotlin-app/actions

**View Security Alerts:**
https://github.com/canuk40/termux-kotlin-app/security

**View Dependabot PRs:**
https://github.com/canuk40/termux-kotlin-app/pulls?q=is%3Apr+author%3Aapp%2Fdependabot

## 🐛 Troubleshooting

### Build Fails

1. Check the workflow logs in the Actions tab
2. Look for Gradle errors in the build output
3. Ensure all dependencies are accessible

### Release Not Created

1. Verify tag format matches `v*` (e.g., `v2.1.0`)
2. Check workflow logs for errors
3. Ensure GITHUB_TOKEN has correct permissions

### Unsigned APKs Built

1. Verify keystore secrets are added correctly
2. Check secret names match exactly
3. Verify base64 encoding is correct

## 📚 Additional Resources

- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [Gradle Build Tool](https://gradle.org/)
- [Conventional Commits](https://www.conventionalcommits.org/)
- [Dependabot Documentation](https://docs.github.com/en/code-security/dependabot)
