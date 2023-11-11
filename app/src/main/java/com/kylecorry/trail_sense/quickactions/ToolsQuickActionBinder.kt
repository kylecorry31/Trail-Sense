package com.kylecorry.trail_sense.quickactions

import android.widget.ImageButton
import androidx.core.view.setMargins
import com.google.android.flexbox.FlexboxLayout
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.fragments.AndromedaFragment
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentTools2Binding

class ToolsQuickActionBinder(
    private val fragment: AndromedaFragment,
    private val binding: FragmentTools2Binding
) : IQuickActionBinder {

    private fun createButton(): ImageButton {
        val size = Resources.dp(fragment.requireContext(), 40f).toInt()
        val margins = Resources.dp(fragment.requireContext(), 8f).toInt()
        val button = ImageButton(fragment.requireContext())
        button.layoutParams = FlexboxLayout.LayoutParams(size, size).apply {
            setMargins(margins)
        }
        button.background =
            Resources.drawable(fragment.requireContext(), R.drawable.rounded_rectangle)
        button.elevation = 2f

        binding.quickActions.addView(button)

        return button
    }

    override fun bind() {
        binding.quickActions.removeAllViews()
        // TODO: Weather monitor
        // TODO: Backtrack quick action should be a toggle rather than opening the path
//        QuickActionBacktrack(createButton(), fragment).bind(fragment.viewLifecycleOwner)
        QuickActionFlashlight(createButton(), fragment).bind(fragment.viewLifecycleOwner)
        QuickActionWhistle(createButton(), fragment).bind(fragment.viewLifecycleOwner)
        LowPowerQuickAction(createButton(), fragment).bind(fragment.viewLifecycleOwner)
        QuickActionSunsetAlert(createButton(), fragment).bind(fragment.viewLifecycleOwner)
        QuickActionWhiteNoise(createButton(), fragment).bind(fragment.viewLifecycleOwner)
    }
}