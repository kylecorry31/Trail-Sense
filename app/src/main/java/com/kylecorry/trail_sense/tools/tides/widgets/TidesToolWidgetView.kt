package com.kylecorry.trail_sense.tools.tides.widgets

import android.content.Context
import android.widget.RemoteViews
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.extensions.setImageViewResourceAsIcon
import com.kylecorry.trail_sense.shared.navigation.NavigationUtils
import com.kylecorry.trail_sense.tools.tides.subsystem.TidesSubsystem
import com.kylecorry.trail_sense.tools.tides.ui.TideFormatter
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.ui.widgets.SimpleToolWidgetView

class TidesToolWidgetView : SimpleToolWidgetView() {

    override suspend fun getPopulatedView(context: Context): RemoteViews {
        val views = getView(context)
        val formatter = TideFormatter(context)
        val tide = TidesSubsystem.getInstance(context).getNearestTide()

        views.setImageViewResourceAsIcon(
            context,
            ICON_IMAGEVIEW,
            formatter.getTideTypeImage(tide?.now?.type)
        )

        views.setTextViewText(
            TITLE_TEXTVIEW,
            if (tide == null) context.getString(R.string.no_tides) else tide.table.name
        )
        if (tide != null) {
            views.setTextViewCompoundDrawables(
                SUBTITLE_TEXTVIEW,
                if (tide.now.rising) R.drawable.ic_arrow_up_widget else R.drawable.ic_arrow_down_widget,
                0,
                0,
                0
            )
        }
        views.setTextViewText(
            SUBTITLE_TEXTVIEW,
            if (tide == null) null else formatter.getTideTypeName(tide.now.type) + "  "
        )
        views.setOnClickPendingIntent(
            ROOT,
            NavigationUtils.toolPendingIntent(context, Tools.TIDES)
        )
        return views
    }

}