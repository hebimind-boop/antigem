package com.example.gemini.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb

/**
 * One app theme: a name, whether it is dark, and its colors by token (see [ThemeTokens]).
 *
 * Built-in themes carry their own colors. A custom theme (made in the app or by a model through theme_mcp) stores only
 * what it changes on top of [base]; everything else comes from the base, and colors that depend on a changed one
 * (e.g. the primary container on a new primary) are worked out again ([ThemeTokens.derive]).
 */
data class AppTheme(
    val id: String,
    val name: String,
    val dark: Boolean,
    val about: String = "",
    val builtIn: Boolean = false,
    /** Custom themes: the theme they build on. */
    val base: String? = null,
    /** Token → ARGB. Built-in: the theme's colors. Custom: only the changed ones. */
    val colors: Map<String, Long> = emptyMap()
)

/** Every color a theme can set, with what it is used for. */
object ThemeTokens {
    class Token(val key: String, val group: String, val about: String)

    /** Material color slots (the whole UI reads these). */
    val material = listOf(
        Token("primary", "accent", "Main accent: buttons, selected items, links, the send button, active icons"),
        Token("on_primary", "accent", "Text and icons on primary"),
        Token("primary_container", "accent", "Soft accent fill (selected chips, highlighted rows)"),
        Token("on_primary_container", "accent", "Text on primary_container"),
        Token("secondary", "accent", "Second accent (pressed / darker accent, secondary buttons)"),
        Token("on_secondary", "accent", "Text on secondary"),
        Token("secondary_container", "accent", "Soft fill of the second accent"),
        Token("on_secondary_container", "accent", "Text on secondary_container"),
        Token("tertiary", "accent", "Third accent (badges, special highlights)"),
        Token("on_tertiary", "accent", "Text on tertiary"),
        Token("tertiary_container", "accent", "Soft fill of the third accent"),
        Token("on_tertiary_container", "accent", "Text on tertiary_container"),
        Token("background", "surface", "Screen background behind everything"),
        Token("on_background", "text", "Main text on the background"),
        Token("surface", "surface", "Default surface (top bars, sheets, the chat area)"),
        Token("on_surface", "text", "Main text on surfaces (used almost everywhere)"),
        Token("surface_variant", "surface", "Raised or tinted surface (inputs, chips, user message bubble)"),
        Token("on_surface_variant", "text", "Secondary text and icons (hints, captions, inactive icons)"),
        Token("surface_container_lowest", "surface", "Lowest container level"),
        Token("surface_container_low", "surface", "Low container level"),
        Token("surface_container", "surface", "Container level (menus, cards)"),
        Token("surface_container_high", "surface", "High container level (dialogs, raised menus)"),
        Token("surface_container_highest", "surface", "Highest container level"),
        Token("outline", "line", "Strong borders (text fields, outlined buttons)"),
        Token("outline_variant", "line", "Subtle dividers and card borders"),
        Token("error", "status", "Errors (Material components)"),
        Token("on_error", "status", "Text on error"),
        Token("error_container", "status", "Soft error fill"),
        Token("on_error_container", "status", "Text on error_container"),
        Token("inverse_surface", "surface", "Snackbars and toasts background"),
        Token("inverse_on_surface", "text", "Text on inverse_surface"),
        Token("inverse_primary", "accent", "Accent on inverse_surface")
    )

