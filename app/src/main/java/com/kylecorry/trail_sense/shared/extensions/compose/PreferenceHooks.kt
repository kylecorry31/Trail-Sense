package com.kylecorry.trail_sense.shared.extensions.compose

import androidx.compose.runtime.Composable
import com.kylecorry.andromeda.preferences.IPreferences
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.sol.units.Speed
import com.kylecorry.sol.units.TimeUnits
import com.kylecorry.sol.units.Weight
import com.kylecorry.sol.units.WeightUnits
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem

// Preferences

@Composable
fun <T> usePreference(
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

@Composable
fun useFloatPreference(
    key: String
): Pair<Float?, (Float?) -> Unit> {
    return usePreference(key, IPreferences::getFloat, IPreferences::putFloat)
}

@Composable
fun useIntPreference(
    key: String
): Pair<Int?, (Int?) -> Unit> {
    return usePreference(key, IPreferences::getInt, IPreferences::putInt)
}

@Composable
fun useCoordinatePreference(
    key: String
): Pair<Coordinate?, (Coordinate?) -> Unit> {
    return usePreference(key, IPreferences::getCoordinate, IPreferences::putCoordinate)
}

@Composable
fun useDistancePreference(
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

@Composable
fun useWeightPreference(
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

@Composable
fun useSpeedPreference(
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


@Composable
fun useBooleanPreference(
    key: String
): Pair<Boolean?, (Boolean?) -> Unit> {
    return usePreference(key, IPreferences::getBoolean, IPreferences::putBoolean)
}
