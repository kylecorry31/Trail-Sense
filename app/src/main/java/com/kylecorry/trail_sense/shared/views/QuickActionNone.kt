package com.kylecorry.trail_sense.shared.views

import androidx.core.view.isInvisible
import androidx.fragment.app.Fragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.kylecorry.trail_sense.shared.QuickActionButton

class QuickActionNone(button: FloatingActionButton, fragment: Fragment) : QuickActionButton(
    button,
    fragment
) {
    override fun onCreate() {
        super.onCreate()
        button.isInvisible = true
    }
}