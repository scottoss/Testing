#!/usr/bin/env bash
# ==============================================================================
# DawnChat iOS Build, Code Sign & App Store Connect Upload Script
# ==============================================================================
# Compiles the Kotlin Multiplatform (:shared) release framework for iOS arm64,
# generates the Xcode project via XcodeGen, archives & signs the iOS app,
# exports a signed .ipa, and uploads it to Apple App Store Connect / TestFlight.
#
# Required Environment Variables for Signing & Upload:
#   APPLE_TEAM_ID                  - 10-character Apple Developer Team ID (e.g., A1B2C3D4E5)
#   BUNDLE_ID                      - App Bundle ID (default: com.chillboiostudios.dawnchat)
#
#   Option A: Manual Certificate & Provisioning Profile (Recommended for CI/GitHub Actions)
#     IOS_DIST_CERT_BASE64         - Base64-encoded Apple Distribution .p12 certificate
#     IOS_DIST_CERT_PASSWORD       - Password for the .p12 certificate
#     IOS_PROV_PROFILE_BASE64      - Base64-encoded App Store .mobileprovision profile
#
#   Option B: Automatic Signing via App Store Connect API Key
#     Set CODE_SIGN_STYLE=Automatic and pass ASC_* variables below.
#
#   App Store Connect API Key (for uploading .ipa to TestFlight / App Store):
#     ASC_KEY_ID                   - App Store Connect API Key ID (e.g., 2X9R4HXF34)
#     ASC_ISSUER_ID                - App Store Connect Issuer ID (UUID)
#     ASC_PRIVATE_KEY_BASE64       - Base64-encoded AuthKey_<KEY_ID>.p8 file
#                                    (or raw content via ASC_PRIVATE_KEY)
#
# Optional Environment Variables:
#   MARKETING_VERSION              - App version string (e.g., 0.0.8)
#   BUILD_NUMBER                   - Monotonically increasing build number (e.g., github.run_number)
#   UPLOAD_TO_APP_STORE            - "true" (default) or "false" to skip upload and only produce .ipa
# ==============================================================================

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
IOS_DIR="$ROOT_DIR/iosApp"
BUILD_DIR="$ROOT_DIR/build/ios"
ARCHIVE_PATH="$BUILD_DIR/DawnChat.xcarchive"
EXPORT_PATH="$BUILD_DIR/ipa"
EXPORT_OPTIONS_PLIST="$BUILD_DIR/ExportOptions.plist"

SCHEME="DawnChat"
BUNDLE_ID="${BUNDLE_ID:-com.chillboiostudios.dawnchat}"
APPLE_TEAM_ID="${APPLE_TEAM_ID:-}"
MARKETING_VERSION="${MARKETING_VERSION:-0.0.8}"
BUILD_NUMBER="${BUILD_NUMBER:-8}"
UPLOAD_TO_APP_STORE="${UPLOAD_TO_APP_STORE:-true}"
CODE_SIGN_STYLE="${CODE_SIGN_STYLE:-Manual}"

echo "==> [1/7] Validating environment..."
if [[ "$(uname -s)" != "Darwin" ]]; then
  echo "Error: iOS compilation and signing requires macOS with Xcode installed." >&2
  exit 1
fi

if [[ -z "$APPLE_TEAM_ID" ]]; then
  echo "Error: APPLE_TEAM_ID is required (your 10-character Apple Developer Team ID)." >&2
  exit 1
fi

mkdir -p "$BUILD_DIR"

# Cleanup temporary keychain & API keys on exit
KEYCHAIN_PATH=""
PRIVATE_KEYS_DIR="$HOME/.appstoreconnect/private_keys"
cleanup() {
  if [[ -n "$KEYCHAIN_PATH" && -f "$KEYCHAIN_PATH" ]]; then
    echo "==> Cleaning up temporary signing keychain..."
    security delete-keychain "$KEYCHAIN_PATH" || true
  fi
}
trap cleanup EXIT

