package com.kylecorry.trail_sense.tools.pedometer.infrastructure.step_length

import android.content.Context
import com.kylecorry.trail_sense.shared.sensors.SensorService

class StepLengthEstimatorFactory(context: Context) {

    private val sensors = SensorService(context)

    fun getEstimator(): IStepLengthEstimator {
        return EndPointStepLengthEstimator(sensors.getGPS(), sensors.getPedometer())
    }

}