    /** BoopIDE's own colors (cards, chat, code, status). */
    val app = listOf(
        Token("card", "surface", "Cards and panels (settings cards, pickers, bottom sheets)"),
        Token("text_secondary", "text", "Secondary text in BoopIDE's own screens"),
        Token("user_bubble", "chat", "Your messages in the chat"),
        Token("thinking_bg", "chat", "Thinking / summary blocks in the chat"),
        Token("code_bg", "code", "Code block background in chats"),
        Token("code_text", "code", "Plain code text on code_bg"),
        Token("success", "status", "Success, running OK, online, positive quota"),
        Token("warning", "status", "Warnings, waiting, quota running low"),
        Token("danger", "status", "Failures, stop and delete buttons, offline"),
        Token("info", "status", "Information, links to other places, the browser accent"),
        Token("accent_alt", "accent", "Extra accent for special features (purple by default)"),
        Token("syntax_keyword", "code", "Code: keywords (if, fun, class)"),
        Token("syntax_type", "code", "Code: types and class names"),
        Token("syntax_string", "code", "Code: strings"),
        Token("syntax_number", "code", "Code: numbers"),
        Token("syntax_comment", "code", "Code: comments"),
        Token("syntax_function", "code", "Code: function names"),
        Token("syntax_annotation", "code", "Code: annotations and decorators"),
        Token("syntax_punctuation", "code", "Code: punctuation and operators"),
        Token("syntax_control", "code", "Code: control keywords (return, if, for); default = syntax_keyword"),
        Token("syntax_tag", "code", "Code: HTML/XML tags"),
        Token("syntax_attribute", "code", "Code: attributes and properties"),
        Token("border", "line", "Borders of cards, inputs and chips in BoopIDE's own screens"),
        Token("fill", "surface", "Subtle fills: icon circles, inactive chips, hovered rows"),
        Token("success_container", "status", "Soft success background (added files, OK banners)"),
        Token("warning_container", "status", "Soft warning background (modified files, notices)"),
        Token("danger_container", "status", "Soft danger background (errors, deleted files)"),
        Token("info_container", "status", "Soft info background (tips, renamed files)"),
        Token("accent_indigo", "accent", "Decorative indigo (backups, some settings groups)"),
        Token("accent_teal", "accent", "Decorative teal / cyan (utilities, tools)"),
        Token("accent_pink", "accent", "Decorative pink"),
        Token("editor_bg", "editor", "IDE editor, diff and log viewers background"),
        Token("editor_panel", "editor", "IDE side panels, tab bar, toolbars"),
        Token("editor_text", "editor", "IDE plain text"),
        Token("editor_muted", "editor", "IDE line numbers and hints"),
        Token("editor_border", "editor", "IDE borders and separators"),
        Token("editor_selection", "editor", "IDE selection and search matches"),
        Token("editor_line", "editor", "IDE current line / inactive tab"),
        Token("editor_added_bg", "editor", "Diff: added line background"),
        Token("editor_added_text", "editor", "Diff: added line text"),
        Token("editor_removed_bg", "editor", "Diff: removed line background"),
        Token("editor_removed_text", "editor", "Diff: removed line text"),
        Token("editor_info_bg", "editor", "Diff: hunk header background"),
        Token("editor_info_text", "editor", "Diff: hunk header text"),
        Token("terminal_bg", "terminal", "Terminal background"),
        Token("terminal_fg", "terminal", "Terminal text"),
        Token("terminal_cursor", "terminal", "Terminal cursor")
    ) + (0..15).map { Token("ansi_$it", "terminal", "Terminal ANSI color $it (" + ansiNames[it] + ")") }

    private val ansiNames get() = listOf("black", "red", "green", "yellow", "blue", "magenta", "cyan", "white",
        "bright black", "bright red", "bright green", "bright yellow", "bright blue", "bright magenta", "bright cyan", "bright white")

    val all = material + app
    val keys: Set<String> = all.map { it.key }.toSet()
    private val materialKeys = material.map { it.key }.toSet()
    fun isMaterial(key: String) = key in materialKeys

    /** The colors a new theme should set; the rest is worked out from them. */
    val seeds = listOf(
        "background", "surface", "card", "surface_variant", "on_background", "on_surface_variant",
        "primary", "secondary", "tertiary", "success", "warning", "danger", "info", "code_bg",
        "syntax_keyword", "syntax_type", "syntax_string", "syntax_number", "syntax_comment", "syntax_function",
        "syntax_annotation", "syntax_punctuation"
    )

    // ---------------------------------------------------------------- deriving dependent colors

    private val White = Color.White
    private val Black = Color(0xFF111111)

    /** Text color for a fill: whichever of near-black and white reads better on it. */
    fun on(c: Color): Color = if (contrast(c, Black) >= contrast(c, White)) Black else White

    private class Rule(val key: String, val sources: List<String>, val compute: (Map<String, Long>, Boolean) -> Color)

    private fun Map<String, Long>.c(key: String) = Color(getValue(key).toInt())

