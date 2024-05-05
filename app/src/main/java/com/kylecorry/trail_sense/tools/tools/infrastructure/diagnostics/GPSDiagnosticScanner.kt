package com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics

import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.kylecorry.andromeda.core.sensors.Quality
import com.kylecorry.andromeda.sense.location.GPS
import com.kylecorry.andromeda.sense.location.IGPS
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.sensors.CustomGPS
import com.kylecorry.trail_sense.shared.sensors.SensorService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GPSDiagnosticScanner(private val gps: IGPS? = null) : ToolDiagnosticScanner {
    override fun quickScan(context: Context): List<ToolDiagnosticResult> {
        val issues = mutableListOf<ToolDiagnosticResult>()
        val prefs = UserPreferences(context)
        val sensorService = SensorService(context)

        // Permissions
        if (!sensorService.hasLocationPermission()) {
            issues.add(
                ToolDiagnosticResult(
                    "location-no-permission",
                    ToolDiagnosticSeverity.Error,
                    context.getString(R.string.gps),
                    context.getString(R.string.no_permission),
                    resolution = context.getString(
                        R.string.grant_permission,
                        context.getString(R.string.location)
                    ),
                    action = ToolDiagnosticAction.permissions(context)
                )
            )
        }

        // The location is overridden
        if (!prefs.useAutoLocation || !sensorService.hasLocationPermission()) {
            issues.add(
                ToolDiagnosticResult(
                    LOCATION_OVERRIDDEN,
                    ToolDiagnosticSeverity.Warning,
                    context.getString(R.string.gps),
                    context.getString(R.string.overridden),
                    resolution = context.getString(R.string.location_override_resolution),
                    action = ToolDiagnosticAction.navigate(
                        R.id.calibrateGPSFragment,
                        context.getString(R.string.settings)
                    )
                )
            )
            if (prefs.locationOverride == Coordinate.zero) {
                issues.add(
                    ToolDiagnosticResult(
                        LOCATION_UNSET,
                        ToolDiagnosticSeverity.Error,
                        context.getString(R.string.gps),
                        context.getString(R.string.location_not_set),
                        resolution = context.getString(R.string.location_override_not_set_resolution),
                        action = ToolDiagnosticAction.navigate(
                            R.id.calibrateGPSFragment,
                            context.getString(R.string.settings)
                        )
                    )
                )
            }
        }

        // Location is disabled
        if (prefs.useAutoLocation &&
            sensorService.hasLocationPermission() &&
            !GPS.isAvailable(context)
        ) {
            issues.add(
                ToolDiagnosticResult(
                    GPS_UNAVAILABLE,
                    ToolDiagnosticSeverity.Error,
                    context.getString(R.string.gps),
                    context.getString(R.string.unavailable),
                    resolution = context.getString(R.string.gps_unavailable_resolution),
                    action = ToolDiagnosticAction(context.getString(R.string.settings)) {
                        try {
                            it.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                        } catch (e: Exception) {
                            it.startActivity(Intent(Settings.ACTION_SETTINGS))
                        }
                    }
                )
            )
        }
        return issues
    }

    override fun fullScan(context: Context): Flow<List<ToolDiagnosticResult>> {
        val gps = this.gps ?: SensorService(context).getGPS()
        return gps.flow.map {
            val issues = mutableListOf<ToolDiagnosticResult>()

            issues.addAll(quickScan(context))

            if (gps.quality == Quality.Poor) {
                issues.add(
                    ToolDiagnosticResult(
                        GPS_POOR,
                        ToolDiagnosticSeverity.Warning,
                        context.getString(R.string.gps),
                        context.getString(R.string.quality_poor),
                        resolution = context.getString(R.string.get_gps_signal)
                    )
                )
            }

            if (gps is CustomGPS && gps.isTimedOut) {
                issues.add(
                    ToolDiagnosticResult(
                        GPS_TIMED_OUT,
                        ToolDiagnosticSeverity.Error,
                        context.getString(R.string.gps),
                        context.getString(R.string.gps_signal_lost),
                        resolution = context.getString(R.string.get_gps_signal)
                    )
                )
            }

            issues
        }
    }

    companion object {
        const val LOCATION_UNSET = "location-unset"
        const val LOCATION_OVERRIDDEN = "location-overridden"
        const val GPS_UNAVAILABLE = "gps-unavailable"
        const val GPS_POOR = "gps-poor"
        const val GPS_TIMED_OUT = "gps-timed-out"
    }
}