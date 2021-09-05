package com.kylecorry.trail_sense.weather.domain.sealevel.kalman

import com.kylecorry.sol.math.SolMath.removeOutliers
import com.kylecorry.sol.math.filters.KalmanFilter
import com.kylecorry.trail_sense.weather.domain.PressureAltitudeReading
import com.kylecorry.trail_sense.weather.domain.PressureReading

class KalmanSeaLevelPressureConverter(
    private val defaultGPSError: Float = 10f,
    private val altitudeOutlierThreshold: Float = defaultGPSError,
    private val altitudeProcessError: Float = 0.1f,
    private val defaultPressureError: Float = 2f,
    private val pressureOutlierThreshold: Float = defaultPressureError,
    private val pressureProcessError: Float = 0.01f,
    private val adjustWithTime: Boolean = true,
    private val replaceOutliersWithAverage: Boolean = false,
    private val replaceLastOutlier: Boolean = false,
) {

    fun convert(
        readings: List<PressureAltitudeReading>,
        factorInTemperature: Boolean
    ): List<PressureReading> {
        val altitudes = readings.map { it.altitude.toDouble() }
        val altitudeErrors =
            readings.map { if (it.altitudeError == null || it.altitudeError == 0f) defaultGPSError.toDouble() else it.altitudeError!!.toDouble() }
        val pressures = readings.map { it.pressure.toDouble() }
        val times = readings.map { it.time.toEpochMilli() / (1000.0 * 60.0) }

        val altitudesNoOutliers = removeOutliers(
            altitudes,
            altitudeOutlierThreshold.toDouble(),
            replaceOutliersWithAverage,
            replaceLastOutlier
        )
        val filteredAltitudes = KalmanFilter.filter(
            altitudesNoOutliers,
            altitudeErrors,
            altitudeProcessError.toDouble(),
            if (adjustWithTime) times else null
        )

        val filteredPressures = removeOutliers(
            pressures,
            pressureOutlierThreshold.toDouble(),
            replaceOutliersWithAverage,
            replaceLastOutlier
        )
        val seaLevel = mutableListOf<PressureReading>()

        for (i in readings.indices) {
            val pressure = filteredPressures[i]
            val time = readings[i].time
            val temp = readings[i].temperature
            val altitude = filteredAltitudes[i]
            seaLevel.add(
                PressureAltitudeReading(
                    time,
                    pressure.toFloat(),
                    altitude.toFloat(),
                    temp
                ).seaLevel(factorInTemperature)
            )
        }

        val kalmanSeaLevel = KalmanFilter.filter(
            seaLevel.map { it.value.toDouble() },
            defaultPressureError.toDouble(),
            pressureProcessError.toDouble(),
            if (adjustWithTime) times else null
        )

        return kalmanSeaLevel.mapIndexed { index, pressure ->
            seaLevel[index].copy(value = pressure.toFloat())
        }

    }
}