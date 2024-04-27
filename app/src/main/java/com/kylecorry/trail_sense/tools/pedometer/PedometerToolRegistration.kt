package com.kylecorry.trail_sense.tools.pedometer

import android.content.Context
import android.hardware.Sensor
import com.kylecorry.andromeda.sense.Sensors
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.pedometer.quickactions.QuickActionPedometer
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolRegistration
import com.kylecorry.trail_sense.tools.tools.ui.Tool
import com.kylecorry.trail_sense.tools.tools.ui.ToolCategory
import com.kylecorry.trail_sense.tools.tools.ui.ToolQuickAction
import com.kylecorry.trail_sense.tools.tools.ui.Tools

object PedometerToolRegistration : ToolRegistration {
    override fun getTool(context: Context): Tool {
        return Tool(
            Tools.PEDOMETER,
            context.getString(R.string.pedometer),
            R.drawable.steps,
            R.id.fragmentToolPedometer,
            ToolCategory.Distance,
            guideId = R.raw.guide_tool_pedometer,
            settingsNavAction = R.id.calibrateOdometerFragment,
            quickActions = listOf(
                ToolQuickAction(
                    Tools.QUICK_ACTION_PEDOMETER,
                    context.getString(R.string.pedometer),
                    ::QuickActionPedometer
                )
            ),
            isAvailable = { Sensors.hasSensor(it, Sensor.TYPE_STEP_COUNTER) }
        )
    }
}