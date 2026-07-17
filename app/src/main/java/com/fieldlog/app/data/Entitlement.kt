package com.fieldlog.app.data

import android.content.Context

/**
 * ---------------------------------------------------------------------------
 * THIS FILE DOES NOTHING TODAY. That is on purpose.
 * ---------------------------------------------------------------------------
 *
 * The app is free. There is no billing code, no Play Billing library, and no
 * INTERNET permission. Everything below returns "yes, you have it."
 *
 * It exists for one reason: two facts are impossible to recover later if you
 * don't record them now.
 *
 *   1. WHEN SOMEONE INSTALLED.
 *      The day you add a paywall, you'll want to grandfather the people who were
 *      already using the app — free forever, "founding user." They took a chance
 *      on an unknown app and they're the ones who'll vouch for it. But you can
 *      only be fair to them if you know who they are, and you cannot backfill an
 *      install date. It has to be stamped from the very first public release.
 *
 *   2. WHERE THE GATES WOULD GO.
 *      Every screen asks this file "can the user do X?" instead of just doing X.
 *      Right now the answer is always yes. When you add billing, you change the
 *      body of isPro() and NOTHING ELSE IN THE APP CHANGES. No screen gets
 *      rewritten, no logic gets untangled.
 *
 * When you're ready to charge, read the "Adding billing later" section in README.md.
 */
object Entitlement {

    private const val PREFS = "fieldlog_entitlement"
    private const val KEY_FIRST_INSTALL = "first_install_at"

    /**
     * Anyone who installed before this date is a founding user and keeps everything
     * for free, permanently. Today it's set far in the future, so EVERY user is a
     * founding user — correct, because the app is free.
     *
     * On the day you ship billing: change this to that day's date. Everyone already
     * using the app stays free. Everyone after it sees the paywall.
     */
    private const val FOUNDING_CUTOFF = Long.MAX_VALUE

    /** Call once on app start. Stamps the install date if it isn't stamped yet. */
    fun ensureStamped(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!prefs.contains(KEY_FIRST_INSTALL)) {
            prefs.edit()
                .putLong(KEY_FIRST_INSTALL, System.currentTimeMillis())
                .apply()
        }
    }

    fun firstInstalledAt(context: Context): Long =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getLong(KEY_FIRST_INSTALL, System.currentTimeMillis())

    /** True for anyone who was already using the app before you started charging. */
    fun isFoundingUser(context: Context): Boolean =
        firstInstalledAt(context) < FOUNDING_CUTOFF

    /**
     * The single question the whole app asks.
     *
     * TODAY: always true. The app is free and complete.
     *
     * LATER: `isFoundingUser(context) || hasActivePurchase(context)`
     * — and that is the only line you need to change.
     */
    fun isPro(context: Context): Boolean = true

    // ---- The gates. All open today. ----

    /** Free tier would cap jobs; paid tier is unlimited. No cap right now. */
    fun maxJobs(context: Context): Int =
        if (isPro(context)) Int.MAX_VALUE else 2

    /** CSV export is the most likely thing to put behind a paywall. Open today. */
    fun canExportCsv(context: Context): Boolean = isPro(context)
}
