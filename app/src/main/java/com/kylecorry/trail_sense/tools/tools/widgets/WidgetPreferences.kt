package com.kylecorry.trail_sense.tools.tools.widgets

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import androidx.annotation.ColorInt
import com.kylecorry.andromeda.preferences.CachedPreferences
import com.kylecorry.andromeda.preferences.SharedPreferences
import com.kylecorry.trail_sense.shared.data.Identifiable
import com.kylecorry.trail_sense.shared.withId
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolWidget

enum class WidgetBackgroundColor(override val id: Long) : Identifiable {
    System(1),
    Transparent(2)
}

class WidgetPreferences(
    private val context: Context,
    private val widget: ToolWidget,
    private val appWidgetId: Int
) {

    private val prefs = SharedPreferences(
        context,
        "${context.packageName}_widget_preferences",
        commitChanges = true
    )

    fun setBackgroundColor(backgroundColor: WidgetBackgroundColor) {
        prefs.putLong(getBackgroundColorKey(appWidgetId), backgroundColor.id)
        // Refresh the widget

    }

    fun getBackgroundColor(): WidgetBackgroundColor {
        val colorId =
            prefs.getLong(getBackgroundColorKey(appWidgetId)) ?: WidgetBackgroundColor.System.id
        return WidgetBackgroundColor.entries.withId(colorId) ?: WidgetBackgroundColor.System
    }

    fun clear() {
        prefs.remove(getBackgroundColorKey(appWidgetId))
    }

    private fun getBackgroundColorKey(appWidgetId: Int) = "pref_background_color_$appWidgetId"
}
