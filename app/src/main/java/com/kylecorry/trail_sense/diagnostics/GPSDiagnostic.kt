package com.kylecorry.trail_sense.diagnostics

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import com.kylecorry.andromeda.core.sensors.Quality
import com.kylecorry.andromeda.location.GPS
import com.kylecorry.andromeda.location.IGPS
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.sensors.CustomGPS
import com.kylecorry.trail_sense.shared.sensors.SensorService

class GPSDiagnostic(context: Context, lifecycleOwner: LifecycleOwner?) :
    BaseSensorQualityDiagnostic<IGPS>(
        context,
        lifecycleOwner,
        SensorService(context).getGPS(false)
    ) {

    override fun scan(): List<DiagnosticCode> {
        val issues = mutableListOf<DiagnosticCode>()
        val prefs = UserPreferences(context)
        val sensorService = SensorService(context)

        if (!sensorService.hasLocationPermission()) {
            issues.add(DiagnosticCode.LocationNoPermission)
        }

        if (!sensorService.hasLocationPermission(true)) {
            issues.add(DiagnosticCode.BackgroundLocationNoPermission)
        }

        if (!prefs.useAutoLocation || !sensorService.hasLocationPermission()) {
            issues.add(DiagnosticCode.LocationOverridden)
            if (prefs.locationOverride == Coordinate.zero) {
                issues.add(DiagnosticCode.LocationUnset)
            }
        }

        if (sensorService.hasLocationPermission() && !GPS.isAvailable(context)) {
            issues.add(DiagnosticCode.GPSUnavailable)
        }

        if (canRun && sensor!!.quality == Quality.Poor){
            issues.add(DiagnosticCode.GPSPoor)
        }

        if (canRun && sensor is CustomGPS && sensor.isTimedOut){
            issues.add(DiagnosticCode.GPSTimedOut)
        }

        return issues
    }
}