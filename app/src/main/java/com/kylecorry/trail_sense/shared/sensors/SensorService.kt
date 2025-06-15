package com.kylecorry.trail_sense.shared.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import com.kylecorry.andromeda.battery.Battery
import com.kylecorry.andromeda.core.sensors.IAltimeter
import com.kylecorry.andromeda.core.sensors.ISpeedometer
import com.kylecorry.andromeda.core.sensors.IThermometer
import com.kylecorry.andromeda.permissions.Permissions
import com.kylecorry.andromeda.sense.Sensors
import com.kylecorry.andromeda.sense.accelerometer.Accelerometer
import com.kylecorry.andromeda.sense.accelerometer.GravitySensor
import com.kylecorry.andromeda.sense.accelerometer.IAccelerometer
import com.kylecorry.andromeda.sense.accelerometer.LowPassAccelerometer
import com.kylecorry.andromeda.sense.altitude.BarometricAltimeter
import com.kylecorry.andromeda.sense.altitude.FusedAltimeter
import com.kylecorry.andromeda.sense.barometer.Barometer
import com.kylecorry.andromeda.sense.barometer.IBarometer
import com.kylecorry.andromeda.sense.compass.ICompass
import com.kylecorry.andromeda.sense.hygrometer.Hygrometer
import com.kylecorry.andromeda.sense.hygrometer.IHygrometer
import com.kylecorry.andromeda.sense.light.ILightSensor
import com.kylecorry.andromeda.sense.light.LightSensor
import com.kylecorry.andromeda.sense.location.GPS
import com.kylecorry.andromeda.sense.location.IGPS
import com.kylecorry.andromeda.sense.location.ISatelliteGPS
import com.kylecorry.andromeda.sense.location.filters.GPSGaussianAltitudeFilter
import com.kylecorry.andromeda.sense.location.filters.GPSPassThroughAltitudeFilter
import com.kylecorry.andromeda.sense.magnetometer.IMagnetometer
import com.kylecorry.andromeda.sense.magnetometer.LowPassMagnetometer
import com.kylecorry.andromeda.sense.magnetometer.Magnetometer
import com.kylecorry.andromeda.sense.mock.MockBarometer
import com.kylecorry.andromeda.sense.mock.MockGyroscope
import com.kylecorry.andromeda.sense.orientation.DeviceOrientation
import com.kylecorry.andromeda.sense.orientation.GameRotationSensor
import com.kylecorry.andromeda.sense.orientation.Gyroscope
import com.kylecorry.andromeda.sense.orientation.IOrientationSensor
import com.kylecorry.andromeda.sense.pedometer.IPedometer
import com.kylecorry.andromeda.sense.pedometer.Pedometer
import com.kylecorry.andromeda.sense.temperature.AmbientThermometer
import com.kylecorry.andromeda.sense.temperature.Thermometer
import com.kylecorry.andromeda.signal.CellSignalSensor
import com.kylecorry.andromeda.signal.ICellSignalSensor
import com.kylecorry.sol.math.filters.LowPassFilter
import com.kylecorry.sol.units.Pressure
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.sensors.altimeter.AltimeterWrapper
import com.kylecorry.trail_sense.shared.sensors.altimeter.AutoInitializeBarometricAltimeter
import com.kylecorry.trail_sense.shared.sensors.altimeter.CachedAltimeter
import com.kylecorry.trail_sense.shared.sensors.altimeter.CachingAltimeterWrapper
import com.kylecorry.trail_sense.shared.sensors.altimeter.DigitalElevationModel
import com.kylecorry.trail_sense.shared.sensors.altimeter.GaussianAltimeterWrapper
import com.kylecorry.trail_sense.shared.sensors.altimeter.OverrideAltimeter
import com.kylecorry.trail_sense.shared.sensors.barometer.CalibratedBarometer
import com.kylecorry.trail_sense.shared.sensors.hygrometer.MockHygrometer
import com.kylecorry.trail_sense.shared.sensors.overrides.CachedGPS
import com.kylecorry.trail_sense.shared.sensors.overrides.OverrideGPS
import com.kylecorry.trail_sense.shared.sensors.providers.CompassProvider
import com.kylecorry.trail_sense.shared.sensors.speedometer.BacktrackSpeedometer
import com.kylecorry.trail_sense.shared.sensors.thermometer.CalibratedThermometerWrapper
import com.kylecorry.trail_sense.shared.sensors.thermometer.HistoricThermometer
import com.kylecorry.trail_sense.shared.sensors.thermometer.ThermometerSource
import com.kylecorry.trail_sense.tools.navigation.infrastructure.NavigationPreferences
import com.kylecorry.trail_sense.tools.pedometer.domain.StrideLengthPaceCalculator
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.AveragePaceSpeedometer
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.CurrentPaceSpeedometer
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.StepCounter
import java.time.Duration

// Maybe use the concept of a use case
// Ex. SensorPurpose.Background, SensorPurpose.Calibration, SensorPurpose.Diagnostics
// Using those, it can adjust settings to be more appropriate for the use case

