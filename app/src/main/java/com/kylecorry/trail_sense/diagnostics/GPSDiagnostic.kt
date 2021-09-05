package com.kylecorry.trail_sense.diagnostics

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import com.kylecorry.andromeda.core.sensors.Quality
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.andromeda.location.GPS
import com.kylecorry.andromeda.location.IGPS
import com.kylecorry.andromeda.permissions.Permissions
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.sensors.CustomGPS
import com.kylecorry.trail_sense.shared.sensors.SensorService

class GPSDiagnostic(context: Context, lifecycleOwner: LifecycleOwner) :
    BaseSensorQualityDiagnostic<IGPS>(
        context,
        lifecycleOwner,
        SensorService(context).getGPS(false)
    ) {

    override fun scan(): List<DiagnosticCode> {
        val issues = mutableListOf<DiagnosticCode>()
        val prefs = UserPreferences(context)

        if (!Permissions.canGetFineLocation(context)) {
            issues.add(DiagnosticCode.LocationNoPermission)
        }

        if (!Permissions.isBackgroundLocationEnabled(context)) {
            issues.add(DiagnosticCode.BackgroundLocationNoPermission)
        }

        if (!prefs.useAutoLocation || !Permissions.canGetFineLocation(context)) {
            issues.add(DiagnosticCode.LocationOverridden)
            if (prefs.locationOverride == Coordinate.zero) {
                issues.add(DiagnosticCode.LocationUnset)
            }
        }

        if (Permissions.canGetFineLocation(context) && !GPS.isAvailable(context)) {
            issues.add(DiagnosticCode.GPSUnavailable)
        }

        if (sensor.quality == Quality.Poor){
            issues.add(DiagnosticCode.GPSPoor)
        }

        if (sensor is CustomGPS && sensor.isTimedOut){
            issues.add(DiagnosticCode.GPSTimedOut)
        }

        return issues
    }
}