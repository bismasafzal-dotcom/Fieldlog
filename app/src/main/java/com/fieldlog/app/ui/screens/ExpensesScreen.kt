package com.fieldlog.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fieldlog.app.data.EXPENSE_CATEGORIES
import com.fieldlog.app.data.Expense
import com.fieldlog.app.data.Job
import com.fieldlog.app.ui.DocketLabel
import com.fieldlog.app.ui.EmptyState
import com.fieldlog.app.ui.FlatCard
import com.fieldlog.app.ui.theme.Meter
import com.fieldlog.app.util.formatDay
import com.fieldlog.app.util.formatMoney
import com.fieldlog.app.util.parseMoneyToCents

@Composable
fun ExpensesScreen(
    expenses: List<Expense>,
    jobs: List<Job>,
    onSave: (Expense) -> Unit,
    onDelete: (Expense) -> Unit
) {
    var showEditor by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Expense?>(null) }

    val jobNames = remember(jobs) { jobs.associate { it.id to it.name } }

    Column(Modifier.fillMaxSize()) {

        if (expenses.isEmpty()) {
            EmptyState(
                headline = "No expenses logged",
                instruction = "Log fuel, materials, parking — anything you spend on a job. It all lands in the CSV you send your accountant.",
                modifier = Modifier.weight(1f)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        DocketLabel("All expenses")
                        Text(
                            formatMoney(expenses.sumOf { it.amountCents }),
                            style = Meter.copy(fontSize = 16.sp),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                items(expenses, key = { it.id }) { x ->
                    FlatCard(
                        modifier = Modifier
                            .heightIn(min = 64.dp)
                            .clickable {
                                editing = x
                                showEditor = true
                            }
                    ) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(x.category, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    listOfNotNull(
                                        formatDay(x.occurredAt),
                                        x.jobId?.let { jobNames[it] },
                                        x.note.takeIf { it.isNotBlank() },
                                        if (!x.billable) "not billable" else null
                                    ).joinToString("  ·  "),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                formatMoney(x.amountCents),
                                style = Meter.copy(fontSize = 18.sp),
                                color = MaterialTheme.colorScheme.error
                            )
                            IconButton(onClick = { onDelete(x) }, modifier = Modifier.height(48.dp)) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete this expense",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
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
            Text(
                "LOG AN EXPENSE",
                style = Meter.copy(fontSize = 18.sp),
                color = MaterialTheme.colorScheme.onSecondary
            )
        }
    }

    if (showEditor) {
        ExpenseEditor(
            existing = editing,
            jobs = jobs,
            onDismiss = { showEditor = false },
            onSave = {
                onSave(it)
                showEditor = false
            }
        )
    }
}

@Composable
private fun ExpenseEditor(
    existing: Expense?,
    jobs: List<Job>,
    onDismiss: () -> Unit,
    onSave: (Expense) -> Unit
) {
    var amount by remember {
        mutableStateOf(existing?.amountCents?.let { formatMoney(it) } ?: "")
    }
    var category by remember { mutableStateOf(existing?.category ?: EXPENSE_CATEGORIES.first()) }
    var note by remember { mutableStateOf(existing?.note ?: "") }
    var jobId by remember { mutableStateOf(existing?.jobId) }
    var billable by remember { mutableStateOf(existing?.billable ?: true) }
    var amountError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Log an expense" else "Edit expense") },
        text = {
            Column {
                OutlinedTextField(
                    value = amount,
                    onValueChange = {
                        amount = it
                        amountError = false
                    },
                    label = { Text("Amount") },
                    placeholder = { Text("24.50") },
                    isError = amountError,
                    supportingText = if (amountError) {
                        { Text("Enter an amount, like 24.50") }
                    } else null,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(14.dp))
                DocketLabel("Category")
                Spacer(Modifier.height(6.dp))
                Row(Modifier.horizontalScroll(rememberScrollState())) {
                    EXPENSE_CATEGORIES.forEach { c ->
                        Chip(
                            label = c,
                            selected = category == c,
                            onClick = { category = c }
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                }

                if (jobs.isNotEmpty()) {
                    Spacer(Modifier.height(14.dp))
                    DocketLabel("Job")
                    Spacer(Modifier.height(6.dp))
                    Row(Modifier.horizontalScroll(rememberScrollState())) {
                        Chip(
                            label = "None",
                            selected = jobId == null,
                            onClick = { jobId = null }
                        )
                        Spacer(Modifier.width(8.dp))
                        jobs.forEach { j ->
                            Chip(
                                label = j.name,
                                selected = jobId == j.id,
                                onClick = { jobId = j.id }
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { billable = !billable }
                ) {
                    Checkbox(checked = billable, onCheckedChange = { billable = it })
                    Text("Bill this back to the client", style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val cents = parseMoneyToCents(amount)
                if (cents == null || cents <= 0) {
                    amountError = true
                    return@TextButton
                }
                onSave(
                    (existing ?: Expense(amountCents = 0)).copy(
                        amountCents = cents,
                        category = category,
                        note = note.trim(),
                        jobId = jobId,
                        billable = billable
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

@Composable
private fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(3.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (selected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
