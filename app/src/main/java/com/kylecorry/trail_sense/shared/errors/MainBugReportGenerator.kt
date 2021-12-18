package com.kylecorry.trail_sense.shared.errors

import com.kylecorry.trail_sense.MainActivity

class MainBugReportGenerator(
    private val activity: MainActivity,
    private val throwable: Throwable?
) : IBugReportGenerator {
    override fun generate(): String {
        val reports = listOfNotNull(
            AppDetailsBugReportGenerator(activity),
            AndroidDetailsBugReportGenerator(),
            DeviceDetailsBugReportGenerator(),
            try {
                val fragment = activity.getFragment()
                fragment?.let {
                    FragmentDetailsBugReportGenerator(it)
                }
            } catch (e: Exception) {
                null
            },
            DiagnosticsBugReportGenerator(activity),
            throwable?.let { StackTraceBugReportGenerator(it) }
        )

        return reports.joinToString("\n") {
            try {
                it.generate()
            } catch (e: Exception) {
                ""
            }
        }

    }
}