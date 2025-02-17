package com.kylecorry.trail_sense.tools.diagnostics.ui

import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.andromeda.fragments.AndromedaFragment
import com.kylecorry.andromeda.markdown.MarkdownService
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics.ToolDiagnosticSeverity
import com.kylecorry.trail_sense.tools.tools.ui.items.DiagnosticItem

class DiagnosticAlerter(private val fragment: AndromedaFragment) {

    private val context = fragment.requireContext()

    fun alert(item: DiagnosticItem) {
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
            AppServiceRegistry.get<MarkdownService>().toMarkdown(message),
            cancelText = if (item.result.action != null) context.getString(android.R.string.cancel) else null,
            okText = item.result.action?.name ?: context.getString(android.R.string.ok)
        ) { cancelled ->
            if (!cancelled) {
                item.result.action?.action?.invoke(fragment)
            }
        }
    }


    private fun getSeverityName(status: ToolDiagnosticSeverity): String {
        return when (status) {
            ToolDiagnosticSeverity.Error -> context.getString(R.string.error)
            ToolDiagnosticSeverity.Warning -> context.getString(R.string.warning)
        }
    }

}