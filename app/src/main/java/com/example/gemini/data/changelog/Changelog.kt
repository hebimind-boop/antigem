package com.example.gemini.data.changelog

import com.example.gemini.BuildConfig
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/** One released version, from version.json at the commit that last changed it (see the generateChangelog Gradle task). */
data class ChangelogEntry(
    val versionCode: Int,
    val versionName: String,
    /** yyyy-MM-dd of that commit, or null for a version that isn't committed yet. */
    val date: String?,
    val changes: List<String>
)

/** What a line of the changelog is, guessed from its first word. */
enum class ChangeKind(val label: String) { NEW("New"), FIXED("Fixed"), IMPROVED("Improved"), REMOVED("Removed") }

class ChangeLine(val text: String, val kind: ChangeKind)

/** A version ready to show: everything the page needs is worked out once, not while scrolling. */
class ChangelogVersion(
    val versionCode: Int,
    val versionName: String,
    /** "Nov 4, 2026", or null when not dated. */
    val dateLabel: String?,
    /** "Today", "3 days ago", "2 months ago"…, or null when not dated. */
    val relativeLabel: String?,
    /** "November 2026", or null when not dated; the page starts a month section when this changes. */
    val monthLabel: String?,
    /** "Nov 2026", or null when not dated. */
    val shortMonthLabel: String?,
    val lines: List<ChangeLine>,
    val counts: Map<ChangeKind, Int>,
    val isInstalled: Boolean,
    val isLatest: Boolean
)

object Changelog {
    /** Newest first. Built on first use (a few dozen entries, so it's cheap). */
    val versions: List<ChangelogVersion> by lazy {
        val cleanEntries = listOf(
            ChangelogEntry(
                versionCode = 145,
                versionName = "1.4.4",
                date = "2026-11-23",
                changes = listOf(
                    "New: Integrated LimitX Studio navigation line-by-line inside sidebar drawer",
                    "Improved: Moved Profile & account status button to top-right of chat bar, replacing '+' button",
                    "Improved: Moved Themes cleanly to Settings & Preferences and streamlined drawer footer",
                    "Removed: Completely removed Mini Apps feature, background workers, and MCP tools",
                    "Improved: Simplified conversation 3-dot menu strictly to Copy ID, Share, and Pin/Unpin"
                )
            ),
            ChangelogEntry(
                versionCode = 144,
                versionName = "1.4.3",
                date = "2026-11-23",
                changes = listOf(
                    "New: Complete LimitX Studio visual architecture redesign with distinctive developer command center",
                    "Improved: Streamlined bottom input dock with dedicated Send/Stop controls and studio dictation trigger",
                    "New: Prominent '+ New Workspace Session' studio action in navigation drawer",
                    "New: Standalone private repository migration script and documentation",
                    "Improved: Purged third-party repository URLs and personal upstream references across the app"
                )
            ),
            ChangelogEntry(
                versionCode = 143,
                versionName = "1.4.2",
                date = "2026-11-23",
                changes = listOf(
                    "Removed: Floating assistant overlay bubble and streamlined settings menu",
                    "New: System notifications when AI replies complete while outside the app, with direct one-tap return to chat",
                    "New: Automated system notifications when new LimitX updates are detected",
                    "Improved: Streamlined update dialog without external repository buttons",
                    "Improved: Hidden internal build package information from About screen"
                )
            ),
            ChangelogEntry(
                versionCode = 142,
                versionName = "1.4.1",
                date = "2026-11-22",
                changes = listOf(
                    "Improved: Sleek, compact 2x2 'Add to Chat' menu with crisp icons and concise action hints",
                    "New: Clean rounded square image preview thumbnail directly inside prompt box with floating top-right close button",
                    "Improved: Modernized model selection sheet with clean section headers, subtle quota pills, and compact thinking controls"
                )
            ),
            ChangelogEntry(
                versionCode = 141,
                versionName = "1.4.0",
                date = "2026-11-21",
                changes = listOf(
                    "New: Complete flagship visual UI redesign with signature LimitX Studio Header, pulsing status indicator, and sharp session buttons",
                    "New: LimitX Command Center welcome view with interactive Quick-Action Prompt Cards for full-stack apps, refactoring, terminal, and GitHub",
                    "Improved: Floating Studio Input Dock with crisp 1dp border, refined attachment dock, and sleek model selector badge",
                    "Fixed: Automatic Git installation in Ubuntu container during environment setup and GitHub login with dedicated one-tap Install Git button"
                )
            ),
            ChangelogEntry(
                versionCode = 140,
                versionName = "1.3.1",
                date = "2026-11-21",
                changes = listOf(
                    "Fixed: High-contrast Send and Stop/Pause buttons in chat input bar across Monochrome Dark, Cyberpunk, and Light themes",
                    "Improved: Complete purge of legacy BoopIDE references across system prompts, MCP servers, bridge, and environment to LimitX",
                    "New: Restored GitHub Connect integration in Settings and Accounts with full support for cloning, pulling, and pushing repositories"
                )
            ),
            ChangelogEntry(
                versionCode = 139,
                versionName = "1.3.0",
                date = "2026-11-20",
                changes = listOf(
                    "Fixed: High-contrast buttons in Monochrome Dark theme (Sign In, Update, Commit, Setup)",
                    "New: Crisp geometric LimitX brand wordmark directly beneath the circular logo on splash and installer screens",
                    "Improved: Marked Claude Code, OpenCode, and Codex as Coming Soon in agent settings and setup dialog",
                    "Improved: Official LimitX release APK artifact naming with backward-compatible in-app update support"
                )
            ),
            ChangelogEntry(
                versionCode = 138,
                versionName = "1.2.9",
                date = "2026-11-20",
                changes = listOf(
                    "New: Pre-bundled Antigravity CLI (agy) directly in app assets for instant offline availability",
                    "Improved: Auto-extraction of bundled agy to rootfs environment with executable permissions",
                    "Fixed: Clean circular logo transparency with radial feathering, eliminating edge artifacts on OLED black",
                    "Improved: Redesigned agent selection dialog with ready status badges and Continue with Ready Agents"
                )
            ),
            ChangelogEntry(
                versionCode = 137,
                versionName = "1.2.8",
                date = "2026-11-19",
                changes = listOf(
                    "Fixed: AI agent CLI installer verification, robust multi-path detection and direct download fallbacks",
                    "Fixed: Background service notifications to display official LimitX branding",
                    "Improved: Clean circular logo presentation with cyber-cyan glow on onboarding and setup screens",
                    "Improved: Terminal logs diagnostics, copy logs action, and retry controls on agent setup"
                )
            ),
            ChangelogEntry(
                versionCode = 136,
                versionName = "1.2.7",
                date = "2026-11-18",
                changes = listOf(
                    "New: LimitX Official Release — AI Coding Studio, Cyber Terminal & Multi-Agent Hub",
                    "New: Custom LimitX cyber aesthetic, OLED Monochrome Dark & Clean Monochrome Light themes",
                    "New: First-time AI agent CLI selection and auto-installer for Antigravity, Claude Code, OpenCode & Codex",
                    "Improved: High-performance terminal emulator with native Android Automation MCP tools"
                )
            )
        )
        build(cleanEntries)
    }

