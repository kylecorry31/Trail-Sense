package com.kylecorry.trail_sense.main.errors

import androidx.core.os.bundleOf
import com.kylecorry.andromeda.core.tryOrDefault
import com.kylecorry.andromeda.core.tryOrLog
import com.kylecorry.andromeda.exceptions.AggregateBugReportGenerator
import com.kylecorry.andromeda.exceptions.AndroidDetailsBugReportGenerator
import com.kylecorry.andromeda.exceptions.AppDetailsBugReportGenerator
import com.kylecorry.andromeda.exceptions.DeviceDetailsBugReportGenerator
import com.kylecorry.andromeda.exceptions.StackTraceBugReportGenerator
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.MainActivity
import com.kylecorry.trail_sense.shared.extensions.findNavController
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

class TrailSenseExceptionHandler(
    private val activity: MainActivity
) : BaseExceptionHandler(
    activity.applicationContext,
    AggregateBugReportGenerator(
        listOf(
            AppDetailsBugReportGenerator(activity.getString(R.string.app_name)),
            PackageNameBugReportGenerator(),
            BuildTypeBugReportGenerator(),
            AndroidDetailsBugReportGenerator(),
            DeviceDetailsBugReportGenerator(),
            FragmentDetailsBugReportGenerator(),
            DiagnosticsBugReportGenerator(),
            StackTraceBugReportGenerator()
        )
    ),
    "errors/error.txt",
    shouldRestartApp = false
) {

    override fun handleBugReport(log: String) {
        error = log
    }

    override fun handleException(throwable: Throwable, details: String): Boolean {
        return tryOrDefault(false) {
            val currentNavId = activity.findNavController().currentDestination?.id
            val tools = Tools.getTools(activity)
            val selectedTool = tools.firstOrNull { it.isOpen(currentNavId ?: 0) }

            if (activity.isRunning && selectedTool != null) {
                activity.runOnUiThread {
                    tryOrLog {
                        activity.findNavController().navigate(
                            R.id.fragmentToolErrorHandler,
                            bundleOf("tool_id" to selectedTool.id, "error" to details)
                        )
                    }
                }
                return true
            }
            return false
        }
    }

    companion object {
        var error: String? = null
    }
}