# ------------------------------------------------------------------------------
# Step 2: Set up Temporary Keychain, Certificate & Provisioning Profile
# ------------------------------------------------------------------------------
PROFILE_UUID=""
PROFILE_NAME=""

if [[ -n "${IOS_DIST_CERT_BASE64:-}" && -n "${IOS_PROV_PROFILE_BASE64:-}" ]]; then
  echo "==> [2/7] Installing Apple Distribution certificate & provisioning profile..."
  CODE_SIGN_STYLE="Manual"

  KEYCHAIN_PASSWORD="$(openssl rand -base64 24)"
  KEYCHAIN_PATH="$RUNNER_TEMP/dawnchat-signing.keychain-db"
  if [[ -z "${RUNNER_TEMP:-}" ]]; then
    KEYCHAIN_PATH="$BUILD_DIR/dawnchat-signing.keychain-db"
  fi

  CERT_PATH="$BUILD_DIR/dist_cert.p12"
  PROFILE_PATH="$BUILD_DIR/profile.mobileprovision"

  printf '%s' "$IOS_DIST_CERT_BASE64" | base64 --decode > "$CERT_PATH"
  printf '%s' "$IOS_PROV_PROFILE_BASE64" | base64 --decode > "$PROFILE_PATH"

  security create-keychain -p "$KEYCHAIN_PASSWORD" "$KEYCHAIN_PATH"
  security set-keychain-settings -lut 21600 "$KEYCHAIN_PATH"
  security unlock-keychain -p "$KEYCHAIN_PASSWORD" "$KEYCHAIN_PATH"

  # Add temporary keychain to the user search list
  EXISTING_KEYCHAINS=$(security list-keychains -d user | sed -e 's/^[[:space:]]*//' -e 's/"//g')
  # shellcheck disable=SC2086
  security list-keychains -d user -s "$KEYCHAIN_PATH" $EXISTING_KEYCHAINS

  security import "$CERT_PATH" \
    -P "${IOS_DIST_CERT_PASSWORD:-}" \
    -A \
    -t cert \
    -f pkcs12 \
    -k "$KEYCHAIN_PATH"

  security set-key-partition-list \
    -S apple-tool:,apple:,codesign: \
    -s -k "$KEYCHAIN_PASSWORD" \
    "$KEYCHAIN_PATH" >/dev/null

  # Extract UUID and Name from the provisioning profile
  PROFILE_PLIST="$BUILD_DIR/profile.plist"
  security cms -D -i "$PROFILE_PATH" > "$PROFILE_PLIST"
  PROFILE_UUID=$(/usr/libexec/PlistBuddy -c "Print UUID" "$PROFILE_PLIST")
  PROFILE_NAME=$(/usr/libexec/PlistBuddy -c "Print Name" "$PROFILE_PLIST")

  PROFILES_DIR="$HOME/Library/MobileDevice/Provisioning Profiles"
  mkdir -p "$PROFILES_DIR"
  cp "$PROFILE_PATH" "$PROFILES_DIR/$PROFILE_UUID.mobileprovision"

  echo "    Installed Provisioning Profile: '$PROFILE_NAME' ($PROFILE_UUID)"
  rm -f "$CERT_PATH" "$PROFILE_PATH" "$PROFILE_PLIST"
else
  echo "==> [2/7] Using Automatic Code Signing (no IOS_DIST_CERT_BASE64 provided)..."
  CODE_SIGN_STYLE="Automatic"
fi

