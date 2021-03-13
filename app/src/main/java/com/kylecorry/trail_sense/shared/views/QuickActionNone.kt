package com.kylecorry.trail_sense.shared.views

import android.view.View
import androidx.fragment.app.Fragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.kylecorry.trail_sense.shared.QuickActionButton

class QuickActionNone(button: FloatingActionButton, fragment: Fragment) : QuickActionButton(
    button,
    fragment
) {
    override fun onCreate() {
        button.visibility = View.INVISIBLE
    }

    override fun onResume() {
    }

    override fun onPause() {
    }

    override fun onDestroy() {
    }
}