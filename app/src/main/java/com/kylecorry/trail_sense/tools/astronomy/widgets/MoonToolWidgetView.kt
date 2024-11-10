package com.kylecorry.trail_sense.tools.astronomy.widgets

import android.content.Context
import android.widget.RemoteViews
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.luna.coroutines.onMain
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.extensions.setImageViewResourceAsIcon
import com.kylecorry.trail_sense.shared.navigation.NavigationUtils
import com.kylecorry.trail_sense.tools.astronomy.domain.AstronomySubsystem
import com.kylecorry.trail_sense.tools.astronomy.ui.MoonPhaseImageMapper
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.ui.widgets.SimpleToolWidgetView
import kotlinx.coroutines.launch

class MoonToolWidgetView : SimpleToolWidgetView() {
    override fun onUpdate(context: Context, views: RemoteViews, commit: () -> Unit) {
        scope.launch {
            populateMoonDetails(context, views)
            onMain {
                commit()
            }
        }
    }

    private fun populateMoonDetails(context: Context, views: RemoteViews) {
        val astronomy = AstronomySubsystem.getInstance(context)
        val formatter = FormatService.getInstance(context)
        val moon = astronomy.moon
        val image = MoonPhaseImageMapper().getPhaseImage(moon.phase)
        views.setImageViewResourceAsIcon(context, ICON_IMAGEVIEW, image)
        views.setFloat(ICON_IMAGEVIEW, "setRotation", moon.tilt)
        views.setTextViewText(TITLE_TEXTVIEW, context.getString(R.string.moon))
        views.setTextViewText(SUBTITLE_TEXTVIEW, formatter.formatMoonPhase(moon.phase))
        views.setOnClickPendingIntent(
            ROOT,
            NavigationUtils.toolPendingIntent(context, Tools.ASTRONOMY)
        )
    }
}