class SensorService(ctx: Context) {

    private var context = ctx.applicationContext
    private val userPrefs by lazy { UserPreferences(context) }

    // TODO: This should control update frequency
    fun getGPS(frequency: Duration = Duration.ofMillis(20)): ISatelliteGPS {

        val hasPermission = hasLocationPermission()

        if (!userPrefs.useAutoLocation || !hasPermission) {
            return OverrideGPS(context, frequency.toMillis())
        }

        if (GPS.isAvailable(context)) {
            return CustomGPS(
                context,
                frequency,
                frequency
            )
        }

        return CachedGPS(context, frequency.toMillis())
    }

    fun getGPSFromAltimeter(altimeter: IAltimeter): IGPS? {
        return if (altimeter is IGPS) {
            altimeter
        } else if (altimeter is AltimeterWrapper && altimeter.altimeter is IGPS) {
            altimeter.altimeter as IGPS
        } else if (altimeter is AltimeterWrapper && altimeter.altimeter is AltimeterWrapper) {
            getGPSFromAltimeter(altimeter.altimeter)
        } else {
            null
        }
    }

    fun hasLocationPermission(background: Boolean = false): Boolean {
        return if (background) {
            Permissions.isBackgroundLocationEnabled(context)
        } else {
            Permissions.canGetFineLocation(context)
        }
    }

    fun getPedometer(): IPedometer {
        return if (Permissions.canRecognizeActivity(context)) {
            Pedometer(context, ENVIRONMENT_SENSOR_DELAY)
        } else {
            MockPedometer()
        }
    }

    fun getSpeedometer(gps: IGPS? = null): ISpeedometer {
        return when (userPrefs.navigation.speedometerMode) {
            NavigationPreferences.SpeedometerMode.Backtrack -> BacktrackSpeedometer(context)
            NavigationPreferences.SpeedometerMode.GPS -> gps ?: getGPS()
            NavigationPreferences.SpeedometerMode.CurrentPace -> CurrentPaceSpeedometer(
                getPedometer(), StrideLengthPaceCalculator(userPrefs.pedometer.strideLength)
            )

            NavigationPreferences.SpeedometerMode.AveragePace -> AveragePaceSpeedometer(
                StepCounter(
                    PreferencesSubsystem.getInstance(context).preferences
                ), StrideLengthPaceCalculator(userPrefs.pedometer.strideLength)
            )
        }
    }

    private fun getGPSAltimeter(gps: IGPS? = null): IAltimeter {
        val mode = userPrefs.altimeterMode

        if (mode == UserPreferences.AltimeterMode.Override) {
            return OverrideAltimeter(context)
        } else {
            if (!GPS.isAvailable(context)) {
                return CachedAltimeter(context)
            }

            return gps ?: getGPS()
        }
    }

    private fun getDigitalElevationModel(gps: IGPS? = null): IGPS {
        return DigitalElevationModel(
            context, gps ?: getGPS()
        )
    }

    fun getAltimeter(
        preferGPS: Boolean = false, gps: IGPS? = null
    ): IAltimeter {
        if (preferGPS) {
            return CachingAltimeterWrapper(
                context, GaussianAltimeterWrapper(
                    getGPSAltimeter(gps), userPrefs.altimeterSamples
                )
            )
        }

        val hasBarometer = Sensors.hasBarometer(context)

        val mode = userPrefs.altimeterMode

        if (mode == UserPreferences.AltimeterMode.Override) {
            return OverrideAltimeter(context)
        } else if (mode == UserPreferences.AltimeterMode.Barometer && hasBarometer) {
            return CachingAltimeterWrapper(
                context, BarometricAltimeter(
                    getBarometer(),
                    seaLevelPressure = Pressure.hpa(userPrefs.seaLevelPressureOverride)
                )
            )
        } else if (mode == UserPreferences.AltimeterMode.DigitalElevationModel) {
            return CachingAltimeterWrapper(context, getDigitalElevationModel(gps))
        } else {
            if (!GPS.isAvailable(context)) {
                if (mode == UserPreferences.AltimeterMode.GPSBarometer && hasBarometer) {
                    return CachingAltimeterWrapper(
                        context, AutoInitializeBarometricAltimeter(
                            getBarometer(),
                            CachedAltimeter(context)
                        )
                    )
                }

                return CachedAltimeter(context)
            }

            var gps = gps ?: getGPS()
            if (mode == UserPreferences.AltimeterMode.DigitalElevationModelBarometer) {
                gps = getDigitalElevationModel(gps)
            }

            return if ((mode == UserPreferences.AltimeterMode.GPSBarometer || mode == UserPreferences.AltimeterMode.DigitalElevationModelBarometer) && hasBarometer) {
                CachingAltimeterWrapper(
                    context, FusedAltimeter(
                        gps,
                        getBarometer(),
                        PreferencesSubsystem.getInstance(context).preferences,
                        gpsFilter = if (mode == UserPreferences.AltimeterMode.DigitalElevationModelBarometer) GPSPassThroughAltitudeFilter() else GPSGaussianAltitudeFilter(
                            userPrefs.altimeterSamples
                        ),
                        useContinuousCalibration = userPrefs.altimeter.useFusedAltimeterContinuousCalibration,
                        recalibrationInterval = userPrefs.altimeter.fusedAltimeterForcedRecalibrationInterval,
                        useMSLAltitude = false,
                        shouldLog = false

                    )
                )
            } else {
                CachingAltimeterWrapper(
                    context, GaussianAltimeterWrapper(gps, userPrefs.altimeterSamples)
                )
            }
        }
    }

