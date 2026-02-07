package com.kylecorry.trail_sense.main.errors

import android.content.Context
import android.os.Looper
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
    private val shouldRestartApp: Boolean = true
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
            if (handleException(throwable, details)) {
                true
            } else {
                recordException(details)
                if (shouldRestartApp) {
                    tryOrLog {
                        CurrentApp.restart(context)
                    }
                }
                false
            }
        }

        wrapOnUncaughtException(handler)
    }

    private fun recordException(details: String) {
        fileSystem.write(filename, details, false)
    }

    private fun wrapOnUncaughtException(exceptionHandler: (throwable: Throwable) -> Boolean) {
        val originalHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            var handled = false
            try {
                handled = exceptionHandler(throwable)
            } finally {
                if (!handled) {
                    originalHandler?.uncaughtException(thread, throwable)
                } else if (thread == Looper.getMainLooper().thread) {
                    Looper.loop()
                }
            }
        }
    }

}