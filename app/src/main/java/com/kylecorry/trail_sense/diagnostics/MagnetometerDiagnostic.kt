package com.kylecorry.trail_sense.diagnostics

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import androidx.lifecycle.LifecycleOwner
import com.kylecorry.andromeda.core.sensors.Quality
import com.kylecorry.andromeda.sense.Sensors
import com.kylecorry.andromeda.sense.magnetometer.IMagnetometer
import com.kylecorry.andromeda.sense.magnetometer.Magnetometer

class MagnetometerDiagnostic(context: Context, lifecycleOwner: LifecycleOwner?) :
    BaseSensorQualityDiagnostic<IMagnetometer>(
        context,
        lifecycleOwner,
        Magnetometer(context, SensorManager.SENSOR_DELAY_NORMAL)
    ) {

    override fun scan(): List<DiagnosticCode> {
        if (!Sensors.hasSensor(context, Sensor.TYPE_MAGNETIC_FIELD)) {
            return listOf(DiagnosticCode.MagnetometerUnavailable)
        }

        if (canRun && sensor!!.quality == Quality.Poor) {
            return listOf(DiagnosticCode.MagnetometerPoor)
        }

        return emptyList()
    }

}