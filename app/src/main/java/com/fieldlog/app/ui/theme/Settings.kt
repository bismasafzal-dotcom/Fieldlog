package com.fieldlog.app.ui.theme

import android.content.Context
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Appearance settings, saved on the phone. No network, no account — same as everything else.
 */

enum class ThemeMode(val label: String) {
    LIGHT("Light"),
    DARK("Dark"),
    SEPIA("Sepia")
}

/**
 * The accent is the CLOCK IN slab — the one loud thing in the app.
 *
 * Deliberately no red and no green options: green already means "on the clock" and
 * red already means "money going out". If the accent could be either of those, the
 * two colours that carry meaning would stop carrying it.
 *
 * Safety orange keeps its text BLACK rather than white — that's how real hi-vis gear
 * and traffic cones are marked, and it's the higher-contrast pairing in sunlight.
 */
enum class Accent(val label: String, val color: Color, val onColor: Color) {
    ORANGE("Safety orange", Color(0xFFF25C05), Color(0xFF14171A)),
    BLUE("Signal blue", Color(0xFF1565C0), Color.White),
    TEAL("Deep teal", Color(0xFF00695C), Color.White),
    VIOLET("Violet", Color(0xFF5E35B1), Color.White)
}

data class AppSettings(
    val mode: ThemeMode = ThemeMode.LIGHT,
    val accent: Accent = Accent.ORANGE
)

object SettingsStore {

    private const val PREFS = "fieldlog_settings"
    private const val KEY_MODE = "theme_mode"
    private const val KEY_ACCENT = "accent"

    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Called once when the app starts. */
    fun load(context: Context) {
        val p = prefs(context)
        // runCatching so a bad/old saved value can never crash the app on launch —
        // it just falls back to the default.
        val mode = runCatching {
            ThemeMode.valueOf(p.getString(KEY_MODE, null) ?: ThemeMode.LIGHT.name)
        }.getOrDefault(ThemeMode.LIGHT)

        val accent = runCatching {
            Accent.valueOf(p.getString(KEY_ACCENT, null) ?: Accent.ORANGE.name)
        }.getOrDefault(Accent.ORANGE)

        _settings.value = AppSettings(mode, accent)
    }

    fun setMode(context: Context, mode: ThemeMode) {
        prefs(context).edit().putString(KEY_MODE, mode.name).apply()
        _settings.value = _settings.value.copy(mode = mode)
    }

    fun setAccent(context: Context, accent: Accent) {
        prefs(context).edit().putString(KEY_ACCENT, accent.name).apply()
        _settings.value = _settings.value.copy(accent = accent)
    }
}
