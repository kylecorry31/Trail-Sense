package com.kylecorry.trail_sense.tools.weather.infrastructure.subsystem

import android.annotation.SuppressLint
import android.content.Context
import com.kylecorry.andromeda.core.cache.MemoryCachedValue
import com.kylecorry.andromeda.core.coroutines.onDefault
import com.kylecorry.andromeda.core.coroutines.onIO
import com.kylecorry.andromeda.core.topics.ITopic
import com.kylecorry.andromeda.core.topics.Topic
import com.kylecorry.andromeda.sense.Sensors
import com.kylecorry.sol.math.Range
import com.kylecorry.sol.science.meteorology.KoppenGeigerClimateClassification
import com.kylecorry.sol.science.meteorology.Meteorology
import com.kylecorry.sol.science.meteorology.clouds.CloudGenus
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.Reading
import com.kylecorry.sol.units.Temperature
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.data.DataUtils
import com.kylecorry.trail_sense.shared.debugging.DebugWeatherCommand
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.sensors.LocationSubsystem
import com.kylecorry.trail_sense.tools.climate.infrastructure.ClimateSubsystem
import com.kylecorry.trail_sense.tools.climate.infrastructure.precipitation.HistoricMonthlyPrecipitationRepo
import com.kylecorry.trail_sense.tools.climate.infrastructure.temperatures.HistoricTemperatureRepo
import com.kylecorry.trail_sense.tools.clouds.infrastructure.persistence.CloudRepo
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.weather.WeatherToolRegistration
import com.kylecorry.trail_sense.tools.weather.domain.CurrentWeather
import com.kylecorry.trail_sense.tools.weather.domain.RawWeatherObservation
import com.kylecorry.trail_sense.tools.weather.domain.WeatherObservation
import com.kylecorry.trail_sense.tools.weather.domain.forecasting.IWeatherForecaster
import com.kylecorry.trail_sense.tools.weather.domain.forecasting.WeatherForecaster
import com.kylecorry.trail_sense.tools.weather.domain.forecasting.temperatures.CalibratedTemperatureService
import com.kylecorry.trail_sense.tools.weather.domain.forecasting.temperatures.HistoricTemperatureService
import com.kylecorry.trail_sense.tools.weather.domain.forecasting.temperatures.ITemperatureService
import com.kylecorry.trail_sense.tools.weather.domain.sealevel.SeaLevelCalibrationFactory
import com.kylecorry.trail_sense.tools.weather.infrastructure.commands.MonitorWeatherCommand
import com.kylecorry.trail_sense.tools.weather.infrastructure.commands.SendWeatherAlertsCommand
import com.kylecorry.trail_sense.tools.weather.infrastructure.persistence.WeatherRepo
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.Month
import java.time.ZonedDateTime


class WeatherSubsystem private constructor(private val context: Context) : IWeatherSubsystem {

    private val weatherRepo by lazy { WeatherRepo.getInstance(context) }
    private val climate by lazy { ClimateSubsystem.getInstance(context) }
    private val cloudRepo by lazy { CloudRepo.getInstance(context) }
    private val prefs by lazy { UserPreferences(context) }
    private val sharedPrefs by lazy { PreferencesSubsystem.getInstance(context).preferences }
    private val location by lazy { LocationSubsystem.getInstance(context) }

    private var cachedValue = MemoryCachedValue<CurrentWeather>()
    private var validLock = Object()
    private var isValid = false
    private var updateWeatherMutex = Mutex()

    private val _weatherChanged = Topic()
    override val weatherChanged: ITopic = _weatherChanged

    private val invalidationPrefKeys = listOf(
        R.string.pref_use_sea_level_pressure,
        R.string.pref_barometer_pressure_smoothing,
        R.string.pref_adjust_for_temperature,
        R.string.pref_forecast_sensitivity,
        R.string.pref_storm_alert_sensitivity,
        R.string.pref_altimeter_calibration_mode,
        R.string.pref_pressure_history,
        R.string.pref_temperature_smoothing,
        R.string.pref_weather_forecast_source,
        R.string.pref_barometer_offset,
        R.string.pref_pressure_units
    ).map { context.getString(it) }

    private val weatherMonitorStatePrefKeys = listOf(
        R.string.pref_monitor_weather,
        R.string.pref_low_power_mode_weather,
        R.string.pref_low_power_mode
    ).map { context.getString(it) }

    private val weatherMonitorFrequencyPrefKeys = listOf(
        R.string.pref_weather_update_frequency
    ).map { context.getString(it) }

