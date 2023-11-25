package com.kylecorry.trail_sense.diagnostics

import android.content.Context
import android.hardware.SensorManager
import androidx.lifecycle.LifecycleOwner
import com.kylecorry.andromeda.core.sensors.Quality
import com.kylecorry.andromeda.sense.magnetometer.IMagnetometer
import com.kylecorry.andromeda.sense.magnetometer.Magnetometer
import com.kylecorry.trail_sense.shared.sensors.SensorService

class MagnetometerDiagnostic(context: Context, lifecycleOwner: LifecycleOwner?) :
    BaseSensorQualityDiagnostic<IMagnetometer>(
        context,
        lifecycleOwner,
        Magnetometer(context, SensorManager.SENSOR_DELAY_NORMAL)
    ) {

    override fun scan(): List<DiagnosticCode> {
        if (!SensorService(context).hasCompass()) {
            return listOf(DiagnosticCode.MagnetometerUnavailable)
        }

        if (canRun && sensor!!.quality == Quality.Poor) {
            return listOf(DiagnosticCode.MagnetometerPoor)
        }

        return emptyList()
    }

}