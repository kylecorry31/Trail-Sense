package com.kylecorry.trail_sense.shared.views

import android.widget.ImageButton
import androidx.core.view.isInvisible
import androidx.fragment.app.Fragment
import com.kylecorry.trail_sense.shared.QuickActionButton

class QuickActionNone(button: ImageButton, fragment: Fragment) : QuickActionButton(
    button,
    fragment
) {
    override fun onCreate() {
        super.onCreate()
        button.isInvisible = true
    }
}