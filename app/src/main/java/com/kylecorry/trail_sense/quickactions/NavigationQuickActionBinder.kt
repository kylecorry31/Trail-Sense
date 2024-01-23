package com.kylecorry.trail_sense.quickactions

import com.kylecorry.trail_sense.databinding.ActivityNavigatorBinding
import com.kylecorry.trail_sense.navigation.infrastructure.NavigationPreferences
import com.kylecorry.trail_sense.navigation.ui.NavigatorFragment

class NavigationQuickActionBinder(
    private val fragment: NavigatorFragment,
    private val binding: ActivityNavigatorBinding,
    private val prefs: NavigationPreferences
) : IQuickActionBinder {

    override fun bind() {
        val factory = QuickActionFactory()
        val left = factory.create(prefs.leftButton, binding.navigationTitle.leftButton, fragment)
        val right = factory.create(prefs.rightButton, binding.navigationTitle.rightButton, fragment)
        left.bind(fragment)
        right.bind(fragment)
    }

}