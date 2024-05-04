package com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics

import android.content.Context
import kotlinx.coroutines.flow.Flow

interface ToolDiagnosticScanner {
    fun quickScan(context: Context): List<ToolDiagnosticResult>
    fun fullScan(context: Context): Flow<List<ToolDiagnosticResult>>
}