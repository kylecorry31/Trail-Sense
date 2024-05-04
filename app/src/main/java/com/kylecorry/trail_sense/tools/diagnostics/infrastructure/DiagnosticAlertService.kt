package com.kylecorry.trail_sense.tools.diagnostics.infrastructure

import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.fragments.AndromedaFragment
import com.kylecorry.andromeda.markdown.MarkdownService
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.diagnostics.domain.Severity
import com.kylecorry.trail_sense.tools.diagnostics.ui.IDiagnosticAlertService
import com.kylecorry.trail_sense.tools.tools.ui.items.DiagnosticItem

class DiagnosticAlertService(private val fragment: AndromedaFragment) : IDiagnosticAlertService {

    private val context = fragment.requireContext()

    override fun alert(item: DiagnosticItem) {
        val affectedTools = item.tools.sortedBy { it.name }.joinToString("\n") { "- ${it.name}" }

        val message = context.getString(
            R.string.diagnostic_message_template,
            getSeverityName(item.result.severity),
            item.result.description,
            affectedTools,
            item.result.resolution ?: context.getString(R.string.no_resolution)
        )

        Alerts.dialog(
            context,
            item.result.name,
            MarkdownService(context).toMarkdown(message),
            cancelText = if (item.result.action != null) context.getString(android.R.string.cancel) else null,
            okText = item.result.action?.name ?: context.getString(android.R.string.ok)
        ) { cancelled ->
            if (!cancelled) {
                item.result.action?.action?.invoke(fragment)
            }
        }
    }


    private fun getSeverityName(status: Severity): String {
        return when (status) {
            Severity.Error -> context.getString(R.string.error)
            Severity.Warning -> context.getString(R.string.warning)
        }
    }

}