package com.kylecorry.trail_sense.diagnostics

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.navigation.NavController
import com.kylecorry.andromeda.core.units.Coordinate
import com.kylecorry.andromeda.location.GPS
import com.kylecorry.andromeda.permissions.Permissions
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.UserPreferences

class GPSDiagnostic(private val context: Context, private val navController: NavController) :
    IDiagnostic {
    override fun getIssues(): List<DiagnosticIssue> {
        val issues = mutableListOf<DiagnosticIssue>()
        val prefs = UserPreferences(context)

        if (!prefs.useAutoLocation || !Permissions.canGetFineLocation(context)) {
            if (prefs.locationOverride == Coordinate.zero) {
                issues.add(
                    DiagnosticIssue(
                        context.getString(R.string.gps),
                        context.getString(R.string.location_not_set),
                        IssueSeverity.Error,
                        IssueMessage(actionTitle = context.getString(R.string.settings)) {
                            navController.navigate(R.id.calibrateGPSFragment)
                        }
                    )
                )
            }

            issues.add(
                DiagnosticIssue(
                    context.getString(R.string.gps),
                    context.getString(R.string.location_mocked),
                    IssueSeverity.Warning,
                    IssueMessage(actionTitle = context.getString(R.string.settings)) {
                        navController.navigate(R.id.calibrateGPSFragment)
                    }
                )
            )
            return issues
        }

        if (Permissions.canGetFineLocation(context) && !GPS.isAvailable(context)) {
            issues.add(
                DiagnosticIssue(
                    context.getString(R.string.gps),
                    context.getString(R.string.gps_unavailable),
                    IssueSeverity.Error,
                    IssueMessage(actionTitle = context.getString(R.string.update)) {
                        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                        val canOpenLocationSource =
                            intent.resolveActivity(context.packageManager) != null
                        if (canOpenLocationSource) {
                            context.startActivity(intent)
                        } else {
                            val settings = Intent(Settings.ACTION_SETTINGS)
                            context.startActivity(settings)
                        }
                    }
                )
            )
        }

        return issues
    }
}