package com.kylecorry.trail_sense.tools.tools.ui.widgets

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.setPadding
import androidx.lifecycle.Lifecycle
import com.google.android.flexbox.FlexboxLayout
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.tryOrLog
import com.kylecorry.andromeda.fragments.BoundBottomSheetDialogFragment
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.luna.coroutines.onDefault
import com.kylecorry.luna.coroutines.onMain
import com.kylecorry.luna.timer.CoroutineTimer
import com.kylecorry.trail_sense.databinding.FragmentToolWidgetSheetBinding
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
            .sortedByDescending { it.size.ordinal }

        // Only show the selected widgets
        val widgets = allWidgets.filter { selectedWidgets.contains(it.id) }

        binding.widgets.removeAllViews()

        // 2 cells on a Pixel phone = 102dp
        val summaryHeight = Resources.dp(requireContext(), 102f).toInt()
        val summaryGap = Resources.dp(requireContext(), 4f).toInt()

        widgets.forEach { widget ->
            // The wrapper which allows for a gap between the widgets
            val layout = FrameLayout(requireContext())
            layout.layoutParams = FlexboxLayout.LayoutParams(
                FlexboxLayout.LayoutParams.MATCH_PARENT,
                summaryHeight
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
                            layout.removeAllViews()
                            val widgetView = views.apply(requireContext(), layout)
                            widgetView.backgroundTintList = ColorStateList.valueOf(
                                Resources.androidBackgroundColorSecondary(requireContext())
                            )
                            layout.addView(widgetView)
                        }
                    }
                }
            }

            val timer = CoroutineTimer {
                updateFunction()
            }

            val subscriptions = mutableMapOf<String, (Bundle) -> Boolean>()

            // Subscribe to broadcasts
            widget.updateBroadcasts.forEach { broadcastId ->
                val subscription: (Bundle) -> Boolean = { _ ->
                    updateFunction()
                    true
                }
                Tools.subscribe(broadcastId, subscription)
                subscriptions[broadcastId] = subscription
            }

            this.widgets.add(WidgetInstance(widget, timer, updateFunction, subscriptions))
            val widgetView =
                widget.widgetView.getView(requireContext()).apply(requireContext(), layout)
            widgetView.backgroundTintList = ColorStateList.valueOf(
                Resources.androidBackgroundColorSecondary(requireContext())
            )
            layout.addView(widgetView)
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
        widgets.forEach {
            it.timer.interval(it.widget.inAppUpdateFrequencyMs)
        }
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
        widgets.forEach {
            it.timer.stop()
        }
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
            it.subscriptions.forEach { (broadcastId, subscription) ->
                Tools.unsubscribe(broadcastId, subscription)
            }
            it.widget.widgetView.onInAppEvent(
                requireContext(),
                Lifecycle.Event.ON_DESTROY,
                it.updateFunction
            )
        }
        this.widgets.clear()
    }

    private data class WidgetInstance(
        val widget: ToolWidget,
        var timer: CoroutineTimer,
        val updateFunction: () -> Unit = {},
        val subscriptions: Map<String, (Bundle) -> Boolean> = emptyMap()
    )
}