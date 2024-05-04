package com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics

import com.kylecorry.trail_sense.tools.diagnostics.domain.Severity

data class ToolDiagnosticResult(
    val id: String,
    val severity: Severity,
    val name: String,
    val description: String,
    val resolution: String? = null,
    val action: ToolDiagnosticAction? = null
)