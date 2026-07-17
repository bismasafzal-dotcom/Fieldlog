package com.fieldlog.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * A DAO ("Data Access Object") is just a list of database questions you can ask.
 * Room turns each @Query into real SQLite code at build time.
 *
 * Anything returning Flow<...> is LIVE: the screen redraws by itself the moment
 * the underlying rows change. You never have to remember to refresh.
 */

@Dao
interface JobDao {

    @Query("SELECT * FROM jobs WHERE archived = 0 ORDER BY name COLLATE NOCASE ASC")
    fun activeJobs(): Flow<List<Job>>

    @Query("SELECT * FROM jobs ORDER BY archived ASC, name COLLATE NOCASE ASC")
    fun allJobs(): Flow<List<Job>>

    @Query("SELECT * FROM jobs WHERE id = :id")
    suspend fun byId(id: Long): Job?

    @Query("SELECT * FROM jobs")
    suspend fun allJobsNow(): List<Job>

    @Upsert
    suspend fun upsert(job: Job): Long

    @Delete
    suspend fun delete(job: Job)
}

@Dao
interface TimeEntryDao {

    /** The one entry with no end time, if the user is currently on the clock. */
    @Query("SELECT * FROM time_entries WHERE endedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    fun running(): Flow<TimeEntry?>

    @Query("SELECT * FROM time_entries WHERE endedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    suspend fun runningNow(): TimeEntry?

    @Query(
        """
        SELECT e.*, j.name AS jobName, j.hourlyRateCents AS hourlyRateCents
        FROM time_entries e
        JOIN jobs j ON j.id = e.jobId
        WHERE e.startedAt >= :from AND e.startedAt < :to
        ORDER BY e.startedAt DESC
        """
    )
    fun entriesBetween(from: Long, to: Long): Flow<List<EntryWithJobRow>>

    @Query(
        """
        SELECT e.*, j.name AS jobName, j.hourlyRateCents AS hourlyRateCents
        FROM time_entries e
        JOIN jobs j ON j.id = e.jobId
        ORDER BY e.startedAt DESC
        """
    )
    suspend fun allEntriesWithJob(): List<EntryWithJobRow>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: TimeEntry): Long

    @Update
    suspend fun update(entry: TimeEntry)

    @Delete
    suspend fun delete(entry: TimeEntry)

    @Query("DELETE FROM time_entries WHERE jobId = :jobId")
    suspend fun deleteForJob(jobId: Long)
}

@Dao
interface ExpenseDao {

    @Query("SELECT * FROM expenses WHERE occurredAt >= :from AND occurredAt < :to ORDER BY occurredAt DESC")
    fun expensesBetween(from: Long, to: Long): Flow<List<Expense>>

    @Query("SELECT * FROM expenses ORDER BY occurredAt DESC")
    fun allExpenses(): Flow<List<Expense>>

    @Query("SELECT * FROM expenses ORDER BY occurredAt DESC")
    suspend fun allExpensesNow(): List<Expense>

    @Upsert
    suspend fun upsert(expense: Expense): Long

    @Delete
    suspend fun delete(expense: Expense)

    @Query("DELETE FROM expenses WHERE jobId = :jobId")
    suspend fun deleteForJob(jobId: Long)
}

/**
 * Room needs a flat class to pour a JOIN result into — it can't build a nested
 * object on its own. We map this into the nicer [EntryWithJob] right after.
 */
data class EntryWithJobRow(
    val id: Long,
    val jobId: Long,
    val startedAt: Long,
    val endedAt: Long?,
    val note: String,
    val jobName: String,
    val hourlyRateCents: Long
) {
    fun toEntryWithJob() = EntryWithJob(
        entry = TimeEntry(id, jobId, startedAt, endedAt, note),
        jobName = jobName,
        hourlyRateCents = hourlyRateCents
    )
}
