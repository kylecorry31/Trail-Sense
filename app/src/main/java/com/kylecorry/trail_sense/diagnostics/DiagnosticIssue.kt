package com.kylecorry.trail_sense.diagnostics

data class DiagnosticIssue(
    val title: String,
    val message: String,
    val severity: IssueSeverity,
    val fullMessage: IssueMessage? = null
)