    init {
        sharedPrefs.onChange.subscribe { key ->
            if (key in invalidationPrefKeys) {
                invalidate()
            }
            true
        }
        weatherRepo.readingsChanged.subscribe {
            invalidate()
            true
        }

        cloudRepo.readingsChanged.subscribe {
            invalidate()
            true
        }

        weatherChanged.subscribe {
            Tools.broadcast(WeatherToolRegistration.BROADCAST_WEATHER_PREDICTION_CHANGED)
            true
        }
    }

    override suspend fun getWeather(): CurrentWeather = onIO {
        if (!isValid) {
            refresh()
        }
        cachedValue.getOrPut { populateCache() }
    }

    override suspend fun getHistory(): List<WeatherObservation> = onIO {
        val readings = getRawHistory()

        val precalibrated = calibrateHumidity(calibrateTemperatures(readings))

        val calibrator = SeaLevelCalibrationFactory().create(prefs)
        val pressures = calibrator.calibrate(precalibrated)

        val offset = prefs.weather.barometerOffset

        val combined = pressures.map {
            val reading = precalibrated.firstOrNull { r -> r.time == it.time }
            WeatherObservation(
                reading?.value?.id ?: 0L,
                it.time,
                it.value.copy(pressure = it.value.pressure + offset),
                Temperature.celsius(reading?.value?.temperature ?: 0f),
                reading?.value?.humidity
            )
        }

        onIO {
            DebugWeatherCommand(
                context,
                readings,
                combined,
                prefs.weather.seaLevelFactorInTemp
            ).execute()
        }

        combined
    }

    override suspend fun getTemperature(
        time: ZonedDateTime,
        location: Coordinate?,
        elevation: Distance?,
        calibrated: Boolean
    ): Reading<Temperature> = onDefault {
        val service = getTemperatureService(location, elevation, calibrated)
        Reading(service.getTemperature(time), time.toInstant())
    }

    override suspend fun getTemperatures(
        start: ZonedDateTime,
        end: ZonedDateTime,
        location: Coordinate?,
        elevation: Distance?,
        calibrated: Boolean
    ): List<Reading<Temperature>> = onDefault {
        val service = getTemperatureService(location, elevation, calibrated)
        service.getTemperatures(start, end)
    }

    override suspend fun getTemperatureRange(
        date: LocalDate,
        location: Coordinate?,
        elevation: Distance?,
        calibrated: Boolean
    ): Range<Temperature> = onDefault {
        val service = getTemperatureService(location, elevation, calibrated)
        service.getTemperatureRange(date)
    }

    override suspend fun getTemperatureRanges(
        year: Int,
        location: Coordinate?,
        elevation: Distance?,
        calibrated: Boolean
    ): List<Pair<LocalDate, Range<Temperature>>> = onDefault {
        val service = getTemperatureService(location, elevation, calibrated)
        service.getTemperatureRanges(year)
    }

    private suspend fun resolveLocation(
        location: Coordinate?,
        elevation: Distance?
    ): Pair<Coordinate, Distance> {
        val lookupLocation: Coordinate
        val lookupElevation: Distance
        if (location == null || elevation == null) {
            val last = weatherRepo.getLast()
            lookupLocation =
                location ?: last?.value?.location ?: this@WeatherSubsystem.location.location
            lookupElevation =
                elevation ?: last?.value?.altitude?.let { Distance.meters(it) }
                        ?: this@WeatherSubsystem.location.elevation
        } else {
            lookupLocation = location
            lookupElevation = elevation
        }

        return lookupLocation to lookupElevation
    }

    override suspend fun getRawHistory(applyPressureOffset: Boolean): List<Reading<RawWeatherObservation>> =
        onIO {
            if (!isValid) {
                refresh()
            }
            val readings = weatherRepo.getAll()
                .asSequence()
                .sortedBy { it.time }
                .filter { it.time <= Instant.now() }
                .map {
                    if (it.value.location == Coordinate.zero) it.copy(
                        value = it.value.copy(
                            location = location.location
                        )
                    ) else it
                }
                .toList()

            if (applyPressureOffset) {
                calibrateBarometerOffset(readings)
            } else {
                readings
            }
        }

    override suspend fun getMonthlyPrecipitation(location: Coordinate?): Map<Month, Distance> {
        val resolved = resolveLocation(location, null)
        return HistoricMonthlyPrecipitationRepo.getMonthlyPrecipitation(
            context,
            resolved.first
        )
    }

