package com.fieldlog.app.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** 1234 cents -> "12.34". No currency symbol; the user's own money is whatever it is. */
fun formatMoney(cents: Long): String {
    val sign = if (cents < 0) "-" else ""
    val abs = kotlin.math.abs(cents)
    return "$sign${abs / 100}.${(abs % 100).toString().padStart(2, '0')}"
}

/**
 * Turns whatever the user typed into cents. Returns null if it isn't a number.
 *
 * People type money in more ways than you'd think, and this app is for a global
 * audience: "45", "45.50", "45,50" (Europe), "1,250.75" (US), "1.250,75" (Europe),
 * "$32.10". Getting this wrong doesn't crash the app — it quietly logs the wrong
 * amount, which is worse. So the separator is worked out rather than assumed:
 *
 *  - Both , and . present -> whichever comes LAST is the decimal point.
 *  - Only one kind, appearing more than once -> it's grouping ("1.250.000").
 *  - Only one, with exactly 3 digits after it -> grouping ("1,500" is 1500, not 1.5).
 *  - Otherwise -> it's the decimal point.
 */
fun parseMoneyToCents(input: String): Long? {
    var s = input.trim().replace(Regex("[^0-9.,]"), "")
    if (s.isEmpty() || s.none { it.isDigit() }) return null

    val hasComma = s.contains(',')
    val hasDot = s.contains('.')

    if (hasComma && hasDot) {
        val decimal = if (s.lastIndexOf(',') > s.lastIndexOf('.')) ',' else '.'
        val grouping = if (decimal == ',') '.' else ','
        s = s.replace(grouping.toString(), "").replace(decimal, '.')
    } else if (hasComma || hasDot) {
        val sep = if (hasComma) ',' else '.'
        val parts = s.split(sep)
        s = when {
            parts.size > 2 -> s.replace(sep.toString(), "")            // grouping
            parts[1].length == 3 && parts[0].isNotEmpty() -> s.replace(sep.toString(), "") // grouping
            parts[1].isEmpty() -> s.replace(sep.toString(), "")        // trailing "45."
            else -> s.replace(sep, '.')                                // decimal
        }
    }

    val value = s.toDoubleOrNull() ?: return null
    return Math.round(value * 100)
}

/** Milliseconds -> "7:42:05" — the big readout on the clock button. */
fun formatClock(ms: Long): String {
    val total = ms / 1000
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return "%d:%02d:%02d".format(h, m, s)
}

/** Milliseconds -> "7h 42m" — for lists and totals, where seconds are noise. */
fun formatDuration(ms: Long): String {
    val total = ms / 60_000
    val h = total / 60
    val m = total % 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}

/** Milliseconds -> "7.70" decimal hours, which is what invoices and payroll want. */
fun formatDecimalHours(ms: Long): String =
    "%.2f".format(ms / 3_600_000.0)

private val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())
private val dayFmt = SimpleDateFormat("EEE d MMM", Locale.getDefault())
private val csvFmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

fun formatTime(epochMs: Long): String = timeFmt.format(Date(epochMs))
fun formatDay(epochMs: Long): String = dayFmt.format(Date(epochMs))
fun formatForCsv(epochMs: Long?): String = epochMs?.let { csvFmt.format(Date(it)) } ?: ""

/** Midnight this morning, in the phone's own timezone. */
fun startOfToday(now: Long = System.currentTimeMillis()): Long =
    Calendar.getInstance().apply {
        timeInMillis = now
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

fun startOfTomorrow(now: Long = System.currentTimeMillis()): Long =
    Calendar.getInstance().apply {
        timeInMillis = startOfToday(now)
        add(Calendar.DAY_OF_YEAR, 1)
    }.timeInMillis

/** Start of the current week, respecting whatever the phone's locale calls day one. */
fun startOfWeek(now: Long = System.currentTimeMillis()): Long {
    val cal = Calendar.getInstance().apply { timeInMillis = startOfToday(now) }
    val firstDay = cal.firstDayOfWeek
    while (cal.get(Calendar.DAY_OF_WEEK) != firstDay) {
        cal.add(Calendar.DAY_OF_YEAR, -1)
    }
    return cal.timeInMillis
}

fun startOfMonth(now: Long = System.currentTimeMillis()): Long =
    Calendar.getInstance().apply {
        timeInMillis = startOfToday(now)
        set(Calendar.DAY_OF_MONTH, 1)
    }.timeInMillis

/** Far enough in the future to mean "everything". */
const val FAR_FUTURE = Long.MAX_VALUE / 2