    private val rules = listOf(
        Rule("on_primary", listOf("primary")) { m, _ -> on(m.c("primary")) },
        Rule("primary_container", listOf("primary", "surface")) { m, d -> lerp(m.c("surface"), m.c("primary"), if (d) 0.30f else 0.18f) },
        Rule("on_primary_container", listOf("primary")) { m, d -> lerp(m.c("primary"), if (d) White else Black, if (d) 0.35f else 0.45f) },
        Rule("inverse_primary", listOf("primary")) { m, _ -> m.c("primary") },
        Rule("on_secondary", listOf("secondary")) { m, _ -> on(m.c("secondary")) },
        Rule("secondary_container", listOf("secondary", "surface")) { m, d -> lerp(m.c("surface"), m.c("secondary"), if (d) 0.30f else 0.18f) },
        Rule("on_secondary_container", listOf("secondary")) { m, d -> lerp(m.c("secondary"), if (d) White else Black, if (d) 0.35f else 0.45f) },
        Rule("on_tertiary", listOf("tertiary")) { m, _ -> on(m.c("tertiary")) },
        Rule("tertiary_container", listOf("tertiary", "surface")) { m, d -> lerp(m.c("surface"), m.c("tertiary"), if (d) 0.30f else 0.18f) },
        Rule("on_tertiary_container", listOf("tertiary")) { m, d -> lerp(m.c("tertiary"), if (d) White else Black, if (d) 0.35f else 0.45f) },
        Rule("on_surface", listOf("on_background")) { m, _ -> m.c("on_background") },
        Rule("text_secondary", listOf("on_surface_variant")) { m, _ -> m.c("on_surface_variant") },
        Rule("outline", listOf("on_surface_variant", "background")) { m, _ -> lerp(m.c("on_surface_variant"), m.c("background"), 0.25f) },
        Rule("outline_variant", listOf("on_background", "background")) { m, d -> lerp(m.c("background"), m.c("on_background"), if (d) 0.18f else 0.14f) },
        Rule("surface_container_lowest", listOf("background")) { m, d -> lerp(m.c("background"), if (d) Color.Black else White, if (d) 0.3f else 0.6f) },
        Rule("surface_container_low", listOf("background", "card")) { m, _ -> lerp(m.c("background"), m.c("card"), 0.5f) },
        Rule("surface_container", listOf("card")) { m, _ -> m.c("card") },
        Rule("surface_container_high", listOf("card", "on_background")) { m, d -> lerp(m.c("card"), m.c("on_background"), if (d) 0.06f else 0.04f) },
        Rule("surface_container_highest", listOf("card", "on_background")) { m, d -> lerp(m.c("card"), m.c("on_background"), if (d) 0.10f else 0.07f) },
        Rule("inverse_surface", listOf("on_background")) { m, _ -> m.c("on_background") },
        Rule("inverse_on_surface", listOf("background")) { m, _ -> m.c("background") },
        Rule("error", listOf("danger")) { m, _ -> m.c("danger") },
        Rule("on_error", listOf("danger")) { m, _ -> on(m.c("danger")) },
        Rule("error_container", listOf("danger", "surface")) { m, d -> lerp(m.c("surface"), m.c("danger"), if (d) 0.28f else 0.15f) },
        Rule("on_error_container", listOf("danger")) { m, d -> lerp(m.c("danger"), if (d) White else Black, if (d) 0.3f else 0.4f) },
        Rule("user_bubble", listOf("surface_variant")) { m, _ -> m.c("surface_variant") },
        Rule("thinking_bg", listOf("surface_variant", "background")) { m, _ -> lerp(m.c("surface_variant"), m.c("background"), 0.45f) },
        Rule("code_text", listOf("code_bg")) { m, _ -> if (m.c("code_bg").luminance() < 0.5f) Color(0xFFE6E6E6) else Color(0xFF1F2328) },
        Rule("accent_alt", listOf("tertiary")) { m, _ -> m.c("tertiary") },
        Rule("syntax_control", listOf("syntax_keyword")) { m, _ -> m.c("syntax_keyword") },
        Rule("syntax_tag", listOf("syntax_function")) { m, _ -> m.c("syntax_function") },
        Rule("syntax_attribute", listOf("syntax_type")) { m, _ -> m.c("syntax_type") },
        Rule("border", listOf("card", "on_background")) { m, d -> lerp(m.c("card"), m.c("on_background"), if (d) 0.14f else 0.11f) },
        Rule("fill", listOf("card", "on_background")) { m, d -> lerp(m.c("card"), m.c("on_background"), if (d) 0.07f else 0.05f) },
        Rule("success_container", listOf("success", "surface")) { m, d -> lerp(m.c("surface"), m.c("success"), if (d) 0.18f else 0.12f) },
        Rule("warning_container", listOf("warning", "surface")) { m, d -> lerp(m.c("surface"), m.c("warning"), if (d) 0.18f else 0.12f) },
        Rule("danger_container", listOf("danger", "surface")) { m, d -> lerp(m.c("surface"), m.c("danger"), if (d) 0.18f else 0.12f) },
        Rule("info_container", listOf("info", "surface")) { m, d -> lerp(m.c("surface"), m.c("info"), if (d) 0.18f else 0.12f) },
        Rule("accent_indigo", listOf("info", "accent_alt")) { m, _ -> lerp(m.c("info"), m.c("accent_alt"), 0.5f) },
        Rule("accent_teal", listOf("info", "success")) { m, _ -> lerp(m.c("info"), m.c("success"), 0.45f) },
        Rule("accent_pink", listOf("danger", "accent_alt")) { m, _ -> lerp(m.c("danger"), m.c("accent_alt"), 0.4f) },
        Rule("editor_bg", listOf("code_bg")) { m, _ -> m.c("code_bg") },
        Rule("editor_text", listOf("code_text")) { m, _ -> m.c("code_text") },
        Rule("editor_panel", listOf("editor_bg", "editor_text")) { m, _ -> lerp(m.c("editor_bg"), m.c("editor_text"), 0.05f) },
        Rule("editor_line", listOf("editor_bg", "editor_text")) { m, _ -> lerp(m.c("editor_bg"), m.c("editor_text"), 0.08f) },
        Rule("editor_border", listOf("editor_bg", "editor_text")) { m, _ -> lerp(m.c("editor_bg"), m.c("editor_text"), 0.15f) },
        Rule("editor_muted", listOf("editor_bg", "editor_text")) { m, _ -> lerp(m.c("editor_bg"), m.c("editor_text"), 0.5f) },
        Rule("editor_selection", listOf("editor_bg", "primary")) { m, _ -> lerp(m.c("editor_bg"), m.c("primary"), 0.32f) },
        Rule("editor_added_bg", listOf("editor_bg", "success")) { m, _ -> lerp(m.c("editor_bg"), m.c("success"), 0.22f) },
        Rule("editor_added_text", listOf("editor_text", "success")) { m, _ -> lerp(m.c("success"), m.c("editor_text"), 0.5f) },
        Rule("editor_removed_bg", listOf("editor_bg", "danger")) { m, _ -> lerp(m.c("editor_bg"), m.c("danger"), 0.22f) },
        Rule("editor_removed_text", listOf("editor_text", "danger")) { m, _ -> lerp(m.c("danger"), m.c("editor_text"), 0.5f) },
        Rule("editor_info_bg", listOf("editor_bg", "info")) { m, _ -> lerp(m.c("editor_bg"), m.c("info"), 0.18f) },
        Rule("editor_info_text", listOf("editor_text", "info")) { m, _ -> lerp(m.c("info"), m.c("editor_text"), 0.3f) },
        Rule("terminal_bg", listOf("code_bg")) { m, _ -> m.c("code_bg") },
        Rule("terminal_fg", listOf("code_text")) { m, _ -> m.c("code_text") },
        Rule("terminal_cursor", listOf("primary")) { m, _ -> m.c("primary") },
        Rule("ansi_0", listOf("terminal_bg", "terminal_fg")) { m, _ -> lerp(m.c("terminal_bg"), m.c("terminal_fg"), 0.18f) },
        Rule("ansi_1", listOf("danger")) { m, _ -> m.c("danger") },
        Rule("ansi_2", listOf("success")) { m, _ -> m.c("success") },
        Rule("ansi_3", listOf("warning")) { m, _ -> m.c("warning") },
        Rule("ansi_4", listOf("info")) { m, _ -> m.c("info") },
        Rule("ansi_5", listOf("accent_alt")) { m, _ -> m.c("accent_alt") },
        Rule("ansi_6", listOf("accent_teal")) { m, _ -> m.c("accent_teal") },
        Rule("ansi_7", listOf("terminal_bg", "terminal_fg")) { m, _ -> lerp(m.c("terminal_fg"), m.c("terminal_bg"), 0.15f) },
        Rule("ansi_8", listOf("terminal_bg", "terminal_fg")) { m, _ -> lerp(m.c("terminal_bg"), m.c("terminal_fg"), 0.45f) },
        Rule("ansi_9", listOf("ansi_1")) { m, _ -> lerp(m.c("ansi_1"), White, 0.25f) },
        Rule("ansi_10", listOf("ansi_2")) { m, _ -> lerp(m.c("ansi_2"), White, 0.25f) },
        Rule("ansi_11", listOf("ansi_3")) { m, _ -> lerp(m.c("ansi_3"), White, 0.25f) },
        Rule("ansi_12", listOf("ansi_4")) { m, _ -> lerp(m.c("ansi_4"), White, 0.25f) },
        Rule("ansi_13", listOf("ansi_5")) { m, _ -> lerp(m.c("ansi_5"), White, 0.25f) },
        Rule("ansi_14", listOf("ansi_6")) { m, _ -> lerp(m.c("ansi_6"), White, 0.25f) },
        Rule("ansi_15", listOf("terminal_fg")) { m, _ -> m.c("terminal_fg") }
    )

