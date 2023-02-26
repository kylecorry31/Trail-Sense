package com.kylecorry.trail_sense.navigation.infrastructure

import android.content.Context
import com.kylecorry.andromeda.core.toFloatCompat
import com.kylecorry.andromeda.core.toIntCompat
import com.kylecorry.andromeda.core.units.CoordinateFormat
import com.kylecorry.andromeda.location.GPS
import com.kylecorry.andromeda.preferences.BooleanPreference
import com.kylecorry.andromeda.preferences.IntEnumPreference
import com.kylecorry.andromeda.preferences.StringEnumPreference
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.navigation.beacons.infrastructure.sort.BeaconSortMethod
import com.kylecorry.trail_sense.navigation.paths.domain.LineStyle
import com.kylecorry.trail_sense.navigation.paths.domain.PathPointColoringStyle
import com.kylecorry.trail_sense.navigation.paths.domain.PathStyle
import com.kylecorry.trail_sense.navigation.paths.infrastructure.persistence.IPathPreferences
import com.kylecorry.trail_sense.navigation.paths.ui.PathSortMethod
import com.kylecorry.trail_sense.settings.infrastructure.IBeaconPreferences
import com.kylecorry.trail_sense.settings.infrastructure.ICompassStylePreferences
import com.kylecorry.trail_sense.settings.infrastructure.IMapPreferences
import com.kylecorry.trail_sense.shared.QuickActionType
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.tools.maps.domain.sort.MapSortMethod
import java.time.Duration

