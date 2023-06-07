package com.kylecorry.trail_sense.astronomy.ui.items

import android.content.Context
import android.text.Layout
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.core.text.color
import androidx.core.text.scale
import com.google.android.flexbox.AlignItems
import com.google.android.flexbox.JustifyContent
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.ui.Colors.withAlpha
import com.kylecorry.ceres.list.ListIcon
import com.kylecorry.ceres.list.ListItem
import com.kylecorry.ceres.list.ListItemData
import com.kylecorry.ceres.list.ListItemDataAlignment
import com.kylecorry.ceres.list.ResourceListIcon
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.astronomy.domain.AstronomyService
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.align
import com.kylecorry.trail_sense.shared.appendImage
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


    private fun title(title: CharSequence, subtitle: CharSequence): CharSequence {
        return buildSpannedString {
            append(title)
            color(secondaryColor.withAlpha(220)) {
                scale(subtitleScale) {
                    append("  •  ")
                    append(subtitle)
                }
            }
        }
    }

    protected fun time(
        time: ZonedDateTime?,
        displayDate: LocalDate? = time?.toLocalDate()
    ): String {
        if (time == null) {
            return "--:--"
        }
        return formatter.formatTime(time, false) + if (time.toLocalDate() != displayDate) {
            " (${formatter.formatRelativeDate(time.toLocalDate(), true)})"
        } else {
            ""
        }
    }

    protected fun timeRange(
        start: ZonedDateTime?,
        end: ZonedDateTime?,
        displayDate: LocalDate? = start?.toLocalDate()
    ): String {
        if (start == null || end == null) {
            return "--:--"
        }
        return "${time(start, displayDate)} - ${time(end, displayDate)}"
    }

    protected fun riseSet(rise: ZonedDateTime?, set: ZonedDateTime?): CharSequence {

        val setBeforeRise = set != null && rise != null && set.isBefore(rise)

        val firstIcon = if (setBeforeRise) {
            R.drawable.ic_arrow_down
        } else {
            R.drawable.ic_arrow_up
        }

        val firstTime = if (setBeforeRise) {
            set
        } else {
            rise
        }

        val secondIcon = if (setBeforeRise) {
            R.drawable.ic_arrow_up
        } else {
            R.drawable.ic_arrow_down
        }

        val secondTime = if (setBeforeRise) {
            rise
        } else {
            set
        }

        return buildSpannedString {
            appendImage(
                context,
                firstIcon,
                imageSize,
                tint = secondaryColor
            )
            append(" ${time(firstTime)}    ")
            appendImage(
                context,
                secondIcon,
                imageSize,
                tint = secondaryColor
            )
            append(" ${time(secondTime)}")
        }
    }

    protected fun riseSetData(rise: ZonedDateTime?, set: ZonedDateTime?): List<ListItemData> {
        val setBeforeRise = set != null && rise != null && set.isBefore(rise)

        val first = if (setBeforeRise) {
            datapoint(
                time(set),
                context.getString(R.string.astronomy_set)
            )
        } else {
            datapoint(
                time(rise),
                context.getString(R.string.astronomy_rise)
            )
        }

        val second = if (setBeforeRise) {
            datapoint(
                time(rise),
                context.getString(R.string.astronomy_rise),
                alignment = Layout.Alignment.ALIGN_OPPOSITE
            )
        } else {
            datapoint(
                time(set),
                context.getString(R.string.astronomy_set),
                alignment = Layout.Alignment.ALIGN_OPPOSITE
            )
        }

        return listOf(first, arrow(), second)
    }

    protected fun timeRangeData(
        start: ZonedDateTime?,
        end: ZonedDateTime?,
        displayDate: LocalDate? = start?.toLocalDate()
    ): List<ListItemData> {
        val startLabel = if (start != null && end != null && start.toLocalDate() != displayDate) {
            formatter.formatRelativeDate(start.toLocalDate(), true)
        } else {
            null
        }

        val endLabel = if (start != null && end != null && end.toLocalDate() != displayDate) {
            formatter.formatRelativeDate(end.toLocalDate(), true)
        } else {
            null
        }

        return listOf(
            datapoint(time(start), startLabel),
            arrow(),
            datapoint(time(end), endLabel, alignment = Layout.Alignment.ALIGN_OPPOSITE)
        )
    }

    protected fun timeData(
        time: ZonedDateTime?,
        displayDate: LocalDate? = time?.toLocalDate()
    ): ListItemData {
        val label = if (time != null && time.toLocalDate() != displayDate) {
            formatter.formatRelativeDate(time.toLocalDate(), true)
        } else {
            null
        }
        return datapoint(time(time), label)
    }

    protected fun datapoint(
        value: CharSequence,
        label: CharSequence? = null,
        icon: Int? = null,
        alignment: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL
    ): ListItemData {
        return ListItemData(
            buildSpannedString {
                align(alignment) {
                    bold { scale(textScale) { append(value) } }
                    if (label != null) {
                        append("\n")
                        append(label)
                    }
                }
            },
            icon?.let { ResourceListIcon(it, secondaryColor) },
            basisPercentage = 0f,
            shrink = 1f,
            grow = 1f
        )
    }

    protected fun percent(label: String, percent: Float): CharSequence {
        return "$label (${formatter.formatPercentage(percent)})"
    }

    protected fun listItem(
        id: Long,
        title: CharSequence,
        subtitle: CharSequence,
        icon: ListIcon,
        data: List<ListItemData> = listOf(),
        onClick: () -> Unit
    ): ListItem {
        return ListItem(
            id,
            title(title, subtitle),
            null,
            icon = icon,
            trailingIcon = ResourceListIcon(R.drawable.ic_keyboard_arrow_right),
            data = data,
            dataAlignment = ListItemDataAlignment(
                justifyContent = JustifyContent.SPACE_BETWEEN,
                alignItems = AlignItems.CENTER
            )
        ) {
            onClick()
        }
    }

    protected fun arrow(): ListItemData {
        val text = buildSpannedString {
            appendImage(
                context,
                R.drawable.ic_arrow_right,
                imageSize,
                tint = secondaryColor,
                flags = 2
            )
        }

        return datapoint(text, alignment = Layout.Alignment.ALIGN_CENTER)
    }

    protected fun fields(
        vararg fields: Pair<CharSequence, CharSequence?>
    ): CharSequence {
        return buildSpannedString {
            fields.forEachIndexed { index, (title, value) ->
                bold {
                    append(title)
                    append("\n")
                }
                append(value ?: "-")
                if (index != fields.lastIndex) {
                    append("\n\n")
                }
            }
        }
    }
}