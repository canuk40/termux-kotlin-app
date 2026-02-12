#!/bin/bash
# Build the termux-api C binary for all Android architectures using the Android NDK.
#
# This cross-compiles termux-api-broadcast (the IPC bridge between shell scripts
# and the TermuxApiReceiver BroadcastReceiver) for aarch64, arm, x86_64, and i686.
#
# Usage:
#   ./scripts/build-termux-api-binary.sh [--ndk-path /path/to/ndk]
#
# Environment variables:
#   ANDROID_NDK_HOME  - Path to Android NDK (auto-detected from ANDROID_HOME if not set)
#   ANDROID_HOME      - Path to Android SDK (used for NDK detection)
#
# Output:
#   build/termux-api-binaries/{aarch64,arm,x86_64,i686}/termux-api-broadcast

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
SOURCE_DIR="$PROJECT_ROOT/app/src/main/cpp/termux-api"
OUTPUT_DIR="$PROJECT_ROOT/build/termux-api-binaries"

NDK_VERSION="29.0.14206865"
MIN_API=24  # Android 7.0 (matches project's minSdk)

# Compiler flags matching the project's existing native build
CFLAGS="-std=c11 -Wall -Wextra -Os -fno-stack-protector -DANDROID -D__ANDROID__"
LDFLAGS="-Wl,--gc-sections"

# Architecture mapping: termux arch → NDK target triple × API level
declare -A ARCH_TARGET=(
    [aarch64]="aarch64-linux-android"
    [arm]="armv7a-linux-androideabi"
    [x86_64]="x86_64-linux-android"
    [i686]="i686-linux-android"
)

# Parse arguments
NDK_PATH=""
while [[ $# -gt 0 ]]; do
    case "$1" in
        --ndk-path)
            NDK_PATH="$2"
            shift 2
            ;;
        --help|-h)
            echo "Usage: $0 [--ndk-path /path/to/ndk]"
            echo ""
            echo "Cross-compiles the termux-api binary for all Android architectures."
            echo ""
            echo "Options:"
            echo "  --ndk-path PATH  Path to Android NDK root"
            echo ""
            echo "Environment:"
            echo "  ANDROID_NDK_HOME  NDK path (auto-detected if not set)"
            echo "  ANDROID_HOME      SDK path (used for NDK detection)"
            exit 0
            ;;
        *)
            echo "Unknown option: $1"
            exit 1
            ;;
    esac
done

# Detect NDK path
detect_ndk() {
    if [[ -n "$NDK_PATH" ]]; then
        echo "$NDK_PATH"
        return
    fi

    if [[ -n "${ANDROID_NDK_HOME:-}" ]] && [[ -d "$ANDROID_NDK_HOME" ]]; then
        echo "$ANDROID_NDK_HOME"
        return
    fi

    if [[ -n "${ANDROID_HOME:-}" ]]; then
        local ndk_dir="$ANDROID_HOME/ndk/$NDK_VERSION"
        if [[ -d "$ndk_dir" ]]; then
            echo "$ndk_dir"
            return
        fi
        # Try any available NDK version
        local any_ndk
        any_ndk=$(find "$ANDROID_HOME/ndk" -maxdepth 1 -mindepth 1 -type d | sort -V | tail -1)
        if [[ -n "$any_ndk" ]]; then
            echo "$any_ndk"
            return
        fi
    fi

    # Try common locations
    for dir in \
        "$HOME/Android/Sdk/ndk/$NDK_VERSION" \
        "$HOME/Library/Android/sdk/ndk/$NDK_VERSION" \
        "/usr/local/lib/android/sdk/ndk/$NDK_VERSION"; do
        if [[ -d "$dir" ]]; then
            echo "$dir"
            return
        fi
    done

    echo ""
}

NDK_ROOT=$(detect_ndk)
if [[ -z "$NDK_ROOT" ]]; then
    echo "ERROR: Android NDK not found."
    echo "Set ANDROID_NDK_HOME or ANDROID_HOME, or use --ndk-path."
    echo "Install with: sdkmanager --install 'ndk;$NDK_VERSION'"
    exit 1
fi

echo "=== Termux API Binary Builder ==="
echo "NDK:     $NDK_ROOT"
echo "Source:  $SOURCE_DIR"
echo "Output:  $OUTPUT_DIR"
echo ""

# Verify source files exist
for src in termux-api.c termux-api-broadcast.c termux-api.h; do
    if [[ ! -f "$SOURCE_DIR/$src" ]]; then
        echo "ERROR: Missing source file: $SOURCE_DIR/$src"
        exit 1
    fi
done

# Detect NDK toolchain path
HOST_TAG=""
case "$(uname -s)" in
    Linux)  HOST_TAG="linux-x86_64" ;;
    Darwin) HOST_TAG="darwin-x86_64" ;;
    *)      echo "ERROR: Unsupported host OS: $(uname -s)"; exit 1 ;;
esac

TOOLCHAIN="$NDK_ROOT/toolchains/llvm/prebuilt/$HOST_TAG"
if [[ ! -d "$TOOLCHAIN" ]]; then
    echo "ERROR: NDK toolchain not found at: $TOOLCHAIN"
    exit 1
fi

# Clean and create output directories
rm -rf "$OUTPUT_DIR"
mkdir -p "$OUTPUT_DIR"

compile_for_arch() {
    local arch="$1"
    local target="${ARCH_TARGET[$arch]}"
    local cc="$TOOLCHAIN/bin/${target}${MIN_API}-clang"

    echo "--- Compiling for $arch (target: $target) ---"

    if [[ ! -f "$cc" ]]; then
        echo "ERROR: Compiler not found: $cc"
        return 1
    fi

    local arch_out="$OUTPUT_DIR/$arch"
    mkdir -p "$arch_out"

    # Compile termux-api.c → object file
    "$cc" $CFLAGS -c "$SOURCE_DIR/termux-api.c" -o "$arch_out/termux-api.o"

    # Compile and link termux-api-broadcast
    "$cc" $CFLAGS $LDFLAGS \
        "$SOURCE_DIR/termux-api-broadcast.c" \
        "$arch_out/termux-api.o" \
        -o "$arch_out/termux-api-broadcast"

    # Strip the binary for size
    "$TOOLCHAIN/bin/llvm-strip" "$arch_out/termux-api-broadcast"

    local size
    size=$(stat -c%s "$arch_out/termux-api-broadcast" 2>/dev/null || stat -f%z "$arch_out/termux-api-broadcast")
    echo "  ✓ $arch_out/termux-api-broadcast ($size bytes)"

    # Clean intermediate files
    rm -f "$arch_out/termux-api.o"
}

# Compile for all architectures
FAILED=0
for arch in aarch64 arm x86_64 i686; do
    if ! compile_for_arch "$arch"; then
        echo "  ✗ Failed to compile for $arch"
        FAILED=1
    fi
done

echo ""
if [[ $FAILED -eq 0 ]]; then
    echo "=== All architectures compiled successfully ==="
    echo ""
    echo "Binaries:"
    find "$OUTPUT_DIR" -name "termux-api-broadcast" -exec ls -lh {} \;
else
    echo "=== Some architectures failed to compile ==="
    exit 1
fi
