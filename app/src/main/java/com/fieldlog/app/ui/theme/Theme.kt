package com.fieldlog.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Three backgrounds the user picks between, and four accents.
 *
 * TWO COLOURS ARE NOT USER-CHOOSABLE, ON PURPOSE:
 *   green  = on the clock
 *   red    = money going out
 * They carry meaning. If the accent could also be red or green, that meaning would
 * blur, and this is an app people use to get paid correctly. So the accent list has
 * no red and no green in it.
 *
 * Flat throughout: no shadows, no gradients. Renders fast on a cheap phone and stays
 * readable in direct sunlight.
 */

// ---- LIGHT: chalk and steel ----
private val LightBg = Color(0xFFF4F5F2)
private val LightSurface = Color(0xFFFFFFFF)
private val LightInk = Color(0xFF14171A)
private val LightMuted = Color(0xFF5A6672)
private val LightOutline = Color(0xFFDBDDD8)

// ---- DARK: carbon. Night shifts are real. ----
private val DarkBg = Color(0xFF0E1013)
private val DarkSurface = Color(0xFF191D21)
private val DarkInk = Color(0xFFECEDE9)
private val DarkMuted = Color(0xFF9AA5AE)
private val DarkOutline = Color(0xFF2E353B)

// ---- SEPIA: warm paper. Easier on the eyes over a long day, and it's the
//      colour of the paper docket this app is replacing. ----
private val SepiaBg = Color(0xFFF0E6D2)
private val SepiaSurface = Color(0xFFFAF3E4)
private val SepiaInk = Color(0xFF3A2E20)
private val SepiaMuted = Color(0xFF7A6A55)
private val SepiaOutline = Color(0xFFDCC9A8)

// ---- Fixed meaning colours ----
private val RunningLight = Color(0xFF1E7A46)   // on the clock
private val RunningDark = Color(0xFF33B06A)
private val SpendLight = Color(0xFFB3261E)     // money out
private val SpendDark = Color(0xFFFF6B5E)

@Composable
fun FieldLogTheme(
    mode: ThemeMode = ThemeMode.LIGHT,
    accent: Accent = Accent.ORANGE,
    content: @Composable () -> Unit
) {
    val colors = when (mode) {
        ThemeMode.DARK -> darkColorScheme(
            primary = DarkInk,
            onPrimary = DarkBg,
            secondary = accent.color,
            onSecondary = accent.onColor,
            tertiary = RunningDark,
            onTertiary = DarkBg,
            background = DarkBg,
            onBackground = DarkInk,
            surface = DarkSurface,
            onSurface = DarkInk,
            surfaceVariant = DarkSurface,
            onSurfaceVariant = DarkMuted,
            outline = DarkOutline,
            error = SpendDark,
            onError = DarkBg
        )

        ThemeMode.SEPIA -> lightColorScheme(
            primary = SepiaInk,
            onPrimary = SepiaSurface,
            secondary = accent.color,
            onSecondary = accent.onColor,
            tertiary = RunningLight,
            onTertiary = Color.White,
            background = SepiaBg,
            onBackground = SepiaInk,
            surface = SepiaSurface,
            onSurface = SepiaInk,
            surfaceVariant = SepiaBg,
            onSurfaceVariant = SepiaMuted,
            outline = SepiaOutline,
            error = SpendLight,
            onError = Color.White
        )

        ThemeMode.LIGHT -> lightColorScheme(
            primary = LightInk,
            onPrimary = LightBg,
            secondary = accent.color,
            onSecondary = accent.onColor,
            tertiary = RunningLight,
            onTertiary = Color.White,
            background = LightBg,
            onBackground = LightInk,
            surface = LightSurface,
            onSurface = LightInk,
            surfaceVariant = LightBg,
            onSurfaceVariant = LightMuted,
            outline = LightOutline,
            error = SpendLight,
            onError = Color.White
        )
    }

    MaterialTheme(
        colorScheme = colors,
        typography = AppTypography,
        content = content
    )
}

/**
 * Numbers are the content of this app — hours, money, a running clock. So the numerals
 * get the characterful face (monospace, like a meter or a printed work docket) and the
 * prose stays out of the way. Monospace also stops the timer digits jittering as they tick.
 * Both faces ship with Android, so the app stays tiny and needs no font download.
 */
val Meter = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontWeight = FontWeight.Bold,
    letterSpacing = (-1).sp
)

/** Small all-caps labels, like the headings printed on a paper timesheet. */
val Docket = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.Bold,
    fontSize = 12.sp,
    letterSpacing = 1.2.sp
)

private val AppTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Black,
        fontSize = 30.sp,
        letterSpacing = (-0.5).sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 17.sp   // read at arm's length, outdoors
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 15.sp
    ),
    labelSmall = Docket
)

/** Minimum size for anything tappable. Assume gloves. Assume a bumpy van. */
val TapTarget = 64.dp
