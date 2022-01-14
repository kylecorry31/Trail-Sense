package com.kylecorry.trail_sense.diagnostics

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import com.kylecorry.andromeda.battery.Battery
import com.kylecorry.andromeda.battery.BatteryHealth
import com.kylecorry.andromeda.battery.IBattery
import com.kylecorry.andromeda.permissions.Permissions
import com.kylecorry.trail_sense.shared.UserPreferences

class BatteryDiagnostic(
    context: Context,
    lifecycleOwner: LifecycleOwner?
) :
    BaseSensorQualityDiagnostic<IBattery>(context, lifecycleOwner, Battery(context)) {


    override fun scan(): List<DiagnosticCode> {
        val issues = mutableListOf<DiagnosticCode>()
        val prefs = UserPreferences(context)

        if (prefs.isLowPowerModeOn) {
            issues.add(DiagnosticCode.PowerSavingMode)
        }

        if (!Permissions.isIgnoringBatteryOptimizations(context)) {
            issues.add(DiagnosticCode.BatteryUsageRestricted)
        }

        if (canRun && sensor!!.health != BatteryHealth.Good && sensor.health != BatteryHealth.Unknown) {
            issues.add(DiagnosticCode.BatteryHealthPoor)
        }

        return issues
    }
}