# ------------------------------------------------------------------------------
# Step 3: Set up App Store Connect API Key (.p8)
# ------------------------------------------------------------------------------
ASC_KEY_FILEPATH=""
if [[ -n "${ASC_KEY_ID:-}" && ( -n "${ASC_PRIVATE_KEY_BASE64:-}" || -n "${ASC_PRIVATE_KEY:-}" ) ]]; then
  echo "==> [3/7] Configuring App Store Connect API Key ($ASC_KEY_ID)..."
  mkdir -p "$PRIVATE_KEYS_DIR"
  ASC_KEY_FILEPATH="$PRIVATE_KEYS_DIR/AuthKey_${ASC_KEY_ID}.p8"

  if [[ -n "${ASC_PRIVATE_KEY_BASE64:-}" ]]; then
    printf '%s' "$ASC_PRIVATE_KEY_BASE64" | base64 --decode > "$ASC_KEY_FILEPATH"
  else
    printf '%s\n' "$ASC_PRIVATE_KEY" > "$ASC_KEY_FILEPATH"
  fi
  chmod 600 "$ASC_KEY_FILEPATH"
else
  echo "==> [3/7] No App Store Connect API key provided; upload step will require credentials if enabled."
fi

# ------------------------------------------------------------------------------
# Step 4: Compile Kotlin Multiplatform Shared Framework for iOS (arm64 Release)
# ------------------------------------------------------------------------------
echo "==> [4/7] Compiling Kotlin Multiplatform ':shared' Release Framework (iosArm64)..."
cd "$ROOT_DIR"
chmod +x ./gradlew
./gradlew :shared:linkReleaseFrameworkIosArm64 --no-daemon

FRAMEWORK_OUTPUT="$ROOT_DIR/shared/build/bin/iosArm64/releaseFramework/DawnChatShared.framework"
if [[ ! -d "$FRAMEWORK_OUTPUT" ]]; then
  echo "Error: Expected framework not found at $FRAMEWORK_OUTPUT" >&2
  exit 1
fi
echo "    Built KMP framework: $FRAMEWORK_OUTPUT"

# ------------------------------------------------------------------------------
# Step 5: Generate Xcode Project via XcodeGen
# ------------------------------------------------------------------------------
echo "==> [5/7] Preparing Xcode project in $IOS_DIR..."
cd "$IOS_DIR"

# Update Config.xcconfig with current version, build number, bundle ID, and team ID
cat > "$IOS_DIR/Configuration/Config.xcconfig" <<EOF
TEAM_ID=${APPLE_TEAM_ID}
PRODUCT_NAME=DawnChat
PRODUCT_BUNDLE_IDENTIFIER=${BUNDLE_ID}
CURRENT_PROJECT_VERSION=${BUILD_NUMBER}
MARKETING_VERSION=${MARKETING_VERSION}
EOF

if ! command -v xcodegen >/dev/null 2>&1; then
  echo "    Installing xcodegen via Homebrew..."
  brew install xcodegen
fi

xcodegen generate --spec "$IOS_DIR/project.yml"

# ------------------------------------------------------------------------------
# Step 6: Archive & Export Signed .ipa
# ------------------------------------------------------------------------------
echo "==> [6/7] Archiving and signing DawnChat ($MARKETING_VERSION+$BUILD_NUMBER)..."

ARCHIVE_ARGS=(
  -project "$IOS_DIR/DawnChat.xcodeproj"
  -scheme "$SCHEME"
  -configuration Release
  -destination "generic/platform=iOS"
  -archivePath "$ARCHIVE_PATH"
  DEVELOPMENT_TEAM="$APPLE_TEAM_ID"
  PRODUCT_BUNDLE_IDENTIFIER="$BUNDLE_ID"
  MARKETING_VERSION="$MARKETING_VERSION"
  CURRENT_PROJECT_VERSION="$BUILD_NUMBER"
)

if [[ "$CODE_SIGN_STYLE" == "Manual" ]]; then
  ARCHIVE_ARGS+=(
    CODE_SIGN_STYLE="Manual"
    CODE_SIGN_IDENTITY="Apple Distribution"
    PROVISIONING_PROFILE_SPECIFIER="$PROFILE_NAME"
  )
