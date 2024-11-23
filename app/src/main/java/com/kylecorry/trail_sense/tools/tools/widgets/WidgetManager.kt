package com.kylecorry.trail_sense.tools.tools.widgets

import android.content.Context
import com.kylecorry.andromeda.core.system.Package
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

class WidgetManager {
    fun registerWidgets(context: Context) {
        val tools = Tools.getTools(context, availableOnly = false)
        tools.filter { it.widgets.any() }.forEach {
            val isAvailable = it.isAvailable(context)
            it.widgets.forEach { widget ->
                Package.setComponentEnabled(
                    context,
                    widget.widgetClass.name,
                    isAvailable && widget.canPlaceOnHomeScreen && widget.isEnabled(context)
                )
            }
        }
    }
}