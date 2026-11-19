package com.example.gemini.data.agent

import android.content.Context
import android.util.Log
import com.example.gemini.data.local.LocalEnvironmentManager
import com.example.gemini.data.preferences.AuthPreferences
import com.example.gemini.domain.model.AgentKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Manages checking, installing, and status detection for AI agent CLIs (agy, claude, opencode, codex)
 * inside the local Linux environment (PRoot Ubuntu) or Termux.
 */
object AgentCliManager {
    private const val TAG = "AgentCliManager"

    data class AgentItem(
        val agent: AgentKind,
        val title: String,
        val commandName: String,
        val description: String,
        val isDefaultSelected: Boolean = false
    )

    val AGENT_ITEMS = listOf(
        AgentItem(
            agent = AgentKind.AGY,
            title = "Antigravity (agy)",
            commandName = "agy",
            description = "Google's coding agent through the LimitX Hub: Gemini and Claude models on your Google plan.",
            isDefaultSelected = true
        ),
        AgentItem(
            agent = AgentKind.CLAUDE,
            title = "Claude Code (claude)",
            commandName = "claude",
            description = "Anthropic's Claude Code CLI on this device: your Claude subscription or Anthropic Console account."
        ),
        AgentItem(
            agent = AgentKind.OPENCODE,
            title = "OpenCode (opencode)",
            commandName = "opencode",
            description = "Open-source coding agent: 200+ providers with your own keys, any OpenAI-compatible API, and free models."
        ),
        AgentItem(
            agent = AgentKind.CODEX,
            title = "Codex (codex)",
            commandName = "codex",
            description = "OpenAI's Codex CLI on this device: sign in with your ChatGPT account (or an API key)."
        )
    )

    /**
     * Returns all potential file locations on disk where the [agent]'s CLI binary might reside.
     */
    fun getCandidatePaths(context: Context, agent: AgentKind): List<File> {
        val binaryName = when (agent) {
            AgentKind.AGY -> "agy"
            AgentKind.CLAUDE -> "claude"
            AgentKind.OPENCODE -> "opencode"
            AgentKind.CODEX -> "codex"
        }
        val isTermux = LocalEnvironmentManager.isTermuxPackage(context)
        return if (isTermux) {
            val home = LocalEnvironmentManager.getHomeDir(context)
            val prefix = LocalEnvironmentManager.getPrefixDir(context)
            listOf(
                File(home, ".local/bin/$binaryName"),
                File(prefix, "bin/$binaryName"),
                File(home, ".opencode/bin/$binaryName"),
                File(home, ".bun/bin/$binaryName"),
                File(home, ".cargo/bin/$binaryName"),
                File(home, ".claude/local/$binaryName"),
                File(home, ".antigravity/bin/$binaryName"),
                File(home, ".gemini/bin/$binaryName"),
                File(home, "usr/bin/$binaryName"),
                File(home, "bin/$binaryName")
            )
        } else {
            val ubuntuDir = LocalEnvironmentManager.getUbuntuRootDir(context)
            if (!ubuntuDir.exists()) return emptyList()
            listOf(
                File(ubuntuDir, "root/.local/bin/$binaryName"),
                File(ubuntuDir, "usr/local/bin/$binaryName"),
                File(ubuntuDir, "usr/bin/$binaryName"),
                File(ubuntuDir, "bin/$binaryName"),
                File(ubuntuDir, "root/.opencode/bin/$binaryName"),
                File(ubuntuDir, "root/.bun/bin/$binaryName"),
                File(ubuntuDir, "root/.cargo/bin/$binaryName"),
                File(ubuntuDir, "root/.npm-global/bin/$binaryName"),
                File(ubuntuDir, "root/.claude/local/$binaryName"),
                File(ubuntuDir, "root/.codex/packages/standalone/current/bin/$binaryName"),
                File(ubuntuDir, "root/.antigravity/bin/$binaryName"),
                File(ubuntuDir, "root/.gemini/bin/$binaryName"),
                File(ubuntuDir, "antigem/bin/$binaryName"),
                File(ubuntuDir, "boopide/bin/$binaryName")
            )
        }
    }

