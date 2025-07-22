package com.kylecorry.trail_sense.shared.extensions

import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.core.sensors.IAltimeter
import com.kylecorry.andromeda.core.ui.ReactiveComponent
import com.kylecorry.andromeda.core.ui.useCallback
import com.kylecorry.andromeda.core.ui.useService
import com.kylecorry.andromeda.fragments.AndromedaFragment
import com.kylecorry.andromeda.fragments.LifecycleHookTrigger
import com.kylecorry.andromeda.fragments.onBackPressed
import com.kylecorry.andromeda.fragments.useBackgroundEffect
import com.kylecorry.andromeda.fragments.useTopic
import com.kylecorry.andromeda.preferences.IPreferences
import com.kylecorry.andromeda.sense.compass.ICompass
import com.kylecorry.andromeda.sense.location.IGPS
import com.kylecorry.andromeda.signal.ICellSignalSensor
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.sol.units.Speed
import com.kylecorry.sol.units.TimeUnits
import com.kylecorry.sol.units.Weight
import com.kylecorry.sol.units.WeightUnits
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.alerts.ILoadingIndicator
import com.kylecorry.trail_sense.shared.declination.DeclinationFactory
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.sensors.SensorSubsystem
import com.kylecorry.trail_sense.shared.views.CoordinateInputView
import com.kylecorry.trail_sense.shared.views.ElevationInputView
import com.kylecorry.trail_sense.shared.views.SearchView
import java.time.Duration

// Sensors

fun ReactiveComponent.useGPSSensor(frequency: Duration = Duration.ofMillis(20)): IGPS {
    val sensors = useService<SensorService>()
    return useMemo(sensors, frequency.seconds, frequency.nano) { sensors.getGPS(frequency) }
}

fun ReactiveComponent.useCellSignalSensor(removeUnregisteredSignals: Boolean = true): ICellSignalSensor {
    val sensors = useService<SensorService>()
    return useMemo(sensors, removeUnregisteredSignals) {
        sensors.getCellSignal(
            removeUnregisteredSignals
        )
    }
}

fun AndromedaFragment.useCompassSensor(): ICompass {
    val sensors = useService<SensorService>()
    return useMemo(sensors) { sensors.getCompass() }
}

fun AndromedaFragment.useAltimeterSensor(gps: IGPS? = null): IAltimeter {
    val sensors = useService<SensorService>()
    return useMemo(sensors, gps) {
        sensors.getAltimeter(gps = gps)
    }
}

// Common sensor readings
fun AndromedaFragment.useGPSLocation(frequency: Duration = Duration.ofMillis(20)): Pair<Coordinate, Float?> {
    val gps = useGPSSensor(frequency)
    return useTopic(gps, gps.location to gps.horizontalAccuracy) {
        it.location to it.horizontalAccuracy
    }
}

fun AndromedaFragment.useCompassBearing(applyDeclination: Boolean): Bearing {
    val sensors = useService<SensorService>()
    val compass = useMemo(sensors) { sensors.getCompass() }
    return useTopic(compass, compass.bearing) {
        compass.bearing
    }
}

data class NavigationSensorValues(
    val location: Coordinate,
    val locationAccuracy: Float?,
    val elevation: Float,
    val elevationAccuracy: Float?,
    val bearing: Bearing,
    val declination: Float,
    val speed: Speed
)

fun AndromedaFragment.useNavigationSensors(
    gpsFrequency: Duration = Duration.ofMillis(20),
    trueNorth: Boolean = false
): NavigationSensorValues {
    val gps = useGPSSensor(gpsFrequency)
    val compass = useCompassSensor()
    val altimeter = useAltimeterSensor(gps)
    val prefs = useService<UserPreferences>()
    val declinationProvider = useMemo(prefs, gps) {
        DeclinationFactory().getDeclinationStrategy(prefs, gps)
    }
    val declination = useMemo(gps.location) { declinationProvider.getDeclination() }
    useEffect(compass, declination) { compass.declination = if (trueNorth) declination else 0f }

    val (location, locationAccuracy, speed) = useTopic(
        gps,
        Triple(gps.location, gps.horizontalAccuracy, gps.speed)
    ) {
        Triple(gps.location, gps.horizontalAccuracy, gps.speed)
    }

    val (elevation, elevationAccuracy) = useTopic(
        altimeter,
        altimeter.altitude to if (altimeter is IGPS) altimeter.verticalAccuracy else gps.verticalAccuracy
    ) {
        altimeter.altitude to if (altimeter is IGPS) altimeter.verticalAccuracy else gps.verticalAccuracy
    }

    val bearing = useTopic(compass, compass.bearing) {
        compass.bearing
    }

    return useMemo(
        location,
        locationAccuracy,
        elevation,
        elevationAccuracy,
        bearing,
        declination,
        speed
    ) {
        NavigationSensorValues(
            location,
            locationAccuracy,
            elevation,
            elevationAccuracy,
            bearing,
            declination,
            speed
        )
    }
}

