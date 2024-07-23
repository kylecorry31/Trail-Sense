package com.kylecorry.trail_sense.tools.diagnostics.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.core.coroutines.BackgroundMinimumState
import com.kylecorry.andromeda.core.ui.Colors
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.fragments.observeFlow
import com.kylecorry.andromeda.list.ListView
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentDiagnosticsBinding
import com.kylecorry.trail_sense.databinding.ListItemPlainIconBinding
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tool
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics.ToolDiagnosticResult
import com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics.ToolDiagnosticSeverity
import com.kylecorry.trail_sense.tools.tools.ui.items.DiagnosticItem

class DiagnosticsFragment : BoundFragment<FragmentDiagnosticsBinding>() {

    private lateinit var diagnosticListView: ListView<DiagnosticItem>

    private var results by state<Map<String, List<ToolDiagnosticResult>>>(emptyMap())
    private val resultsLock = Any()

    private var diagnosticIdToTool = mapOf<String, List<Tool>>()

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentDiagnosticsBinding {
        return FragmentDiagnosticsBinding.inflate(layoutInflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.diagnosticsTitle.rightButton.setOnClickListener {
            findNavController().navigate(R.id.action_diagnostics_to_sensor_details)
        }
        diagnosticListView =
            ListView(binding.diagnosticsList, R.layout.list_item_plain_icon) { itemView, item ->
                val itemBinding = ListItemPlainIconBinding.bind(itemView)
                itemBinding.title.text = item.result.name
                itemBinding.description.text = item.result.description
                itemBinding.icon.setImageResource(R.drawable.ic_alert)
                Colors.setImageColor(itemBinding.icon, getStatusTint(item.result.severity))
                itemBinding.root.setOnClickListener {
                    val alerter = DiagnosticAlerter(this)
                    alerter.alert(item)
                }
            }
        diagnosticListView.addLineSeparator()

        val tools = Tools.getTools(requireContext())
        // TODO: Keep the mapping of tools to diagnostics and the mapping of ID to diagnostic
        val toolMap = mutableMapOf<String, List<Tool>>()

        tools.forEach {
            it.diagnostics.forEach { diagnostic ->
                toolMap[diagnostic.id] = toolMap.getOrDefault(diagnostic.id, listOf()) + it
            }
        }

        diagnosticIdToTool = toolMap

        val toolDiagnostics = tools
            .flatMap { it.diagnostics }
            .distinctBy { it.id }

        // Start all scanners
        toolDiagnostics.forEach { diagnostic ->
            observeFlow(
                diagnostic.scanner.fullScan(requireContext()),
                BackgroundMinimumState.Resumed
            ) {
                synchronized(resultsLock) {
                    results = results + (diagnostic.id to it)
                }
            }
        }
    }

    override fun onUpdate() {
        super.onUpdate()

        effect("results", results) {
            val items = synchronized(resultsLock) {
                results.flatMap { (id, results) ->
                    results.map { DiagnosticItem(it, diagnosticIdToTool[id] ?: listOf()) }
                }.toSet().sortedBy { it.result.severity.ordinal }
            }

            binding.emptyText.isVisible = items.isEmpty()
            diagnosticListView.setData(items)
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