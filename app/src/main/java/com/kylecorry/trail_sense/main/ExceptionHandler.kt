package com.kylecorry.trail_sense.main

import android.util.Log
import com.kylecorry.andromeda.exceptions.*
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.errors.DiagnosticsBugReportGenerator
import com.kylecorry.trail_sense.shared.errors.FragmentDetailsBugReportGenerator

object ExceptionHandler {

    fun initialize(activity: MainActivity) {
        val handler = EmailExceptionHandler(
            activity,
            AggregateBugReportGenerator(
                listOf(
                    AppDetailsBugReportGenerator(activity.getString(R.string.app_name)),
                    AndroidDetailsBugReportGenerator(),
                    DeviceDetailsBugReportGenerator(),
                    FragmentDetailsBugReportGenerator(),
                    DiagnosticsBugReportGenerator(),
                    StackTraceBugReportGenerator()
                )
            )
        ) { context, log ->
            Log.e("Trail Sense", log)
            BugReportEmailMessage(
                context.getString(R.string.error_occurred),
                context.getString(R.string.error_occurred_message),
                context.getString(R.string.pref_email_title),
                context.getString(android.R.string.cancel),
                context.getString(R.string.email),
                "Error in ${context.getString(R.string.app_name)}"
            )
        }
        handler.bind()
    }
}