    /**
     * Works out again every color that depends on one of [changed] (and was not set explicitly in [changed] itself),
     * in an order where a worked-out color can feed the next one.
     */
    fun derive(map: MutableMap<String, Long>, changed: Set<String>, dark: Boolean) {
        val touched = changed.toMutableSet()
        for (r in rules) {
            if (r.key in changed) continue
            if (r.sources.none { it in touched }) continue
            if (r.sources.any { it !in map }) continue
            map[r.key] = r.compute(map, dark).toArgb().toLong() and 0xFFFFFFFFL
            touched += r.key
        }
    }

    /** The Material color scheme of a resolved theme: slots it does not set stay at Material's own defaults. */
    fun scheme(colors: Map<String, Long>, dark: Boolean): ColorScheme {
        val b = if (dark) darkColorScheme() else lightColorScheme()
        fun c(k: String, d: Color) = colors[k]?.let { Color(it.toInt()) } ?: d
        return b.copy(
            primary = c("primary", b.primary), onPrimary = c("on_primary", b.onPrimary),
            primaryContainer = c("primary_container", b.primaryContainer), onPrimaryContainer = c("on_primary_container", b.onPrimaryContainer),
            inversePrimary = c("inverse_primary", b.inversePrimary),
            secondary = c("secondary", b.secondary), onSecondary = c("on_secondary", b.onSecondary),
            secondaryContainer = c("secondary_container", b.secondaryContainer), onSecondaryContainer = c("on_secondary_container", b.onSecondaryContainer),
            tertiary = c("tertiary", b.tertiary), onTertiary = c("on_tertiary", b.onTertiary),
            tertiaryContainer = c("tertiary_container", b.tertiaryContainer), onTertiaryContainer = c("on_tertiary_container", b.onTertiaryContainer),
            background = c("background", b.background), onBackground = c("on_background", b.onBackground),
            surface = c("surface", b.surface), onSurface = c("on_surface", b.onSurface),
            surfaceVariant = c("surface_variant", b.surfaceVariant), onSurfaceVariant = c("on_surface_variant", b.onSurfaceVariant),
            surfaceTint = Color.Transparent,
            inverseSurface = c("inverse_surface", b.inverseSurface), inverseOnSurface = c("inverse_on_surface", b.inverseOnSurface),
            error = c("error", b.error), onError = c("on_error", b.onError),
            errorContainer = c("error_container", b.errorContainer), onErrorContainer = c("on_error_container", b.onErrorContainer),
            outline = c("outline", b.outline), outlineVariant = c("outline_variant", b.outlineVariant),
            surfaceContainerLowest = c("surface_container_lowest", b.surfaceContainerLowest),
            surfaceContainerLow = c("surface_container_low", b.surfaceContainerLow),
            surfaceContainer = c("surface_container", b.surfaceContainer),
            surfaceContainerHigh = c("surface_container_high", b.surfaceContainerHigh),
            surfaceContainerHighest = c("surface_container_highest", b.surfaceContainerHighest)
        )
    }

