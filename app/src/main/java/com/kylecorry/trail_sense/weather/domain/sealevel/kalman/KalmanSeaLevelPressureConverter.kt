package com.kylecorry.trail_sense.weather.domain.sealevel.kalman

import com.kylecorry.sol.math.SolMath.removeOutliers
import com.kylecorry.sol.math.filters.KalmanFilter
import com.kylecorry.sol.units.Pressure
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.weather.domain.RawWeatherObservation

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
        readings: List<Reading<RawWeatherObservation>>,
        factorInTemperature: Boolean
    ): List<Reading<Pressure>> {
        val altitudes = readings.map { it.value.altitude.toDouble() }
        val altitudeErrors =
            readings.map { if (it.value.altitudeError == null || it.value.altitudeError == 0f) defaultGPSError.toDouble() else it.value.altitudeError!!.toDouble() }
        val pressures = readings.map { it.value.pressure.toDouble() }
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
        val seaLevel = mutableListOf<Reading<Pressure>>()

        for (i in readings.indices) {
            val pressure = filteredPressures[i]
            val time = readings[i].time
            val temp = readings[i].value.temperature
            val altitude = filteredAltitudes[i]
            seaLevel.add(
                Reading(
                    RawWeatherObservation(
                        0,
                        pressure.toFloat(),
                        altitude.toFloat(),
                        temp
                    ).seaLevel(factorInTemperature),
                    time
                )
            )
        }

        val kalmanSeaLevel = KalmanFilter.filter(
            seaLevel.map { it.value.hpa().pressure.toDouble() },
            defaultPressureError.toDouble(),
            pressureProcessError.toDouble(),
            if (adjustWithTime) times else null
        )

        return kalmanSeaLevel.mapIndexed { index, pressure ->
            seaLevel[index].copy(value = Pressure.hpa(pressure.toFloat()))
        }

    }
}