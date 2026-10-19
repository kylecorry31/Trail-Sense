package com.kylecorry.trail_sense.tools.paths.infrastructure

import android.content.Context
import com.kylecorry.andromeda.preferences.BooleanPreference
import com.kylecorry.andromeda.preferences.IntEnumPreference
import com.kylecorry.andromeda.preferences.StringEnumPreference
import com.kylecorry.andromeda.sense.location.GPSPowerUsage
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.settings.infrastructure.GPSPreferences
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.sensors.gps.GPSPowerMode
import com.kylecorry.trail_sense.tools.paths.domain.LineStyle
import com.kylecorry.trail_sense.tools.paths.domain.PathPointColoringStyle
import com.kylecorry.trail_sense.tools.paths.domain.PathStyle
import com.kylecorry.trail_sense.tools.paths.domain.hiking.HikingService
import com.kylecorry.trail_sense.tools.paths.infrastructure.persistence.IPathPreferences
import com.kylecorry.trail_sense.tools.paths.ui.PathSortMethod
import java.time.Duration

class PathsPreferences(private val context: Context) : IPathPreferences {

    private val cache by lazy { PreferencesSubsystem.getInstance(context).preferences }
    private val gps by lazy { GPSPreferences(context) }

    var pathSort: PathSortMethod by IntEnumPreference(
        cache,
        context.getString(R.string.pref_path_sort),
        PathSortMethod.entries.associateBy { it.id.toInt() },
        PathSortMethod.MostRecent
    )

    var backtrackEnabled by BooleanPreference(
        cache,
        context.getString(R.string.pref_backtrack_enabled),
        false
    )

    var backtrackSaveCellHistory by BooleanPreference(
        cache,
        context.getString(R.string.pref_backtrack_save_cell),
        true
    )

    override val keepBacktrackPathOnStop by BooleanPreference(
        cache,
        context.getString(R.string.pref_backtrack_keep_on_stop),
        true
    )

    val backtrackKeepDeviceAwake by BooleanPreference(
        cache,
        context.getString(R.string.pref_backtrack_keep_awake),
        false
    )

    private val backtrackGPSPowerMode by StringEnumPreference(
        cache,
        context.getString(R.string.pref_backtrack_gps_power_usage),
        GPSPowerMode.entries.associateBy { it.id.toString() },
        GPSPowerMode.Inherit
    )

    val backtrackGPSPowerUsage: GPSPowerUsage
        get() = backtrackGPSPowerMode.powerUsage ?: gps.powerMode.powerUsage ?: GPSPowerUsage.High

    var backtrackRecordFrequency: Duration
        get() = cache.getDuration(context.getString(R.string.pref_backtrack_frequency))
            ?: Duration.ofMinutes(1)
        set(value) {
            cache.putDuration(context.getString(R.string.pref_backtrack_frequency), value)
        }

    var backtrackMinimumDistance: Distance
        get() {
            val meters = cache.getFloat(context.getString(R.string.pref_backtrack_min_distance))
                ?: DEFAULT_BACKTRACK_MIN_DISTANCE_METERS
            return Distance.meters(meters)
        }
        set(value) {
            cache.putFloat(
                context.getString(R.string.pref_backtrack_min_distance),
                value.meters().value
            )
        }

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

    override val defaultPathStyle: PathStyle
        get() = PathStyle(
            backtrackPathLineStyle,
            PathPointColoringStyle.None,
            defaultPathColor.color,
            true
        )

    override var backtrackHistory: Duration
        get() {
            val days = cache.getInt(context.getString(R.string.pref_backtrack_history_days)) ?: 7
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

    override val useFastPathRendering by BooleanPreference(
        cache,
        context.getString(R.string.pref_fast_path_rendering),
        false
    )

    val elevationSmoothingScale: Int
        get() = context.resources.getInteger(R.integer.elevation_smoothing_scale)

    override val elevationSmoothing: Float
        get() {
            val scale = elevationSmoothingScale
            val scaled = cache.getInt(context.getString(R.string.pref_elevation_smoothing))
                ?: (HikingService.DEFAULT_ELEVATION_SMOOTHING * scale).toInt()
            return scaled / scale.toFloat()
        }

    companion object {
        const val DEFAULT_BACKTRACK_MIN_DISTANCE_METERS = 10f
    }
}
