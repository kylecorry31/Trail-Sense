package com.kylecorry.trail_sense.tools.astronomy.ui.items

import android.content.Context
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.core.text.color
import androidx.core.text.scale
import com.google.android.flexbox.AlignItems
import com.google.android.flexbox.JustifyContent
import com.kylecorry.andromeda.core.math.DecimalFormatter
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.ui.Colors.withAlpha
import com.kylecorry.andromeda.views.list.ListIcon
import com.kylecorry.andromeda.views.list.ListItem
import com.kylecorry.andromeda.views.list.ListItemData
import com.kylecorry.andromeda.views.list.ListItemDataAlignment
import com.kylecorry.andromeda.views.list.ResourceListIcon
import com.kylecorry.sol.science.astronomy.RiseSetTransitTimes
import com.kylecorry.sol.units.Bearing
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.tools.astronomy.domain.AstronomyService
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime

abstract class BaseAstroListItemProducer(protected val context: Context) :
    AstronomyListItemProducer {

    private val subtitleScale = 0.7f
    private val textScale = 1.2f
    protected val imageSize = Resources.sp(context, 12f * textScale).toInt()
    protected val secondaryColor = Resources.androidTextColorSecondary(context)
    protected val formatter = FormatService.getInstance(context)
    protected val astronomyService = AstronomyService()
    protected val prefs = UserPreferences(context)


    private fun title(title: CharSequence, subtitle: CharSequence?): CharSequence {
        return buildSpannedString {
            append(title)
            if (subtitle != null) {
                color(secondaryColor.withAlpha(220)) {
                    scale(subtitleScale) {
                        append("  •  ")
                        append(subtitle)
                    }
                }
            }
        }
    }

    private fun formatTime(time: ZonedDateTime?): String {
        if (time == null) {
            return "--:--"
        }
        return formatter.formatTime(time, false)
    }


    private fun datapoint(
        value: CharSequence,
        label: CharSequence? = null
    ): ListItemData {
        return ListItemData(
            buildSpannedString {
                bold {
                    append(value)
                }
                if (label != null) {
                    append("\n")
                    append(label)
                }
            }, null, basisPercentage = 0f, shrink = 1f, grow = 1f
        )
    }

    protected fun percent(label: String, percent: Float): CharSequence {
        return "$label (${formatter.formatPercentage(percent)})"
    }

    protected fun list(
        id: Long,
        title: CharSequence,
        subtitle: CharSequence? = null,
        icon: ListIcon? = null,
        data: List<ListItemData> = listOf(),
        onClick: (() -> Unit)? = null
    ): ListItem {
        return ListItem(
            id,
            title(title, subtitle),
            null,
            icon = icon,
            trailingIcon = onClick?.let { ResourceListIcon(R.drawable.ic_keyboard_arrow_right){ onClick() } },
            data = data,
            dataAlignment = ListItemDataAlignment(
                justifyContent = JustifyContent.SPACE_BETWEEN, alignItems = AlignItems.CENTER
            )
        ) {
            onClick?.invoke()
        }
    }

    protected fun showAdvancedData(
        title: String,
        advancedData: List<Pair<CharSequence, List<ListItemData>?>>
    ) {
        CustomUiUtils.showList(
            context,
            title,
            advancedData.filter { it.second != null }.mapIndexed { index, (title, data) ->
                list(index.toLong(), title, data = data ?: emptyList())
            }
        )
    }

    // COMMON DATA POINT FIELDS
    protected fun data(value: CharSequence, label: CharSequence? = null): List<ListItemData> {
        return listOf(datapoint(value, label))
    }

    protected fun degrees(value: Float): List<ListItemData> {
        return data(formatter.formatDegrees(value))
    }

    protected fun direction(bearing: Bearing): List<ListItemData> {
        return data(formatter.formatDirection(bearing.direction))
    }

    protected fun decimal(value: Float, places: Int): List<ListItemData> {
        return data(DecimalFormatter.format(value, places))
    }

    protected fun percent(value: Float): List<ListItemData> {
        return data(formatter.formatPercentage(value))
    }

    protected fun duration(value: Duration): List<ListItemData> {
        return data(formatter.formatDuration(value, false))
    }

    protected fun riseSetTransit(times: RiseSetTransitTimes): List<ListItemData> {
        return listOf(
            context.getString(R.string.astronomy_rise) to times.rise,
            context.getString(R.string.noon) to times.transit,
            context.getString(R.string.astronomy_set) to times.set
        ).sortedBy { it.second }.map {
            datapoint(formatTime(it.second), it.first)
        }
    }

    protected fun times(
        start: ZonedDateTime?,
        peak: ZonedDateTime?,
        end: ZonedDateTime?,
        displayDate: LocalDate? = start?.toLocalDate()
    ): List<ListItemData> {
        return listOf(
            context.getString(R.string.start_time) to start,
            context.getString(R.string.peak_time) to peak,
            context.getString(R.string.end_time) to end
        ).flatMap {
            time(it.second, displayDate, it.first)
        }
    }

    protected fun time(
        time: ZonedDateTime?,
        displayDate: LocalDate? = time?.toLocalDate(),
        todayLabel: CharSequence? = null
    ): List<ListItemData> {
        val label = if (time != null && time.toLocalDate() != displayDate) {
            formatter.formatRelativeDate(time.toLocalDate(), true)
        } else {
            todayLabel
        }
        return listOf(datapoint(formatTime(time), label))
    }


}