    /**
     * Checks if the binary for the given [agent] exists on disk across all common install locations.
     */
    fun isCliInstalled(context: Context, agent: AgentKind): Boolean {
        if (AuthPreferences.currentRemoteMode) {
            return when (agent) {
                AgentKind.AGY -> com.example.gemini.data.remote.BridgeInfo.agyCli.value?.installed == true
                else -> true
            }
        }
        return getCandidatePaths(context, agent).any { it.exists() }
    }

    /**
     * Actively checks if the binary is accessible via container PATH (`command -v` or `which`).
     */
    suspend fun checkCliInstalledViaContainer(context: Context, agent: AgentKind): Boolean {
        if (isCliInstalled(context, agent)) return true
        val binaryName = when (agent) {
            AgentKind.AGY -> "agy"
            AgentKind.CLAUDE -> "claude"
            AgentKind.OPENCODE -> "opencode"
            AgentKind.CODEX -> "codex"
        }
        val res = LocalEnvironmentManager.executeCommand(
            command = "command -v $binaryName || which $binaryName",
            context = context,
            timeoutSeconds = 5
        )
        return res.exitCode == 0 && res.output.isNotBlank()
    }

    /**
     * Checks if initial agent selection or CLI installation is still pending.
     */
    fun isInitialSetupNeeded(context: Context): Boolean {
        if (AuthPreferences.currentRemoteMode) {
            return !AgentPreferences.hasChosenSync(context)
        }
        if (!LocalEnvironmentManager.isInstalled(context)) {
            return false
        }
        if (!AgentPreferences.hasChosenSync(context)) {
            return true
        }
        val enabled = AgentPreferences.enabledSync(context)
        if (enabled.isEmpty()) return true
        for (agent in enabled) {
            if (!isCliInstalled(context, agent)) {
                return true
            }
        }
        return false
    }