    /** WCAG contrast ratio of two colors (1 = none, 21 = black on white). */
    fun contrast(a: Color, b: Color): Float {
        val la = a.luminance() + 0.05f
        val lb = b.luminance() + 0.05f
        return if (la > lb) la / lb else lb / la
    }

    /** Readability problems of a resolved theme, in plain words (empty = fine). */
    fun check(colors: Map<String, Long>): List<String> {
        fun c(k: String) = colors[k]?.let { Color(it.toInt()) }
        val pairs = listOf(
            Triple("on_background", "background", 4.5f), Triple("on_surface", "surface", 4.5f),
            Triple("on_surface", "card", 4.5f), Triple("on_surface_variant", "surface", 3f),
            Triple("text_secondary", "background", 3f), Triple("on_primary", "primary", 3f),
            Triple("primary", "background", 2.5f), Triple("on_surface", "user_bubble", 4.5f),
            Triple("code_text", "code_bg", 4.5f), Triple("syntax_comment", "code_bg", 2.5f),
            Triple("danger", "background", 2.5f), Triple("success", "background", 2.5f),
            Triple("editor_text", "editor_bg", 4.5f), Triple("editor_muted", "editor_bg", 2.5f),
            Triple("editor_added_text", "editor_added_bg", 4f), Triple("editor_removed_text", "editor_removed_bg", 4f),
            Triple("terminal_fg", "terminal_bg", 4.5f), Triple("on_background", "fill", 4.5f)
        )
        return pairs.mapNotNull { (fg, bg, min) ->
            val f = c(fg) ?: return@mapNotNull null
            val b = c(bg) ?: return@mapNotNull null
            val r = contrast(f, b)
            if (r < min) "$fg on $bg has contrast ${"%.1f".format(r)} (needs at least $min)" else null
        }
    }

    fun hex(argb: Long): String = "#%08X".format(argb and 0xFFFFFFFFL)

    /** "#RRGGBB", "#AARRGGBB", "RRGGBB" or "0xAARRGGBB" → ARGB, or null. */
    fun parse(text: String): Long? {
        val t = text.trim().removePrefix("#").removePrefix("0x").removePrefix("0X")
        if (!t.matches(Regex("[0-9a-fA-F]{6}|[0-9a-fA-F]{8}"))) return null
        val v = t.toLong(16)
        return if (t.length == 6) (0xFF000000L or v) else v
    }
}

/** The themes that come with the app. */
object BuiltInThemes {
    const val LIGHT_DEFAULT = "monochrome_light"
    const val DARK_DEFAULT = "limitx_cyber"

    /** How many themes at the top of each side's list are marked as recommended (the first is the default). */
    const val RECOMMENDED_COUNT = 3

