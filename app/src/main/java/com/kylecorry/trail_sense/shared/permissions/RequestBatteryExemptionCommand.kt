package com.kylecorry.trail_sense.shared.permissions

import android.content.Context
import com.kylecorry.andromeda.permissions.Permissions
import com.kylecorry.trail_sense.diagnostics.DiagnosticCode
import com.kylecorry.trail_sense.diagnostics.IDiagnosticAlertService
import com.kylecorry.trail_sense.shared.commands.Command

class RequestBatteryExemptionCommand(
    private val context: Context,
    private val alerter: IDiagnosticAlertService
) : Command {

    override fun execute() {
        if (Permissions.isIgnoringBatteryOptimizations(context)) {
            return
        }

        val isRequired = IsBatteryExemptionRequired().isSatisfiedBy(context)
        if (!isRequired) {
            return
        }

        alerter.alert(DiagnosticCode.BatteryUsageRestricted)
    }
}