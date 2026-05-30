package com.kylecorry.trail_sense.tools.tools.quickactions

import androidx.core.view.setMargins
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.flexbox.FlexboxLayout
import com.google.android.material.button.MaterialButton
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.ViewQuickActionSheetBinding
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.quickactions.IQuickActionBinder
import com.kylecorry.trail_sense.shared.quickactions.MaterialButtonQuickActionView
import com.kylecorry.trail_sense.shared.quickactions.QuickActionButtonView
import com.kylecorry.trail_sense.shared.quickactions.QuickActionFactory
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

class MainActivityQuickActionBinder(
    private val fragment: Fragment,
    private val binding: ViewQuickActionSheetBinding
) : IQuickActionBinder {

    private val prefs by lazy { UserPreferences(fragment.requireContext()) }

    private fun createButton(recommended: Boolean): QuickActionButtonView {
        val margins = Resources.dp(fragment.requireContext(), 4f).toInt()
        val button = MaterialButton(
            fragment.requireContext(),
            null,
            R.attr.quickActionButtonStyle
        )
        button.isCheckable = true
        button.layoutParams =
            FlexboxLayout.LayoutParams(FlexboxLayout.LayoutParams.WRAP_CONTENT, FlexboxLayout.LayoutParams.WRAP_CONTENT)
                .apply {
                    setMargins(margins)
                }

        if (recommended) {
            binding.recommendedQuickActions.addView(button)
        } else {
            binding.quickActions.addView(button)
        }

        return MaterialButtonQuickActionView(button)
    }

    override fun bind() {
        binding.quickActions.removeAllViews()
        binding.recommendedQuickActions.removeAllViews()

        val selected = prefs.toolQuickActions.sorted()

        val navController = fragment.findNavController()
        val tools = Tools.getTools(fragment.requireContext())
        val currentDestination = navController.currentDestination?.id ?: 0
        val activeTools = tools
            .filter { it.isOpen(currentDestination) || it.settingsNavAction == currentDestination }

        val alwaysRecommended = listOf(
            Tools.QUICK_ACTION_USER_GUIDE,
            Tools.QUICK_ACTION_SETTINGS
        )

        val activeToolQuickActions = activeTools
            .flatMap { it.quickActions }
            .map { it.id }
            .filterNot { it in alwaysRecommended }
            .sorted()

        val recommended = activeToolQuickActions + alwaysRecommended

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
