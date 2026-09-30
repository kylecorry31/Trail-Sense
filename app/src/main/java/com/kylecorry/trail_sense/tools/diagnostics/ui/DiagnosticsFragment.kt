package com.kylecorry.trail_sense.tools.diagnostics.ui

import android.view.View
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.kylecorry.andromeda.core.coroutines.BackgroundMinimumState
import com.kylecorry.andromeda.core.ui.Colors
import com.kylecorry.andromeda.fragments.useFlow
import com.kylecorry.andromeda.list.ListView
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.ListItemPlainIconBinding
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.extensions.TrailSenseReactiveFragment
import com.kylecorry.trail_sense.shared.extensions.useNavController
import com.kylecorry.trail_sense.shared.views.Toolbar
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics.ToolDiagnosticResult
import com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics.ToolDiagnosticSeverity
import com.kylecorry.trail_sense.tools.tools.ui.items.DiagnosticItem
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.scan

class DiagnosticsFragment : TrailSenseReactiveFragment(R.layout.fragment_diagnostics) {

    override fun update() {
        // Views
        val titleView = useView<Toolbar>(R.id.diagnostics_title)
        val recyclerView = useView<RecyclerView>(R.id.diagnostics_list)
        val downloadLogsView = useView<View>(R.id.download_logs)
        val emptyTextView = useView<TextView>(R.id.empty_text)

        // Services
        val context = useAndroidContext()
        val navController = useNavController()

        // Diagnostics
        val tools = useMemo(context) { Tools.getTools(context) }

        // TODO: Keep the mapping of tools to diagnostics and the mapping of ID to diagnostic
        val diagnosticIdToTool = useMemo(tools) {
            tools
                .flatMap { tool -> tool.diagnostics.map { it.id to tool } }
                .groupBy({ it.first }, { it.second })
        }

        // Every scanner runs while resumed, emitting the latest results seen for each diagnostic
        val resultsFlow = useMemo(tools, context) {
            val scans = tools
                .flatMap { it.diagnostics }
                .distinctBy { it.id }
                .map { diagnostic ->
                    flow {
                        emitAll(diagnostic.scanner.fullScan(context))
                    }.map { diagnostic.id to it }
                }

            merge(*scans.toTypedArray())
                .scan(emptyMap<String, List<ToolDiagnosticResult>>()) { results, (id, result) ->
                    results + (id to result)
                }
        }

        val results = useFlow(
            resultsFlow,
            state = BackgroundMinimumState.Resumed
        ) ?: emptyMap()

        val items = useMemo(results, diagnosticIdToTool) {
            results.flatMap { (id, results) ->
                results.map { DiagnosticItem(it, diagnosticIdToTool[id] ?: listOf()) }
            }.toSet().sortedBy { it.result.severity.ordinal }
        }

        val listView = useMemo(recyclerView) {
            ListView<DiagnosticItem>(recyclerView, R.layout.list_item_plain_icon) { itemView, item ->
                val itemBinding = ListItemPlainIconBinding.bind(itemView)
                itemBinding.title.text = item.result.name
                itemBinding.description.text = item.result.description
                itemBinding.icon.setImageResource(R.drawable.ic_alert)
                Colors.setImageColor(itemBinding.icon, getStatusTint(item.result.severity))
                itemBinding.root.setOnClickListener {
                    DiagnosticAlerter(this@DiagnosticsFragment).alert(item)
                }
            }.also { it.addLineSeparator() }
        }

        // Effects
        useEffect(titleView, navController) {
            titleView.rightButton.setOnClickListener {
                navController.navigate(R.id.action_diagnostics_to_sensor_details)
            }
        }

        useEffect(downloadLogsView) {
            downloadLogsView.setOnClickListener {
                DownloadDiagnosticsLogsCommand(this).execute()
            }
        }

        useEffect(listView, emptyTextView, items) {
            emptyTextView.isVisible = items.isEmpty()
            listView.setData(items)
        }
    }

    @ColorInt
    private fun getStatusTint(status: ToolDiagnosticSeverity): Int {
        return when (status) {
            ToolDiagnosticSeverity.Error -> AppColor.Red.color
            ToolDiagnosticSeverity.Warning -> AppColor.Yellow.color
        }
    }
}
