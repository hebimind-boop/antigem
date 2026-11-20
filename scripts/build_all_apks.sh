#!/usr/bin/env bash
set -euo pipefail

# -----------------------------------------------------------------------------
# BoopIDE Multi-Package APK Build & Output Packager
# Builds standard (com.boopide) and termux (com.termux) flavors.
# -----------------------------------------------------------------------------

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
OUTPUT_DIR="${ROOT_DIR}/output"

MODE="debug"
if [[ "${1:-}" == "--release" || "${1:-}" == "-r" ]]; then
    MODE="release"
fi

echo "================================================================="
echo "🚀 Building BoopIDE Packages in [${MODE^^}] mode"
echo "================================================================="

# Extract version from version.json
VERSION_NAME="1.0.0"
VERSION_CODE="1"
if [[ -f "${ROOT_DIR}/version.json" ]]; then
    VERSION_NAME=$(python3 -c "import json; print(json.load(open('${ROOT_DIR}/version.json')).get('version_name', '1.0.0'))" 2>/dev/null || echo "1.0.0")
    VERSION_CODE=$(python3 -c "import json; print(json.load(open('${ROOT_DIR}/version.json')).get('version_code', 1))" 2>/dev/null || echo "1")
fi

echo "📦 Version Name: v${VERSION_NAME} (Code: ${VERSION_CODE})"
mkdir -p "${OUTPUT_DIR}"

# -----------------------------------------------------------------------------
# Pre-bundle Antigravity CLI (agy) into APK assets
# -----------------------------------------------------------------------------
ASSETS_DIR="${ROOT_DIR}/app/src/main/assets"
mkdir -p "${ASSETS_DIR}"

if [[ ! -f "${ASSETS_DIR}/agy_arm64.zip" && ! -f "${ASSETS_DIR}/agy.zip" ]]; then
    echo "⬇️  Checking Antigravity CLI (agy) pre-bundled asset..."
    GCS_URL="https://storage.googleapis.com/antigravity-public/antigravity-cli/1.5.13-5999302598852608/linux-arm/cli_linux_arm64.tar.gz"
    MANIFEST_URL="https://antigravity-cli-auto-updater-974169037036.us-central1.run.app/manifests/linux_arm64.json"

    TMP_DIR=$(mktemp -d)
    TARBALL_URL="${GCS_URL}"
    RESOLVED_URL=$(curl -sL --max-time 10 "${MANIFEST_URL}" 2>/dev/null | grep -o 'https://[^"]*cli_linux_arm64.tar.gz' || true)
    if [[ -n "${RESOLVED_URL}" ]]; then
        TARBALL_URL="${RESOLVED_URL}"
    fi

    echo "⬇️  Downloading agy ARM64 binary from: ${TARBALL_URL}"
    if curl -sSL --max-time 300 "${TARBALL_URL}" -o "${TMP_DIR}/cli.tar.gz"; then
        echo "📦 Extracting tarball..."
        tar -xzf "${TMP_DIR}/cli.tar.gz" -C "${TMP_DIR}"
        AGY_BIN=$(find "${TMP_DIR}" -type f \( -name "antigravity" -o -name "agy" \) | head -n 1)
        if [[ -n "${AGY_BIN}" && -f "${AGY_BIN}" ]]; then
            echo "📦 Found binary: ${AGY_BIN} ($(wc -c < "${AGY_BIN}") bytes)"
            echo "📦 Creating compressed assets/agy_arm64.zip..."
            (cd "$(dirname "${AGY_BIN}")" && zip -9 "${ASSETS_DIR}/agy_arm64.zip" "$(basename "${AGY_BIN}")")
            ls -lh "${ASSETS_DIR}/agy_arm64.zip"
        else
            echo "⚠️ Could not find agy binary in tarball, continuing build."
        fi
    else
        echo "⚠️ Failed to download agy tarball, continuing build."
    fi
    rm -rf "${TMP_DIR}"
else
    echo "✅ Pre-bundled Antigravity CLI asset already present: $(ls -lh ${ASSETS_DIR}/agy* 2>/dev/null)"