    private fun limitxCyber() = AppTheme(
        id = "limitx_cyber",
        name = "LimitX Cyber (Logo Theme)",
        dark = true,
        builtIn = true,
        about = "Deep cyber-slate with electric neon cyan-mint accents and ice-white text matching the LimitX logo",
        colors = linkedMapOf(
            "primary" to 0xFF00F5D4, "on_primary" to 0xFF021714, "primary_container" to 0xFF0B3832, "on_primary_container" to 0xFF5EEAD4,
            "secondary" to 0xFF10B981, "on_secondary" to 0xFF021714, "secondary_container" to 0xFF0E382B, "on_secondary_container" to 0xFF6EE7B7,
            "tertiary" to 0xFF38BDF8, "on_tertiary" to 0xFF021714, "tertiary_container" to 0xFF0C354A, "on_tertiary_container" to 0xFF7DD3FC,
            "background" to 0xFF0B131B, "on_background" to 0xFFF0FDFC, "surface" to 0xFF0B131B, "on_surface" to 0xFFF0FDFC,
            "surface_variant" to 0xFF182635, "on_surface_variant" to 0xFF8FA7B8, "surface_container_lowest" to 0xFF070C12,
            "surface_container_low" to 0xFF0E1722, "surface_container" to 0xFF111D29, "surface_container_high" to 0xFF172432,
            "surface_container_highest" to 0xFF1D2D3E, "outline" to 0xFF2A435E, "outline_variant" to 0xFF1E3247, "error" to 0xFFF43F5E,
            "on_error" to 0xFFFFFFFF, "error_container" to 0xFF4C1D24, "on_error_container" to 0xFFFDA4AF, "inverse_surface" to 0xFFF0FDFC,
            "inverse_on_surface" to 0xFF0B131B, "inverse_primary" to 0xFF00F5D4, "card" to 0xFF111D29, "text_secondary" to 0xFF8FA7B8,
            "user_bubble" to 0xFF162536, "thinking_bg" to 0xFF0F1A25, "code_bg" to 0xFF080E15, "code_text" to 0xFFE0F2FE, "success" to 0xFF10B981,
            "warning" to 0xFFFBBF24, "danger" to 0xFFF43F5E, "info" to 0xFF00F5D4, "accent_alt" to 0xFF00F5D4, "syntax_keyword" to 0xFF00F5D4,
            "syntax_type" to 0xFF38BDF8, "syntax_string" to 0xFF6EE7B7, "syntax_number" to 0xFFFBBF24, "syntax_comment" to 0xFF54708A,
            "syntax_function" to 0xFF67E8F9, "syntax_annotation" to 0xFFF472B6, "syntax_punctuation" to 0xFF8FA7B8, "syntax_control" to 0xFF00F5D4,
            "syntax_tag" to 0xFF38BDF8, "syntax_attribute" to 0xFF6EE7B7, "border" to 0xFF1E3247, "fill" to 0xFF132230,
            "success_container" to 0xFF0E382B, "warning_container" to 0xFF382C10, "danger_container" to 0xFF3E151E, "info_container" to 0xFF0C354A,
            "accent_indigo" to 0xFF6366F1, "accent_teal" to 0xFF00F5D4, "accent_pink" to 0xFFF472B6, "editor_bg" to 0xFF080E15,
            "editor_panel" to 0xFF0E1722, "editor_text" to 0xFFE0F2FE, "editor_muted" to 0xFF54708A, "editor_border" to 0xFF1E3247,
            "editor_selection" to 0xFF1E3D52, "editor_line" to 0xFF111E2C, "editor_added_bg" to 0xFF0E382B, "editor_added_text" to 0xFF6EE7B7,
            "editor_removed_bg" to 0xFF3E151E, "editor_removed_text" to 0xFFFDA4AF, "editor_info_bg" to 0xFF0C354A, "editor_info_text" to 0xFF7DD3FC,
            "terminal_bg" to 0xFF080E15, "terminal_fg" to 0xFFE0F2FE, "terminal_cursor" to 0xFF00F5D4, "ansi_0" to 0xFF0B131B,
            "ansi_1" to 0xFFF43F5E, "ansi_2" to 0xFF10B981, "ansi_3" to 0xFFFBBF24, "ansi_4" to 0xFF38BDF8, "ansi_5" to 0xFFC084FC,
            "ansi_6" to 0xFF00F5D4, "ansi_7" to 0xFFE0F2FE, "ansi_8" to 0xFF54708A, "ansi_9" to 0xFFFB7185, "ansi_10" to 0xFF34D399,
            "ansi_11" to 0xFFFDE047, "ansi_12" to 0xFF60A5FA, "ansi_13" to 0xFFE879F9, "ansi_14" to 0xFF5EEAD4, "ansi_15" to 0xFFFFFFFF
        )
    )

