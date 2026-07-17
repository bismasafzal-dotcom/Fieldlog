package com.fieldlog.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fieldlog.app.data.JobTotals
import com.fieldlog.app.ui.DocketLabel
import com.fieldlog.app.ui.EmptyState
import com.fieldlog.app.ui.FlatCard
import com.fieldlog.app.ui.Period
import com.fieldlog.app.ui.StatBlock
import com.fieldlog.app.ui.theme.Meter
import com.fieldlog.app.util.formatDecimalHours
import com.fieldlog.app.util.formatDuration
import com.fieldlog.app.util.formatMoney

@Composable
fun SummaryScreen(
    period: Period,
    totals: List<JobTotals>,
    onPeriodChange: (Period) -> Unit,
    onExportTime: () -> Unit,
    onExportExpenses: () -> Unit
) {
    val workedMs = totals.sumOf { it.workedMs }
    val earned = totals.sumOf { it.earnedCents }
    val spent = totals.sumOf { it.expensesCents }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Spacer(Modifier.height(4.dp)) }

        // Period switch
        item {
            Row(Modifier.fillMaxWidth()) {
                Period.entries.forEach { p ->
                    PeriodTab(
                        label = p.label,
                        selected = p == period,
                        onClick = { onPeriodChange(p) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        if (totals.isEmpty()) {
            item {
                EmptyState(
                    headline = "Nothing in ${period.label.lowercase()}",
                    instruction = "Clock in on a job and your hours will show up here.",
                    modifier = Modifier.height(280.dp)
                )
            }
        } else {
            // Headline numbers
            item {
                FlatCard {
                    Column(Modifier.padding(16.dp)) {
                        Row(Modifier.fillMaxWidth()) {
                            StatBlock(
                                label = "Hours",
                                value = formatDecimalHours(workedMs),
                                modifier = Modifier.weight(1f)
                            )
                            StatBlock(
                                label = "Earned",
                                value = formatMoney(earned),
                                valueColor = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.weight(1f)
                            )
                            StatBlock(
                                label = "Spent",
                                value = formatMoney(spent),
                                valueColor = MaterialTheme.colorScheme.error,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(Modifier.height(14.dp))
                        Text(
                            "Net ${formatMoney(earned - spent)} · ${formatDuration(workedMs)} worked",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item { DocketLabel("By job", Modifier.padding(top = 8.dp)) }

            items(totals, key = { it.jobId }) { t ->
                FlatCard {
                    Column(Modifier.padding(16.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                t.jobName,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                formatDecimalHours(t.workedMs) + " h",
                                style = Meter.copy(fontSize = 18.sp)
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            buildString {
                                append("Earned ${formatMoney(t.earnedCents)}")
                                if (t.expensesCents > 0) append("  ·  Spent ${formatMoney(t.expensesCents)}")
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Export
        item {
            Spacer(Modifier.height(12.dp))
            DocketLabel("Export")
            Spacer(Modifier.height(2.dp))
            Text(
                "Writes a spreadsheet file on your phone. Save it, or send it when you next get signal.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ExportButton("Hours CSV", onExportTime, Modifier.weight(1f))
                ExportButton("Expenses CSV", onExportExpenses, Modifier.weight(1f))
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun PeriodTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier
            .clip(RoundedCornerShape(3.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surface
            )
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(3.dp))
            .clickable { onClick() }
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (selected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ExportButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
            .clickable { onClick() }
            .padding(vertical = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label.uppercase(),
            style = Meter.copy(fontSize = 14.sp),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