fun AndromedaFragment.useLocation(refreshPolicy: SensorSubsystem.SensorRefreshPolicy = SensorSubsystem.SensorRefreshPolicy.RefreshIfInvalid): Pair<Coordinate, Boolean> {
    val sensors = useService<SensorSubsystem>()
    val lastLocation = useMemo(sensors) {
        sensors.lastKnownLocation
    }
    val (location, setLocation) = useState(lastLocation)
    val (isUpToDate, setIsUpToDate) = useState(refreshPolicy == SensorSubsystem.SensorRefreshPolicy.Cache)

    useBackgroundEffect(refreshPolicy, cancelWhenRerun = true) {
        setLocation(sensors.getLocation(refreshPolicy))
        setIsUpToDate(true)
    }

    return location to isUpToDate
}

fun <T> T.useNavController(): NavController where T : ReactiveComponent, T : Fragment {
    return useMemo(useRootView()) { findNavController() }
}

fun <T> T.useBackPressedCallback(
    vararg values: Any?,
    callback: OnBackPressedCallback.() -> Boolean
) where T : Fragment, T : ReactiveComponent {
    val navController = useNavController()
    useEffectWithCleanup(*values) {
        val listener = onBackPressed {
            val consumed = callback()
            if (!consumed) {
                remove()
                navController.popBackStack()
            }
        }

        return@useEffectWithCleanup {
            listener.remove()
        }
    }
}

fun ReactiveComponent.useSearch(view: SearchView, onSearch: (String) -> Unit) {
    useEffect(view) {
        view.setOnSearchListener(onSearch)
    }
}

fun AndromedaFragment.useShowDisclaimer(
    title: String,
    message: CharSequence,
    shownKey: String,
    okText: String = getString(android.R.string.ok),
    cancelText: String? = getString(android.R.string.cancel),
    considerShownIfCancelled: Boolean = true,
    shownValue: Boolean = true,
    onClose: (cancelled: Boolean, agreed: Boolean) -> Unit = { _, _ -> }
) {
    val context = useAndroidContext()
    useEffect {
        CustomUiUtils.disclaimer(
            context,
            title,
            message,
            shownKey,
            okText,
            cancelText,
            considerShownIfCancelled,
            shownValue,
            onClose
        )
    }
}

fun ReactiveComponent.useLoadingIndicator(isLoading: Boolean, indicator: ILoadingIndicator) {
    useEffect(indicator, isLoading) {
        if (isLoading) {
            indicator.show()
        } else {
            indicator.hide()
        }
    }
}

// Preferences

fun <T> ReactiveComponent.usePreference(
    key: String,
    load: (IPreferences, String) -> T?,
    save: (IPreferences, String, T) -> Unit
): Pair<T?, (T?) -> Unit> {
    val prefs = useService<PreferencesSubsystem>()

    val initialValue = useMemo(prefs, key) {
        load(prefs.preferences, key)
    }

    val (value, setValue) = useState(initialValue)

    val updatePreference = useCallback(key, prefs) { newValue: T? ->
        if (newValue == null) {
            prefs.preferences.remove(key)
        } else {
            save(prefs.preferences, key, newValue)
        }
        setValue(newValue)
    }

    return value to updatePreference
}

fun ReactiveComponent.useFloatPreference(
    key: String
): Pair<Float?, (Float?) -> Unit> {
    return usePreference(key, IPreferences::getFloat, IPreferences::putFloat)
}

fun ReactiveComponent.useIntPreference(
    key: String
): Pair<Int?, (Int?) -> Unit> {
    return usePreference(key, IPreferences::getInt, IPreferences::putInt)
}

