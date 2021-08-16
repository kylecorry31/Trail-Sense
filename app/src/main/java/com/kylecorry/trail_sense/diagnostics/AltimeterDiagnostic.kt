package com.kylecorry.trail_sense.diagnostics

import android.content.Context
import androidx.navigation.NavController
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.UserPreferences

class AltimeterDiagnostic(private val context: Context, private val navController: NavController) :
    IDiagnostic {
    override fun getIssues(): List<DiagnosticIssue> {
        val issues = mutableListOf<DiagnosticIssue>()
        val prefs = UserPreferences(context)

        if (prefs.altimeterMode == UserPreferences.AltimeterMode.Override) {
            issues.add(
                DiagnosticIssue(
                    context.getString(R.string.pref_altimeter_calibration_title),
                    context.getString(R.string.altitude_overridden),
                    IssueSeverity.Warning,
                    IssueMessage(actionTitle = context.getString(R.string.update)) {
                        navController.navigate(R.id.calibrateAltimeterFragment)
                    }
                )
            )
        }


        return issues
    }
}