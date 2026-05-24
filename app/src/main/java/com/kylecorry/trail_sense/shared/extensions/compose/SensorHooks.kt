package com.kylecorry.trail_sense.shared.extensions.compose

import androidx.compose.runtime.Composable
import com.kylecorry.andromeda.core.sensors.IAltimeter
import com.kylecorry.andromeda.core.sensors.ISpeedometer
import com.kylecorry.andromeda.sense.compass.ICompass
import com.kylecorry.andromeda.sense.location.IGPS
import com.kylecorry.andromeda.signal.ICellSignalSensor
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.Speed
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.declination.DeclinationFactory
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.sensors.SensorSubsystem
import java.time.Duration

// Sensors

@Composable
fun useGPSSensor(frequency: Duration = Duration.ofMillis(20)): IGPS {
    val sensors = useService<SensorService>()
    return useMemo(sensors, frequency.seconds, frequency.nano) { sensors.getGPS(frequency) }
}

@Composable
fun useCellSignalSensor(removeUnregisteredSignals: Boolean = true): ICellSignalSensor {
    val sensors = useService<SensorService>()
    return useMemo(sensors, removeUnregisteredSignals) {
        sensors.getCellSignal(
            removeUnregisteredSignals
        )
    }
}

@Composable
fun useCompassSensor(): ICompass {
    val sensors = useService<SensorService>()
    return useMemo(sensors) { sensors.getCompass() }
}

@Composable
fun useAltimeterSensor(gps: IGPS? = null): IAltimeter {
    val sensors = useService<SensorService>()
    return useMemo(sensors, gps) {
        sensors.getAltimeter(gps = gps)
    }
}

@Composable
fun useSpeedometerSensor(gps: IGPS? = null): ISpeedometer {
    val sensors = useService<SensorService>()
    return useMemo(sensors, gps) {
        sensors.getSpeedometer(gps = gps)
    }
}

// Common sensor readings

@Composable
fun useGPSLocation(frequency: Duration = Duration.ofMillis(20)): Pair<Coordinate, Float?> {
    val gps = useGPSSensor(frequency)
    return useTopic(gps, gps.location to gps.horizontalAccuracy) {
        it.location to it.horizontalAccuracy
    }
}

data class NavigationSensorValues(
    val location: Coordinate,
    val locationAccuracy: Distance?,
    val elevation: Distance,
    val elevationAccuracy: Distance?,
    val bearing: Bearing,
    val declination: Float,
    val speed: Speed,
    val gpsSpeed: Speed,
    val gps: IGPS? = null,
    val compass: ICompass? = null
)

@Composable
fun useNavigationSensors(
    gpsFrequency: Duration = Duration.ofMillis(20),
    trueNorth: Boolean = false
): NavigationSensorValues {
    val gps = useGPSSensor(gpsFrequency)
    val compass = useCompassSensor()
    val altimeter = useAltimeterSensor(gps)
    val speedometer = useSpeedometerSensor(gps)
    val prefs = useService<UserPreferences>()

    val defaultGpsReading = useMemo(gps) {
        Triple(gps.location, gps.horizontalAccuracy?.let { Distance.meters(it) }, gps.speed)
    }

    val defaultSpeedReading = useMemo(speedometer) {
        speedometer.speed
    }

    val defaultElevationReading = useMemo(altimeter, gps) {
        Distance.meters(altimeter.altitude) to (if (altimeter is IGPS) altimeter.verticalAccuracy else gps.verticalAccuracy)?.let {
            Distance.meters(
                it
            )
        }
    }

    val defaultCompassReading = useMemo(compass) {
        compass.bearing
    }

    val declinationProvider = useMemo(prefs, gps) {
        DeclinationFactory().getDeclinationStrategy(prefs, gps)
    }
    val declination = useMemo(gps.location) { declinationProvider.getDeclination() }
    useEffect(compass, declination) { compass.declination = if (trueNorth) declination else 0f }

    val (location, locationAccuracy, gpsSpeed) = useTopic(
        gps,
        defaultGpsReading
    ) {
        Triple(gps.location, gps.horizontalAccuracy?.let { Distance.meters(it) }, gps.speed)
    }

    val speed = useTopic(speedometer, defaultSpeedReading) {
        speedometer.speed
    }

    val (elevation, elevationAccuracy) = useTopic(
        altimeter,
        defaultElevationReading
    ) {
        Distance.meters(altimeter.altitude) to (if (altimeter is IGPS) altimeter.verticalAccuracy else gps.verticalAccuracy)?.let {
            Distance.meters(
                it
            )
        }
    }

    val bearing = useTopic(compass, defaultCompassReading) {
        compass.bearing
    }

    return useMemo(
        location,
        locationAccuracy,
        elevation,
        elevationAccuracy,
        bearing,
        declination,
        speed,
        gpsSpeed,
        gps,
        compass
    ) {
        NavigationSensorValues(
            location,
            locationAccuracy,
            elevation,
            elevationAccuracy,
            bearing,
            declination,
            speed,
            gpsSpeed,
            gps,
            compass
        )
    }
}

@Composable
fun useLocation(refreshPolicy: SensorSubsystem.SensorRefreshPolicy = SensorSubsystem.SensorRefreshPolicy.RefreshIfInvalid): Pair<Coordinate, Boolean> {
    val sensors = useService<SensorSubsystem>()
    val lastLocation = useMemo(sensors) {
        sensors.lastKnownLocation
    }
    val (location, setLocation) = useState(lastLocation)
    val (isUpToDate, setIsUpToDate) = useState(refreshPolicy == SensorSubsystem.SensorRefreshPolicy.Cache)

    useEffect(refreshPolicy) {
        setLocation(sensors.getLocation(refreshPolicy))
        setIsUpToDate(true)
    }

    return location to isUpToDate
}
