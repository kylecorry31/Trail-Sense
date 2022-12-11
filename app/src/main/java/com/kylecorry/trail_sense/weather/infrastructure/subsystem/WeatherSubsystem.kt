package com.kylecorry.trail_sense.weather.infrastructure.subsystem

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
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
import com.kylecorry.trail_sense.weather.domain.sealevel.SeaLevelCalibrationFactory
import com.kylecorry.trail_sense.weather.infrastructure.*
import com.kylecorry.trail_sense.weather.infrastructure.commands.MonitorWeatherCommand
import com.kylecorry.trail_sense.weather.infrastructure.commands.SendWeatherAlertsCommand
import com.kylecorry.trail_sense.weather.infrastructure.persistence.CloudRepo
import com.kylecorry.trail_sense.weather.infrastructure.persistence.WeatherRepo
import com.kylecorry.trail_sense.weather.infrastructure.temperatures.TemperatureEstimator
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
    private val cloudRepo by lazy { CloudRepo.getInstance(context) }
    private val prefs by lazy { UserPreferences(context) }
    private val sharedPrefs by lazy { Preferences(context) }
    private val location by lazy { LocationSubsystem.getInstance(context) }
    private val estimator by lazy { TemperatureEstimator(context) }

    private lateinit var weatherService: WeatherService

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

        resetWeatherService()
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

    override suspend fun getTemperatureForecast(
        time: ZonedDateTime,
        location: Coordinate?,
        elevation: Distance?
    ): Reading<Temperature> = onDefault {
        val resolved = resolveLocation(location, elevation)
        val lookupLocation = resolved.first
        val lookupElevation = resolved.second
        val temperature = estimator.getTemperature(lookupLocation, time)
        Reading(
            Meteorology.getTemperatureAtElevation(
                temperature,
                Distance.meters(0f),
                lookupElevation
            ),
            time.toInstant()
        )
    }

    override suspend fun getTemperatureForecast(
        start: ZonedDateTime,
        end: ZonedDateTime,
        location: Coordinate?,
        elevation: Distance?
    ): List<Reading<Temperature>> = onDefault {
        val resolved = resolveLocation(location, elevation)
        val lookupLocation = resolved.first
        val lookupElevation = resolved.second
        val temperatures = estimator.getTemperatures(start, end, lookupLocation)
        temperatures.map {
            it.copy(
                value = Meteorology.getTemperatureAtElevation(
                    it.value,
                    Distance.meters(0f),
                    lookupElevation
                )
            )
        }
    }

    override suspend fun getTemperatureRange(
        date: LocalDate,
        location: Coordinate?,
        elevation: Distance?
    ): Range<Temperature> = onDefault {
        val resolved = resolveLocation(location, elevation)
        val lookupLocation = resolved.first
        val lookupElevation = resolved.second
        val temperatures = estimator.getDailyTemperatureRange(lookupLocation, date)
        Range(
            Meteorology.getTemperatureAtElevation(
                temperatures.start,
                Distance.meters(0f),
                lookupElevation
            ),
            Meteorology.getTemperatureAtElevation(
                temperatures.end,
                Distance.meters(0f),
                lookupElevation
            )
        )
    }

    override suspend fun getTemperatureRanges(
        year: Int,
        location: Coordinate?,
        elevation: Distance?
    ): List<Pair<LocalDate, Range<Temperature>>> = onDefault {
        val resolved = resolveLocation(location, elevation)
        val lookupLocation = resolved.first
        val lookupElevation = resolved.second
        val temperatures = estimator.getYearlyTemperatures(year, lookupLocation)
        temperatures.map {
            it.first to Range(
                Meteorology.getTemperatureAtElevation(
                    it.second.start,
                    Distance.meters(0f),
                    lookupElevation
                ),
                Meteorology.getTemperatureAtElevation(
                    it.second.end,
                    Distance.meters(0f),
                    lookupElevation
                )
            )
        }
    }

    private suspend fun resolveLocation(
        location: Coordinate?,
        elevation: Distance?
    ): Pair<Coordinate, Distance> {
        val lookupLocation: Coordinate
        val lookupElevation: Distance
        if (location == null || elevation == null) {
            val last = getRawHistory().lastOrNull()
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
            val last = getRawHistory().lastOrNull()?.time
            val maxPeriod = getWeatherMonitorFrequency().dividedBy(3)

            if (last != null && Duration.between(last, Instant.now()).abs() < maxPeriod) {
                // Still send out the weather alerts, just don't log a new reading
                SendWeatherAlertsCommand(context).execute(getWeather())
                return
            }

            MonitorWeatherCommand.create(context, background).execute()
        }
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
        resetWeatherService()
        synchronized(validLock) {
            isValid = true
        }
    }

    private fun getForecast(
        readings: List<WeatherObservation>,
        clouds: List<Reading<CloudGenus?>>
    ): List<WeatherForecast> {
        return weatherService.getForecast(
            readings.map { it.pressureReading() },
            clouds
        )
    }

    override suspend fun getCloudHistory(): List<Reading<CloudGenus?>> = onIO {
        cloudRepo.getAll().sortedBy { it.time }.map { Reading(it.value.genus, it.time) }
    }

    private suspend fun populateCache(): CurrentWeather {
        val history = getHistory()
        val allClouds = getCloudHistory()
        val forecast = getForecast(history, allClouds)
        val last = history.lastOrNull()
        var clouds = allClouds.lastOrNull()
        val maxCloudTime = Duration.ofHours(4)
        if (clouds == null || Duration.between(clouds.time, Instant.now()).abs() > maxCloudTime) {
            clouds = null
        }

        val stormCloudsSeen = listOf<CloudGenus?>(
            CloudGenus.Cumulonimbus,
            CloudGenus.Nimbostratus
        ).contains(clouds?.value)

        val tendency =
            forecast.first().tendency ?: PressureTendency(PressureCharacteristic.Steady, 0f)

        val currentConditions = forecast.first().conditions
        val primaryCondition = WeatherPrediction(
            currentConditions,
            listOf(),
            null,
            HourlyArrivalTime.Now,
            null
        ).primaryHourly

        val steadySystem =
            tendency.characteristic == PressureCharacteristic.Steady && listOf(
                WeatherCondition.Clear,
                WeatherCondition.Overcast
            ).contains(primaryCondition)

        // TODO: Replace with hourly forecast
        val arrival = when {
            primaryCondition == null -> null
            steadySystem || stormCloudsSeen -> HourlyArrivalTime.Now
            primaryCondition == WeatherCondition.Storm || tendency.characteristic.isRapid -> HourlyArrivalTime.VerySoon
            tendency.characteristic != PressureCharacteristic.Steady -> HourlyArrivalTime.Soon
            else -> HourlyArrivalTime.Later
        }

        // TODO: Calculate min, max, and current instead of average
        val historicalTemperature = try {
            val lastRawReading = last?.id?.let {
                weatherRepo.get(it)
            }
            val location = lastRawReading?.value?.location ?: location.location
            val elevation = lastRawReading?.value?.altitude?.let { Distance.meters(it) }
                ?: this.location.elevation

            val range = estimator.getDailyTemperatureRange(
                location,
                LocalDate.now()
            )
            val currentRaw = estimator.getTemperature(location, ZonedDateTime.now())
            val low = Meteorology.getTemperatureAtElevation(
                range.start,
                Distance.meters(0f),
                elevation
            )

            val high = Meteorology.getTemperatureAtElevation(
                range.end,
                Distance.meters(0f),
                elevation
            )

            val current = Meteorology.getTemperatureAtElevation(
                currentRaw,
                Distance.meters(0f),
                elevation
            )

            val average = Temperature((low.temperature + high.temperature) / 2f, low.units)
            TemperaturePrediction(average, low, high, current)
        } catch (e: Exception) {
            Log.e(javaClass.simpleName, "Unable to lookup average temperature", e)
            null
        }

        return CurrentWeather(
            WeatherPrediction(
                forecast.first().conditions,
                forecast.last().conditions,
                forecast.first().front,
                arrival,
                historicalTemperature
            ),
            tendency.copy(amount = tendency.amount * 3),
            last,
            clouds
        )
    }

    private fun resetWeatherService() {
        weatherService = WeatherService(prefs.weather)
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

    }

}