package com.kylecorry.trail_sense.main

import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.system.CurrentApp
import com.kylecorry.andromeda.core.system.Intents
import com.kylecorry.andromeda.core.tryOrLog
import com.kylecorry.andromeda.files.LocalFiles
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.errors.MainBugReportGenerator

object ExceptionHandler {

    fun initialize(activity: MainActivity) {
        if (!LocalFiles.getFile(activity, FILENAME, create = false).exists()) {
            setupHandler(activity)
        }
        handleLastException(activity)
    }

    private fun handleLastException(context: MainActivity) {
        val file = LocalFiles.getFile(context, FILENAME, create = false)
        if (!file.exists()) {
            return
        }
        val body = LocalFiles.read(context, FILENAME)
        LocalFiles.delete(context, FILENAME)

        Alerts.dialog(
            context,
            context.getString(R.string.error_occurred),
            context.getString(R.string.error_occurred_message),
            okText = context.getString(R.string.pref_email_title)
        ) { cancelled ->
            if (!cancelled) {
                val intent = Intents.email(
                    context.getString(R.string.email),
                    "Error in ${context.getString(R.string.app_name)}",
                    body
                )

                context.startActivity(intent)
            } else {
                setupHandler(context)
            }
        }
    }

    private fun setupHandler(context: MainActivity) {
        Thread.setDefaultUncaughtExceptionHandler { _, throwable ->
            recordException(context, throwable)
            tryOrLog {
                CurrentApp.restart(context)
            }
        }
    }

    private fun recordException(activity: MainActivity, throwable: Throwable) {
        val details = MainBugReportGenerator(activity, throwable).generate()
        LocalFiles.write(activity, FILENAME, details, false)
    }

    private const val FILENAME = "errors/error.txt"

}