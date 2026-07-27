package com.kylecorry.trail_sense.tools.diagnostics.infrastructure

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.content.getSystemService
import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class DiagnosticsLogExportService(
    private val context: Context,
    private val files: FileSubsystem = FileSubsystem.getInstance(context)
) {

    suspend fun export(destination: Uri): Unit = onIO {
        files.output(destination)?.use { output ->
            ZipOutputStream(output).use { zip ->
                addStackTraces(zip)
                addExitReasons(zip)
                addLogcat(zip)
            }
        } ?: throw IllegalStateException("Unable to open diagnostics log destination")
    }

    private fun addStackTraces(zip: ZipOutputStream) {
        getStackTraceFiles()
            .take(DiagnosticsLogConfig.STACK_TRACE_COUNT)
            .forEachIndexed { index, file ->
                val timestamp = filenameTimestamp(file.lastModified())
                zip.putNextEntry(ZipEntry("stack-traces/crash-$timestamp-$index.txt"))
                file.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
    }

    private fun getStackTraceFiles(): List<File> {
        val history = files.list(ERROR_HISTORY_DIRECTORY)
            .filter { it.isFile }
        val pending = files.get(PENDING_ERROR_FILE)
            .takeIf { it.isFile }
            ?.let { listOf(it) }
            ?: emptyList()
        return (history + pending)
            .distinctBy { it.absolutePath }
            .sortedByDescending { it.lastModified() }
    }

    private fun addExitReasons(zip: ZipOutputStream) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return
        }

        val activityManager = context.getSystemService<ActivityManager>() ?: return
        activityManager.getHistoricalProcessExitReasons(
            context.packageName,
            0,
            DiagnosticsLogConfig.EXIT_REASON_COUNT
        )
            .sortedByDescending { it.timestamp }
            .take(DiagnosticsLogConfig.EXIT_REASON_COUNT)
            .forEachIndexed { index, info ->
                val timestamp = filenameTimestamp(info.timestamp)
                zip.putText(
                    "exit-reasons/exit-$timestamp-$index.txt",
                    formatExitReason(info)
                )
                info.traceInputStream?.use { trace ->
                    zip.putNextEntry(ZipEntry("exit-reasons/exit-$timestamp-$index.trace"))
                    trace.copyTo(zip)
                    zip.closeEntry()
                }
            }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun formatExitReason(info: ApplicationExitInfo): String {
        return buildString {
            appendLine("Timestamp: ${Instant.ofEpochMilli(info.timestamp)}")
            appendLine("Reason: ${getReasonName(info.reason)} (${info.reason})")
            appendLine("Description: ${info.description.orEmpty()}")
            appendLine("Process: ${info.processName}")
            appendLine("PID: ${info.pid}")
            appendLine("Real UID: ${info.realUid}")
            appendLine("Package UID: ${info.packageUid}")
            appendLine("Defining UID: ${info.definingUid}")
            appendLine("Importance: ${info.importance}")
            appendLine("Status: ${info.status}")
            appendLine("PSS: ${info.pss}")
            appendLine("RSS: ${info.rss}")
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun getReasonName(reason: Int): String {
        return when (reason) {
            ApplicationExitInfo.REASON_ANR -> "ANR"
            ApplicationExitInfo.REASON_CRASH -> "Crash"
            ApplicationExitInfo.REASON_CRASH_NATIVE -> "Native crash"
            ApplicationExitInfo.REASON_DEPENDENCY_DIED -> "Dependency died"
            ApplicationExitInfo.REASON_EXCESSIVE_RESOURCE_USAGE -> "Excessive resource usage"
            ApplicationExitInfo.REASON_EXIT_SELF -> "Self exit"
            ApplicationExitInfo.REASON_INITIALIZATION_FAILURE -> "Initialization failure"
            ApplicationExitInfo.REASON_LOW_MEMORY -> "Low memory"
            ApplicationExitInfo.REASON_OTHER -> "Other"
            ApplicationExitInfo.REASON_PERMISSION_CHANGE -> "Permission change"
            ApplicationExitInfo.REASON_SIGNALED -> "Signaled"
            ApplicationExitInfo.REASON_UNKNOWN -> "Unknown"
            ApplicationExitInfo.REASON_USER_REQUESTED -> "User requested"
            else -> "Unknown"
        }
    }

    private fun addLogcat(zip: ZipOutputStream) {
        val cutoff = Instant.now()
            .minus(DiagnosticsLogConfig.LOGCAT_HISTORY_DURATION)
            .atZone(ZoneId.systemDefault())
            .format(LOGCAT_TIMESTAMP_FORMAT)
        val process = ProcessBuilder(
            "logcat",
            "-d",
            "-v",
            "threadtime",
            "-T",
            cutoff
        )
            .redirectErrorStream(true)
            .start()

        val output = try {
            process.inputStream.bufferedReader().use { it.readText() }
        } finally {
            process.destroy()
        }

        zip.putText(
            "logcat/logcat-${filenameTimestamp(System.currentTimeMillis())}.txt",
            output
        )
    }

    private fun ZipOutputStream.putText(path: String, text: String) {
        putNextEntry(ZipEntry(path))
        write(text.toByteArray())
        closeEntry()
    }

    private fun filenameTimestamp(timestamp: Long): String {
        return Instant.ofEpochMilli(timestamp)
            .atZone(ZoneId.systemDefault())
            .format(FILENAME_TIMESTAMP_FORMAT)
    }

    companion object {
        private const val ERROR_HISTORY_DIRECTORY = "errors/history"
        private const val PENDING_ERROR_FILE = "errors/error.txt"
        private val LOGCAT_TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("MM-dd HH:mm:ss.SSS", Locale.US)
        private val FILENAME_TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss-SSS", Locale.US)
    }
}