    /** The date of the first release ("Sep 30, 2026"), for the end of the timeline. */
    val firstDateLabel: String? get() = versions.lastOrNull { it.dateLabel != null }?.dateLabel

    /** The month of the first release ("Sep 2026"), for the page header. */
    val firstMonthLabel: String? get() = versions.lastOrNull { it.shortMonthLabel != null }?.shortMonthLabel

    private fun build(entries: List<ChangelogEntry>): List<ChangelogVersion> {
        val iso = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = TimeZone.getDefault() }
        val day = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        val month = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        val shortMonth = SimpleDateFormat("MMM yyyy", Locale.getDefault())
        val today = Calendar.getInstance().apply { clearTime() }
        val newest = entries.maxOfOrNull { it.versionCode }
        return entries.sortedByDescending { it.versionCode }.map { e ->
            val parsed = e.date?.let { runCatching { iso.parse(it) }.getOrNull() }
            val lines = e.changes.map { ChangeLine(it.capitalizeFirst(), kindOf(it)) }
            ChangelogVersion(
                versionCode = e.versionCode,
                versionName = e.versionName,
                dateLabel = parsed?.let(day::format),
                relativeLabel = parsed?.let { relative(today, Calendar.getInstance().apply { time = it; clearTime() }) },
                monthLabel = parsed?.let(month::format),
                shortMonthLabel = parsed?.let(shortMonth::format),
                lines = lines,
                counts = lines.groupingBy { it.kind }.eachCount(),
                isInstalled = e.versionCode == BuildConfig.VERSION_CODE,
                isLatest = e.versionCode == newest
            )
        }
    }

    private fun kindOf(line: String): ChangeKind {
        val first = line.trim().lowercase().split(' ', '(', ':', ',').firstOrNull().orEmpty()
        return when {
            first.startsWith("add") || first.startsWith("new") || first.startsWith("introduc") || first == "initial" -> ChangeKind.NEW
            first.startsWith("fix") || first.startsWith("bug") || first == "resolved" -> ChangeKind.FIXED
            first.startsWith("remov") || first.startsWith("drop") || first.startsWith("delet") -> ChangeKind.REMOVED
            else -> ChangeKind.IMPROVED
        }
    }

    private fun relative(today: Calendar, date: Calendar): String {
        val days = ((today.timeInMillis - date.timeInMillis) / 86_400_000L).toInt()
        return when {
            days <= 0 -> "Today"
            days == 1 -> "Yesterday"
            days < 7 -> "$days days ago"
            days < 14 -> "Last week"
            days < 31 -> "${days / 7} weeks ago"
            days < 61 -> "Last month"
            days < 365 -> "${days / 30} months ago"
            days < 730 -> "Last year"
            else -> "${days / 365} years ago"
        }
    }

    private fun Calendar.clearTime() {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }

    private fun String.capitalizeFirst() = replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
}
