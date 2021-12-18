package com.kylecorry.trail_sense.diagnostics

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import com.kylecorry.andromeda.core.sensors.Quality
import com.kylecorry.andromeda.sense.Sensors
import com.kylecorry.andromeda.sense.barometer.IBarometer
import com.kylecorry.trail_sense.shared.sensors.SensorService

class BarometerDiagnostic(context: Context, lifecycleOwner: LifecycleOwner?) :
    BaseSensorQualityDiagnostic<IBarometer>(
        context,
        lifecycleOwner,
        SensorService(context).getBarometer()
    ) {

    override fun scan(): List<DiagnosticCode> {
        if (!Sensors.hasBarometer(context)) {
            return listOf(DiagnosticCode.BarometerUnavailable)
        }

        if (canRun && sensor!!.quality == Quality.Poor) {
            return listOf(DiagnosticCode.BarometerPoor)
        }

        return emptyList()
    }

}