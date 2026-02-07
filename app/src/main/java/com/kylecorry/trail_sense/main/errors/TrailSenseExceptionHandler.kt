package com.kylecorry.trail_sense.main.errors

import android.content.Intent
import com.kylecorry.andromeda.core.tryOrDefault
import com.kylecorry.andromeda.exceptions.AggregateBugReportGenerator
import com.kylecorry.andromeda.exceptions.AndroidDetailsBugReportGenerator
import com.kylecorry.andromeda.exceptions.AppDetailsBugReportGenerator
import com.kylecorry.andromeda.exceptions.DeviceDetailsBugReportGenerator
import com.kylecorry.andromeda.exceptions.StackTraceBugReportGenerator
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.MainActivity
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.extensions.findNavController
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
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
            try {
                val prefs = getAppService<PreferencesSubsystem>().preferences
                val now = System.currentTimeMillis()
                val lastTime = prefs.getLong(PREF_LAST_EXCEPTION_TIME) ?: 0L
                if (now - lastTime < RESTART_COOLDOWN_MS) {
                    return false
                }
                prefs.putLong(PREF_LAST_EXCEPTION_TIME, now)
            } catch (_: Throwable) {
                return false
            }

            val currentNavId = activity.findNavController().currentDestination?.id ?: return false
            val tools = Tools.getTools(activity)
            val selectedTool = tools.firstOrNull { it.isOpen(currentNavId) } ?: return false

            val intent = Intent(context, MainActivity::class.java).apply {
                action = ACTION_TOOL_ERROR
                putExtra(EXTRA_TOOL_ID, selectedTool.id)
                putExtra(EXTRA_ERROR, details)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            context.startActivity(intent)
            true
        }
    }

    companion object {
        const val ACTION_TOOL_ERROR = "com.kylecorry.trail_sense.TOOL_ERROR"
        const val EXTRA_TOOL_ID = "tool_id"
        const val EXTRA_ERROR = "error"
        private const val RESTART_COOLDOWN_MS = 250L
        private const val PREF_LAST_EXCEPTION_TIME = "last_exception_time"
        var error: String? = null
    }
}