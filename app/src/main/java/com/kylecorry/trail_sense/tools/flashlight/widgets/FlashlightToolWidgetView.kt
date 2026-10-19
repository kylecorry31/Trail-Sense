package com.kylecorry.trail_sense.tools.flashlight.widgets

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.View
import android.widget.RemoteViews
import com.kylecorry.andromeda.permissions.Permissions
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.MainActivity
import com.kylecorry.trail_sense.tools.flashlight.domain.FlashlightMode
import com.kylecorry.trail_sense.tools.flashlight.infrastructure.FlashlightSubsystem
import com.kylecorry.trail_sense.tools.tools.ui.widgets.SimpleToolWidgetView
import com.kylecorry.trail_sense.tools.tools.widgets.WidgetHelper
import com.kylecorry.trail_sense.tools.tools.widgets.WidgetPreferences

class FlashlightToolWidgetView : SimpleToolWidgetView() {

    override suspend fun getPopulatedView(
        context: Context,
        prefs: WidgetPreferences?
    ): RemoteViews {
        val views = getView(context, prefs)
        val flashlight = FlashlightSubsystem.getInstance(context)
        val isOn = flashlight.getMode() != FlashlightMode.Off

        views.setViewVisibility(TITLE_TEXTVIEW, View.GONE)
        views.setTextViewText(
            SUBTITLE_TEXTVIEW,
            context.getString(if (isOn) R.string.on else R.string.off)
        )
        WidgetHelper.setIcon(
            context,
            views,
            ICON_IMAGEVIEW_TEXT_COLOR,
            R.drawable.flashlight
        )
        views.setViewVisibility(ICON_IMAGEVIEW, View.GONE)
        views.setViewVisibility(ICON_IMAGEVIEW_TEXT_COLOR, View.VISIBLE)

        // Create a pending intent to toggle the flashlight
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = if (isForegroundServiceWorkaroundNeeded(context)) {
            val intent = Intent(context, FlashlightWidgetActivityWorkaround::class.java)
            PendingIntent.getActivity(context, PENDING_INTENT_ID, intent, flags)
        } else {
            val intent = Intent(context, FlashlightWidgetReceiver::class.java)
            PendingIntent.getBroadcast(context, PENDING_INTENT_ID, intent, flags)
        }

        views.setOnClickPendingIntent(ROOT, pendingIntent)
        return views
    }

    private fun isForegroundServiceWorkaroundNeeded(context: Context): Boolean {
        if ((context as? MainActivity) != null) {
            return false
        }

        // The bug only happens on Android 13+
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return false
        }

        // Normally we would check if the app is in the foreground, but widgets don't initialize often

        return !Permissions.isIgnoringBatteryOptimizations(context)
    }

    private companion object {
        const val PENDING_INTENT_ID = 49822730
    }
}
