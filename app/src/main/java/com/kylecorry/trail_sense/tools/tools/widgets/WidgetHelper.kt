package com.kylecorry.trail_sense.tools.tools.widgets

import android.content.Context
import android.widget.RemoteViews
import androidx.annotation.DrawableRes
import androidx.annotation.LayoutRes
import androidx.core.graphics.drawable.toBitmap
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.views.remote.setImageViewResourceAsIcon
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.UserPreferences

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

    /**
     * Sets the icon of an image view in a widget.
     *
     * On some devices, [android.graphics.drawable.Icon.createWithResource] does not reliably
     * render vector drawables in RemoteViews, leaving the widget icon blank. When the experimental
     * widget icon compatibility setting is enabled, the drawable is rendered to a bitmap in the app
     * process instead, which avoids the issue by handing the launcher a fully rasterized image.
     */
    fun setIcon(
        context: Context,
        views: RemoteViews,
        viewId: Int,
        @DrawableRes resourceId: Int,
        sizeDp: Float = 32f
    ) {
        if (getAppService<UserPreferences>().useWidgetIconCompatibilityMode) {
            val size = Resources.dp(context, sizeDp).toInt()
            val bitmap = Resources.drawable(context, resourceId)?.toBitmap(size, size)
            views.setImageViewBitmap(viewId, bitmap)
        } else {
            views.setImageViewResourceAsIcon(context, viewId, resourceId)
        }
    }

}
