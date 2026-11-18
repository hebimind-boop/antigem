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
     * Checks if the binary for the given [agent] exists on disk.
     */
    fun isCliInstalled(context: Context, agent: AgentKind): Boolean {
        if (AuthPreferences.currentRemoteMode) {
            return when (agent) {
                AgentKind.AGY -> com.example.gemini.data.remote.BridgeInfo.agyCli.value?.installed == true
                else -> true
            }
        }
        val isTermux = LocalEnvironmentManager.isTermuxPackage(context)
        return if (isTermux) {
            val home = LocalEnvironmentManager.getHomeDir(context)
            val prefix = LocalEnvironmentManager.getPrefixDir(context)
            when (agent) {
                AgentKind.AGY -> File(home, ".local/bin/agy").exists() || File(prefix, "bin/agy").exists()
                AgentKind.CLAUDE -> File(home, ".local/bin/claude").exists() || File(prefix, "bin/claude").exists()
                AgentKind.OPENCODE -> File(home, ".opencode/bin/opencode").exists() || File(home, ".local/bin/opencode").exists() || File(prefix, "bin/opencode").exists()
                AgentKind.CODEX -> File(prefix, "bin/codex").exists() || File(home, ".local/bin/codex").exists()
            }
        } else {
            val ubuntuDir = LocalEnvironmentManager.getUbuntuRootDir(context)
            if (!ubuntuDir.exists()) return false
            when (agent) {
                AgentKind.AGY -> {
                    File(ubuntuDir, "root/.local/bin/agy").exists() ||
                    File(ubuntuDir, "usr/local/bin/agy").exists() ||
                    File(ubuntuDir, "usr/bin/agy").exists()
                }
                AgentKind.CLAUDE -> {
                    File(ubuntuDir, "root/.local/bin/claude").exists() ||
                    File(ubuntuDir, "root/.claude/local/claude").exists() ||
                    File(ubuntuDir, "usr/local/bin/claude").exists() ||
                    File(ubuntuDir, "usr/bin/claude").exists()
                }
                AgentKind.OPENCODE -> {
                    File(ubuntuDir, "root/.opencode/bin/opencode").exists() ||
                    File(ubuntuDir, "root/.local/bin/opencode").exists() ||
                    File(ubuntuDir, "usr/local/bin/opencode").exists() ||
                    File(ubuntuDir, "usr/bin/opencode").exists()
                }
                AgentKind.CODEX -> {
                    File(ubuntuDir, "root/.local/bin/codex").exists() ||
                    File(ubuntuDir, "root/.codex/packages/standalone/current/bin/codex").exists() ||
                    File(ubuntuDir, "usr/local/bin/codex").exists() ||
                    File(ubuntuDir, "usr/bin/codex").exists()
                }
            }
        }
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
     * Generates the shell install command for the specified [agent].
     */
    fun getInstallCommand(context: Context, agent: AgentKind): String {
        val isTermux = LocalEnvironmentManager.isTermuxPackage(context)
        return if (isTermux) {
            when (agent) {
                AgentKind.AGY -> "curl -fsSL https://antigravity.google/cli/install.sh | bash"
                AgentKind.CLAUDE -> "curl -fsSL https://claude.ai/install.sh | bash"
                AgentKind.OPENCODE -> "curl -fsSL https://opencode.ai/v2/install | bash"
                AgentKind.CODEX -> "pkg install -y codex || (curl -fsSL https://github.com/openai/codex/releases/latest/download/install.sh | bash)"
            }
        } else {
            // Inside PRoot Ubuntu container
            val curlGuard = "if ! command -v curl >/dev/null 2>&1 || ! command -v tar >/dev/null 2>&1; then apt-get update && apt-get install -y curl ca-certificates tar; fi"
            when (agent) {
                AgentKind.AGY -> "$curlGuard && mkdir -p /root/.local/bin && curl -fsSL https://antigravity.google/cli/install.sh | bash"
                AgentKind.CLAUDE -> "$curlGuard && mkdir -p /root/.local/bin && curl -fsSL https://claude.ai/install.sh | bash"
                AgentKind.OPENCODE -> "$curlGuard && mkdir -p /root/.opencode/bin && curl -fsSL https://opencode.ai/v2/install | bash"
                AgentKind.CODEX -> "$curlGuard && mkdir -p /root/.local/bin && (curl -fsSL https://github.com/openai/codex/releases/latest/download/install.sh | bash || { ARCH=\$(uname -m); if [ \"\$ARCH\" = \"aarch64\" ]; then TARGET=\"aarch64-unknown-linux-musl\"; else TARGET=\"x86_64-unknown-linux-musl\"; fi; curl -fsSL \"https://github.com/openai/codex/releases/latest/download/codex-\$TARGET.tar.gz\" | tar -xz -C /root/.local/bin && chmod +x /root/.local/bin/codex; })"
            }
        }
    }

    /**
     * Installs the CLI for [agent] inside the local container and streams log output.
     */
    suspend fun installAgent(
        context: Context,
        agent: AgentKind,
        onLogLine: (String) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        val cmd = getInstallCommand(context, agent)
        onLogLine("📦 Starting installation for ${agent.name}...")
        onLogLine("$ $cmd\n")
        val result = LocalEnvironmentManager.executeCommand(
            command = cmd,
            context = context,
            timeoutSeconds = 300,
            onOutputLine = onLogLine
        )
        val success = result.exitCode == 0 || isCliInstalled(context, agent)
        if (success) {
            onLogLine("✅ ${agent.name} CLI installed successfully!\n")
        } else {
            onLogLine("❌ ${agent.name} installation finished with exit code ${result.exitCode}.\n")
        }
        success
    }
}
