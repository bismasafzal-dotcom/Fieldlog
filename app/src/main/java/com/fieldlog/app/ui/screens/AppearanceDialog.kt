package com.fieldlog.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.fieldlog.app.ui.DocketLabel
import com.fieldlog.app.ui.theme.Accent
import com.fieldlog.app.ui.theme.ThemeMode

@Composable
fun AppearanceDialog(
    mode: ThemeMode,
    accent: Accent,
    onModeChange: (ThemeMode) -> Unit,
    onAccentChange: (Accent) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Appearance") },
        text = {
            Column {
                DocketLabel("Background")
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeMode.entries.forEach { m ->
                        ChoiceBox(
                            label = m.label,
                            selected = m == mode,
                            onClick = { onModeChange(m) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))
                DocketLabel("Accent")
                Spacer(Modifier.height(8.dp))

                // Swatches. Tapping one repaints the whole app instantly —
                // the accent is a theme colour, so every screen follows automatically.
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Accent.entries.forEach { a ->
                        Box(
                            Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(a.color)
                                .border(
                                    width = if (a == accent) 3.dp else 1.dp,
                                    color = if (a == accent) MaterialTheme.colorScheme.onSurface
                                    else MaterialTheme.colorScheme.outline,
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .clickable { onAccentChange(a) }
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))
                Text(
                    accent.label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(16.dp))
                Text(
                    "Green always means on the clock, and red always means money going out — " +
                        "so neither is offered as an accent. Those two colours have a job to do.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )
}

@Composable
private fun ChoiceBox(
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
            .height(48.dp),
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
