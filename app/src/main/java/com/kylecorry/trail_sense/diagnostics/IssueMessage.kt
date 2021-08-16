package com.kylecorry.trail_sense.diagnostics

data class IssueMessage(
    val message: String? = null,
    val actionTitle: String? = null,
    val action: () -> Unit
)