fi

if [[ "${MODE}" == "release" ]]; then
    echo "🔨 Compiling Standard Release (clean :app:assembleStandardRelease)..."
    "${ROOT_DIR}/gradlew" -p "${ROOT_DIR}" clean :app:assembleStandardRelease

    # echo "🔨 Compiling Termux Release (:app:assembleTermuxRelease)..."
    # "${ROOT_DIR}/gradlew" -p "${ROOT_DIR}" :app:assembleTermuxRelease

    # echo "🔨 Compiling Termux Shared Release (:app:assembleTermuxSharedRelease)..."
    # "${ROOT_DIR}/gradlew" -p "${ROOT_DIR}" :app:assembleTermuxSharedRelease

    APKS_SRC="${ROOT_DIR}/app/build/outputs/apk"
    find "${APKS_SRC}/standard/release" -name "*.apk" -exec cp {} "${OUTPUT_DIR}/LimitX-standard-v${VERSION_NAME}-release.apk" \;
    find "${APKS_SRC}/standard/release" -name "*.apk" -exec cp {} "${OUTPUT_DIR}/BoopIDE-standard-v${VERSION_NAME}-release.apk" \;
    # find "${APKS_SRC}/termux/release" -name "*.apk" -exec cp {} "${OUTPUT_DIR}/LimitX-termux-v${VERSION_NAME}-release.apk" \;
    # find "${APKS_SRC}/termuxShared/release" -name "*.apk" -exec cp {} "${OUTPUT_DIR}/LimitX-termux-shared-v${VERSION_NAME}-release.apk" \;

    # if [[ -f "${ROOT_DIR}/app/src/main/assets/bin/agy_ide_bridge" ]]; then
    #     cp "${ROOT_DIR}/app/src/main/assets/bin/agy_ide_bridge" "${OUTPUT_DIR}/agy_ide_bridge-v${VERSION_NAME}-android-arm64"
    # fi
else
    echo "🔨 Compiling Standard Debug (:app:assembleStandardDebug)..."
    "${ROOT_DIR}/gradlew" -p "${ROOT_DIR}" :app:assembleStandardDebug

    echo "🔨 Compiling Termux Debug (:app:assembleTermuxDebug)..."
    "${ROOT_DIR}/gradlew" -p "${ROOT_DIR}" :app:assembleTermuxDebug

    echo "🔨 Compiling Termux Shared Debug (:app:assembleTermuxSharedDebug)..."
    "${ROOT_DIR}/gradlew" -p "${ROOT_DIR}" :app:assembleTermuxSharedDebug

    APKS_SRC="${ROOT_DIR}/app/build/outputs/apk"
    find "${APKS_SRC}/standard/debug" -name "*.apk" -exec cp {} "${OUTPUT_DIR}/LimitX-standard-v${VERSION_NAME}-debug.apk" \;
    find "${APKS_SRC}/termux/debug" -name "*.apk" -exec cp {} "${OUTPUT_DIR}/LimitX-termux-v${VERSION_NAME}-debug.apk" \;
    find "${APKS_SRC}/termuxShared/debug" -name "*.apk" -exec cp {} "${OUTPUT_DIR}/LimitX-termux-shared-v${VERSION_NAME}-debug.apk" \;

    # the APK ships the bridge xz-compressed; the uncompressed build is kept under app/build
    if [[ -f "${ROOT_DIR}/app/build/intermediates/agyIdeBridge/agy_ide_bridge" ]]; then
        cp "${ROOT_DIR}/app/build/intermediates/agyIdeBridge/agy_ide_bridge" "${OUTPUT_DIR}/agy_ide_bridge-v${VERSION_NAME}-android-arm64"
    fi
fi

echo ""
echo "================================================================="
echo "✅ Build Complete! Generated Artifacts in: ${OUTPUT_DIR}"
echo "================================================================="
ls -lh "${OUTPUT_DIR}"/* 2>/dev/null || true
echo "================================================================="
