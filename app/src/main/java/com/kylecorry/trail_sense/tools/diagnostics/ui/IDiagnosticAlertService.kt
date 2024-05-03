package com.kylecorry.trail_sense.tools.diagnostics.ui

import com.kylecorry.trail_sense.tools.tools.ui.items.DiagnosticItem

interface IDiagnosticAlertService {
    fun alert(code: DiagnosticItem)
}