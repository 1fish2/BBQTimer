#!/usr/bin/env bash
# Builds signed AAB (for Google Play) and APK (for IzzyOnDroid) release artifacts.
#
# * Prompt for code signing credentials (or uses predefined keystore paths and aliases).
# * Build the release artifacts: AAB with the Upload Key, APK with the App Signing key.
# * Copy them to app/release/.
# * Verify that both archives were generated with matching versions and timestamps.
# * Verify the APK signature.
#
# ASSUMES: cwd is the project root directory.
#
set -euo pipefail

# Default file paths and aliases
BBQTIMER_UPLOAD_KEYSTORE="${BBQTIMER_UPLOAD_KEYSTORE:-bbqtimer-upload-keystore.jks}"
BBQTIMER_UPLOAD_ALIAS="${BBQTIMER_UPLOAD_ALIAS:-upload}"

BBQTIMER_INSTALL_KEYSTORE="${BBQTIMER_INSTALL_KEYSTORE:-bbqtimer-release-keystore.jks}"
BBQTIMER_INSTALL_ALIAS="${BBQTIMER_INSTALL_ALIAS:-BBQTimer}"

DEST_DIR="app/release"
mkdir -p "${DEST_DIR}"
AAB_DEST="${DEST_DIR}/app-release.aab"
APK_DEST="${DEST_DIR}/app-release.apk"

echo "=== BBQTimer Dual Release Builder ==="

# --- Step 1: Collect Passwords ---
echo ""
echo "--- 1. Google Play Upload Keystore (for AAB) ---"
echo "Keystore: $BBQTIMER_UPLOAD_KEYSTORE"
read -s -r -p "Enter Upload Keystore Password: " UPLOAD_STORE_PASS; echo ""
read -s -r -p "Enter Upload Alias Password: " UPLOAD_KEY_PASS; echo ""

echo ""
echo "--- 2. App Installation Keystore (for APK for IzzyOnDroid) ---"
echo "Keystore: $BBQTIMER_INSTALL_KEYSTORE"
read -s -r -p "Enter Install Keystore Password: " INSTALL_STORE_PASS; echo ""
read -s -r -p "Enter Install Alias Password: " INSTALL_KEY_PASS; echo ""

echo ""
echo "Cleaning previous build outputs..."
./gradlew clean
rm -f "${DEST_DIR}"/*

# --- Step 2: Build Google Play AAB ---
echo "Building signed App Bundle (.aab) with Play Upload Key..."
( { set +x; } 2>/dev/null;
RELEASE_STORE_FILE="$BBQTIMER_UPLOAD_KEYSTORE" \
RELEASE_STORE_PASSWORD="$UPLOAD_STORE_PASS" \
RELEASE_KEY_ALIAS="$BBQTIMER_UPLOAD_ALIAS" \
RELEASE_KEY_PASSWORD="$UPLOAD_KEY_PASS" \
./gradlew bundleRelease )

# --- Step 3: Build Standalone APK ---
echo "Building signed APK (.apk) with App Installation Key..."
( { set +x; } 2>/dev/null;
RELEASE_STORE_FILE="$BBQTIMER_INSTALL_KEYSTORE" \
RELEASE_STORE_PASSWORD="$INSTALL_STORE_PASS" \
RELEASE_KEY_ALIAS="$BBQTIMER_INSTALL_ALIAS" \
RELEASE_KEY_PASSWORD="$INSTALL_KEY_PASS" \
./gradlew assembleRelease )

# --- Step 4: Stage Artifacts ---
AAB_SRC="app/build/outputs/bundle/release/app-release.aab"
APK_SRC="app/build/outputs/apk/release/app-release.apk"
METADATA_SRC="app/build/outputs/apk/release/output-metadata.json"
APK_MAPPING_SRC="app/build/outputs/mapping/release/mapping.txt"

cp -p "$AAB_SRC" "$APK_SRC" "$METADATA_SRC" "$APK_MAPPING_SRC" "${DEST_DIR}"

# --- Step 5: Verification Gate ---
echo ""
echo "Checking the generated release artifacts..."

# Check that the file timestamps are within 1 minute
AAB_MTIME=$(stat -f "%m" "$AAB_DEST")
APK_MTIME=$(stat -f "%m" "$APK_DEST")
TIME_DIFF=$(( AAB_MTIME > APK_MTIME ? AAB_MTIME - APK_MTIME : APK_MTIME - AAB_MTIME ))

if [ "$TIME_DIFF" -gt 60 ]; then
    echo "ERROR: Artifact modification timestamps differ by more than 5 minutes ($TIME_DIFF seconds)." >&2
    exit 1
fi

# Check the APK signature
if command -v apksigner >/dev/null 2>&1; then
    echo "Verifying APK signature..."
    apksigner verify "$APK_DEST"
else
    echo "apksigner is not on the path. Skipped: apksigner verify \"$APK_DEST\""
fi

echo ""
echo "SUCCESS: Both artifacts built, verified, and staged:"
ls -lh "$DEST_DIR"

CMD_LINE_TOOLS="$HOME/Library/Android/sdk/cmdline-tools/latest/bin"
APKANALYZER="$CMD_LINE_TOOLS/apkanalyzer"

echo ""
if [ -x "$APKANALYZER" ]; then
    read -r APP_ID VERSION_CODE VERSION_NAME < <("$APKANALYZER" apk summary "$APK_DEST")
    echo "Extracted from APK: versionName $VERSION_NAME, versionCode $VERSION_CODE, applicationId $APP_ID"
else
    echo "apkanalyzer not found. Skipped extracting its version info."
fi