else
  ARCHIVE_ARGS+=(
    CODE_SIGN_STYLE="Automatic"
    -allowProvisioningUpdates
  )
  if [[ -n "$ASC_KEY_FILEPATH" ]]; then
    ARCHIVE_ARGS+=(
      -authenticationKeyPath "$ASC_KEY_FILEPATH"
      -authenticationKeyID "$ASC_KEY_ID"
      -authenticationKeyIssuerID "$ASC_ISSUER_ID"
    )
  fi
fi

SKIP_KMP_GRADLE_BUILD=true xcodebuild archive "${ARCHIVE_ARGS[@]}"

# Generate ExportOptions.plist
if [[ "$CODE_SIGN_STYLE" == "Manual" ]]; then
  cat > "$EXPORT_OPTIONS_PLIST" <<EOF
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
	<key>method</key>
	<string>app-store-connect</string>
	<key>signingStyle</key>
	<string>manual</string>
	<key>teamID</key>
	<string>${APPLE_TEAM_ID}</string>
	<key>uploadSymbols</key>
	<true/>
	<key>stripSwiftSymbols</key>
	<true/>
	<key>provisioningProfiles</key>
	<dict>
		<key>${BUNDLE_ID}</key>
		<string>${PROFILE_NAME}</string>
	</dict>
</dict>
</plist>
EOF
else
  cat > "$EXPORT_OPTIONS_PLIST" <<EOF
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
	<key>method</key>
	<string>app-store-connect</string>
	<key>signingStyle</key>
	<string>automatic</string>
	<key>teamID</key>
	<string>${APPLE_TEAM_ID}</string>
	<key>uploadSymbols</key>
	<true/>
	<key>stripSwiftSymbols</key>
	<true/>
</dict>
</plist>
EOF
fi

echo "==> Exporting signed .ipa..."
EXPORT_ARGS=(
  -exportArchive
  -archivePath "$ARCHIVE_PATH"
  -exportOptionsPlist "$EXPORT_OPTIONS_PLIST"
  -exportPath "$EXPORT_PATH"
)

if [[ "$CODE_SIGN_STYLE" == "Automatic" ]]; then
  EXPORT_ARGS+=(-allowProvisioningUpdates)
  if [[ -n "$ASC_KEY_FILEPATH" ]]; then
    EXPORT_ARGS+=(
      -authenticationKeyPath "$ASC_KEY_FILEPATH"
      -authenticationKeyID "$ASC_KEY_ID"
      -authenticationKeyIssuerID "$ASC_ISSUER_ID"
    )
  fi
fi

xcodebuild "${EXPORT_ARGS[@]}"

IPA_FILE=$(find "$EXPORT_PATH" -name "*.ipa" -print -quit)
if [[ -z "$IPA_FILE" ]]; then
  echo "Error: Failed to locate exported .ipa in $EXPORT_PATH" >&2
  exit 1
fi
echo "    Signed IPA ready at: $IPA_FILE"

# ------------------------------------------------------------------------------
# Step 7: Upload to Apple App Store Connect / TestFlight
# ------------------------------------------------------------------------------
if [[ "$UPLOAD_TO_APP_STORE" == "true" ]]; then
  echo "==> [7/7] Uploading $IPA_FILE to Apple App Store Connect..."
  if [[ -z "${ASC_KEY_ID:-}" || -z "${ASC_ISSUER_ID:-}" || -z "$ASC_KEY_FILEPATH" ]]; then
    echo "Error: ASC_KEY_ID, ASC_ISSUER_ID, and ASC_PRIVATE_KEY_BASE64 are required to upload to App Store Connect." >&2
    exit 1
  fi

  xcrun altool --upload-app \
    --type ios \
    --file "$IPA_FILE" \
    --apiKey "$ASC_KEY_ID" \
    --apiIssuer "$ASC_ISSUER_ID" \
    --verbose

  echo "==> Successfully uploaded DawnChat ($MARKETING_VERSION+$BUILD_NUMBER) to App Store Connect!"
else
  echo "==> [7/7] Skipping App Store Connect upload (UPLOAD_TO_APP_STORE=$UPLOAD_TO_APP_STORE)."
fi
