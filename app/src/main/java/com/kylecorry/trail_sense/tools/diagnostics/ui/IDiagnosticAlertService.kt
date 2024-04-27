package com.kylecorry.trail_sense.tools.diagnostics.ui

import com.kylecorry.trail_sense.tools.diagnostics.domain.DiagnosticCode

interface IDiagnosticAlertService {
    fun alert(code: DiagnosticCode)
}