package com.kylecorry.trail_sense.astronomy.ui.items

import android.content.Context
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.core.text.scale
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.ceres.list.ListIcon
import com.kylecorry.ceres.list.ListItem
import com.kylecorry.ceres.list.ResourceListIcon
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.astronomy.domain.AstronomyService
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
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
            bold { append(title) }
            append("\n")
            scale(subtitleScale) {
                append(subtitle)
            }
        }
    }

    private fun body(text: CharSequence): CharSequence {
        return buildSpannedString {
            scale(textScale) {
                append(text)
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
        return buildSpannedString {
            appendImage(
                context,
                R.drawable.ic_arrow_up,
                imageSize,
                tint = secondaryColor
            )
            append(" ${time(rise)}    ")
            appendImage(
                context,
                R.drawable.ic_arrow_down,
                imageSize,
                tint = secondaryColor
            )
            append(" ${time(set)}")
        }
    }

    protected fun percent(label: String, percent: Float): CharSequence {
        return "$label (${formatter.formatPercentage(percent)})"
    }

    protected fun listItem(
        id: Long,
        title: CharSequence,
        subtitle: CharSequence,
        body: CharSequence,
        icon: ListIcon,
        onClick: () -> Unit
    ): ListItem {
        return ListItem(
            id,
            title(title, subtitle),
            body(body),
            icon = icon,
            trailingIcon = ResourceListIcon(R.drawable.ic_keyboard_arrow_right)
        ) {
            onClick()
        }
    }

}