package com.kylecorry.trail_sense.weather.infrastructure.subsystem

import android.annotation.SuppressLint
import android.content.Context
import com.kylecorry.andromeda.core.cache.MemoryCachedValue
import com.kylecorry.andromeda.core.topics.ITopic
import com.kylecorry.andromeda.core.topics.Topic
import com.kylecorry.andromeda.core.topics.generic.distinct
import com.kylecorry.andromeda.preferences.Preferences
import com.kylecorry.andromeda.sense.Sensors
import com.kylecorry.sol.math.Range
import com.kylecorry.sol.science.meteorology.*
import com.kylecorry.sol.science.meteorology.clouds.CloudGenus
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.Reading
import com.kylecorry.sol.units.Temperature
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FeatureState
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.data.DataUtils
import com.kylecorry.trail_sense.shared.debugging.DebugWeatherCommand
import com.kylecorry.trail_sense.shared.extensions.getOrNull
import com.kylecorry.trail_sense.shared.extensions.onDefault
import com.kylecorry.trail_sense.shared.extensions.onIO
import com.kylecorry.trail_sense.shared.sensors.LocationSubsystem
import com.kylecorry.trail_sense.weather.domain.*
import com.kylecorry.trail_sense.weather.domain.forecasting.IWeatherForecaster
import com.kylecorry.trail_sense.weather.domain.forecasting.WeatherForecaster
import com.kylecorry.trail_sense.weather.domain.forecasting.temperatures.CalibratedTemperatureService
import com.kylecorry.trail_sense.weather.domain.forecasting.temperatures.HistoricTemperatureService
import com.kylecorry.trail_sense.weather.domain.forecasting.temperatures.ITemperatureService
import com.kylecorry.trail_sense.weather.domain.sealevel.SeaLevelCalibrationFactory
import com.kylecorry.trail_sense.weather.infrastructure.*
import com.kylecorry.trail_sense.weather.infrastructure.commands.MonitorWeatherCommand
import com.kylecorry.trail_sense.weather.infrastructure.commands.SendWeatherAlertsCommand
import com.kylecorry.trail_sense.weather.infrastructure.persistence.CloudRepo
import com.kylecorry.trail_sense.weather.infrastructure.persistence.WeatherRepo
import com.kylecorry.trail_sense.weather.infrastructure.temperatures.HistoricTemperatureRepo
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.*


class WeatherSubsystem private constructor(private val context: Context) : IWeatherSubsystem {

    private val weatherRepo by lazy { WeatherRepo.getInstance(context) }
    private val temperatureRepo by lazy { HistoricTemperatureRepo(context) }
    private val cloudRepo by lazy { CloudRepo.getInstance(context) }
    private val prefs by lazy { UserPreferences(context) }
    private val sharedPrefs by lazy { Preferences(context) }
    private val location by lazy { LocationSubsystem.getInstance(context) }

    private var cachedValue = MemoryCachedValue<CurrentWeather>()
    private var validLock = Object()
    private var isValid = false
    private var updateWeatherMutex = Mutex()

    private val _weatherChanged = Topic()
    override val weatherChanged: ITopic = _weatherChanged

    private val _weatherMonitorState =
        com.kylecorry.andromeda.core.topics.generic.Topic(
            defaultValue = Optional.of(
                calculateWeatherMonitorState()
            )
        )
    override val weatherMonitorState: com.kylecorry.andromeda.core.topics.generic.ITopic<FeatureState>
        get() = _weatherMonitorState.distinct()

    private val _weatherMonitorFrequency =
        com.kylecorry.andromeda.core.topics.generic.Topic(
            defaultValue = Optional.of(
                calculateWeatherMonitorFrequency()
            )
        )
    override val weatherMonitorFrequency: com.kylecorry.andromeda.core.topics.generic.ITopic<Duration>
        get() = _weatherMonitorFrequency.distinct()

    override fun getWeatherMonitorState(): FeatureState {
        return weatherMonitorState.getOrNull() ?: FeatureState.Off
    }

    override fun getWeatherMonitorFrequency(): Duration {
        return weatherMonitorFrequency.getOrNull() ?: Duration.ofMinutes(15)
    }

    private val invalidationPrefKeys = listOf(
        R.string.pref_use_sea_level_pressure,
        R.string.pref_barometer_pressure_smoothing,
        R.string.pref_adjust_for_temperature,
        R.string.pref_forecast_sensitivity,
        R.string.pref_storm_alert_sensitivity,
        R.string.pref_altimeter_calibration_mode,
        R.string.pref_pressure_history,
        R.string.pref_temperature_smoothing
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
        // Keep them up to date
        weatherMonitorFrequency.subscribe { true }
        weatherMonitorState.subscribe { true }

        sharedPrefs.onChange.subscribe { key ->
            if (key in invalidationPrefKeys) {
                invalidate()
            }

            if (key in weatherMonitorStatePrefKeys) {
                val state = calculateWeatherMonitorState()
                _weatherMonitorState.publish(state)
            }

            if (key in weatherMonitorFrequencyPrefKeys) {
                val frequency = calculateWeatherMonitorFrequency()
                _weatherMonitorFrequency.publish(frequency)
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

        val combined = pressures.map {
            val reading = precalibrated.firstOrNull { r -> r.time == it.time }
            WeatherObservation(
                reading?.value?.id ?: 0L,
                it.time,
                it.value,
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
            lookupLocation = last?.value?.location ?: this@WeatherSubsystem.location.location
            lookupElevation =
                last?.value?.altitude?.let { Distance.meters(it) }
                    ?: this@WeatherSubsystem.location.elevation
        } else {
            lookupLocation = location
            lookupElevation = elevation
        }

        return lookupLocation to lookupElevation
    }

    override suspend fun getRawHistory(): List<Reading<RawWeatherObservation>> = onIO {
        if (!isValid) {
            refresh()
        }
        weatherRepo.getAll()
            .asSequence()
            .sortedBy { it.time }
            .filter { it.time <= Instant.now() }
            .map { if (it.value.location == Coordinate.zero) it.copy(value = it.value.copy(location = location.location)) else it }
            .toList()
    }

    override fun enableMonitor() {
        prefs.weather.shouldMonitorWeather = true
        WeatherUpdateScheduler.start(context)
    }

    override fun disableMonitor() {
        prefs.weather.shouldMonitorWeather = false
        WeatherUpdateScheduler.stop(context)
    }

    override suspend fun updateWeather(background: Boolean) {
        updateWeatherMutex.withLock {
            val last = weatherRepo.getLast()?.time
            val maxPeriod = getWeatherMonitorFrequency().dividedBy(3)

            if (last != null && Duration.between(last, Instant.now()).abs() < maxPeriod) {
                // Still send out the weather alerts, just don't log a new reading
                SendWeatherAlertsCommand(context).execute(getWeather())
                return
            }

            MonitorWeatherCommand.create(context, background).execute()
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
        val service = HistoricTemperatureService(temperatureRepo, lookupLocation, lookupElevation)

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
        return forecaster.forecast(history, allClouds)
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

    private fun calculateWeatherMonitorFrequency(): Duration {
        return prefs.weather.weatherUpdateFrequency
    }

    private fun calculateWeatherMonitorState(): FeatureState {
        return if (WeatherMonitorIsEnabled().isSatisfiedBy(context)) {
            FeatureState.On
        } else if (WeatherMonitorIsAvailable().not().isSatisfiedBy(context)) {
            FeatureState.Unavailable
        } else {
            FeatureState.Off
        }
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