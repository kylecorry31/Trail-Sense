package com.kylecorry.trail_sense.tools.tools.quickactions

import android.widget.ImageButton
import androidx.core.view.setMargins
import androidx.navigation.fragment.findNavController
import com.google.android.flexbox.FlexboxLayout
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.fragments.AndromedaFragment
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.ActivityMainBinding
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.quickactions.IQuickActionBinder
import com.kylecorry.trail_sense.shared.quickactions.QuickActionFactory
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

class MainActivityQuickActionBinder(
    private val fragment: AndromedaFragment,
    private val binding: ActivityMainBinding
) : IQuickActionBinder {

    private val prefs by lazy { UserPreferences(fragment.requireContext()) }

    private fun createButton(recommended: Boolean): ImageButton {
        val size = Resources.dp(fragment.requireContext(), 40f).toInt()
        val margins = Resources.dp(fragment.requireContext(), 8f).toInt()
        val button = ImageButton(fragment.requireContext())
        button.layoutParams = FlexboxLayout.LayoutParams(size, size).apply {
            setMargins(margins)
        }
        button.background =
            Resources.drawable(fragment.requireContext(), R.drawable.rounded_rectangle)
        button.elevation = 2f

        if (recommended) {
            binding.recommendedQuickActions.addView(button)
        } else {
            binding.quickActions.addView(button)
        }

        CustomUiUtils.setButtonState(button, false)

        return button
    }

    override fun bind() {
        binding.quickActions.removeAllViews()
        binding.recommendedQuickActions.removeAllViews()

        val selected = prefs.toolQuickActions.sorted()

        val navController = fragment.findNavController()
        val activeTool = Tools.getTools(fragment.requireContext())
            .firstOrNull { it.isOpen(navController.currentDestination?.id ?: 0) }

        val recommended = (activeTool?.quickActions?.map { it.id }
            ?: emptyList()) + listOf(Tools.QUICK_ACTION_USER_GUIDE, Tools.QUICK_ACTION_SETTINGS)

        val factory = QuickActionFactory()

        recommended.distinct().forEach {
            val action = factory.create(it, createButton(true), fragment)
            action.bind(fragment.viewLifecycleOwner)
        }

        selected.forEach {
            val action = factory.create(it, createButton(false), fragment)
            action.bind(fragment.viewLifecycleOwner)
        }
    }
}