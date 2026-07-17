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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fieldlog.app.data.Job
import com.fieldlog.app.ui.DocketLabel
import com.fieldlog.app.ui.EmptyState
import com.fieldlog.app.ui.FlatCard
import com.fieldlog.app.ui.theme.Meter
import com.fieldlog.app.util.formatMoney
import com.fieldlog.app.util.parseMoneyToCents

@Composable
fun JobsScreen(
    jobs: List<Job>,
    onSave: (Job) -> Unit,
    onDelete: (Job) -> Unit
) {
    var editing by remember { mutableStateOf<Job?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf<Job?>(null) }

    Column(Modifier.fillMaxSize()) {

        if (jobs.isEmpty()) {
            EmptyState(
                headline = "No jobs yet",
                instruction = "A job is anything you track hours against — a client, a site, a contract. Add the first one below.",
                modifier = Modifier.weight(1f)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { DocketLabel("Your jobs", Modifier.padding(top = 8.dp)) }
                items(jobs, key = { it.id }) { job ->
                    FlatCard(
                        modifier = Modifier
                            .heightIn(min = 64.dp)
                            .clickable {
                                editing = job
                                showEditor = true
                            }
                    ) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(job.name, style = MaterialTheme.typography.bodyLarge)
                                if (job.client.isNotBlank()) {
                                    Text(
                                        job.client,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Text(
                                if (job.hourlyRateCents > 0) "${formatMoney(job.hourlyRateCents)}/hr" else "—",
                                style = Meter.copy(fontSize = 16.sp),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }

        Box(
            Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.secondary)
                .clickable {
                    editing = null
                    showEditor = true
                }
                .padding(vertical = 22.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondary)
                Spacer(Modifier.height(0.dp))
                Text(
                    "  ADD A JOB",
                    style = Meter.copy(fontSize = 18.sp),
                    color = MaterialTheme.colorScheme.onSecondary
                )
            }
        }
    }

    if (showEditor) {
        JobEditor(
            existing = editing,
            onDismiss = { showEditor = false },
            onSave = {
                onSave(it)
                showEditor = false
            },
            onRequestDelete = {
                showEditor = false
                confirmDelete = it
            }
        )
    }

    confirmDelete?.let { job ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("Delete ${job.name}?") },
            text = {
                Text("This also deletes every hour and expense logged against it. It can't be undone.")
            },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(job)
                    confirmDelete = null
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = null }) { Text("Keep") }
            }
        )
    }
}

@Composable
private fun JobEditor(
    existing: Job?,
    onDismiss: () -> Unit,
    onSave: (Job) -> Unit,
    onRequestDelete: (Job) -> Unit
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var client by remember { mutableStateOf(existing?.client ?: "") }
    var rate by remember {
        mutableStateOf(
            existing?.hourlyRateCents?.takeIf { it > 0 }?.let { formatMoney(it) } ?: ""
        )
    }
    var nameError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "New job" else "Edit job") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = false
                    },
                    label = { Text("Job name") },
                    placeholder = { Text("Miller kitchen rewire") },
                    isError = nameError,
                    supportingText = if (nameError) {
                        { Text("Give the job a name so you can find it later.") }
                    } else null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = client,
                    onValueChange = { client = it },
                    label = { Text("Client (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = rate,
                    onValueChange = { rate = it },
                    label = { Text("Hourly rate (optional)") },
                    placeholder = { Text("45.00") },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    supportingText = { Text("Leave blank to track hours only.") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (existing != null) {
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { onRequestDelete(existing) }) {
                        Text("Delete this job", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isBlank()) {
                    nameError = true
                    return@TextButton
                }
                onSave(
                    (existing ?: Job(name = "")).copy(
                        name = name.trim(),
                        client = client.trim(),
                        hourlyRateCents = parseMoneyToCents(rate) ?: 0L
                    )
                )
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
