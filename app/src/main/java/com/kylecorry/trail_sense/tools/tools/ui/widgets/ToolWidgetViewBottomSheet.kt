package com.kylecorry.trail_sense.tools.tools.ui.widgets

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.setPadding
import androidx.lifecycle.Lifecycle
import com.google.android.flexbox.FlexboxLayout
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.tryOrLog
import com.kylecorry.andromeda.fragments.BoundBottomSheetDialogFragment
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.luna.coroutines.onDefault
import com.kylecorry.luna.coroutines.onMain
import com.kylecorry.trail_sense.databinding.FragmentToolWidgetSheetBinding
import com.kylecorry.trail_sense.settings.SettingsToolRegistration
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolSummarySize
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolWidget
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

class ToolWidgetViewBottomSheet :
    BoundBottomSheetDialogFragment<FragmentToolWidgetSheetBinding>() {

    private val widgets = mutableListOf<WidgetInstance>()
    private val prefs by lazy { UserPreferences(requireContext()) }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentToolWidgetSheetBinding {
        return FragmentToolWidgetSheetBinding.inflate(layoutInflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val selectedWidgets = prefs.toolWidgets
        val allWidgets = Tools.getTools(requireContext())
            .flatMap { it.widgets }
            .filter { it.isEnabled(requireContext()) && it.canPlaceInApp }
            .sortedByDescending { it.size.ordinal }

        // Only show the selected widgets
        val widgets = allWidgets.filter { selectedWidgets.contains(it.id) }

        binding.widgets.removeAllViews()

        // 2 cells on a Pixel phone = 102dp
        val halfSummaryHeight = Resources.dp(requireContext(), 102f).toInt()
        val fullSummaryHeight = halfSummaryHeight * 2
        val summaryGap = Resources.dp(requireContext(), 4f).toInt()

        val host = AppWidgetHost(requireContext(), 1)

        widgets.forEachIndexed { index, widget ->
            val layout = host.createView(
                requireContext(),
                host.allocateAppWidgetId(),
                AppWidgetManager.getInstance(context).installedProviders?.find {
                    it.provider == ComponentName(
                        requireContext(),
                        widget.widgetClass
                    )
                })

            layout.layoutParams = FlexboxLayout.LayoutParams(
                FlexboxLayout.LayoutParams.MATCH_PARENT,
                if (widget.size == ToolSummarySize.Full) fullSummaryHeight else halfSummaryHeight
            ).apply {
                flexBasisPercent = when (widget.size) {
                    ToolSummarySize.Full -> 1f
                    ToolSummarySize.Half -> 0.5f
                }
            }
            layout.setPadding(summaryGap)

            binding.widgets.addView(layout)

            val updateFunction = {
                inBackground {
                    val views = onDefault {
                        widget.widgetView.getPopulatedView(requireContext())
                    }
                    onMain {
                        tryOrLog {
                            layout.updateAppWidget(views)
                        }
                    }
                }
            }

            this.widgets.add(WidgetInstance(widget, updateFunction))
            val widgetView =
                widget.widgetView.getView(requireContext())
            layout.updateAppWidget(widgetView)
        }

        this.widgets.forEach {
            it.widget.widgetView.onInAppEvent(
                requireContext(),
                Lifecycle.Event.ON_CREATE,
                it.updateFunction
            )
        }
    }

    override fun onResume() {
        super.onResume()
        Tools.subscribe(SettingsToolRegistration.BROADCAST_UPDATE_IN_APP_WIDGET, this::onUpdate)

        // TODO: Get a GPS / elevation reading

        // Update all widgets
        widgets.forEach { it.updateFunction() }
        this.widgets.forEach {
            it.widget.widgetView.onInAppEvent(
                requireContext(),
                Lifecycle.Event.ON_RESUME,
                it.updateFunction
            )
        }
    }

    override fun onPause() {
        super.onPause()
        Tools.unsubscribe(SettingsToolRegistration.BROADCAST_UPDATE_IN_APP_WIDGET, this::onUpdate)
        this.widgets.forEach {
            it.widget.widgetView.onInAppEvent(
                requireContext(),
                Lifecycle.Event.ON_PAUSE,
                it.updateFunction
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        this.widgets.forEach {
            it.widget.widgetView.onInAppEvent(
                requireContext(),
                Lifecycle.Event.ON_DESTROY,
                it.updateFunction
            )
        }
        AppWidgetHost.deleteAllHosts()
        this.widgets.clear()
    }

    private fun onUpdate(data: Bundle): Boolean {
        val widgetId = data.getString("widgetId") ?: return true
        val widget = widgets.find { it.widget.id == widgetId } ?: return true
        widget.updateFunction()
        return true
    }

    private data class WidgetInstance(
        val widget: ToolWidget,
        val updateFunction: () -> Unit = {}
    )
}