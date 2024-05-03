package com.kylecorry.trail_sense.tools.diagnostics.infrastructure

import android.content.Context
import android.hardware.SensorManager
import androidx.lifecycle.LifecycleOwner
import com.kylecorry.andromeda.core.sensors.Quality
import com.kylecorry.andromeda.sense.Sensors
import com.kylecorry.andromeda.sense.orientation.Gyroscope
import com.kylecorry.andromeda.sense.orientation.IGyroscope
import com.kylecorry.trail_sense.tools.diagnostics.domain.DiagnosticCode

class GyroscopeDiagnostic(context: Context, lifecycleOwner: LifecycleOwner?) :
    BaseSensorQualityDiagnostic<IGyroscope>(
        context,
        lifecycleOwner,
        Gyroscope(context, SensorManager.SENSOR_DELAY_NORMAL)
    ) {

    override fun scan(): List<DiagnosticCode> {
        if (!Sensors.hasGyroscope(context)) {
            return listOf(DiagnosticCode.GyroscopeUnavailable)
        }

        if (canRun && sensor!!.quality == Quality.Poor) {
            return listOf(DiagnosticCode.GyroscopePoor)
        }

        return emptyList()
    }

}