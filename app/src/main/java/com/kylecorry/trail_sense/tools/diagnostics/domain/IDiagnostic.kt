package com.kylecorry.trail_sense.tools.diagnostics.domain

interface IDiagnostic {
    fun scan(): List<DiagnosticCode>
}