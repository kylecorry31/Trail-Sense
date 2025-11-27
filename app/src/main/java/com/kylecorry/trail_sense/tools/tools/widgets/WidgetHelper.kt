package com.kylecorry.trail_sense.tools.tools.widgets

import android.content.Context
import android.widget.RemoteViews
import androidx.annotation.LayoutRes
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.trail_sense.R

object WidgetHelper {

    fun createThemedRemoteViews(
        context: Context,
        theme: WidgetTheme?,
        @LayoutRes layoutId: Int
    ): RemoteViews {
        if (theme?.themeId != null) {
            Resources.reloadTheme(context, theme.themeId)
        }

        val frame =
            RemoteViews(context.packageName, theme?.layoutId ?: R.layout.widget_theme_in_app)
        val child = RemoteViews(context.packageName, layoutId)
        frame.removeAllViews(R.id.widget_frame)
        frame.addView(R.id.widget_frame, child)
        return frame
    }

}