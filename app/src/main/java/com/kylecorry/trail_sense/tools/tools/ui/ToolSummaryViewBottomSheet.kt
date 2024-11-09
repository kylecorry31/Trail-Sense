package com.kylecorry.trail_sense.tools.tools.ui

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.setPadding
import com.google.android.flexbox.FlexboxLayout
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.fragments.BoundBottomSheetDialogFragment
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentToolSummarySheetBinding
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolSummarySize
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

class ToolSummaryViewBottomSheet :
    BoundBottomSheetDialogFragment<FragmentToolSummarySheetBinding>() {
    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentToolSummarySheetBinding {
        return FragmentToolSummarySheetBinding.inflate(layoutInflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // TODO: Filter to what the user selected
        val summaries = Tools.getTools(requireContext())
            .flatMap { it.summaries }
            .sortedByDescending { it.size.ordinal }

        binding.summaries.removeAllViews()

        val summaryHeight = Resources.dp(requireContext(), 150f).toInt()
        val summaryGap = Resources.dp(requireContext(), 4f).toInt()
        val summaryElevation = Resources.dp(requireContext(), 4f)
        val summaryBackgroundColor = Resources.androidBackgroundColorSecondary(requireContext())
        val summaryPadding = Resources.dp(requireContext(), 8f).toInt()

        // For each summary, create a linear layout with the title and the summary view
        summaries.forEach { summary ->
            // The root of the summary
            val root = FrameLayout(requireContext())
            root.layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            root.setPadding(summaryPadding)
            root.setBackgroundResource(R.drawable.rounded_rectangle)
            root.backgroundTintList = ColorStateList.valueOf(summaryBackgroundColor)
            root.elevation = summaryElevation

            // The wrapper which allows for a gap between the summaries
            val layout = FrameLayout(requireContext())
            layout.addView(root)
            layout.layoutParams = FlexboxLayout.LayoutParams(
                FlexboxLayout.LayoutParams.MATCH_PARENT,
                summaryHeight
            ).apply {
                flexBasisPercent = when (summary.size) {
                    ToolSummarySize.Full -> 1f
                    ToolSummarySize.Half -> 0.5f
                }
            }
            layout.setPadding(summaryGap)

            binding.summaries.addView(layout)

            val summaryView = summary.create(root, this)
            summaryView.bind(viewLifecycleOwner)
        }

    }
}