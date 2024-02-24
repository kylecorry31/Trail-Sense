package com.kylecorry.trail_sense.main.errors

import android.content.Context
import android.util.Log
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.system.Intents
import com.kylecorry.andromeda.exceptions.BugReportEmailMessage
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.debugging.isDebug

object ExceptionHandler {

    fun initialize(context: Context) {
        val handler = TrailSenseExceptionHandler(context)
        handler.bind()

        TrailSenseExceptionHandler.error?.let {
            Log.e("Trail Sense", it)
            val message = BugReportEmailMessage(
                context.getString(R.string.error_occurred),
                context.getString(R.string.error_occurred_message) + if (isDebug()) {
                    "\n\n$it"
                } else {
                    ""
                },
                context.getString(R.string.pref_email_title),
                context.getString(android.R.string.cancel),
                context.getString(R.string.email),
                "Error in ${context.getString(R.string.app_name)}"
            )
            Alerts.dialog(
                context,
                message.title,
                message.description,
                okText = message.emailAction,
                cancelText = message.ignoreAction
            ) { cancelled ->
                if (!cancelled) {
                    val intent = Intents.email(
                        message.emailAddress,
                        message.emailSubject,
                        it
                    )

                    context.startActivity(intent)
                }
            }
        }

        TrailSenseExceptionHandler.error = null
    }
}