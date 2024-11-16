package com.kylecorry.trail_sense.tools.flashlight.widgets

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.kylecorry.luna.coroutines.onMain
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.extensions.setImageViewResourceAsIcon
import com.kylecorry.trail_sense.tools.flashlight.domain.FlashlightMode
import com.kylecorry.trail_sense.tools.flashlight.infrastructure.FlashlightSubsystem
import com.kylecorry.trail_sense.tools.tools.ui.widgets.SimpleToolWidgetView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FlashlightToolWidgetView : SimpleToolWidgetView() {

    override fun onUpdate(context: Context, views: RemoteViews, commit: () -> Unit) {
        CoroutineScope(Dispatchers.Default).launch {
            val flashlight = FlashlightSubsystem.getInstance(context)
            val isOn = flashlight.getMode() != FlashlightMode.Off

            views.setViewVisibility(TITLE_TEXTVIEW, View.GONE)
            views.setTextViewText(
                SUBTITLE_TEXTVIEW,
                if (isOn) context.getString(R.string.on) else context.getString(R.string.off)
            )
            views.setImageViewResourceAsIcon(
                context,
                ICON_IMAGEVIEW_TEXT_COLOR,
                R.drawable.flashlight
            )
            views.setViewVisibility(ICON_IMAGEVIEW, View.GONE)
            views.setViewVisibility(ICON_IMAGEVIEW_TEXT_COLOR, View.VISIBLE)

            // Create a pending intent to toggle the flashlight
            val intent = Intent(context, FlashlightWidgetReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                49822730,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(ROOT, pendingIntent)

            onMain {
                commit()
            }
        }
    }
}