    fun hasCompass(): Boolean {
        return Sensors.hasCompass(context)
    }

    fun hasGyroscope(): Boolean {
        return Sensors.hasGyroscope(context)
    }

    fun getCompass(): ICompass {
        return CompassProvider(context, userPrefs.compass).get(MOTION_SENSOR_DELAY)
    }

    fun getOrientation(): IOrientationSensor {
        return CompassProvider(context, userPrefs.compass).getOrientationSensor(MOTION_SENSOR_DELAY)
    }

    fun getDeviceOrientationSensor(): DeviceOrientation {
        // While not technically an environment sensor, it doesn't need to update often - and can match their rate
        return DeviceOrientation(context, ENVIRONMENT_SENSOR_DELAY)
    }

    fun getBarometer(calibrated: Boolean = true): IBarometer {
        if (!userPrefs.weather.hasBarometer) {
            return MockBarometer()
        }

        val rawBarometer = Barometer(context, ENVIRONMENT_SENSOR_DELAY)

        val barometer = if (calibrated) CalibratedBarometer(
            rawBarometer,
            userPrefs.weather.barometerOffset
        ) else rawBarometer

        return FilteredBarometer(barometer, 3) {
            LowPassFilter(0.1f, it)
        }
    }

    fun getThermometer(calibrated: Boolean = true): IThermometer {
        val thermometer = when (userPrefs.thermometer.source) {
            ThermometerSource.Historic -> HistoricThermometer(context)
            ThermometerSource.Sensor -> getThermometerSensor()
        }
        return if (calibrated) {
            CalibratedThermometerWrapper(
                thermometer, userPrefs.thermometer.calibrator
            )
        } else {
            thermometer
        }
    }

    @Suppress("DEPRECATION")
    private fun getThermometerSensor(): IThermometer {
        if (Sensors.hasSensor(context, Sensor.TYPE_AMBIENT_TEMPERATURE)) {
            return AmbientThermometer(context, ENVIRONMENT_SENSOR_DELAY)
        }

        if (Sensors.hasSensor(context, Sensor.TYPE_TEMPERATURE)) {
            return Thermometer(context, ENVIRONMENT_SENSOR_DELAY)
        }

        return Battery(context)
    }

    fun getHygrometer(): IHygrometer {
        if (Sensors.hasHygrometer(context)) {
            return Hygrometer(context, ENVIRONMENT_SENSOR_DELAY)
        }

        return MockHygrometer()
    }

    fun getCellSignal(removeUnregisteredSignals: Boolean = true): ICellSignalSensor {
        if (!hasLocationPermission()) {
            return MockCellSignalSensor()
        }
        return CellSignalSensor(
            context,
            userPrefs.cellSignal.populateCache,
            removeUnregisteredSignals
        )
    }

    fun getGravity(): IAccelerometer {
        return if (Sensors.hasSensor(context, Sensor.TYPE_GRAVITY)) {
            GravitySensor(context, MOTION_SENSOR_DELAY)
        } else {
            LowPassAccelerometer(context, MOTION_SENSOR_DELAY)
        }
    }

    fun getMagnetometer(filtered: Boolean = false): IMagnetometer {
        return if (filtered) {
            LowPassMagnetometer(context, MOTION_SENSOR_DELAY)
        } else {
            Magnetometer(context, MOTION_SENSOR_DELAY)
        }
    }

    fun getGyroscope(): IOrientationSensor {
        if (!Sensors.hasGyroscope(context)) {
            return MockGyroscope()
        }
        if (Sensors.hasSensor(context, Sensor.TYPE_GAME_ROTATION_VECTOR)) {
            return GameRotationSensor(context, MOTION_SENSOR_DELAY)
        }
        return Gyroscope(context, MOTION_SENSOR_DELAY)
    }

    fun getAccelerometer(): IAccelerometer {
        return Accelerometer(context, MOTION_SENSOR_DELAY)
    }

    fun getLightSensor(): ILightSensor {
        return LightSensor(context, ENVIRONMENT_SENSOR_DELAY)
    }

    companion object {
        const val MOTION_SENSOR_DELAY = SensorManager.SENSOR_DELAY_GAME
        private const val ENVIRONMENT_SENSOR_DELAY = SensorManager.SENSOR_DELAY_NORMAL
    }

}