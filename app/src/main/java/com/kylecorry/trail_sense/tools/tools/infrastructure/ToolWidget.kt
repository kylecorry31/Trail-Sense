package com.kylecorry.trail_sense.tools.tools.infrastructure

import android.appwidget.AppWidgetProvider
import android.content.Context
import com.kylecorry.trail_sense.tools.tools.ui.widgets.ToolWidgetView

data class ToolWidget(
    val id: String,
    val name: String,
    val size: ToolSummarySize = ToolSummarySize.Full,
    val widgetView: ToolWidgetView,
    val widgetClass: Class<out AppWidgetProvider>,
    val inAppUpdateFrequencyMs: Long = 60000,
    val updateBroadcasts: List<String> = emptyList(),
    val isEnabled: (context: Context) -> Boolean = { true },
    val usesLocation: Boolean = false,
    val canPlaceOnHomeScreen: Boolean = true,
    val canPlaceInApp: Boolean = true
)

enum class ToolSummarySize {
    Half,
    Full
}
