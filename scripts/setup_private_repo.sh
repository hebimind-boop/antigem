#!/usr/bin/env bash
set -euo pipefail

# ==============================================================================
# LimitX: Standalone Private Repository Setup Helper
# ==============================================================================
# This script guides you through migrating your LimitX codebase to a brand-new,
# completely private, standalone GitHub repository—without any upstream fork links
# and without breaking your current release/keystore setup.
# ==============================================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"

echo "================================================================="
echo "🔒 LimitX Standalone Private Repository Setup"
echo "================================================================="

cd "${ROOT_DIR}"

# 1. Keystore Extraction for GitHub Actions Secrets
echo ""
echo "🔑 Step 1: Keystore Secret (Matches Your Current App Signature)"
echo "-----------------------------------------------------------------"
if [[ -f "${ROOT_DIR}/app/release.jks" ]]; then
    KEYSTORE_B64=$(base64 -w 0 "${ROOT_DIR}/app/release.jks" 2>/dev/null || base64 "${ROOT_DIR}/app/release.jks" | tr -d '\n')
    echo "✅ Found app/release.jks!"
    echo "In your new GitHub repository -> Settings -> Secrets and variables -> Actions:"
    echo "Add the following repository secrets:"
    echo ""
    echo "  1. RELEASE_KEYSTORE_BASE64"
    echo "     (Copy the Base64 value printed below):"
    echo "-----------------------------------------------------------------"
    echo "${KEYSTORE_B64}"
    echo "-----------------------------------------------------------------"
    echo "  2. RELEASE_KEYSTORE_PASSWORD = 123456"
    echo "  3. RELEASE_KEY_ALIAS         = antigem"
    echo "  4. RELEASE_KEY_PASSWORD      = 123456"
    echo ""
    echo "💡 Having this exact keystore ensures that APKs built by your new"
    echo "   private repo will have the EXACT same cryptographic signature,"
    echo "   so users won't get an 'App not installed / signature mismatch' error!"
else
    echo "⚠️ Warning: app/release.jks not found. Run a build to generate one."
fi

# 2. Creating New Private Standalone Repository
echo ""
echo "🚀 Step 2: Create Your New Standalone Private Repository on GitHub"
echo "-----------------------------------------------------------------"
echo "Option A (Using GitHub CLI in terminal):"
echo "  gh repo create my-private-limitx --private --source=. --remote=private"
echo ""
echo "Option B (Using GitHub Web UI):"
echo "  1. Go to https://github.com/new"
echo "  2. Enter repository name (e.g. 'limitx-studio' or 'limitx-engine')"
echo "  3. Select 'Private' (Do NOT initialize with README/license/gitignore)"
echo "  4. Click 'Create repository'"
echo "  5. In this terminal, run:"
echo "     git remote add private git@github.com:<YOUR_USERNAME>/<NEW_REPO_NAME>.git"
echo "     git push private master --tags"

# 3. Public Releases vs Private Source Architecture
echo ""
echo "📦 Step 3: Understanding In-App Updates with a Private Codebase"
echo "-----------------------------------------------------------------"
echo "When your source code repository is Private, unauthorized end-users"
echo "cannot download releases from a private repo without a GitHub token."
echo ""
echo "Recommended Pro Architecture:"
echo "  • Repo A (PRIVATE): houses all your source code, commits, and secrets."
echo "  • Repo B (PUBLIC - e.g. limitx-releases): houses ONLY compiled APKs and version.json."
echo ""
echo "Whenever you are ready to point in-app updates to a new repository:"
echo "  Update DEFAULT_VERSION_URL and primaryDownloadUrl in:"
echo "  app/src/main/java/com/example/gemini/data/updater/AppUpdateManager.kt"
echo "================================================================="
