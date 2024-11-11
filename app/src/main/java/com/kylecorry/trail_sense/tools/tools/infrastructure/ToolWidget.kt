package com.kylecorry.trail_sense.tools.tools.infrastructure

import android.appwidget.AppWidgetProvider
import com.kylecorry.trail_sense.tools.tools.ui.widgets.ToolWidgetView

data class ToolWidget(
    val id: String,
    val name: String,
    val size: ToolSummarySize = ToolSummarySize.Full,
    val widgetResourceId: Int,
    val widgetView: ToolWidgetView,
    val widgetClass: Class<out AppWidgetProvider>,
    val inAppUpdateFrequencyMs: Long = 60000
)

enum class ToolSummarySize {
    Half,
    Full
}
