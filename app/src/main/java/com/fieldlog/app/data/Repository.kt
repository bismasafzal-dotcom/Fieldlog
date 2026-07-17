package com.fieldlog.app.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * The repository is the only part of the app allowed to touch the database.
 * Screens ask it for things; it enforces the rules. Keeping this in one place
 * means "you can only be on the clock for one job at a time" is a rule that
 * cannot be broken by accident from some other screen later on.
 */
class Repository(
    private val jobs: JobDao,
    private val times: TimeEntryDao,
    private val expenses: ExpenseDao
) {

    // ---------- Jobs ----------

    val activeJobs: Flow<List<Job>> = jobs.activeJobs()
    val allJobs: Flow<List<Job>> = jobs.allJobs()

    suspend fun saveJob(job: Job): Long = jobs.upsert(job)

    /** Deleting a job removes its time and expenses too, so no orphan rows are left behind. */
    suspend fun deleteJob(job: Job) {
        times.deleteForJob(job.id)
        expenses.deleteForJob(job.id)
        jobs.delete(job)
    }

    // ---------- The clock ----------

    val runningEntry: Flow<TimeEntry?> = times.running()

    /**
     * Start the clock on a job. If the clock was already running on a different job,
     * that one is closed out first — you're never billing two clients the same minute.
     */
    suspend fun clockIn(jobId: Long, at: Long = System.currentTimeMillis()) {
        times.runningNow()?.let { open ->
            if (open.jobId == jobId) return  // already on the clock here; do nothing
            times.update(open.copy(endedAt = at))
        }
        times.insert(TimeEntry(jobId = jobId, startedAt = at))
    }

    suspend fun clockOut(at: Long = System.currentTimeMillis(), note: String = "") {
        val open = times.runningNow() ?: return
        times.update(
            open.copy(
                endedAt = at.coerceAtLeast(open.startedAt),
                note = if (note.isBlank()) open.note else note
            )
        )
    }

    /** For the times you forgot to clock in and are fixing it afterwards. */
    suspend fun addManualEntry(jobId: Long, startedAt: Long, endedAt: Long, note: String = "") {
        times.insert(
            TimeEntry(
                jobId = jobId,
                startedAt = startedAt,
                endedAt = endedAt.coerceAtLeast(startedAt),
                note = note
            )
        )
    }

    suspend fun updateEntry(entry: TimeEntry) = times.update(entry)
    suspend fun deleteEntry(entry: TimeEntry) = times.delete(entry)

    fun entriesBetween(from: Long, to: Long): Flow<List<EntryWithJob>> =
        times.entriesBetween(from, to).map { rows -> rows.map { it.toEntryWithJob() } }

    // ---------- Expenses ----------

    val allExpenses: Flow<List<Expense>> = expenses.allExpenses()

    fun expensesBetween(from: Long, to: Long): Flow<List<Expense>> =
        expenses.expensesBetween(from, to)

    suspend fun saveExpense(expense: Expense): Long = expenses.upsert(expense)
    suspend fun deleteExpense(expense: Expense) = expenses.delete(expense)

    // ---------- Export ----------

    suspend fun allEntriesForExport(): List<EntryWithJob> =
        times.allEntriesWithJob().map { it.toEntryWithJob() }

    suspend fun allExpensesForExport(): List<Expense> = expenses.allExpensesNow()

    /** Lookup used when writing the expenses CSV. Reads the jobs table directly so a
     *  job with expenses but no hours logged still exports with its real name. */
    suspend fun jobNamesById(): Map<Long, String> =
        jobs.allJobsNow().associate { it.id to it.name }
}

/**
 * Roll a list of entries and expenses up into per-job totals.
 * Kept as a plain function (not a database query) so it's easy to read and change.
 */
fun buildTotals(
    entries: List<EntryWithJob>,
    expenses: List<Expense>,
    now: Long = System.currentTimeMillis()
): List<JobTotals> {
    val byJob = linkedMapOf<Long, JobTotals>()

    entries.forEach { e ->
        val ms = e.entry.durationMs(now)
        val earned = (ms * e.hourlyRateCents) / 3_600_000L   // ms -> hours, at the job's rate
        val current = byJob[e.entry.jobId]
        byJob[e.entry.jobId] = JobTotals(
            jobId = e.entry.jobId,
            jobName = e.jobName,
            workedMs = (current?.workedMs ?: 0L) + ms,
            earnedCents = (current?.earnedCents ?: 0L) + earned,
            expensesCents = current?.expensesCents ?: 0L
        )
    }

    expenses.forEach { x ->
        val id = x.jobId ?: return@forEach
        val current = byJob[id] ?: return@forEach
        byJob[id] = current.copy(expensesCents = current.expensesCents + x.amountCents)
    }

    return byJob.values.sortedByDescending { it.workedMs }
}
