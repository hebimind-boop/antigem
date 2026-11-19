package com.example.gemini.ui.agents

import android.app.Activity
import android.content.Context
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.gemini.data.agent.AgentCliManager
import com.example.gemini.data.agent.AgentPreferences
import com.example.gemini.data.local.LocalServerManager
import com.example.gemini.domain.model.AgentKind
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.widget.Toast
import com.example.gemini.TermuxActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class InstallerPhase {
    SELECTING,
    INSTALLING,
    SUCCESS,
    ERROR
}

/**
 * First-Time Agent Selection & Auto-Installer dialog.
 * Allows users to choose AI coding agents (Antigravity, Claude Code, OpenCode, Codex),
 * displays installed status, runs CLI installation commands with live terminal logs,
 * and restarts the local bridge + hub server.
 */
@Composable
fun AgentSetupAndInstallerDialog(
    onDismissOrCompleted: () -> Unit,
    onOpenTerminal: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val agentPrefs = remember { AgentPreferences(context) }

    // Initial enabled agents from prefs, defaulting to AGY if empty
    var selectedAgents by remember {
        val initial = AgentPreferences.enabledSync(context)
        mutableStateOf(if (initial.isEmpty()) setOf(AgentKind.AGY) else initial)
    }

    var phase by remember { mutableStateOf(InstallerPhase.SELECTING) }
    var currentInstallingAgent by remember { mutableStateOf<AgentKind?>(null) }
    var currentAgentIndex by remember { mutableIntStateOf(0) }
    var totalToInstall by remember { mutableIntStateOf(0) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val terminalLogs = remember { mutableStateListOf<String>() }

    // Keep screen on while installing
    val isInstalling = phase == InstallerPhase.INSTALLING
    val activity = (context as? Activity) ?: ((context as? android.content.ContextWrapper)?.baseContext as? Activity)
    DisposableEffect(isInstalling) {
        val window = activity?.window
        if (isInstalling) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Refresh installation states
    var installedMap by remember {
        mutableStateOf(AgentKind.entries.associateWith { AgentCliManager.isCliInstalled(context, it) })
    }

    fun refreshInstalledMap() {
        installedMap = AgentKind.entries.associateWith { AgentCliManager.isCliInstalled(context, it) }
    }

    val uninstalledSelected = remember(selectedAgents, installedMap) {
        selectedAgents.filter { installedMap[it] != true }
    }

    val allSelected = selectedAgents.size == AgentKind.entries.size

    fun runBatchInstallation() {
        if (selectedAgents.isEmpty()) return

        // Save preferences first
        agentPrefs.setEnabled(selectedAgents)

        val toInstall = selectedAgents.filter { installedMap[it] != true }
        if (toInstall.isEmpty()) {
            // All already installed: immediately start server and finish
            scope.launch {
                LocalServerManager.startServer(context, forceRestart = true)
                onDismissOrCompleted()
            }
            return
        }

        phase = InstallerPhase.INSTALLING
        totalToInstall = toInstall.size
        currentAgentIndex = 0
        terminalLogs.clear()
        terminalLogs.add("🚀 Preparing to install ${toInstall.size} selected agent CLI(s)...")

        scope.launch {
            val failedAgents = mutableListOf<AgentKind>()
            for ((idx, agent) in toInstall.withIndex()) {
                currentInstallingAgent = agent
                currentAgentIndex = idx + 1
                terminalLogs.add("\n========================================")
                terminalLogs.add("▶ Installing [${agent.label()}] (${idx + 1}/${toInstall.size})")
                terminalLogs.add("========================================")

                val result = AgentCliManager.installAgent(context, agent) { line ->
                    terminalLogs.add(line)
                }
                refreshInstalledMap()
                if (!result.success && installedMap[agent] != true) {
                    failedAgents.add(agent)
                    terminalLogs.add("❌ ${agent.label()} installer error (code ${result.exitCode}): ${result.errorMessage ?: "binary not detected"}")
                }
            }

            refreshInstalledMap()
            terminalLogs.add("\n========================================")
            val stillMissing = selectedAgents.filter { installedMap[it] != true }
            if (stillMissing.isNotEmpty() || failedAgents.isNotEmpty()) {
                val names = (failedAgents + stillMissing).distinct().joinToString { it.label() }
                terminalLogs.add("❌ Installation incomplete for: $names")
                errorMessage = "Could not install or locate the binary for: $names.\nSee logs below for details."
                phase = InstallerPhase.ERROR
            } else {
                terminalLogs.add("🎉 All selected agent CLIs are installed and verified!")
                terminalLogs.add("▶ Starting LimitX Hub & IDE Bridge...")
                terminalLogs.add("========================================")
                LocalServerManager.startServer(context, forceRestart = true)
                phase = InstallerPhase.SUCCESS
            }
        }
    }

    Dialog(
        onDismissRequest = {
            if (phase != InstallerPhase.INSTALLING) {
                onDismissOrCompleted()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = phase != InstallerPhase.INSTALLING,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.90f)
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SmartToy,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Choose & Install AI Agents",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Select your desired agents. LimitX will install their CLIs.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(10.dp))

                // Body content based on Phase
                when (phase) {
                    InstallerPhase.SELECTING -> {
                        // Select All / Deselect All Bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "AVAILABLE AGENTS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            TextButton(
                                onClick = {
                                    selectedAgents = if (allSelected) {
                                        setOf(AgentKind.AGY) // Keep at least AGY
                                    } else {
                                        AgentKind.entries.toSet()
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (allSelected) "Deselect All" else "Select All",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // Agent list
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            AgentCliManager.AGENT_ITEMS.forEach { item ->
                                val isSelected = item.agent in selectedAgents
                                val isInstalled = installedMap[item.agent] == true
                                val accent = item.agent.accent()

                                Surface(
                                    onClick = {
                                        val next = if (isSelected) {
                                            selectedAgents - item.agent
                                        } else {
                                            selectedAgents + item.agent
                                        }
                                        if (next.isNotEmpty()) {
                                            selectedAgents = next
                                        }
                                    },
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (isSelected) accent.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                    border = BorderStroke(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) accent.copy(alpha = 0.8f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = isSelected,
                                            onCheckedChange = { checked ->
                                                val next = if (checked) selectedAgents + item.agent else selectedAgents - item.agent
                                                if (next.isNotEmpty()) selectedAgents = next
                                            },
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = accent,
                                                checkmarkColor = Color.White
                                            ),
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        AgentMark(item.agent, size = 38.dp)
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Text(
                                                    text = item.title,
                                                    fontSize = 14.5.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                // Installed / Not Installed Badge
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(
                                                            if (isInstalled) Color(0xFF10B981).copy(alpha = 0.15f)
                                                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                                                        )
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = if (isInstalled) "✓ Installed" else "Not installed",
                                                        fontSize = 10.5.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = if (isInstalled) Color(0xFF10B981)
                                                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = item.description,
                                                fontSize = 11.5.sp,
                                                lineHeight = 15.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Footer Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = onDismissOrCompleted,
                                modifier = Modifier.weight(0.4f)
                            ) {
                                Text("Skip", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            Button(
                                onClick = { runBatchInstallation() },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(0.6f),
                                contentPadding = PaddingValues(vertical = 12.dp)
                            ) {
                                Icon(
                                    imageVector = if (uninstalledSelected.isEmpty()) Icons.Default.Check else Icons.Default.Download,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (uninstalledSelected.isEmpty()) "Continue" else "Install & Continue",
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    InstallerPhase.INSTALLING -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .animateContentSize()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.5.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Installing ${currentInstallingAgent?.label() ?: "Agent"} ($currentAgentIndex/$totalToInstall)...",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            LinearProgressIndicator(
                                progress = { if (totalToInstall > 0) currentAgentIndex.toFloat() / totalToInstall.toFloat() else 0f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "Live Installation Logs:",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            // Terminal Log Box
                            val logListState = rememberLazyListState()
                            LaunchedEffect(terminalLogs.size) {
                                if (terminalLogs.isNotEmpty()) {
                                    logListState.animateScrollToItem(terminalLogs.size - 1)
                                }
                            }

                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF1E1E1E)
                            ) {
                                LazyColumn(
                                    state = logListState,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(10.dp)
                                ) {
                                    items(terminalLogs) { line ->
                                        Text(
                                            text = line,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp,
                                            lineHeight = 15.sp,
                                            color = when {
                                                line.startsWith("✅") || line.contains("successfully") -> Color(0xFF4ADE80)
                                                line.startsWith("❌") || line.contains("failed") -> Color(0xFFF87171)
                                                line.startsWith("⚠️") -> Color(0xFFFBBF24)
                                                line.startsWith("$") -> Color(0xFF60A5FA)
                                                else -> Color(0xFFE2E8F0)
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Please wait while packages and dependencies are configured...",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    InstallerPhase.SUCCESS -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF10B981).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Setup Complete!",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "All selected agents are installed. The LimitX Hub daemon and bridge have been started.",
                                fontSize = 13.5.sp,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = onDismissOrCompleted,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth(0.8f)
                            ) {
                                Text("Start Chatting", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    InstallerPhase.ERROR -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(vertical = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(42.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Installation Failed / Incomplete",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = errorMessage ?: "One or more agent CLIs failed to install.",
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            // Terminal Logs Box with Copy button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Terminal Output & Diagnostics:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                TextButton(
                                    onClick = {
                                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                        cm?.setPrimaryClip(ClipData.newPlainText("LimitX Agent Install Logs", terminalLogs.joinToString("\n")))
                                        Toast.makeText(context, "Logs copied to clipboard", Toast.LENGTH_SHORT).show()
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Copy Logs", fontSize = 11.5.sp)
                                }
                            }

                            val errorLogListState = rememberLazyListState()
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF1E1E1E)
                            ) {
                                LazyColumn(
                                    state = errorLogListState,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(8.dp)
                                ) {
                                    items(terminalLogs) { line ->
                                        Text(
                                            text = line,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 10.5.sp,
                                            lineHeight = 14.sp,
                                            color = when {
                                                line.startsWith("✅") || line.contains("successfully") -> Color(0xFF4ADE80)
                                                line.startsWith("❌") || line.contains("failed") -> Color(0xFFF87171)
                                                line.startsWith("⚠️") -> Color(0xFFFBBF24)
                                                line.startsWith("$") -> Color(0xFF60A5FA)
                                                else -> Color(0xFFE2E8F0)
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Action buttons
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { runBatchInstallation() },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Retry Installation", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            if (onOpenTerminal != null) {
                                                onOpenTerminal()
                                            } else {
                                                try {
                                                    context.startActivity(Intent(context, TermuxActivity::class.java))
                                                } catch (_: Exception) {}
                                            }
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Terminal, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Open Terminal", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { phase = InstallerPhase.SELECTING },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Change Selection", fontSize = 12.5.sp)
                                    }
                                    TextButton(
                                        onClick = {
                                            LocalServerManager.startServer(context, forceRestart = true)
                                            onDismissOrCompleted()
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Continue Anyway", fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
