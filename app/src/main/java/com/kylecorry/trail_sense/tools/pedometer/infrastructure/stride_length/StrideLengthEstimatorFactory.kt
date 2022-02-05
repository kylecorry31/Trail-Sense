package com.kylecorry.trail_sense.tools.pedometer.infrastructure.stride_length

import android.content.Context
import com.kylecorry.trail_sense.shared.sensors.SensorService

class StrideLengthEstimatorFactory(context: Context) {

    private val sensors = SensorService(context)

    fun getEstimator(): IStrideLengthEstimator {
        return EndPointStrideLengthEstimator(sensors.getGPS(), sensors.getPedometer())
    }

}