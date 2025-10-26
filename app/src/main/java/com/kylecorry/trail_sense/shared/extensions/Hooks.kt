package com.kylecorry.trail_sense.shared.extensions

import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.sensors.IAltimeter
import com.kylecorry.andromeda.core.sensors.ISpeedometer
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
import com.kylecorry.luna.timer.CoroutineTimer
import com.kylecorry.luna.timer.TimerActionBehavior
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.sol.units.Speed
import com.kylecorry.sol.units.TimeUnits
import com.kylecorry.sol.units.Weight
import com.kylecorry.sol.units.WeightUnits
import com.kylecorry.trail_sense.R
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.time.Duration
import java.util.UUID
import kotlin.coroutines.CoroutineContext

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

fun ReactiveComponent.useSpeedometerSensor(gps: IGPS? = null): ISpeedometer {
    val sensors = useService<SensorService>()
    return useMemo(sensors, gps) {
        sensors.getSpeedometer(gps = gps)
    }
}

// Common sensor readings
fun AndromedaFragment.useGPSLocation(frequency: Duration = Duration.ofMillis(20)): Pair<Coordinate, Float?> {
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
    val gpsSpeed: Speed
)

fun AndromedaFragment.useNavigationSensors(
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
        gpsSpeed
    ) {
        NavigationSensorValues(
            location,
            locationAccuracy,
            elevation,
            elevationAccuracy,
            bearing,
            declination,
            speed,
            gpsSpeed
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

fun <T> T.useUnsavedChangesPrompt(hasChanges: Boolean) where T : Fragment, T : ReactiveComponent {
    val activity = useActivity() as? FragmentActivity
    useBackPressedCallback(hasChanges, activity) {
        if (hasChanges && activity != null) {
            Alerts.dialog(
                activity,
                getString(R.string.unsaved_changes),
                getString(R.string.unsaved_changes_message),
                okText = getString(R.string.dialog_leave)
            ) { cancelled ->
                if (!cancelled) {
                    remove()
                    activity.onBackPressedDispatcher.onBackPressed()
                }
            }
            true
        } else {
            false
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

fun ReactiveComponent.useCoordinatePreference(
    key: String
): Pair<Coordinate?, (Coordinate?) -> Unit> {
    return usePreference(key, IPreferences::getCoordinate, IPreferences::putCoordinate)
}

fun ReactiveComponent.useDistancePreference(
    key: String
): Pair<Distance?, (Distance?) -> Unit> {
    val (value, setValue) = useFloatPreference("$key-value")
    val (unit, setUnit) = useIntPreference("$key-unit")

    val setter = useCallback { distance: Distance? ->
        setValue(distance?.value)
        setUnit(distance?.units?.id)
    }

    val distance = useMemo(value, unit) {
        if (value == null || unit == null) {
            return@useMemo null
        }

        Distance.from(
            value,
            DistanceUnits.entries.firstOrNull { it.id == unit } ?: DistanceUnits.Meters)
    }

    return distance to setter
}

fun ReactiveComponent.useWeightPreference(
    key: String
): Pair<Weight?, (Weight?) -> Unit> {
    val (value, setValue) = useFloatPreference("$key-value")
    val (unit, setUnit) = useIntPreference("$key-unit")

    val setter = useCallback { weight: Weight? ->
        setValue(weight?.value)
        setUnit(weight?.units?.id)
    }

    val weight = useMemo(value, unit) {
        if (value == null || unit == null) {
            return@useMemo null
        }

        Weight.from(
            value,
            WeightUnits.entries.firstOrNull { it.id == unit } ?: WeightUnits.Kilograms)
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

        Speed.from(
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

fun ReactiveComponent.useBindCoordinateAndElevationViews(
    coordinateView: CoordinateInputView,
    elevationView: ElevationInputView
) {
    val prefs = useService<UserPreferences>()

    useEffect(coordinateView, elevationView, prefs) {
        coordinateView.setOnAutoLocationClickListener {
            if (elevationView.elevation == null) {
                elevationView.autofill()
            }
        }

        elevationView.setOnAutoElevationClickListener {
            if (coordinateView.coordinate == null) {
                coordinateView.autofill()
            }
        }

        coordinateView.setOnBeaconSelectedListener {
            it.elevation?.let { elevation ->
                elevationView.elevation =
                    Distance.meters(elevation).convertTo(prefs.baseDistanceUnits)
            }
        }
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

fun ReactiveComponent.usePauseEffect(vararg values: Any?, action: () -> Unit) {
    useLifecycleEffect(
        Lifecycle.Event.ON_PAUSE,
        *values
    ) {
        action()
    }
}

fun ReactiveComponent.useResumeEffect(vararg values: Any?, action: () -> Unit) {
    useLifecycleEffect(
        Lifecycle.Event.ON_RESUME,
        *values
    ) {
        action()
    }
}


// LiveData
fun <T : Any, V> ReactiveComponent.useLiveData(
    data: LiveData<T>,
    default: V,
    mapper: (T) -> V
): V {
    val (state, setState) = useState(default)
    val owner = useLifecycleOwner()

    // Note: This does not change when the mapper changes
    useEffect(data, owner) {
        data.observe(owner) {
            setState(mapper(it))
        }
    }

    return state
}

fun <T : Any, V> ReactiveComponent.useLiveData(
    data: LiveData<T>,
    mapper: (T) -> V?
): V? {
    return useLiveData(data, null, mapper)
}

fun ReactiveComponent.useTimer(
    interval: Long,
    scope: CoroutineScope = CoroutineScope(Dispatchers.Default),
    observeOn: CoroutineContext = Dispatchers.Main,
    actionBehavior: TimerActionBehavior = TimerActionBehavior.Wait,
    runnable: suspend () -> Unit
) {
    val timer = useMemo {
        CoroutineTimer(scope, observeOn, actionBehavior, runnable)
    }

    useResumeEffect(timer, interval) {
        timer.interval(interval)
    }

    usePauseEffect(timer) {
        timer.stop()
    }
}

fun ReactiveComponent.useTrigger(): Pair<String, () -> Unit> {
    val (key, setKey) = useState("")
    val trigger = useCallback<Unit> {
        setKey(UUID.randomUUID().toString())
    }
    return useMemo(key, trigger) { key to trigger }
}