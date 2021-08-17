package com.kylecorry.trail_sense.diagnostics

import android.content.Context
import android.hardware.Sensor
import androidx.lifecycle.LifecycleOwner
import com.kylecorry.andromeda.core.sensors.Quality
import com.kylecorry.andromeda.sense.Sensors
import com.kylecorry.andromeda.sense.magnetometer.IMagnetometer
import com.kylecorry.trail_sense.shared.sensors.SensorService

class MagnetometerDiagnostic(context: Context, lifecycleOwner: LifecycleOwner) :
    BaseSensorQualityDiagnostic<IMagnetometer>(
        context,
        lifecycleOwner,
        SensorService(context).getMagnetometer()
    ) {

    override fun scan(): List<DiagnosticCode> {
        if (!Sensors.hasSensor(context, Sensor.TYPE_MAGNETIC_FIELD)) {
            return listOf(DiagnosticCode.MagnetometerUnavailable)
        }

        if (sensor.quality == Quality.Poor) {
            return listOf(DiagnosticCode.MagnetometerPoor)
        }

        return emptyList()
    }

}