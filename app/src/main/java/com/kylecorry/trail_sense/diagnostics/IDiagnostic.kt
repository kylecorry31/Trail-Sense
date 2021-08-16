package com.kylecorry.trail_sense.diagnostics

interface IDiagnostic {
    fun getIssues(): List<DiagnosticIssue>
}