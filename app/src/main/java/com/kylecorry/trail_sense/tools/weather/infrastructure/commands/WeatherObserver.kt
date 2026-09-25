package com.kylecorry.trail_sense.tools.weather.infrastructure.commands

import android.content.Context
import android.os.SystemClock
import com.kylecorry.andromeda.sense.mock.MockSensor
import com.kylecorry.luna.concurrency.onDefault
import com.kylecorry.andromeda.sense.location.IGPS
import com.kylecorry.andromeda.sense.readAll
import com.kylecorry.sol.math.MathExtensions.real
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.logging.Logger
import com.kylecorry.trail_sense.shared.safeRoundPlaces
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.sensors.altimeter.AltimeterWrapper
import com.kylecorry.trail_sense.shared.sensors.thermometer.HistoricThermometer
import com.kylecorry.trail_sense.shared.sensors.thermometer.ThermometerSource
import com.kylecorry.trail_sense.tools.weather.domain.RawWeatherObservation
import java.time.Duration
import java.time.Instant

internal class WeatherObserver(
    private val context: Context,
    private val timeout: Duration = SensorService.GPS_READ_TIMEOUT
) : IWeatherObserver {

    private val sensorService by lazy { SensorService(context) }
    private val prefs by lazy { UserPreferences(context) }
    private val altimeter by lazy {
        sensorService.getAltimeter(
            preferGPS = true,
            frequency = SensorService.SINGLE_FIX_GPS_FREQUENCY,
            tag = "WeatherObserver"
        )
    }
    private val altimeterAsGPS by lazy { sensorService.getGPSFromAltimeter(altimeter) }
    private val gps: IGPS by lazy {
        altimeterAsGPS ?: sensorService.getGPS(SensorService.SINGLE_FIX_GPS_FREQUENCY, tag = "WeatherObserver")
    }
    private val barometer by lazy { sensorService.getBarometer(false) }
    private val thermometer by lazy { sensorService.getThermometer() }
    private val hygrometer by lazy { sensorService.getHygrometer() }

    override suspend fun getWeatherObservation(): Reading<RawWeatherObservation>? = onDefault {
        val start = SystemClock.elapsedRealtime()
        readAll(
            listOfNotNull(
                altimeter,
                if (altimeterAsGPS != gps) gps else null,
                barometer,
                thermometer,
                hygrometer
            ),
            timeout,
            forceStopOnCompletion = true
        )

        // Read the thermometer one last time - historic thermometer depends on updated location/elevation reading
        if (thermometer is HistoricThermometer) {
            readAll(listOf(thermometer), Duration.ofSeconds(1), forceStopOnCompletion = true)
        }

        if (barometer.pressure == 0f) {
            getAppService<Logger>().warn(
                TAG,
                "Barometer did not report a pressure after ${SystemClock.elapsedRealtime() - start}ms, no weather reading recorded"
            )
            return@onDefault null
        }

        getAppService<Logger>().info(
            TAG,
            "Observed weather in ${SystemClock.elapsedRealtime() - start}ms (Altitude: ${altimeter.hasValidReading}, GPS: ${gps.hasValidReading}, " +
                "Barometer: ${getSensorStatusLogMessage(barometer !is MockSensor, barometer.hasValidReading)}, " +
                "Thermometer: ${getSensorStatusLogMessage(prefs.thermometer.source != ThermometerSource.Historic, thermometer.hasValidReading)}, " +
                "Hygrometer: ${getSensorStatusLogMessage(hygrometer !is MockSensor, hygrometer.hasValidReading)})"
        )

        Reading(
            RawWeatherObservation(
                0,
                barometer.pressure.real(1013f),
                altimeter.altitude.real(),
                thermometer.temperature.real(16f),
                if (altimeter is AltimeterWrapper) (altimeter as AltimeterWrapper).altitudeAccuracy else null,
                hygrometer.humidity,
                gps.location
            ),
            Instant.now()
        )
    }

    companion object {
        private const val TAG = "WeatherObserver"

        private fun getSensorStatusLogMessage(usesRealSensor: Boolean, hasValidReading: Boolean): String {
            return if (usesRealSensor) hasValidReading.toString() else "N/A"
        }
    }
}
