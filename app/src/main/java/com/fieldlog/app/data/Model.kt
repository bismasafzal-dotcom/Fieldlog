package com.fieldlog.app.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * All money is stored as whole CENTS (Long), never as a decimal.
 * Decimals lose pennies to rounding; cents never do. Divide by 100 only when displaying.
 *
 * All times are stored as epoch milliseconds (Long) — a plain number, no timezone
 * baked in, so the data stays correct if the user crosses a timezone.
 */

@Entity(tableName = "jobs")
data class Job(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val client: String = "",
    val hourlyRateCents: Long = 0,
    val archived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "time_entries",
    indices = [Index("jobId"), Index("startedAt")]
)
data class TimeEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val jobId: Long,
    val startedAt: Long,
    /** null means the clock is still running on this entry. */
    val endedAt: Long? = null,
    val note: String = ""
) {
    val isRunning: Boolean get() = endedAt == null

    /** Duration in milliseconds. For a running entry, pass [now] to get live elapsed time. */
    fun durationMs(now: Long = System.currentTimeMillis()): Long =
        ((endedAt ?: now) - startedAt).coerceAtLeast(0)
}

@Entity(
    tableName = "expenses",
    indices = [Index("jobId"), Index("occurredAt")]
)
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** null = a general expense not tied to any one job. */
    val jobId: Long? = null,
    val amountCents: Long,
    val category: String = "Other",
    val note: String = "",
    val occurredAt: Long = System.currentTimeMillis(),
    /** Billable expenses get passed on to the client; non-billable you eat yourself. */
    val billable: Boolean = true
)

val EXPENSE_CATEGORIES = listOf(
    "Materials", "Fuel", "Tools", "Parking", "Food", "Permit", "Other"
)

/** A time entry joined with the job it belongs to — what the lists actually need. */
data class EntryWithJob(
    val entry: TimeEntry,
    val jobName: String,
    val hourlyRateCents: Long
)

/** A single job's rolled-up numbers for the summary screen. */
data class JobTotals(
    val jobId: Long,
    val jobName: String,
    val workedMs: Long,
    val earnedCents: Long,
    val expensesCents: Long
)
