package com.fieldlog.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fieldlog.app.data.EntryWithJob
import com.fieldlog.app.data.Job
import com.fieldlog.app.data.TimeEntry
import com.fieldlog.app.ui.DocketLabel
import com.fieldlog.app.ui.EmptyState
import com.fieldlog.app.ui.FlatCard
import com.fieldlog.app.ui.theme.Docket
import com.fieldlog.app.ui.theme.Meter
import com.fieldlog.app.util.formatClock
import com.fieldlog.app.util.formatDuration
import com.fieldlog.app.util.formatMoney
import com.fieldlog.app.util.formatTime
import kotlinx.coroutines.delay

/**
 * Ticks once a second, but only while this screen is actually on-screen.
 * Compose stops it the moment the user leaves — no battery burned in a pocket.
 */
@Composable
private fun rememberTicker(): State<Long> = produceState(System.currentTimeMillis()) {
    while (true) {
        value = System.currentTimeMillis()
        delay(1000)
    }
}

@Composable
fun ClockScreen(
    jobs: List<Job>,
    running: TimeEntry?,
    runningJob: Job?,
    todayEntries: List<EntryWithJob>,
    onClockIn: (Long) -> Unit,
    onClockOut: () -> Unit,
    onDeleteEntry: (TimeEntry) -> Unit,
    onGoToJobs: () -> Unit
) {
    if (jobs.isEmpty()) {
        Column(Modifier.fillMaxSize()) {
            EmptyState(
                headline = "No jobs yet",
                instruction = "Add your first job on the Jobs tab, then come back here to start the clock.",
                modifier = Modifier.weight(1f)
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.secondary)
                    .clickable { onGoToJobs() }
                    .padding(vertical = 22.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "ADD A JOB",
                    style = Meter.copy(fontSize = 20.sp),
                    color = MaterialTheme.colorScheme.onSecondary
                )
            }
        }
        return
    }

    val now by rememberTicker()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Spacer(Modifier.height(4.dp)) }

        // ---- THE SLAB ----
        item {
            if (running != null && runningJob != null) {
                RunningSlab(
                    jobName = runningJob.name,
                    elapsedMs = running.durationMs(now),
                    startedAt = running.startedAt,
                    hourlyRateCents = runningJob.hourlyRateCents,
                    onClockOut = onClockOut
                )
            } else {
                ClockedOutSlab()
            }
        }

        // ---- Pick a job to start (hidden while running) ----
        if (running == null) {
            item { DocketLabel("Start the clock on") }

            // NOTE THE KEY PREFIX. A LazyColumn's keys must be unique across the WHOLE
            // list, and this list holds BOTH jobs and time entries. Job #1 and time
            // entry #1 would both produce the key 1, and Compose crashes hard on a
            // duplicate key. Prefixing keeps them in separate namespaces.
            items(jobs, key = { "job-${it.id}" }) { job ->
                JobStartRow(job = job, onClick = { onClockIn(job.id) })
            }
        }

        // ---- Today's entries ----
        item {
            Spacer(Modifier.height(8.dp))
            val todayMs = todayEntries.sumOf { it.entry.durationMs(now) }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                DocketLabel("Today")
                Text(
                    formatDuration(todayMs),
                    style = Meter.copy(fontSize = 16.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (todayEntries.isEmpty()) {
            item {
                Text(
                    "Nothing logged today.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        } else {
            // Same reason as above — a different prefix, so these can never collide
            // with the job rows even when the database IDs are identical.
            items(todayEntries, key = { "entry-${it.entry.id}" }) { e ->
                EntryRow(
                    entry = e,
                    now = now,
                    onDelete = { onDeleteEntry(e.entry) }
                )
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}

/**
 * The signature element. Clocked out: a quiet slab. Clocked in: it turns green and
 * becomes a live meter you can read from across a worksite.
 */
@Composable
private fun ClockedOutSlab() {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 28.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "0:00:00",
                style = Meter.copy(fontSize = 52.sp),
                color = MaterialTheme.colorScheme.outline
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Off the clock",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RunningSlab(
    jobName: String,
    elapsedMs: Long,
    startedAt: Long,
    hourlyRateCents: Long,
    onClockOut: () -> Unit
) {
    val earnedSoFar = (elapsedMs * hourlyRateCents) / 3_600_000L
    val onGreen = MaterialTheme.colorScheme.onTertiary

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.tertiary)   // green = on the clock, always
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            jobName.uppercase(),
            style = Docket,
            color = onGreen.copy(alpha = 0.75f),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(10.dp))

        // The big readout. Monospace so the digits don't jitter as they tick.
        Text(
            formatClock(elapsedMs),
            style = Meter.copy(fontSize = 56.sp),
            color = onGreen
        )

        Spacer(Modifier.height(6.dp))
        Text(
            buildString {
                append("Since ${formatTime(startedAt)}")
                if (hourlyRateCents > 0) append("  ·  ${formatMoney(earnedSoFar)} earned")
            },
            style = MaterialTheme.typography.bodyMedium,
            color = onGreen.copy(alpha = 0.85f)
        )

        Spacer(Modifier.height(18.dp))

        // Clock out is the loud action while running — the accent colour, on green.
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.secondary)
                .clickable { onClockOut() }
                .padding(vertical = 22.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "CLOCK OUT",
                style = Meter.copy(fontSize = 20.sp),
                color = MaterialTheme.colorScheme.onSecondary
            )
        }
    }
}

@Composable
private fun JobStartRow(job: Job, onClick: () -> Unit) {
    FlatCard(
        modifier = Modifier
            .heightIn(min = 64.dp)   // glove-sized
            .clickable { onClick() }
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(job.name, style = MaterialTheme.typography.bodyLarge)
                if (job.client.isNotBlank() || job.hourlyRateCents > 0) {
                    Text(
                        listOfNotNull(
                            job.client.takeIf { it.isNotBlank() },
                            job.hourlyRateCents.takeIf { it > 0 }?.let { "${formatMoney(it)}/hr" }
                        ).joinToString("  ·  "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Box(
                Modifier
                    .clip(RoundedCornerShape(3.dp))
                    .background(MaterialTheme.colorScheme.secondary)
                    .padding(horizontal = 18.dp, vertical = 12.dp)
            ) {
                Text(
                    "CLOCK IN",
                    style = Meter.copy(fontSize = 14.sp),
                    color = MaterialTheme.colorScheme.onSecondary
                )
            }
        }
    }
}

@Composable
private fun EntryRow(entry: EntryWithJob, now: Long, onDelete: () -> Unit) {
    FlatCard {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(entry.jobName, style = MaterialTheme.typography.bodyLarge)
                Text(
                    buildString {
                        append(formatTime(entry.entry.startedAt))
                        append(" – ")
                        append(entry.entry.endedAt?.let { formatTime(it) } ?: "running")
                        if (entry.entry.note.isNotBlank()) append("  ·  ${entry.entry.note}")
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                formatDuration(entry.entry.durationMs(now)),
                style = Meter.copy(fontSize = 16.sp),
                color = if (entry.entry.isRunning) MaterialTheme.colorScheme.tertiary
                else MaterialTheme.colorScheme.onSurface
            )
            IconButton(onClick = onDelete, modifier = Modifier.height(48.dp)) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete this entry",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
