package com.kylecorry.trail_sense.tools.tides.widgets

import android.content.Context
import android.widget.RemoteViews
import com.kylecorry.luna.coroutines.onMain
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.extensions.setImageViewResourceAsIcon
import com.kylecorry.trail_sense.shared.navigation.NavigationUtils
import com.kylecorry.trail_sense.tools.tides.domain.TideService
import com.kylecorry.trail_sense.tools.tides.domain.loading.TideLoaderFactory
import com.kylecorry.trail_sense.tools.tides.ui.TideFormatter
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.ui.widgets.SimpleToolWidgetView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TidesToolWidgetView : SimpleToolWidgetView() {

    override fun onUpdate(context: Context, views: RemoteViews, commit: () -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            populateTideDetails(context, views)
            onMain {
                commit()
            }
        }
    }

    private suspend fun populateTideDetails(context: Context, views: RemoteViews) {
        val loader = TideLoaderFactory().getTideLoader(context, false)
        val service = TideService()
        val table = loader.getTideTable()
        val formatter = TideFormatter(context)
        val tide = table?.let { service.getCurrentTide(it) }
        val isRising = table?.let { service.isRising(it) } ?: false

        views.setImageViewResourceAsIcon(context, ICON_IMAGEVIEW, formatter.getTideTypeImage(tide))

        views.setTextViewText(
            TITLE_TEXTVIEW,
            if (table == null) context.getString(R.string.no_tides) else table.name
        )
        views.setTextViewCompoundDrawables(
            SUBTITLE_TEXTVIEW,
            if (isRising) R.drawable.ic_arrow_up_widget else R.drawable.ic_arrow_down_widget,
            0,
            0,
            0
        )
        views.setTextViewText(
            SUBTITLE_TEXTVIEW,
            if (table == null) null else formatter.getTideTypeName(tide) + "  "
        )
        views.setOnClickPendingIntent(
            ROOT,
            NavigationUtils.toolPendingIntent(context, Tools.TIDES)
        )
    }

}