    private fun monochromeDark() = AppTheme(
        id = "monochrome_dark",
        name = "Monochrome Dark (Black & White)",
        dark = true,
        builtIn = true,
        about = "Pure OLED Black with crisp dark surfaces, pure white primary accents, and high-contrast borders",
        colors = linkedMapOf(
            "primary" to 0xFFFFFFFF, "on_primary" to 0xFF000000, "primary_container" to 0xFF262626, "on_primary_container" to 0xFFFFFFFF,
            "secondary" to 0xFFE5E5E5, "on_secondary" to 0xFF000000, "secondary_container" to 0xFF262626, "on_secondary_container" to 0xFFE5E5E5,
            "tertiary" to 0xFFD4D4D4, "on_tertiary" to 0xFF000000, "tertiary_container" to 0xFF262626, "on_tertiary_container" to 0xFFD4D4D4,
            "background" to 0xFF000000, "on_background" to 0xFFFFFFFF, "surface" to 0xFF000000, "on_surface" to 0xFFFFFFFF,
            "surface_variant" to 0xFF141414, "on_surface_variant" to 0xFFA3A3A3, "surface_container_lowest" to 0xFF000000,
            "surface_container_low" to 0xFF050505, "surface_container" to 0xFF0A0A0A, "surface_container_high" to 0xFF121212,
            "surface_container_highest" to 0xFF181818, "outline" to 0xFF333333, "outline_variant" to 0xFF262626, "error" to 0xFFEF4444,
            "on_error" to 0xFFFFFFFF, "error_container" to 0xFF3D1414, "on_error_container" to 0xFFFCA5A5, "inverse_surface" to 0xFFFFFFFF,
            "inverse_on_surface" to 0xFF000000, "inverse_primary" to 0xFFFFFFFF, "card" to 0xFF0A0A0A, "text_secondary" to 0xFFA3A3A3,
            "user_bubble" to 0xFF1A1A1A, "thinking_bg" to 0xFF0F0F0F, "code_bg" to 0xFF050505, "code_text" to 0xFFEDEDED, "success" to 0xFF22C55E,
            "warning" to 0xFFF59E0B, "danger" to 0xFFEF4444, "info" to 0xFFFFFFFF, "accent_alt" to 0xFFE5E5E5, "syntax_keyword" to 0xFFFFFFFF,
            "syntax_type" to 0xFFE5E5E5, "syntax_string" to 0xFFD4D4D4, "syntax_number" to 0xFFA3A3A3, "syntax_comment" to 0xFF737373,
            "syntax_function" to 0xFFFFFFFF, "syntax_annotation" to 0xFFD4D4D4, "syntax_punctuation" to 0xFFA3A3A3, "syntax_control" to 0xFFFFFFFF,
            "syntax_tag" to 0xFFE5E5E5, "syntax_attribute" to 0xFFD4D4D4, "border" to 0xFF262626, "fill" to 0xFF171717,
            "success_container" to 0xFF14301B, "warning_container" to 0xFF3A280D, "danger_container" to 0xFF3D1414, "info_container" to 0xFF262626,
            "accent_indigo" to 0xFFFFFFFF, "accent_teal" to 0xFFE5E5E5, "accent_pink" to 0xFFD4D4D4, "editor_bg" to 0xFF050505,
            "editor_panel" to 0xFF0A0A0A, "editor_text" to 0xFFEDEDED, "editor_muted" to 0xFF737373, "editor_border" to 0xFF262626,
            "editor_selection" to 0xFF333333, "editor_line" to 0xFF121212, "editor_added_bg" to 0xFF14301B, "editor_added_text" to 0xFF86EFAC,
            "editor_removed_bg" to 0xFF3D1414, "editor_removed_text" to 0xFFFCA5A5, "editor_info_bg" to 0xFF262626, "editor_info_text" to 0xFFE5E5E5,
            "terminal_bg" to 0xFF000000, "terminal_fg" to 0xFFFFFFFF, "terminal_cursor" to 0xFFFFFFFF, "ansi_0" to 0xFF000000,
            "ansi_1" to 0xFFEF4444, "ansi_2" to 0xFF22C55E, "ansi_3" to 0xFFF59E0B, "ansi_4" to 0xFFFFFFFF, "ansi_5" to 0xFFE5E5E5,
            "ansi_6" to 0xFFD4D4D4, "ansi_7" to 0xFFE5E5E5, "ansi_8" to 0xFF737373, "ansi_9" to 0xFFF87171, "ansi_10" to 0xFF4ADE80,
            "ansi_11" to 0xFFFBBF24, "ansi_12" to 0xFFFFFFFF, "ansi_13" to 0xFFF5F5F5, "ansi_14" to 0xFFE5E5E5, "ansi_15" to 0xFFFFFFFF
        )
    )

