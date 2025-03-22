package com.kylecorry.trail_sense.main

import android.content.Context
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.system.Package
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.commands.Command

class NotifyForkPoliciesCommand(private val context: Context) : Command {
    override fun execute() {
        val packageName = Package.getPackageName(context)
        val appName = context.getString(R.string.app_name)
        val supportEmail = context.getString(R.string.email)

        val trailSensePackageNames = listOf(
            "com.kylecorry.trail_sense",
            "com.kylecorry.trail_sense.staging",
            "com.kylecorry.trail_sense.dev",
            "com.kylecorry.trail_sense.nightly",
            "com.kylecorry.trail_sense.github",
        )

        val trailSenseAppNames = listOf(
            "Trail Sense",
            "Trail Sense (Staging)"
        )

        val trailSenseSupportEmails = listOf(
            "trailsense@protonmail.com"
        )

        // If everything matches, then the app is the original Trail Sense
        if (trailSensePackageNames.contains(packageName) && appName in trailSenseAppNames && supportEmail in trailSenseSupportEmails) {
            return
        }

        // If nothing matches, then it is a valid fork
        if (!trailSensePackageNames.contains(packageName) && appName !in trailSenseAppNames && supportEmail !in trailSenseSupportEmails) {
            return
        }

        // Otherwise, show an alert and ask them to change all the values
        Alerts.dialog(
            context,
            "Fork policy",
            "This app is detected to be a fork of Trail Sense. This is completely allowed under the MIT license. However, there is a copying policy in Trail Sense that requires you change the following:\n\n" +
                    "1. The package name (build.gradle.kts -> applicationId)\n" +
                    "2. The app name (strings.xml -> app_name)\n" +
                    "3. The support email (strings.xml -> email)\n" +
                    "4. Optionally, the app icon (ic_launcher / ic_launcher_round)\n\n" +
                    "Please change these values to avoid confusion with the original app. If you have any questions, please contact the developer of Trail Sense.\n\n" +
                    "Once you have changed these values, this message will go away.",
        )
    }
}