    override suspend fun getClimateClassification(
        location: Coordinate?,
        elevation: Distance?,
        calibrated: Boolean
    ): KoppenGeigerClimateClassification {
        val temperatures =
            getTemperatureRanges(LocalDate.now().year, location, elevation, calibrated)
        val precipitation = getMonthlyPrecipitation(location)

        val monthlyAverageTemperatures = temperatures
            .filter { it.first.dayOfMonth == 15 }
            .associate { it.first.month to Temperature.celsius((it.second.start.celsius().temperature + it.second.end.celsius().temperature) / 2) }

        return Meteorology.getKoppenGeigerClimateClassification(
            monthlyAverageTemperatures,
            precipitation
        )
    }

    override suspend fun updateWeather() = onDefault {
        updateWeatherMutex.withLock {
            val last = weatherRepo.getLast()?.time
            val maxPeriod = prefs.weather.weatherUpdateFrequency.dividedBy(3)

            if (last != null && Duration.between(last, Instant.now()).abs() < maxPeriod) {
                // Still send out the weather alerts, just don't log a new reading
                SendWeatherAlertsCommand(context).execute(getWeather())
                return@onDefault
            }

            MonitorWeatherCommand.create(context).execute()
        }
    }

    private suspend fun getTemperatureService(
        location: Coordinate?,
        elevation: Distance?,
        calibrated: Boolean
    ): ITemperatureService {
        val resolved = resolveLocation(location, elevation)
        val lookupLocation = resolved.first
        val lookupElevation = resolved.second
        val service = HistoricTemperatureService(climate, lookupLocation, lookupElevation)

        if (calibrated) {
            return CalibratedTemperatureService(service, prefs.thermometer.calibrator)
        }

        return service
    }

    private suspend fun getWeatherForecaster(
        location: Coordinate?,
        elevation: Distance?
    ): IWeatherForecaster {
        val resolved = resolveLocation(location, elevation)
        val lookupLocation = resolved.first
        val lookupElevation = resolved.second
        val temperatureService = getTemperatureService(lookupLocation, lookupElevation, true)
        return WeatherForecaster(temperatureService, prefs.weather)
    }

    private fun invalidate() {
        synchronized(validLock) {
            isValid = false
        }
        _weatherChanged.publish()
    }

    private suspend fun refresh() {
        cachedValue.reset()
        delay(50)
        synchronized(validLock) {
            isValid = true
        }
    }

    override suspend fun getCloudHistory(): List<Reading<CloudGenus?>> = onIO {
        cloudRepo.getAll().sortedBy { it.time }.map { Reading(it.value.genus, it.time) }
    }

    private suspend fun populateCache(): CurrentWeather {
        val history = getHistory()
        val allClouds = getCloudHistory()
        val last = history.lastOrNull()
        val (location, elevation) = getLocationAndElevation(last?.id)
        val forecaster = getWeatherForecaster(location, elevation)
        return forecaster.forecast(history, allClouds, location)
    }

    private suspend fun getLocationAndElevation(lastReadingId: Long? = null): Pair<Coordinate, Distance> {
        val lastRawReading = lastReadingId?.let {
            weatherRepo.get(it)
        }
        val location = lastRawReading?.value?.location ?: location.location
        val elevation = lastRawReading?.value?.altitude?.let { Distance.meters(it) }
            ?: this.location.elevation

        return location to elevation
    }

    // TODO: Extract this
    private fun calibrateTemperatures(readings: List<Reading<RawWeatherObservation>>): List<Reading<RawWeatherObservation>> {
        val smoothing = prefs.thermometer.smoothing

        return DataUtils.smoothTemporal(
            readings,
            smoothing,
            { it.temperature }
        ) { reading, smoothed ->
            reading.copy(temperature = smoothed)
        }
    }

    // TODO: Extract this
    private fun calibrateHumidity(readings: List<Reading<RawWeatherObservation>>): List<Reading<RawWeatherObservation>> {
        if (!Sensors.hasHygrometer(context)) {
            return readings
        }
        return DataUtils.smoothTemporal(
            readings,
            0.1f,
            { it.humidity ?: 0f }) { reading, smoothed ->
            reading.copy(humidity = if (smoothed == 0f) null else smoothed)
        }
    }

    private fun calibrateBarometerOffset(readings: List<Reading<RawWeatherObservation>>): List<Reading<RawWeatherObservation>> {
        val offset = prefs.weather.barometerOffset
        if (offset == 0f) {
            return readings
        }
        return readings.map { it.copy(value = it.value.copy(pressure = it.value.pressure + offset)) }
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        private var instance: WeatherSubsystem? = null

        @Synchronized
        fun getInstance(context: Context): WeatherSubsystem {
            if (instance == null) {
                instance = WeatherSubsystem(context.applicationContext)
            }
            return instance!!
        }

        const val COLD = 5f
        const val HOT = 32.5f

    }

}