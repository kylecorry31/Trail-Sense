package com.kylecorry.trail_sense.tools.tools.widgets

import android.content.Context
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.andromeda.preferences.SharedPreferences
import com.kylecorry.luna.text.toLongCompat
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.data.Identifiable
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.withId
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolWidget

enum class WidgetTheme(override val id: Long, val themeId: Int?, val layoutId: Int?) :
    Identifiable {
    System(1, R.style.WidgetTheme, R.layout.widget_theme_system),
    TransparentBlack(
        2,
        R.style.WidgetTheme_TransparentBlack,
        R.layout.widget_theme_transparent_black
    ),
    TransparentWhite(
        3,
        R.style.WidgetTheme_TransparentWhite,
        R.layout.widget_theme_transparent_white
    ),
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

    private val appPrefs = AppServiceRegistry.get<PreferencesSubsystem>().preferences

    fun setTheme(theme: WidgetTheme) {
        prefs.putLong(getThemeKey(appWidgetId), theme.id)
        // Refresh the widget

    }

    fun getTheme(): WidgetTheme {
        val colorId =
            prefs.getLong(getThemeKey(appWidgetId))
                ?: appPrefs.getString(context.getString(R.string.pref_default_widget_theme))
                    ?.toLongCompat()
                ?: WidgetTheme.System.id
        return WidgetTheme.entries.withId(colorId) ?: WidgetTheme.System
    }

    fun clear() {
        prefs.remove(getThemeKey(appWidgetId))
    }

    private fun getThemeKey(appWidgetId: Int) = "pref_background_color_$appWidgetId"
}
