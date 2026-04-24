package com.kylecorry.trail_sense.tools.paths.infrastructure

import android.content.Context
import com.kylecorry.andromeda.preferences.BooleanPreference
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.settings.infrastructure.PreferenceRepo
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.tools.paths.domain.LineStyle
import com.kylecorry.trail_sense.tools.paths.domain.PathPointColoringStyle
import com.kylecorry.trail_sense.tools.paths.domain.PathStyle
import com.kylecorry.trail_sense.tools.paths.infrastructure.persistence.IPathPreferences
import java.time.Duration

class PathPreferences(context: Context) : PreferenceRepo(context), IPathPreferences {

    var defaultPathColor: AppColor
        get() {
            val id = cache.getLong(context.getString(R.string.pref_backtrack_path_color))
            return AppColor.entries.firstOrNull { it.id == id } ?: AppColor.Gray
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

}
