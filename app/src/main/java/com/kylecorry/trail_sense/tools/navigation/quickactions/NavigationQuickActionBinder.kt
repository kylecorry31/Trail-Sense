package com.kylecorry.trail_sense.tools.navigation.quickactions

import androidx.fragment.app.Fragment
import com.kylecorry.trail_sense.shared.quickactions.IQuickActionBinder
import com.kylecorry.trail_sense.shared.quickactions.MaterialButtonQuickActionView
import com.kylecorry.trail_sense.shared.quickactions.QuickActionFactory
import com.kylecorry.trail_sense.shared.views.Toolbar
import com.kylecorry.trail_sense.tools.navigation.infrastructure.NavigationPreferences

class NavigationQuickActionBinder(
    private val fragment: Fragment,
    private val toolbar: Toolbar,
    private val prefs: NavigationPreferences
) : IQuickActionBinder {

    override fun bind() {
        val factory = QuickActionFactory()
        val left = factory.create(
            prefs.leftButton,
            MaterialButtonQuickActionView(toolbar.leftButton),
            fragment
        )
        val right = factory.create(
            prefs.rightButton,
            MaterialButtonQuickActionView(toolbar.rightButton),
            fragment
        )
        left.bind(fragment)
        right.bind(fragment)
    }

}
