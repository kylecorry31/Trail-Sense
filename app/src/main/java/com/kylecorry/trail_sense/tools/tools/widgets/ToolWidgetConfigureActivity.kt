package com.kylecorry.trail_sense.tools.tools.widgets

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.andromeda.fragments.AndromedaActivity
import com.kylecorry.andromeda.fragments.ColorTheme
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.views.MaterialSpinnerView
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

class ToolWidgetConfigureActivity : AndromedaActivity() {

    private var appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        val userPrefs = AppServiceRegistry.get<UserPreferences>()
        setColorTheme(ColorTheme.System, userPrefs.useDynamicColors)
        // TODO: Apply insets
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
        val spinner = findViewById<MaterialSpinnerView>(R.id.theme)
        val save = findViewById<Button>(R.id.button_save)

        val items = listOf(
            WidgetTheme.System to getString(R.string.theme_system),
            WidgetTheme.TransparentBlack to getString(
                R.string.theme_transparent_type,
                getString(R.string.widget_theme_black_text)
            ),
            WidgetTheme.TransparentWhite to getString(
                R.string.theme_transparent_type,
                getString(R.string.widget_theme_white_text)
            )
        )

        spinner.setItems(items.map { it.second })
        spinner.setSelection(items.indexOfFirst { it.first == prefs.getTheme() })

        save.setOnClickListener {
            val theme = items.getOrElse(spinner.selectedItemPosition) { items[0] }.first
            prefs.setTheme(theme)
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