    private fun monochromeLight() = AppTheme(
        id = "monochrome_light",
        name = "Monochrome Light (White & Black)",
        dark = false,
        builtIn = true,
        about = "Pure White background with clean light surfaces, pure black primary accents, and clean borders",
        colors = linkedMapOf(
            "primary" to 0xFF000000, "on_primary" to 0xFFFFFFFF, "primary_container" to 0xFFE4E4E7, "on_primary_container" to 0xFF000000,
            "secondary" to 0xFF27272A, "on_secondary" to 0xFFFFFFFF, "secondary_container" to 0xFFE4E4E7, "on_secondary_container" to 0xFF18181B,
            "tertiary" to 0xFF3F3F46, "on_tertiary" to 0xFFFFFFFF, "tertiary_container" to 0xFFE4E4E7, "on_tertiary_container" to 0xFF18181B,
            "background" to 0xFFFFFFFF, "on_background" to 0xFF000000, "surface" to 0xFFFFFFFF, "on_surface" to 0xFF000000,
            "surface_variant" to 0xFFE4E4E7, "on_surface_variant" to 0xFF52525B, "surface_container_lowest" to 0xFFFFFFFF,
            "surface_container_low" to 0xFFFAFAFA, "surface_container" to 0xFFF4F4F5, "surface_container_high" to 0xFFE4E4E7,
            "surface_container_highest" to 0xFFD4D4D8, "outline" to 0xFFD4D4D8, "outline_variant" to 0xFFE4E4E7, "error" to 0xFFDC2626,
            "on_error" to 0xFFFFFFFF, "error_container" to 0xFFFEE2E2, "on_error_container" to 0xFF991B1B, "inverse_surface" to 0xFF18181B,
            "inverse_on_surface" to 0xFFFFFFFF, "inverse_primary" to 0xFFFFFFFF, "card" to 0xFFF4F4F5, "text_secondary" to 0xFF52525B,
            "user_bubble" to 0xFFE4E4E7, "thinking_bg" to 0xFFF4F4F5, "code_bg" to 0xFF18181B, "code_text" to 0xFFF4F4F5, "success" to 0xFF16A34A,
            "warning" to 0xFFD97706, "danger" to 0xFFDC2626, "info" to 0xFF000000, "accent_alt" to 0xFF27272A, "syntax_keyword" to 0xFF000000,
            "syntax_type" to 0xFF27272A, "syntax_string" to 0xFF3F3F46, "syntax_number" to 0xFF52525B, "syntax_comment" to 0xFFA1A1AA,
            "syntax_function" to 0xFF000000, "syntax_annotation" to 0xFF3F3F46, "syntax_punctuation" to 0xFF71717A, "syntax_control" to 0xFF000000,
            "syntax_tag" to 0xFF27272A, "syntax_attribute" to 0xFF3F3F46, "border" to 0xFFE4E4E7, "fill" to 0xFFF4F4F5,
            "success_container" to 0xFFDCFCE7, "warning_container" to 0xFFFEF3C7, "danger_container" to 0xFFFEE2E2, "info_container" to 0xFFE4E4E7,
            "accent_indigo" to 0xFF000000, "accent_teal" to 0xFF27272A, "accent_pink" to 0xFF3F3F46, "editor_bg" to 0xFFFFFFFF,
            "editor_panel" to 0xFFF4F4F5, "editor_text" to 0xFF18181B, "editor_muted" to 0xFFA1A1AA, "editor_border" to 0xFFE4E4E7,
            "editor_selection" to 0xFFE4E4E7, "editor_line" to 0xFFFAFAFA, "editor_added_bg" to 0xFFDCFCE7, "editor_added_text" to 0xFF15803D,
            "editor_removed_bg" to 0xFFFEE2E2, "editor_removed_text" to 0xFFB91C1C, "editor_info_bg" to 0xFFE4E4E7, "editor_info_text" to 0xFF18181B,
            "terminal_bg" to 0xFF000000, "terminal_fg" to 0xFFFFFFFF, "terminal_cursor" to 0xFFFFFFFF, "ansi_0" to 0xFF000000,
            "ansi_1" to 0xFFDC2626, "ansi_2" to 0xFF16A34A, "ansi_3" to 0xFFD97706, "ansi_4" to 0xFF000000, "ansi_5" to 0xFF27272A,
            "ansi_6" to 0xFF3F3F46, "ansi_7" to 0xFFFFFFFF, "ansi_8" to 0xFF71717A, "ansi_9" to 0xFFEF4444, "ansi_10" to 0xFF22C55E,
            "ansi_11" to 0xFFF59E0B, "ansi_12" to 0xFF18181B, "ansi_13" to 0xFF3F3F46, "ansi_14" to 0xFF52525B, "ansi_15" to 0xFFFFFFFF
        )
    )

    val all: List<AppTheme> by lazy {
        listOf(
            monochromeLight(),
            limitxCyber(),
            monochromeDark()
        )
    }

    fun default(dark: Boolean): AppTheme = all.first { it.id == if (dark) DARK_DEFAULT else LIGHT_DEFAULT }

    /**
     * A built-in theme's place among the recommended ones of its side: 0 = the default (also the top pick),
     * 1 and 2 = the next recommended ones; null when it is not one of them (or a custom theme).
     */
    fun recommendedRank(theme: AppTheme): Int? {
        if (!theme.builtIn) return null
        val i = all.filter { it.dark == theme.dark }.indexOfFirst { it.id == theme.id }
        return i.takeIf { it in 0 until RECOMMENDED_COUNT }
    }
}