    /**
     * Generates the shell install command for the specified [agent], with automated tool checks
     * and direct archive download fallbacks.
     */
    fun getInstallCommand(context: Context, agent: AgentKind): String {
        val isTermux = LocalEnvironmentManager.isTermuxPackage(context)
        return if (isTermux) {
            when (agent) {
                AgentKind.AGY -> "mkdir -p \"\$HOME/.local/bin\" \"\$PREFIX/bin\" && curl -fsSLk https://antigravity.google/cli/install.sh | bash"
                AgentKind.CLAUDE -> "mkdir -p \"\$HOME/.local/bin\" \"\$PREFIX/bin\" && (curl -fsSLk https://claude.ai/install.sh | bash || npm install -g @anthropic-ai/claude-code)"
                AgentKind.OPENCODE -> "mkdir -p \"\$HOME/.opencode/bin\" \"\$PREFIX/bin\" && (curl -fsSLk https://opencode.ai/v2/install | bash || npm install -g opencode-ai)"
                AgentKind.CODEX -> "pkg install -y codex || (curl -fsSLk https://github.com/openai/codex/releases/latest/download/install.sh | bash)"
            }
        } else {
            // Inside PRoot Ubuntu container: resilient tool installation and multi-step fallbacks
            val toolGuard = "(command -v curl >/dev/null 2>&1 && command -v tar >/dev/null 2>&1 && command -v gzip >/dev/null 2>&1) || (export DEBIAN_FRONTEND=noninteractive; (apt-get update -y || true) && (apt-get install -y --no-install-recommends curl ca-certificates tar gzip || true))"
            when (agent) {
                AgentKind.AGY -> {
                    """
                    $toolGuard
                    mkdir -p /root/.local/bin /usr/local/bin
                    export PATH="/root/.local/bin:/usr/local/bin:${'$'}PATH"
                    if ! (curl -fsSLk https://antigravity.google/cli/install.sh | bash); then
                        echo "Official install script failed. Downloading release binary directly..."
                        ARCH=${'$'}(uname -m)
                        if [ "${'$'}ARCH" = "aarch64" ] || [ "${'$'}ARCH" = "arm64" ]; then
                            PLATFORM="linux_arm64"
                            FALLBACK_URL="https://storage.googleapis.com/antigravity-public/antigravity-cli/1.5.12-5281398513336320/linux-arm/cli_linux_arm64.tar.gz"
                        else
                            PLATFORM="linux_amd64"
                            FALLBACK_URL="https://storage.googleapis.com/antigravity-public/antigravity-cli/1.5.12-5281398513336320/linux-x64/cli_linux_amd64.tar.gz"
                        fi
                        MANIFEST_URL="https://antigravity-cli-auto-updater-974169037036.us-central1.run.app/manifests/${'$'}PLATFORM.json"
                        URL=${'$'}(curl -fsSLk "${'$'}MANIFEST_URL" 2>/dev/null | grep -o '"url": *"[^"]*"' | head -n1 | cut -d'"' -f4)
                        if [ -z "${'$'}URL" ]; then URL="${'$'}FALLBACK_URL"; fi
                        echo "Downloading from ${'$'}URL..."
                        curl -fsSLk "${'$'}URL" -o /tmp/agy.tar.gz
                        tar -xzf /tmp/agy.tar.gz -C /root/.local/bin/ antigravity 2>/dev/null || tar -xzf /tmp/agy.tar.gz -C /root/.local/bin/ agy 2>/dev/null || tar -xzf /tmp/agy.tar.gz -C /root/.local/bin/
                        if [ -f /root/.local/bin/antigravity ] && [ ! -f /root/.local/bin/agy ]; then
                            mv -f /root/.local/bin/antigravity /root/.local/bin/agy
                        fi
                        chmod +x /root/.local/bin/agy 2>/dev/null || true
                        rm -f /tmp/agy.tar.gz
                    fi
                    if [ -f /root/.local/bin/agy ]; then
                        cp -f /root/.local/bin/agy /usr/local/bin/agy 2>/dev/null || true
                        chmod +x /root/.local/bin/agy /usr/local/bin/agy 2>/dev/null || true
                    elif [ -f /usr/local/bin/agy ]; then
                        cp -f /usr/local/bin/agy /root/.local/bin/agy 2>/dev/null || true
                        chmod +x /root/.local/bin/agy /usr/local/bin/agy 2>/dev/null || true
                    fi
                    command -v agy || [ -f /root/.local/bin/agy ] || [ -f /usr/local/bin/agy ]
                    """.trimIndent()
                }
                AgentKind.CLAUDE -> {
                    """
                    $toolGuard
                    mkdir -p /root/.local/bin /usr/local/bin
                    export PATH="/root/.local/bin:/usr/local/bin:${'$'}PATH"
                    if ! (curl -fsSLk https://claude.ai/install.sh | bash); then
                        echo "Official install script failed. Attempting npm fallback..."
                        if command -v npm >/dev/null 2>&1; then
                            npm install -g @anthropic-ai/claude-code || true
                        fi
                    fi
                    if [ -f /root/.local/bin/claude ]; then
                        cp -f /root/.local/bin/claude /usr/local/bin/claude 2>/dev/null || true
                        chmod +x /root/.local/bin/claude /usr/local/bin/claude 2>/dev/null || true
                    fi
                    command -v claude || [ -f /root/.local/bin/claude ] || [ -f /usr/local/bin/claude ]
                    """.trimIndent()
                }
                AgentKind.OPENCODE -> {
                    """
                    $toolGuard
                    mkdir -p /root/.local/bin /usr/local/bin /root/.opencode/bin
                    export PATH="/root/.local/bin:/usr/local/bin:/root/.opencode/bin:${'$'}PATH"
                    if ! (curl -fsSLk https://opencode.ai/v2/install | bash); then
                        echo "Official install script failed. Attempting npm fallback..."
                        if command -v npm >/dev/null 2>&1; then
                            npm install -g opencode-ai || npm install -g @opencode/cli || true
                        fi
                    fi
                    if [ -f /root/.opencode/bin/opencode ] && [ ! -f /root/.local/bin/opencode ]; then
                        cp -f /root/.opencode/bin/opencode /root/.local/bin/opencode 2>/dev/null || true
                    fi
                    if [ -f /root/.local/bin/opencode ]; then
                        cp -f /root/.local/bin/opencode /usr/local/bin/opencode 2>/dev/null || true
                        chmod +x /root/.local/bin/opencode /usr/local/bin/opencode 2>/dev/null || true
                    fi
                    command -v opencode || [ -f /root/.opencode/bin/opencode ] || [ -f /root/.local/bin/opencode ] || [ -f /usr/local/bin/opencode ]
                    """.trimIndent()
                }
                AgentKind.CODEX -> {
                    """
                    $toolGuard
                    mkdir -p /root/.local/bin /usr/local/bin
                    export PATH="/root/.local/bin:/usr/local/bin:${'$'}PATH"
                    if ! (curl -fsSLk https://github.com/openai/codex/releases/latest/download/install.sh | bash); then
                        echo "Official install script failed. Attempting release archive fallback..."
                        ARCH=${'$'}(uname -m)
                        if [ "${'$'}ARCH" = "aarch64" ] || [ "${'$'}ARCH" = "arm64" ]; then
                            TARGET="aarch64-unknown-linux-musl"
                        else
                            TARGET="x86_64-unknown-linux-musl"
                        fi
                        curl -fsSLk "https://github.com/openai/codex/releases/latest/download/codex-${'$'}TARGET.tar.gz" -o /tmp/codex.tar.gz || true
                        if [ -f /tmp/codex.tar.gz ]; then
                            tar -xzf /tmp/codex.tar.gz -C /root/.local/bin 2>/dev/null || true
                            rm -f /tmp/codex.tar.gz
                        fi
                    fi
                    if [ -f /root/.local/bin/codex ]; then
                        cp -f /root/.local/bin/codex /usr/local/bin/codex 2>/dev/null || true
                        chmod +x /root/.local/bin/codex /usr/local/bin/codex 2>/dev/null || true
                    fi
                    command -v codex || [ -f /root/.local/bin/codex ] || [ -f /usr/local/bin/codex ]
                    """.trimIndent()
                }
            }
        }
    }

