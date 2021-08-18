package com.kylecorry.trail_sense.diagnostics

import android.content.Context
import android.hardware.Sensor
import androidx.lifecycle.LifecycleOwner
import com.kylecorry.andromeda.sense.Sensors
import com.kylecorry.andromeda.sense.magnetometer.IMagnetometer
import com.kylecorry.trail_sense.shared.sensors.SensorService

class LightSensorDiagnostic(context: Context, lifecycleOwner: LifecycleOwner) :
    BaseSensorQualityDiagnostic<IMagnetometer>(
        context,
        lifecycleOwner,
        SensorService(context).getMagnetometer()
    ) {

    override fun scan(): List<DiagnosticCode> {
        if (!Sensors.hasSensor(context, Sensor.TYPE_LIGHT)) {
            return listOf(DiagnosticCode.LightSensorUnavailable)
        }

        return emptyList()
    }

}