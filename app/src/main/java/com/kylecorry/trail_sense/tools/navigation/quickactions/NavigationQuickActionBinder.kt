package com.kylecorry.trail_sense.tools.navigation.quickactions

import com.kylecorry.trail_sense.databinding.ActivityNavigatorBinding
import com.kylecorry.trail_sense.shared.quickactions.IQuickActionBinder
import com.kylecorry.trail_sense.shared.quickactions.MaterialButtonQuickActionView
import com.kylecorry.trail_sense.shared.quickactions.QuickActionFactory
import com.kylecorry.trail_sense.tools.navigation.infrastructure.NavigationPreferences
import com.kylecorry.trail_sense.tools.navigation.ui.NavigatorFragment

class NavigationQuickActionBinder(
    private val fragment: NavigatorFragment,
    private val binding: ActivityNavigatorBinding,
    private val prefs: NavigationPreferences
) : IQuickActionBinder {

    override fun bind() {
        val factory = QuickActionFactory()
        val left = factory.create(
            prefs.leftButton,
            MaterialButtonQuickActionView(binding.navigationTitle.leftButton),
            fragment
        )
        val right = factory.create(
            prefs.rightButton,
            MaterialButtonQuickActionView(binding.navigationTitle.rightButton),
            fragment
        )
        left.bind(fragment)
        right.bind(fragment)
    }

}
