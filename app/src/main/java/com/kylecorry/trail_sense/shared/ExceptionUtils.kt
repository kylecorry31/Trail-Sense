package com.kylecorry.trail_sense.shared

import com.kylecorry.andromeda.core.system.Intents
import com.kylecorry.trail_sense.MainActivity
import com.kylecorry.trail_sense.shared.errors.MainBugReportGenerator

object ExceptionUtils {

    fun report(activity: MainActivity, throwable: Throwable?, email: String, appName: String) {
        val body = MainBugReportGenerator(activity, throwable).generate()

        val intent = Intents.email(
            email,
            "Error in $appName",
            body
        )

        activity.startActivity(intent)
    }

}