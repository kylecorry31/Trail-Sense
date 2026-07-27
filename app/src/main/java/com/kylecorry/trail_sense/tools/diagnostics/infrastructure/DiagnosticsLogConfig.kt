package com.kylecorry.trail_sense.tools.diagnostics.infrastructure

import java.time.Duration

object DiagnosticsLogConfig {
    const val STACK_TRACE_COUNT = 5
    const val EXIT_REASON_COUNT = 5
    val LOGCAT_HISTORY_DURATION: Duration = Duration.ofHours(1)
}
