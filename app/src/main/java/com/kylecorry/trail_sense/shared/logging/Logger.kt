package com.kylecorry.trail_sense.shared.logging

import android.content.Context
import android.util.Log
import com.kylecorry.andromeda.files.CacheFileSystem
import com.kylecorry.luna.concurrency.CoroutineQueueRunner
import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.luna.subscriptions.generic.Subscription
import kotlinx.coroutines.CancellationException
import java.io.File
import kotlinx.coroutines.delay
import java.time.Instant
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.time.Duration.Companion.milliseconds

class Logger(context: Context) {

    private val files = CacheFileSystem(context)
    private val queue = ConcurrentLinkedQueue<String>()
    private val runner = CoroutineQueueRunner()
    private val onLogReported = Subscription<Unit>()
    private val fileLock = Any()

    @Suppress("TooGenericExceptionCaught")
    private suspend fun writeToFile() {
        runner.enqueue {
            try {
                onIO { writeQueuedLogs() }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Unable to write to the log file", e)
            }
            // Give the queue time to buffer to make writes more efficient
            delay(1000.milliseconds)
        }
    }

    private fun writeQueuedLogs() {
        synchronized(fileLock) {
            val newLogs = mutableListOf<String>()
            while (queue.isNotEmpty()) {
                queue.poll()?.let { newLogs.add(it) }
            }
            if (newLogs.isEmpty()) {
                return
            }

            val file = files.getFile(LOG_FILE_NAME, true)
            if (file.length() > MAX_LOG_LENGTH) {
                // Clear some room
                val tempFile = files.getFile("log_temp.txt", true)
                file.inputStream().use { input ->
                    tempFile.outputStream().use { output ->
                        input.skip(file.length() / 4)
                        input.copyTo(output)
                    }
                }
                file.delete()
                tempFile.renameTo(file)
            }

            file.appendText(newLogs.joinToString("\n", postfix = "\n"))
        }
    }

    init {
        onLogReported.subscribe { writeToFile() }
    }

    @Suppress("TooGenericExceptionCaught")
    fun flush() {
        try {
            writeQueuedLogs()
        } catch (e: Exception) {
            Log.e(TAG, "Unable to write to the log file", e)
        }
    }

    fun getLogFile(): File {
        return files.getFile(LOG_FILE_NAME, false)
    }

    fun debug(tag: String?, message: String, writeToFile: Boolean = true) {
        Log.d(tag, message)
        if (writeToFile) {
            report(formatLog("D", tag, message, null))
        }
    }

    fun debug(tag: String?, message: String, throwable: Throwable?, writeToFile: Boolean = true) {
        Log.d(tag, message, throwable)
        if (writeToFile) {
            report(formatLog("D", tag, message, throwable))
        }
    }

    fun info(tag: String?, message: String) {
        Log.i(tag, message)
        report(formatLog("I", tag, message, null))
    }

    fun info(tag: String?, message: String, throwable: Throwable?) {
        Log.i(tag, message, throwable)
        report(formatLog("I", tag, message, throwable))
    }

    fun warn(tag: String?, message: String) {
        Log.w(tag, message)
        report(formatLog("W", tag, message, null))
    }

    fun warn(tag: String?, message: String, throwable: Throwable?) {
        Log.w(tag, message, throwable)
        report(formatLog("W", tag, message, throwable))
    }

    fun error(tag: String?, message: String) {
        Log.e(tag, message)
        report(formatLog("E", tag, message, null))
    }

    fun error(tag: String?, message: String, throwable: Throwable?) {
        Log.e(tag, message, throwable)
        report(formatLog("E", tag, message, throwable))
    }

    private fun report(log: String) {
        queue.add(log)
        onLogReported.publish(Unit)
    }

    private fun formatLog(type: String, tag: String?, message: String, throwable: Throwable?): String {
        return "${Instant.now()} $tag\t\t[$type] $message\n${throwable?.stackTraceToString() ?: ""}".trim()
    }

    companion object {
        private const val TAG = "Logger"
        private const val LOG_FILE_NAME = "log.txt"

        // 256 KB
        private const val MAX_LOG_LENGTH = 256 * 1024
    }

}
