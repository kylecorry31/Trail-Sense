package com.kylecorry.trail_sense.tools.diagnostics.infrastructure

import android.content.Context
import com.kylecorry.andromeda.notify.Notify
import com.kylecorry.trail_sense.tools.diagnostics.domain.DiagnosticCode
import com.kylecorry.trail_sense.tools.diagnostics.domain.IDiagnostic

class NotificationDiagnostic(
    private val context: Context,
    private val channelId: String,
    private val code: DiagnosticCode
) : IDiagnostic {

    override fun scan(): List<DiagnosticCode> {
        return if (Notify.isChannelBlocked(context, channelId)) {
            listOf(code)
        } else {
            emptyList()
        }
    }

}