    data class InstallResult(
        val success: Boolean,
        val exitCode: Int,
        val output: String,
        val errorMessage: String? = null
    )

    /**
     * Installs the CLI for [agent] inside the local container and streams log output.
     */
    suspend fun installAgent(
        context: Context,
        agent: AgentKind,
        onLogLine: (String) -> Unit
    ): InstallResult = withContext(Dispatchers.IO) {
        val cmd = getInstallCommand(context, agent)
        onLogLine("📦 Starting installation for ${agent.name}...")
        onLogLine("$ $cmd\n")
        val result = LocalEnvironmentManager.executeCommand(
            command = cmd,
            context = context,
            timeoutSeconds = 300,
            onOutputLine = onLogLine
        )
        val installed = isCliInstalled(context, agent) || checkCliInstalledViaContainer(context, agent)
        val success = installed || result.exitCode == 0
        if (success) {
            onLogLine("✅ ${agent.name} CLI installed successfully!\n")
            InstallResult(success = true, exitCode = result.exitCode, output = result.output)
        } else {
            val err = "Installation failed (exit code ${result.exitCode}): ${agent.name} binary not found after running installer."
            onLogLine("❌ $err\n")
            InstallResult(
                success = false,
                exitCode = result.exitCode,
                output = result.output,
                errorMessage = err
            )
        }
    }
}
