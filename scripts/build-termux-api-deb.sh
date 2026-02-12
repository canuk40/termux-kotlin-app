#!/bin/bash
# Rebuild termux-api .deb packages with the compiled C binary.
#
# Takes the existing .deb packages (which have shell scripts but no binary),
# adds the compiled termux-api-broadcast binary, and fixes the directory
# structure bug where $PREFIX/libexec/termux-api was a directory instead
# of the executable binary.
#
# Prerequisites:
#   Run scripts/build-termux-api-binary.sh first to compile the binaries.
#
# Usage:
#   ./scripts/build-termux-api-deb.sh
#
# Output:
#   Updates .deb files in both:
#     app/src/main/assets/bootstrap-packages/{arch}/
#     repo/{arch}/

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
BINARY_DIR="$PROJECT_ROOT/build/termux-api-binaries"
ASSETS_DIR="$PROJECT_ROOT/app/src/main/assets/bootstrap-packages"
REPO_DIR="$PROJECT_ROOT/repo"
WORK_DIR="$PROJECT_ROOT/build/termux-api-deb-work"

PREFIX="/data/data/com.termux/files/usr"

# Architecture mapping: termux arch name → deb arch name
declare -A DEB_ARCHS=(
    [aarch64]="aarch64"
    [arm]="arm"
    [x86_64]="x86_64"
    [i686]="i686"
)

echo "=== Termux API .deb Rebuilder ==="
echo ""

# Verify binaries exist
for arch in aarch64 arm x86_64 i686; do
    if [[ ! -f "$BINARY_DIR/$arch/termux-api-broadcast" ]]; then
        echo "ERROR: Missing binary for $arch: $BINARY_DIR/$arch/termux-api-broadcast"
        echo "Run scripts/build-termux-api-binary.sh first."
        exit 1
    fi
done

# Clean work directory
rm -rf "$WORK_DIR"

rebuild_deb() {
    local arch="$1"
    local deb_arch="${DEB_ARCHS[$arch]}"
    local deb_name="termux-api_0.59.1-1_${deb_arch}.deb"
    local source_deb="$ASSETS_DIR/$arch/$deb_name"

    echo "--- Rebuilding $deb_name ---"

    if [[ ! -f "$source_deb" ]]; then
        echo "  ERROR: Source .deb not found: $source_deb"
        return 1
    fi

    local work="$WORK_DIR/$arch"
    mkdir -p "$work/extract" "$work/build"

    # Extract the existing .deb
    dpkg-deb -x "$source_deb" "$work/extract/"
    dpkg-deb -e "$source_deb" "$work/extract/DEBIAN/"

    # Fix the libexec structure:
    # BEFORE: $PREFIX/libexec/termux-api/        (directory - WRONG)
    #         $PREFIX/libexec/termux-api/termux-callback  (inside dir)
    # AFTER:  $PREFIX/libexec/termux-api-broadcast (the binary)
    #         $PREFIX/libexec/termux-api          (symlink to binary)
    #         $PREFIX/libexec/termux-callback      (moved out of directory)

    local libexec="$work/extract${PREFIX}/libexec"

    # Save the termux-callback script if it exists inside the directory
    local callback_content=""
    if [[ -f "$libexec/termux-api/termux-callback" ]]; then
        callback_content=$(cat "$libexec/termux-api/termux-callback")
    fi

    # Remove the broken directory structure
    rm -rf "$libexec/termux-api"

    # Install the compiled binary
    cp "$BINARY_DIR/$arch/termux-api-broadcast" "$libexec/termux-api-broadcast"
    chmod 755 "$libexec/termux-api-broadcast"

    # Create the symlink (termux-api → termux-api-broadcast) for backwards compatibility
    ln -sf termux-api-broadcast "$libexec/termux-api"

    # Install termux-callback at the correct location (not in a subdirectory)
    if [[ -n "$callback_content" ]]; then
        echo "$callback_content" > "$libexec/termux-callback"
        chmod 755 "$libexec/termux-callback"
    fi

    # Update the control file:
    # - Remove dependency on termux-am (we use am from the system)
    # - Update description to reflect integrated API
    local control="$work/extract/DEBIAN/control"
    sed -i 's/Depends: bash, util-linux, termux-am (>= 0.8.0)/Depends: bash, util-linux/' "$control"
    sed -i 's/Description: Termux API commands (install also the Termux:API app)/Description: Termux API commands (integrated into Termux-Kotlin app)/' "$control"
    # Remove the misleading "Requires Termux:API app" line
    sed -i '/Requires the Termux:API app/d' "$control"
    # Replace with accurate description
    sed -i 's|to be installed from Google Play or F-Droid\.|The API receiver is built into the main Termux-Kotlin app.|' "$control"

    # Recalculate installed size (in KB)
    local installed_size
    installed_size=$(du -sk "$work/extract" --exclude=DEBIAN | cut -f1)
    sed -i "s/Installed-Size: .*/Installed-Size: $installed_size/" "$control"

    # Build the new .deb
    local output_deb="$work/$deb_name"
    dpkg-deb --build "$work/extract" "$output_deb" >/dev/null 2>&1

    # Copy to both destinations
    cp "$output_deb" "$ASSETS_DIR/$arch/$deb_name"
    cp "$output_deb" "$REPO_DIR/$arch/$deb_name"

    local size
    size=$(stat -c%s "$output_deb" 2>/dev/null || stat -f%z "$output_deb")
    echo "  ✓ $deb_name ($size bytes)"

    # Verify the fixed structure
    echo "  Verifying structure..."
    local verify_dir="$work/verify"
    mkdir -p "$verify_dir"
    dpkg-deb -x "$output_deb" "$verify_dir/"

    if [[ -x "$verify_dir${PREFIX}/libexec/termux-api" ]]; then
        echo "  ✓ $PREFIX/libexec/termux-api is executable"
    else
        echo "  ✗ ERROR: $PREFIX/libexec/termux-api is not executable!"
        return 1
    fi

    if [[ -L "$verify_dir${PREFIX}/libexec/termux-api" ]]; then
        local link_target
        link_target=$(readlink "$verify_dir${PREFIX}/libexec/termux-api")
        echo "  ✓ termux-api → $link_target (symlink)"
    fi

    if [[ -x "$verify_dir${PREFIX}/libexec/termux-api-broadcast" ]]; then
        echo "  ✓ termux-api-broadcast binary present"
    fi

    if [[ -f "$verify_dir${PREFIX}/libexec/termux-callback" ]]; then
        echo "  ✓ termux-callback script present (correct location)"
    fi

    # Verify no directory conflict
    if [[ -d "$verify_dir${PREFIX}/libexec/termux-api" ]] && [[ ! -L "$verify_dir${PREFIX}/libexec/termux-api" ]]; then
        echo "  ✗ ERROR: termux-api is still a directory (not fixed!)"
        return 1
    fi
}

# Rebuild for all architectures
FAILED=0
for arch in aarch64 arm x86_64 i686; do
    if ! rebuild_deb "$arch"; then
        echo "  ✗ Failed for $arch"
        FAILED=1
    fi
    echo ""
done

# Clean up
rm -rf "$WORK_DIR"

if [[ $FAILED -eq 0 ]]; then
    echo "=== All .deb packages rebuilt successfully ==="
    echo ""
    echo "Updated packages:"
    for arch in aarch64 arm x86_64 i686; do
        ls -lh "$ASSETS_DIR/$arch/termux-api_"*.deb
    done
    echo ""
    echo "Next step: Run scripts/generate-repo.sh to update repository metadata."
else
    echo "=== Some architectures failed ==="
    exit 1
fi
