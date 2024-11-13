package com.kylecorry.trail_sense.tools.tools.ui.widgets

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.RemoteViews
import androidx.core.view.setPadding
import androidx.lifecycle.Lifecycle
import com.google.android.flexbox.FlexboxLayout
import com.kylecorry.andromeda.core.system.Package
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.tryOrLog
import com.kylecorry.andromeda.fragments.BoundBottomSheetDialogFragment
import com.kylecorry.luna.timer.CoroutineTimer
import com.kylecorry.trail_sense.databinding.FragmentToolWidgetSheetBinding
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolSummarySize
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolWidget
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

class ToolWidgetViewBottomSheet :
    BoundBottomSheetDialogFragment<FragmentToolWidgetSheetBinding>() {

    private val widgets = mutableListOf<WidgetInstance>()
    private val broadcastSubscriptions = mutableMapOf<String, (Bundle) -> Boolean>()

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentToolWidgetSheetBinding {
        return FragmentToolWidgetSheetBinding.inflate(layoutInflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // TODO: Filter to what the user selected
        val widgets = Tools.getTools(requireContext())
            .flatMap { it.widgets }
            .sortedByDescending { it.size.ordinal }

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

            val views =
                RemoteViews(Package.getPackageName(requireContext()), widget.widgetResourceId)
            val updateFunction = {
                widget.widgetView.onUpdate(requireContext(), views) {
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

            val timer = CoroutineTimer {
                updateFunction()
            }
            this.widgets.add(WidgetInstance(widget, timer, updateFunction))
            val widgetView = views.apply(requireContext(), layout)
            widgetView.backgroundTintList = ColorStateList.valueOf(
                Resources.androidBackgroundColorSecondary(requireContext())
            )
            layout.addView(widgetView)

            // Subscribe to broadcasts
            widget.updateBroadcasts.forEach { broadcastId ->
                val subscription: (Bundle) -> Boolean = { _ ->
                    updateFunction()
                    true
                }
                Tools.subscribe(broadcastId, subscription)
                broadcastSubscriptions[broadcastId] = subscription
            }
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
        // Unsubscribe from broadcasts
        broadcastSubscriptions.forEach { (broadcastId, subscription) ->
            Tools.unsubscribe(broadcastId, subscription)
        }
        broadcastSubscriptions.clear()
        this.widgets.forEach {
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
        val updateFunction: () -> Unit = {}
    )
}