fun ReactiveComponent.useDistancePreference(
    key: String
): Pair<Distance?, (Distance?) -> Unit> {
    val (value, setValue) = useFloatPreference("$key-value")
    val (unit, setUnit) = useIntPreference("$key-unit")

    val setter = useCallback { distance: Distance? ->
        setValue(distance?.distance)
        setUnit(distance?.units?.id)
    }

    val distance = useMemo(value, unit) {
        if (value == null || unit == null) {
            return@useMemo null
        }

        Distance(value, DistanceUnits.entries.firstOrNull { it.id == unit } ?: DistanceUnits.Meters)
    }

    return distance to setter
}

fun ReactiveComponent.useWeightPreference(
    key: String
): Pair<Weight?, (Weight?) -> Unit> {
    val (value, setValue) = useFloatPreference("$key-value")
    val (unit, setUnit) = useIntPreference("$key-unit")

    val setter = useCallback { weight: Weight? ->
        setValue(weight?.weight)
        setUnit(weight?.units?.id)
    }

    val weight = useMemo(value, unit) {
        if (value == null || unit == null) {
            return@useMemo null
        }

        Weight(value, WeightUnits.entries.firstOrNull { it.id == unit } ?: WeightUnits.Kilograms)
    }

    return weight to setter
}

fun ReactiveComponent.useSpeedPreference(
    key: String
): Pair<Speed?, (Speed?) -> Unit> {
    val (value, setValue) = useFloatPreference("$key-value")
    val (distanceUnit, setDistanceUnit) = useIntPreference("$key-distance-unit")
    val (timeUnit, setTimeUnit) = useIntPreference("$key-time-unit")

    val setter = useCallback { speed: Speed? ->
        setValue(speed?.speed)
        setDistanceUnit(speed?.distanceUnits?.id)
        setTimeUnit(speed?.timeUnits?.id)
    }

    val speed = useMemo(value, distanceUnit, timeUnit) {
        if (value == null || distanceUnit == null || timeUnit == null) {
            return@useMemo null
        }

        Speed(
            value,
            DistanceUnits.entries.firstOrNull { it.id == distanceUnit } ?: DistanceUnits.Meters,
            TimeUnits.entries.firstOrNull { it.id == timeUnit } ?: TimeUnits.Seconds
        )
    }

    return speed to setter
}

fun ReactiveComponent.useCoordinateInputView(
    id: Int,
    lifecycleHookTrigger: LifecycleHookTrigger
): CoordinateInputView {
    return useViewWithCleanup(id, lifecycleHookTrigger) {
        it.pause()
    }
}

fun ReactiveComponent.useElevationInputView(
    id: Int,
    lifecycleHookTrigger: LifecycleHookTrigger
): ElevationInputView {
    return useViewWithCleanup(id, lifecycleHookTrigger) {
        it.pause()
    }
}

fun <T : View> ReactiveComponent.useViewWithCleanup(
    id: Int,
    lifecycleHookTrigger: LifecycleHookTrigger,
    cleanup: (T) -> Unit
): T {
    val view = useView<T>(id)
    useEffectWithCleanup(lifecycleHookTrigger.onResume(), view) {
        return@useEffectWithCleanup {
            cleanup(view)
        }
    }
    return view
}

fun ReactiveComponent.useLifecycleEffect(
    lifecycleEvent: Lifecycle.Event,
    vararg values: Any?,
    action: () -> Unit
) {
    val owner = useLifecycleOwner()
    val (lastObserver, setLastObserver) = useState<LifecycleObserver?>(null)
    val observer = useMemo(*values, lifecycleEvent) {
        LifecycleEventObserver { source: LifecycleOwner, event: Lifecycle.Event ->
            if (event == lifecycleEvent) {
                action()
            }
        }
    }

    useEffect(owner, observer) {
        setLastObserver(observer)
        lastObserver?.let {
            owner.lifecycle.removeObserver(it)
        }
        owner.lifecycle.addObserver(observer)
    }
}

fun ReactiveComponent.useDestroyEffect(vararg values: Any?, action: () -> Unit) {
    useLifecycleEffect(
        Lifecycle.Event.ON_DESTROY,
        *values
    ) {
        action()
    }
}