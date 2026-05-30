package com.kylecorry.trail_sense.tools.tools.quickactions

import androidx.core.view.isVisible
import androidx.core.view.setMargins
import com.google.android.flexbox.FlexboxLayout
import com.google.android.material.button.MaterialButton
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.fragments.AndromedaFragment
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentToolsBinding
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.quickactions.IQuickActionBinder
import com.kylecorry.trail_sense.shared.quickactions.MaterialButtonQuickActionView
import com.kylecorry.trail_sense.shared.quickactions.QuickActionButtonView
import com.kylecorry.trail_sense.shared.quickactions.QuickActionFactory

class ToolsQuickActionBinder(
    private val fragment: AndromedaFragment,
    private val binding: FragmentToolsBinding
) : IQuickActionBinder {

    private val prefs by lazy { UserPreferences(fragment.requireContext()) }

    private fun createButton(): QuickActionButtonView {
        val margins = Resources.dp(fragment.requireContext(), 4f).toInt()
        val button = MaterialButton(
            fragment.requireContext(),
            null,
            R.attr.quickActionButtonStyle
        )
        button.isCheckable = true
        button.layoutParams = FlexboxLayout.LayoutParams(
            FlexboxLayout.LayoutParams.WRAP_CONTENT,
            FlexboxLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(margins)
        }

        binding.quickActions.addView(button)

        return MaterialButtonQuickActionView(button)
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
