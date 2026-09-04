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
    private val onLogReported = Subscription<String>()

    @Suppress("TooGenericExceptionCaught")
    private suspend fun writeToFile(log: String) {
        queue.add(log)
        runner.enqueue {
            try {
                writeQueuedLogs()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Unable to write to the log file", e)
            }
            // Give the queue time to buffer to make writes more efficient
            delay(1000.milliseconds)
        }
    }

    private suspend fun writeQueuedLogs() {
        val file = files.getFile(LOG_FILE_NAME, true)
        val newLogs = mutableListOf<String>()
        while (queue.isNotEmpty()) {
            queue.poll()?.let { newLogs.add(it) }
        }
        onIO {
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
        onLogReported.subscribe(this::writeToFile)
    }

    fun getLogFile(): File {
        return files.getFile(LOG_FILE_NAME, false)
    }

    fun debug(tag: String?, message: String) {
        Log.d(tag, message)
        onLogReported.publish(formatLog("D", tag, message, null))
    }

    fun debug(tag: String?, message: String, throwable: Throwable?) {
        Log.d(tag, message, throwable)
        onLogReported.publish(formatLog("D", tag, message, throwable))
    }

    fun info(tag: String?, message: String) {
        Log.i(tag, message)
        onLogReported.publish(formatLog("I", tag, message, null))
    }

    fun info(tag: String?, message: String, throwable: Throwable?) {
        Log.i(tag, message, throwable)
        onLogReported.publish(formatLog("I", tag, message, throwable))
    }

    fun warn(tag: String?, message: String) {
        Log.w(tag, message)
        onLogReported.publish(formatLog("W", tag, message, null))
    }

    fun warn(tag: String?, message: String, throwable: Throwable?) {
        Log.w(tag, message, throwable)
        onLogReported.publish(formatLog("W", tag, message, throwable))
    }

    fun error(tag: String?, message: String) {
        Log.e(tag, message)
        onLogReported.publish(formatLog("E", tag, message, null))
    }

    fun error(tag: String?, message: String, throwable: Throwable?) {
        Log.e(tag, message, throwable)
        onLogReported.publish(formatLog("E", tag, message, throwable))
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
