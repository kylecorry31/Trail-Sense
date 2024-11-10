package com.kylecorry.trail_sense.tools.tools.ui.widgets

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.RemoteViews
import androidx.core.view.setPadding
import com.google.android.flexbox.FlexboxLayout
import com.kylecorry.andromeda.core.system.Package
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.fragments.BoundBottomSheetDialogFragment
import com.kylecorry.luna.timer.CoroutineTimer
import com.kylecorry.trail_sense.databinding.FragmentToolWidgetSheetBinding
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolSummarySize
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

class ToolWidgetViewBottomSheet :
    BoundBottomSheetDialogFragment<FragmentToolWidgetSheetBinding>() {

    private val updateTimers = mutableListOf<Pair<CoroutineTimer, Long>>()
    private val widgets = mutableListOf<ToolWidgetView>()

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

        val summaryHeight = Resources.dp(requireContext(), 150f).toInt()
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
            val timer = CoroutineTimer {
                widget.widget.onUpdate(requireContext(), views) {
                    layout.removeAllViews()
                    val widgetView = views.apply(requireContext(), layout)
                    widgetView.backgroundTintList = ColorStateList.valueOf(
                        Resources.androidBackgroundColorSecondary(requireContext())
                    )
                    layout.addView(widgetView)
                }
            }
            updateTimers.add(timer to widget.updateFrequencyMs)
            this.widgets.add(widget.widget)
            val widgetView = views.apply(requireContext(), layout)
            widgetView.backgroundTintList = ColorStateList.valueOf(
                Resources.androidBackgroundColorSecondary(requireContext())
            )
            layout.addView(widgetView)
        }

        this.widgets.forEach { it.onEnabled(requireContext()) }
    }

    override fun onResume() {
        super.onResume()
        updateTimers.forEach { (timer, frequency) ->
            timer.interval(frequency)
        }
    }

    override fun onPause() {
        super.onPause()
        updateTimers.forEach { (timer, _) ->
            timer.stop()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        widgets.forEach { it.onDisabled(requireContext()) }
    }
}