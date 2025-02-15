package com.kylecorry.trail_sense.main.errors

import android.content.Context
import com.kylecorry.andromeda.exceptions.IBugReportGenerator
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

class DiagnosticsBugReportGenerator : IBugReportGenerator {
    override fun generate(context: Context, throwable: Throwable): String {
        val diagnostics = Tools.getTools(context)
            .flatMap { it.diagnostics }
            .distinctBy { it.id }


        val codes = diagnostics.flatMap { it.scanner.quickScan(context) }.toSet().sortedBy { it.id }

        return "Diagnostics: ${codes.joinToString(", ") { it.id }}"
    }
}