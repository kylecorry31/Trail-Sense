package com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics

import android.content.Context
import com.kylecorry.andromeda.battery.Battery
import com.kylecorry.andromeda.battery.BatteryHealth
import com.kylecorry.trail_sense.R
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BatteryDiagnosticScanner : ToolDiagnosticScanner {
    override fun quickScan(context: Context): List<ToolDiagnosticResult> {
        return emptyList()
    }

    override fun fullScan(context: Context): Flow<List<ToolDiagnosticResult>> {
        val battery = Battery(context)
        return battery.flow.map {
            if (battery.health != BatteryHealth.Good && battery.health != BatteryHealth.Unknown) {
                listOf(
                    ToolDiagnosticResult(
                        "battery-health-poor",
                        ToolDiagnosticSeverity.Error,
                        context.getString(R.string.tool_battery_title),
                        context.getString(R.string.quality_poor)
                    )
                )
            } else {
                emptyList()
            }
        }
    }
}
