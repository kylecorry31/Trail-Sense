package com.kylecorry.trail_sense.tools.astronomy.widgets

import android.content.Context
import android.widget.RemoteViews
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.navigation.NavigationUtils
import com.kylecorry.trail_sense.tools.astronomy.domain.AstronomySubsystem
import com.kylecorry.trail_sense.tools.astronomy.ui.MoonPhaseImageMapper
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.ui.widgets.SimpleToolWidgetView
import com.kylecorry.trail_sense.tools.tools.widgets.WidgetPreferences

class MoonToolWidgetView : SimpleToolWidgetView() {

    override suspend fun getPopulatedView(
        context: Context,
        prefs: WidgetPreferences?
    ): RemoteViews {
        val views = getView(context, prefs)
        val astronomy = AstronomySubsystem.getInstance(context)
        val formatter = FormatService.getInstance(context)
        val moon = astronomy.moon
        val size = Resources.dp(context, 32f).toInt()
        val bitmap = MoonPhaseImageMapper(context).getPhaseImage(
            moon.phaseAngle,
            size,
            size,
            moon.tilt
        )
        views.setImageViewBitmap(ICON_IMAGEVIEW, bitmap)
        views.setTextViewText(TITLE_TEXTVIEW, context.getString(R.string.moon))
        views.setTextViewText(SUBTITLE_TEXTVIEW, formatter.formatMoonPhase(moon.phase))
        views.setOnClickPendingIntent(
            ROOT,
            NavigationUtils.toolPendingIntent(context, Tools.ASTRONOMY)
        )
        return views
    }

}
