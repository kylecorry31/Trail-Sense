package com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics

data class ToolDiagnostic(
    val id: String,
    val name: String,
    val scanner: ToolDiagnosticScanner
)

