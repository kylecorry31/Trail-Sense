package com.kylecorry.trail_sense.tools.astronomy.widgets

import android.content.Context
import android.widget.RemoteViews
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.extensions.setImageViewResourceAsIcon
import com.kylecorry.trail_sense.shared.navigation.NavigationUtils
import com.kylecorry.trail_sense.tools.astronomy.domain.AstronomySubsystem
import com.kylecorry.trail_sense.tools.astronomy.domain.AstronomyTransition
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.ui.widgets.SimpleToolWidgetView

class SunToolWidgetView : SimpleToolWidgetView() {
    override suspend fun getPopulatedView(context: Context): RemoteViews {
        val views = getView(context)
        val formatService = FormatService.getInstance(context)
        val astronomy = AstronomySubsystem.getInstance(context)
        val sun = astronomy.sun

        if (sun.nextRise != null && sun.nextTransition == AstronomyTransition.Rise) {
            views.setTextViewText(
                SUBTITLE_TEXTVIEW,
                formatService.formatTime(sun.nextRise.toLocalTime(), includeSeconds = false)
            )
            views.setTextViewText(TITLE_TEXTVIEW, context.getString(R.string.sunrise))
            views.setImageViewResourceAsIcon(
                context,
                ICON_IMAGEVIEW,
                R.drawable.ic_sunrise_notification
            )
        } else if (sun.nextSet != null && sun.nextTransition == AstronomyTransition.Set) {
            views.setTextViewText(
                SUBTITLE_TEXTVIEW,
                formatService.formatTime(sun.nextSet.toLocalTime(), includeSeconds = false)
            )
            views.setTextViewText(TITLE_TEXTVIEW, context.getString(R.string.sunset))
            views.setImageViewResourceAsIcon(
                context,
                ICON_IMAGEVIEW,
                R.drawable.ic_sunset_notification
            )
        } else if (sun.isUp) {
            views.setTextViewText(TITLE_TEXTVIEW, context.getString(R.string.sun_up_no_set))
            views.setTextViewText(SUBTITLE_TEXTVIEW, context.getString(R.string.sun_does_not_set))
            views.setImageViewResourceAsIcon(context, ICON_IMAGEVIEW, R.drawable.ic_sun)
        } else {
            views.setTextViewText(TITLE_TEXTVIEW, context.getString(R.string.sun_down_no_set))
            views.setTextViewText(SUBTITLE_TEXTVIEW, context.getString(R.string.sun_does_not_rise))
            views.setImageViewResourceAsIcon(context, ICON_IMAGEVIEW, R.drawable.ic_sun)
        }
        views.setOnClickPendingIntent(
            ROOT,
            NavigationUtils.toolPendingIntent(context, Tools.ASTRONOMY)
        )
        return views
    }
}