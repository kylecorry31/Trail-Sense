package com.kylecorry.trail_sense.tools.tools.infrastructure

import com.kylecorry.trail_sense.tools.tools.ui.widgets.ToolWidgetView

data class ToolWidget(
    val id: String,
    val name: String,
    val size: ToolSummarySize = ToolSummarySize.Full,
    val widgetResourceId: Int,
    val widget: ToolWidgetView,
    val updateFrequencyMs: Long = 60000
)

enum class ToolSummarySize {
    Half,
    Full
}
