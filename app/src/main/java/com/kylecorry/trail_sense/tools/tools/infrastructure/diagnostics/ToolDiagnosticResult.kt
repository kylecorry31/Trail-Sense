package com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics

data class ToolDiagnosticResult(
    val id: String,
    val severity: ToolDiagnosticSeverity,
    val name: String,
    val description: String,
    val resolution: String? = null,
    val action: ToolDiagnosticAction? = null
)