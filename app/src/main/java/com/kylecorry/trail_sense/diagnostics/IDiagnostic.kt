package com.kylecorry.trail_sense.diagnostics

interface IDiagnostic {
    fun scan(): List<DiagnosticCode>
}