package com.kylecorry.trail_sense.main.errors

import android.content.Context
import com.kylecorry.andromeda.core.system.CurrentApp
import com.kylecorry.andromeda.core.tryOrLog
import com.kylecorry.andromeda.exceptions.IBugReportGenerator
import com.kylecorry.andromeda.files.IFileSystem
import com.kylecorry.andromeda.files.LocalFileSystem

abstract class BaseExceptionHandler(
    protected val context: Context,
    private val generator: IBugReportGenerator,
    private val filename: String = "errors/error.txt",
    private val fileSystem: IFileSystem = LocalFileSystem(context),
    private val shouldRestartApp: Boolean = true,
    private val retainedErrorCount: Int = 0
) {

    fun bind() {
        if (!fileSystem.getFile(filename, create = false).exists()) {
            setupHandler()
        }
        handleLastException()
    }

    abstract fun handleBugReport(log: String)

    open fun handleException(throwable: Throwable, details: String): Boolean {
        return false
    }

    private fun handleLastException() {
        val file = fileSystem.getFile(filename, create = false)
        if (!file.exists()) {
            return
        }
        val error = fileSystem.read(filename)
        fileSystem.delete(filename)

        handleBugReport(error)
        setupHandler()
    }

    private fun setupHandler() {
        val handler = { throwable: Throwable ->
            val details = generator.generate(context, throwable)
            tryOrLog {
                retainException(details)
            }
            if (!handleException(throwable, details)) {
                recordException(details)
                if (shouldRestartApp) {
                    tryOrLog {
                        CurrentApp.restart(context)
                    }
                }
            }
        }

        wrapOnUncaughtException(handler)
    }

    private fun recordException(details: String) {
        fileSystem.write(filename, details, false)
    }

    private fun retainException(details: String) {
        if (retainedErrorCount <= 0) {
            return
        }

        val directory = filename.substringBeforeLast('/', "")
        val historyDirectory = if (directory.isEmpty()) {
            "history"
        } else {
            "$directory/history"
        }
        val timestamp = System.currentTimeMillis()
        fileSystem.write("$historyDirectory/crash-$timestamp.txt", details, false)
        fileSystem.list(historyDirectory)
            .filter { it.isFile }
            .sortedByDescending { it.lastModified() }
            .drop(retainedErrorCount)
            .forEach { it.delete() }
    }

    private fun wrapOnUncaughtException(exceptionHandler: (throwable: Throwable) -> Unit) {
        val currentHandler = Thread.getDefaultUncaughtExceptionHandler()
        if (currentHandler !== installedHandler) {
            systemHandler = currentHandler
        }

        val originalHandler = systemHandler
        val handler = Thread.UncaughtExceptionHandler { thread, throwable ->
            try {
                exceptionHandler(throwable)
            } finally {
                originalHandler?.uncaughtException(thread, throwable)
            }
        }

        installedHandler = handler
        Thread.setDefaultUncaughtExceptionHandler(handler)
    }

    companion object {
        private var installedHandler: Thread.UncaughtExceptionHandler? = null
        private var systemHandler: Thread.UncaughtExceptionHandler? = null
    }

}
