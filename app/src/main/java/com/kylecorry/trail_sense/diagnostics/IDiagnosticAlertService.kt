package com.kylecorry.trail_sense.diagnostics

interface IDiagnosticAlertService {
    fun alert(code: DiagnosticCode)
}