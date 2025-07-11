package com.kylecorry.trail_sense.tools.navigation.infrastructure

import android.content.Context
import com.kylecorry.andromeda.core.toFloatCompat
import com.kylecorry.andromeda.core.toIntCompat
import com.kylecorry.andromeda.preferences.BooleanPreference
import com.kylecorry.andromeda.preferences.IntEnumPreference
import com.kylecorry.andromeda.preferences.IntPreference
import com.kylecorry.andromeda.preferences.StringEnumPreference
import com.kylecorry.sol.science.geography.CoordinateFormat
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.settings.infrastructure.IBeaconPreferences
import com.kylecorry.trail_sense.settings.infrastructure.ICompassStylePreferences
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.ContourMapLayerPreferences
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.PhotoMapMapLayerPreferences
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.tools.beacons.infrastructure.sort.BeaconSortMethod
import com.kylecorry.trail_sense.tools.paths.domain.LineStyle
import com.kylecorry.trail_sense.tools.paths.domain.PathPointColoringStyle
import com.kylecorry.trail_sense.tools.paths.domain.PathStyle
import com.kylecorry.trail_sense.tools.paths.infrastructure.persistence.IPathPreferences
import com.kylecorry.trail_sense.tools.paths.ui.PathSortMethod
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import java.time.Duration

class NavigationPreferences(private val context: Context) : ICompassStylePreferences,
    IPathPreferences, IBeaconPreferences {

    private val cache by lazy { PreferencesSubsystem.getInstance(context).preferences }
    private val sensors by lazy { SensorService(context) }

    private val _showCalibrationOnNavigateDialog by BooleanPreference(
        cache,
        context.getString(R.string.pref_show_calibrate_on_navigate_dialog),
        true
    )

    val showCalibrationOnNavigateDialog: Boolean
        get() = sensors.hasCompass() && _showCalibrationOnNavigateDialog

    val keepScreenUnlockedWhileNavigating: Boolean
        get() = cache.getBoolean(context.getString(R.string.pref_navigation_lock_screen_presence))
            ?: false

    val keepScreenUnlockedWhileOpen by BooleanPreference(
        cache,
        context.getString(R.string.pref_navigation_keep_unlocked),
        false
    )

    override val showLastSignalBeacon: Boolean
        get() = cache.getBoolean(context.getString(R.string.pref_show_last_signal_beacon)) ?: true

    override val useLinearCompass: Boolean
        get() = cache.getBoolean(context.getString(R.string.pref_show_linear_compass)) ?: true

    val showMultipleBeacons: Boolean
        get() = !sensors.hasCompass() || cache.getBoolean(context.getString(R.string.pref_display_multi_beacons)) ?: true

    val numberOfVisibleBeacons: Int
        get() {
            val raw = cache.getString(context.getString(R.string.pref_num_visible_beacons)) ?: "10"
            return raw.toIntOrNull() ?: 10
        }

    private val _useRadarCompassPref by BooleanPreference(
        cache,
        context.getString(R.string.pref_nearby_radar),
        true
    )

    override val useRadarCompass: Boolean
        get() = !sensors.hasCompass() || (showMultipleBeacons && _useRadarCompassPref)

    override val showDialTicksWhenNoCompass by BooleanPreference(
        cache,
        context.getString(R.string.pref_show_dial_ticks_when_no_compass),
        false
    )

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
    override val useFastPathRendering by BooleanPreference(
        cache,
        context.getString(R.string.pref_fast_path_rendering),
        false
    )

    var maxBeaconDistance: Float
        get() {
            val raw =
                cache.getString(context.getString(R.string.pref_max_beacon_distance)) ?: "0.5"
            return Distance.kilometers(raw.toFloatCompat() ?: 0.5f)
                .meters()
                .distance
                .coerceIn(1f, 25000000f)
        }
        set(value) {
            val meters = Distance.meters(value.coerceIn(1f, 25000000f))
            cache.putString(
                context.getString(R.string.pref_max_beacon_distance),
                meters.convertTo(DistanceUnits.Kilometers).distance.toString()
            )
        }

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

    val leftButton: Int
        get() {
            val id = cache.getString(context.getString(R.string.pref_navigation_quick_action_left))
                ?.toIntCompat()
            return id ?: (Tools.PATHS.toInt() + Tools.TOOL_QUICK_ACTION_OFFSET)
        }

    val rightButton: Int
        get() {
            val id = cache.getString(context.getString(R.string.pref_navigation_quick_action_right))
                ?.toIntCompat()
            return id ?: (Tools.PHOTO_MAPS.toInt() + Tools.TOOL_QUICK_ACTION_OFFSET)
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

    // Layers
    val photoMapLayer = PhotoMapMapLayerPreferences(context, "navigation")
    val contourLayer = ContourMapLayerPreferences(context, "navigation")

    enum class SpeedometerMode {
        Backtrack,
        GPS,
        CurrentPace,
        AveragePace
    }

}