package com.kylecorry.trail_sense.main

import android.content.Context
import android.content.Intent
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.system.CurrentApp
import com.kylecorry.andromeda.core.system.Intents
import com.kylecorry.andromeda.core.tryOrLog
import com.kylecorry.andromeda.files.LocalFiles
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.errors.MainBugReportGenerator

object ExceptionHandler {

    private var hasException: Boolean = false

    fun initialize(activity: MainActivity) {
        hasException = LocalFiles.getFile(activity, FILENAME, create = false).exists()
        Thread.setDefaultUncaughtExceptionHandler { _, throwable ->
            recordException(activity, throwable)
            if (!hasException) {
                tryOrLog {
                    CurrentApp.restart(activity)
                }
            } else {
                CurrentApp.kill()
            }
        }
        handleLastException(activity)
    }

    private fun handleLastException(context: Context) {
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
            hasException = false
            if (!cancelled) {
                val intent = Intents.email(
                    context.getString(R.string.email),
                    "Error in ${context.getString(R.string.app_name)}",
                    body
                )

                context.startActivity(intent)
            }
        }
    }

    private fun recordException(activity: MainActivity, throwable: Throwable) {
        val details = MainBugReportGenerator(activity, throwable).generate()
        LocalFiles.write(activity, FILENAME, details, false)
    }

    private fun restart(context: Context) {
        tryOrLog {
            val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            val componentName = intent!!.component
            val mainIntent = Intent.makeRestartActivityTask(componentName)
            context.startActivity(mainIntent)
            Runtime.getRuntime().exit(0)
        }
    }

    private const val FILENAME = "errors/error.txt"

}