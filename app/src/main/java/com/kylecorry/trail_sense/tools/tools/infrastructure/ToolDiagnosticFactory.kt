package com.kylecorry.trail_sense.tools.tools.infrastructure

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics.BackgroundLocationDiagnosticScanner
import com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics.GPSDiagnosticScanner

object ToolDiagnosticFactory {

    fun gps(context: Context): ToolDiagnostic2 {
        return ToolDiagnostic2(
            "gps",
            context.getString(R.string.gps),
            scanner = GPSDiagnosticScanner()
        )
    }

    fun backgroundLocation(context: Context): ToolDiagnostic2 {
        return ToolDiagnostic2(
            "background_location",
            context.getString(R.string.background_location_permission),
            scanner = BackgroundLocationDiagnosticScanner()
        )
    }

}