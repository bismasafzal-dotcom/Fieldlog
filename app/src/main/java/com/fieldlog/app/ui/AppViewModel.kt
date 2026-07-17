package com.fieldlog.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import com.fieldlog.app.data.Entitlement
import com.fieldlog.app.data.Expense
import com.fieldlog.app.data.FieldLogDatabase
import com.fieldlog.app.data.Job
import com.fieldlog.app.data.JobTotals
import com.fieldlog.app.data.Repository
import com.fieldlog.app.data.TimeEntry
import com.fieldlog.app.data.buildTotals
import com.fieldlog.app.export.CsvExport
import com.fieldlog.app.util.FAR_FUTURE
import com.fieldlog.app.util.startOfMonth
import com.fieldlog.app.util.startOfToday
import com.fieldlog.app.util.startOfWeek
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class Period(val label: String) {
    WEEK("This week"),
    MONTH("This month"),
    ALL("All time");

    fun startMs(): Long = when (this) {
        WEEK -> startOfWeek()
        MONTH -> startOfMonth()
        ALL -> 0L
    }
}

/**
 * One ViewModel for the whole app. For an app this size that's simpler to follow
 * than four of them, and it means the clock state is shared everywhere for free.
 *
 * A ViewModel survives screen rotation and tab switches, so the running clock
 * doesn't reset when the user turns the phone sideways.
 */
class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val repo: Repository = FieldLogDatabase.get(app).let { db ->
        Repository(db.jobDao(), db.timeEntryDao(), db.expenseDao())
    }

    // ---- Jobs ----
    val jobs: StateFlow<List<Job>> = repo.activeJobs
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // ---- The clock ----
    val running: StateFlow<TimeEntry?> = repo.runningEntry
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** The job the running clock belongs to, or null when clocked out. */
    val runningJob: StateFlow<Job?> = combine(running, jobs) { entry, list ->
        entry?.let { e -> list.firstOrNull { it.id == e.jobId } }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Today's finished + running entries, newest first. */
    val todayEntries = repo.entriesBetween(startOfToday(), FAR_FUTURE)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // ---- Summary period ----
    private val _period = MutableStateFlow(Period.WEEK)
    val period: StateFlow<Period> = _period
    fun setPeriod(p: Period) { _period.value = p }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val totals: StateFlow<List<JobTotals>> = _period
        .flatMapLatest { p ->
            combine(
                repo.entriesBetween(p.startMs(), FAR_FUTURE),
                repo.expensesBetween(p.startMs(), FAR_FUTURE)
            ) { entries, expenses -> buildTotals(entries, expenses) }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // ---- Expenses ----
    val expenses: StateFlow<List<Expense>> = repo.allExpenses
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // ---- Messages shown in the snackbar ----

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message
    fun messageShown() { _message.value = null }

    // ---- Actions ----

    fun clockIn(jobId: Long) = viewModelScope.launch { repo.clockIn(jobId) }
    fun clockOut() = viewModelScope.launch { repo.clockOut() }

    fun saveJob(job: Job) = viewModelScope.launch {
        // The gate is wide open today (maxJobs is unlimited). Routing through it now
        // means that when billing lands, this method doesn't change — only Entitlement does.
        val isNew = job.id == 0L
        if (isNew && jobs.value.size >= Entitlement.maxJobs(getApplication())) {
            _message.value = "You've hit the job limit."
            return@launch
        }
        repo.saveJob(job)
    }
    fun deleteJob(job: Job) = viewModelScope.launch { repo.deleteJob(job) }

    fun addManualEntry(jobId: Long, startedAt: Long, endedAt: Long, note: String) =
        viewModelScope.launch { repo.addManualEntry(jobId, startedAt, endedAt, note) }

    fun deleteEntry(entry: TimeEntry) = viewModelScope.launch { repo.deleteEntry(entry) }

    fun saveExpense(expense: Expense) = viewModelScope.launch { repo.saveExpense(expense) }
    fun deleteExpense(expense: Expense) = viewModelScope.launch { repo.deleteExpense(expense) }

    // ---- Export ----

    fun exportTimeCsv() = viewModelScope.launch {
        if (!Entitlement.canExportCsv(getApplication())) {
            _message.value = "Export is a paid feature."
            return@launch
        }
        val entries = repo.allEntriesForExport()
        if (entries.isEmpty()) {
            _message.value = "No hours to export yet."
            return@launch
        }
        val ok = CsvExport.share(
            getApplication(),
            "timesheet",
            CsvExport.buildTimeCsv(entries)
        )
        if (!ok) _message.value = "Couldn't create the file. Free up some storage and try again."
    }

    fun exportExpenseCsv() = viewModelScope.launch {
        if (!Entitlement.canExportCsv(getApplication())) {
            _message.value = "Export is a paid feature."
            return@launch
        }
        val list = repo.allExpensesForExport()
        if (list.isEmpty()) {
            _message.value = "No expenses to export yet."
            return@launch
        }
        val ok = CsvExport.share(
            getApplication(),
            "expenses",
            CsvExport.buildExpenseCsv(list, repo.jobNamesById())
        )
        if (!ok) _message.value = "Couldn't create the file. Free up some storage and try again."
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                AppViewModel(this[APPLICATION_KEY] as Application)
            }
        }
    }
}
