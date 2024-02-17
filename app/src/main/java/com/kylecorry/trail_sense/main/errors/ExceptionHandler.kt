package com.kylecorry.trail_sense.main.errors

import android.content.Context
import android.util.Log
import com.kylecorry.andromeda.exceptions.AggregateBugReportGenerator
import com.kylecorry.andromeda.exceptions.AndroidDetailsBugReportGenerator
import com.kylecorry.andromeda.exceptions.AppDetailsBugReportGenerator
import com.kylecorry.andromeda.exceptions.BugReportEmailMessage
import com.kylecorry.andromeda.exceptions.DeviceDetailsBugReportGenerator
import com.kylecorry.andromeda.exceptions.EmailExceptionHandler
import com.kylecorry.andromeda.exceptions.StackTraceBugReportGenerator
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.MainActivity
import com.kylecorry.trail_sense.shared.debugging.isDebug

object ExceptionHandler {

    fun initialize(context: Context) {
        val handler = EmailExceptionHandler(
            context.applicationContext,
            AggregateBugReportGenerator(
                listOf(
                    AppDetailsBugReportGenerator(context.getString(R.string.app_name)),
                    AndroidDetailsBugReportGenerator(),
                    DeviceDetailsBugReportGenerator(),
                    FragmentDetailsBugReportGenerator(),
                    DiagnosticsBugReportGenerator(),
                    StackTraceBugReportGenerator()
                )
            ),
            shouldRestartApp = false,
            shouldWrapSystemExceptionHandler = true
        ) { context, log ->
            Log.e("Trail Sense", log)
            BugReportEmailMessage(
                context.getString(R.string.error_occurred),
                context.getString(R.string.error_occurred_message) + if (isDebug()) {
                    "\n\n$log"
                } else {
                    ""
                },
                context.getString(R.string.pref_email_title),
                context.getString(android.R.string.cancel),
                context.getString(R.string.email),
                "Error in ${context.getString(R.string.app_name)}"
            )
        }
        handler.bind()
    }
}