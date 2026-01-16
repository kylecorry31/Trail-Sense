package com.kylecorry.trail_sense.main.errors

import android.util.Log
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.system.Intents
import com.kylecorry.andromeda.exceptions.BugReportEmailMessage
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.MainActivity
import com.kylecorry.trail_sense.shared.debugging.isDebug

object ExceptionHandler {

    fun initialize(activity: MainActivity) {
        val handler = TrailSenseExceptionHandler(activity)
        handler.bind()

        TrailSenseExceptionHandler.error?.let {
            Log.e("Trail Sense", it)
            val message = BugReportEmailMessage(
                activity.getString(R.string.error_occurred),
                activity.getString(R.string.error_occurred_message) + if (isDebug()) {
                    "\n\n$it"
                } else {
                    ""
                },
                activity.getString(R.string.pref_email_title),
                activity.getString(android.R.string.cancel),
                activity.getString(R.string.email),
                "Error in ${activity.getString(R.string.app_name)}"
            )
            Alerts.dialog(
                activity,
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

                    activity.startActivity(intent)
                }
            }
        }

        TrailSenseExceptionHandler.error = null
    }
}