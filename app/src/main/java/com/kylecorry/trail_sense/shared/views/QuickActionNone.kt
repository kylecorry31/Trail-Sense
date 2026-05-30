package com.kylecorry.trail_sense.shared.views

import androidx.fragment.app.Fragment
import com.kylecorry.trail_sense.shared.QuickActionButton
import com.kylecorry.trail_sense.shared.quickactions.QuickActionButtonView

class QuickActionNone(button: QuickActionButtonView, fragment: Fragment) : QuickActionButton(
    button,
    fragment
) {
    override fun onCreate() {
        super.onCreate()
        button.isVisible = false
    }
}