class NavigationPreferences(private val context: Context) : ICompassStylePreferences,
    IPathPreferences, IBeaconPreferences, IMapPreferences {

    private val cache by lazy { PreferencesSubsystem.getInstance(context).preferences }

    var useTrueNorth: Boolean
        get() = (cache.getBoolean(
            context.getString(R.string.pref_use_true_north)
        ) ?: true
                ) && GPS.isAvailable(context)
        set(value) = cache.putBoolean(
            context.getString(R.string.pref_use_true_north),
            value
        )

    val showCalibrationOnNavigateDialog: Boolean
        get() = cache.getBoolean(
            context.getString(R.string.pref_show_calibrate_on_navigate_dialog)
        ) ?: true

    val lockScreenPresence: Boolean
        get() = cache.getBoolean(context.getString(R.string.pref_navigation_lock_screen_presence))
            ?: false

    var useLegacyCompass: Boolean
        get() = cache.getBoolean(context.getString(R.string.pref_use_legacy_compass)) ?: false
        set(value) = cache.putBoolean(
            context.getString(R.string.pref_use_legacy_compass),
            value
        )

    var compassSmoothing: Int
        get() = cache.getInt(context.getString(R.string.pref_compass_filter_amt)) ?: 1
        set(value) = cache.putInt(
            context.getString(R.string.pref_compass_filter_amt),
            value
        )

    override val showLastSignalBeacon: Boolean
        get() = cache.getBoolean(context.getString(R.string.pref_show_last_signal_beacon)) ?: true

    override val useLinearCompass: Boolean
        get() = cache.getBoolean(context.getString(R.string.pref_show_linear_compass)) ?: true

    val showMultipleBeacons: Boolean
        get() = cache.getBoolean(context.getString(R.string.pref_display_multi_beacons)) ?: true

    val numberOfVisibleBeacons: Int
        get() {
            val raw = cache.getString(context.getString(R.string.pref_num_visible_beacons)) ?: "10"
            return raw.toIntOrNull() ?: 10
        }

    override val useRadarCompass: Boolean
        get() = showMultipleBeacons && (cache.getBoolean(context.getString(R.string.pref_nearby_radar))
            ?: true)

    var defaultPathColor: AppColor
        get() {
            val id = cache.getLong(context.getString(R.string.pref_backtrack_path_color))
            return AppColor.values().firstOrNull { it.id == id } ?: AppColor.Gray
        }
        set(value) {
            cache.putLong(context.getString(R.string.pref_backtrack_path_color), value.id)
        }

    private val backtrackPathLineStyle: LineStyle
        get() {
            return when (cache.getString(context.getString(R.string.pref_backtrack_path_style))) {
                "solid" -> LineStyle.Solid
                "arrow" -> LineStyle.Arrow
                "dashed" -> LineStyle.Dashed
                "square" -> LineStyle.Square
                "diamond" -> LineStyle.Diamond
                "cross" -> LineStyle.Cross
                else -> LineStyle.Dotted
            }
        }
    private val defaultPathPointStyle: PathPointColoringStyle
        get() = PathPointColoringStyle.None

    override val defaultPathStyle: PathStyle
        get() = PathStyle(
            backtrackPathLineStyle,
            defaultPathPointStyle,
            defaultPathColor.color,
            true
        )

    override var backtrackHistory: Duration
        get() {
            val days = cache.getInt(context.getString(R.string.pref_backtrack_history_days)) ?: 2
            return Duration.ofDays(days.toLong())
        }
        set(value) {
            val d = value.toDays().toInt()
            cache.putInt(
                context.getString(R.string.pref_backtrack_history_days),
                if (d > 0) d else 1
            )
        }
    override val simplifyPathOnImport by BooleanPreference(
        cache,
        context.getString(R.string.pref_auto_simplify_paths),
        true
    )

    override val onlyNavigateToPoints by BooleanPreference(
        cache,
        context.getString(R.string.pref_only_navigate_path_points),
        true
    )

    var maxBeaconDistance: Float
        get() {
            val raw =
                cache.getString(context.getString(R.string.pref_max_beacon_distance)) ?: "0.5"
            return Distance.kilometers(raw.toFloatCompat() ?: 0.5f).meters().distance
        }
        set(value) = cache.putString(
            context.getString(R.string.pref_max_beacon_distance),
            Distance.meters(value).convertTo(DistanceUnits.Kilometers).distance.toString()
        )

    var rulerScale: Float
        get() {
            val raw = cache.getString(context.getString(R.string.pref_ruler_calibration)) ?: "1"
            return raw.toFloatCompat() ?: 1f
        }
        set(value) {
            cache.putString(context.getString(R.string.pref_ruler_calibration), value.toString())
        }

    val coordinateFormat: CoordinateFormat
        get() {
            return when (cache.getString(context.getString(R.string.pref_coordinate_format))) {
                "dms" -> CoordinateFormat.DegreesMinutesSeconds
                "ddm" -> CoordinateFormat.DegreesDecimalMinutes
                "utm" -> CoordinateFormat.UTM
                "mgrs" -> CoordinateFormat.MGRS
                "usng" -> CoordinateFormat.USNG
                "osng" -> CoordinateFormat.OSGB
                else -> CoordinateFormat.DecimalDegrees
            }
        }

    override var beaconSort: BeaconSortMethod by IntEnumPreference(
        cache,
        context.getString(R.string.pref_beacon_sort),
        BeaconSortMethod.values().associateBy { it.id.toInt() },
        BeaconSortMethod.Closest
    )

    var pathSort: PathSortMethod by IntEnumPreference(
        cache,
        context.getString(R.string.pref_path_sort),
        PathSortMethod.values().associateBy { it.id.toInt() },
        PathSortMethod.MostRecent
    )

    val leftButton: QuickActionType
        get() {
            val id = cache.getString(context.getString(R.string.pref_navigation_quick_action_left))
                ?.toIntCompat()
            return QuickActionType.values().firstOrNull { it.id == id } ?: QuickActionType.Backtrack
        }

    val rightButton: QuickActionType
        get() {
            val id = cache.getString(context.getString(R.string.pref_navigation_quick_action_right))
                ?.toIntCompat()
            return QuickActionType.values().firstOrNull { it.id == id }
                ?: QuickActionType.Flashlight
        }

    var speedometerMode by StringEnumPreference(
        cache,
        context.getString(R.string.pref_navigation_speedometer_type),
        mapOf(
            "average" to SpeedometerMode.Backtrack,
            "instant_pedometer" to SpeedometerMode.CurrentPace,
            "average_pedometer" to SpeedometerMode.AveragePace,
            "instant" to SpeedometerMode.GPS
        ),
        SpeedometerMode.GPS
    )

    override val areMapsEnabled by BooleanPreference(
        cache,
        context.getString(R.string.pref_experimental_maps),
        false
    )

    override val autoReduceMaps by BooleanPreference(
        cache,
        context.getString(R.string.pref_low_resolution_maps),
        true
    )

    override val showMapPreviews by BooleanPreference(
        cache,
        context.getString(R.string.pref_show_map_previews),
        true
    )

    override var mapSort: MapSortMethod by IntEnumPreference(
        cache,
        context.getString(R.string.pref_map_sort),
        MapSortMethod.values().associateBy { it.id.toInt() },
        MapSortMethod.MostRecent
    )

    enum class SpeedometerMode {
        Backtrack,
        GPS,
        CurrentPace,
        AveragePace
    }

}