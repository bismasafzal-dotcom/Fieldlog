package com.fieldlog.app.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.fieldlog.app.data.EntryWithJob
import com.fieldlog.app.data.Expense
import com.fieldlog.app.util.formatDecimalHours
import com.fieldlog.app.util.formatForCsv
import com.fieldlog.app.util.formatMoney
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Writes a CSV to the app's own cache folder, then hands it to Android's share sheet.
 *
 * This is still fully offline: the file is written on the phone with no network at all.
 * If the user picks email or WhatsApp with no signal, that app queues it and sends it
 * later by itself. If they pick "Save to Files", it never leaves the phone.
 */
object CsvExport {

    private fun csvCell(value: String): String {
        // A cell containing a comma, quote or newline has to be wrapped in quotes,
        // and any quote inside it doubled. Skip this and one note with a comma in it
        // will shift every column and quietly corrupt the whole file.
        val needsQuotes = value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
        val escaped = value.replace("\"", "\"\"")
        return if (needsQuotes) "\"$escaped\"" else escaped
    }

    private fun row(vararg cells: String) =
        cells.joinToString(",") { csvCell(it) } + "\n"

    fun buildTimeCsv(entries: List<EntryWithJob>): String {
        val sb = StringBuilder()
        sb.append(row("Job", "Client rate/hr", "Start", "End", "Hours", "Earned", "Note"))
        entries.forEach { e ->
            val ms = e.entry.durationMs()
            val earned = (ms * e.hourlyRateCents) / 3_600_000L
            sb.append(
                row(
                    e.jobName,
                    formatMoney(e.hourlyRateCents),
                    formatForCsv(e.entry.startedAt),
                    formatForCsv(e.entry.endedAt),
                    formatDecimalHours(ms),
                    formatMoney(earned),
                    e.entry.note
                )
            )
        }
        return sb.toString()
    }

    fun buildExpenseCsv(expenses: List<Expense>, jobNames: Map<Long, String>): String {
        val sb = StringBuilder()
        sb.append(row("Date", "Job", "Category", "Amount", "Billable", "Note"))
        expenses.forEach { x ->
            sb.append(
                row(
                    formatForCsv(x.occurredAt),
                    x.jobId?.let { jobNames[it] } ?: "",
                    x.category,
                    formatMoney(x.amountCents),
                    if (x.billable) "yes" else "no",
                    x.note
                )
            )
        }
        return sb.toString()
    }

    /** Writes [content] to a file and opens the share sheet. Returns false if it couldn't. */
    fun share(context: Context, prefix: String, content: String): Boolean = try {
        val stamp = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, "${prefix}_$stamp.csv")
        file.writeText(content)

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "${prefix.replaceFirstChar { it.uppercase() }} — $stamp")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            Intent.createChooser(intent, "Export CSV").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
        true
    } catch (e: Exception) {
        false
    }
}
