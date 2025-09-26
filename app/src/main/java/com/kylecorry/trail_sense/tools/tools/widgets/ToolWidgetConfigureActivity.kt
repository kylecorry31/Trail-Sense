package com.kylecorry.trail_sense.tools.tools.widgets

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import androidx.activity.enableEdgeToEdge
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.andromeda.fragments.AndromedaActivity
import com.kylecorry.andromeda.fragments.ColorTheme
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

class ToolWidgetConfigureActivity : AndromedaActivity() {

    private var appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        val userPrefs = AppServiceRegistry.get<UserPreferences>()
        setColorTheme(ColorTheme.System, userPrefs.useDynamicColors)
        enableEdgeToEdge()

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_tool_widget_configure)

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        val resultValue = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        setResult(RESULT_CANCELED, resultValue)

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        val widget = Tools.getWidgetFromAppWidgetId(this, appWidgetId)
        if (widget == null) {
            finish()
            return
        }

        val prefs = WidgetPreferences(this, widget, appWidgetId)
        val checkbox = findViewById<CheckBox>(R.id.checkbox_transparent_background)
        val save = findViewById<Button>(R.id.button_save)

        checkbox.isChecked = prefs.getBackgroundColor() == WidgetBackgroundColor.Transparent

        save.setOnClickListener {
            prefs.setBackgroundColor(if (checkbox.isChecked) WidgetBackgroundColor.Transparent else WidgetBackgroundColor.System)
            inBackground {
                val widgetProvider =
                    widget.widgetClass.getConstructor().newInstance() as? AppWidgetBase
                widgetProvider?.let {
                    AppWidgetBase.forceUpdate(
                        this@ToolWidgetConfigureActivity,
                        it,
                        appWidgetId
                    )
                }

                // Report the result
                val resultValue =
                    Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                setResult(RESULT_OK, resultValue)
                finish()
            }
        }
    }
}
