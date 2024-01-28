package com.kylecorry.trail_sense.tools.tools.quickactions

import android.widget.ImageButton
import androidx.core.view.isVisible
import androidx.core.view.setMargins
import com.google.android.flexbox.FlexboxLayout
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.fragments.AndromedaFragment
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentToolsBinding
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.quickactions.IQuickActionBinder
import com.kylecorry.trail_sense.shared.quickactions.QuickActionFactory

class ToolsQuickActionBinder(
    private val fragment: AndromedaFragment,
    private val binding: FragmentToolsBinding
) : IQuickActionBinder {

    private val prefs by lazy { UserPreferences(fragment.requireContext()) }

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

        CustomUiUtils.setButtonState(button, false)

        return button
    }

    override fun bind() {
        binding.quickActions.removeAllViews()

        val selected = prefs.toolQuickActions.sorted()

        binding.quickActions.isVisible = selected.isNotEmpty()

        val factory = QuickActionFactory()

        selected.forEach {
            val action = factory.create(it, createButton(), fragment)
            action.bind(fragment.viewLifecycleOwner)
        }
    }
}