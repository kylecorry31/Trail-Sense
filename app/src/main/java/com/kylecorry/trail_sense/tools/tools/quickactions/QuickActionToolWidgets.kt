package com.kylecorry.trail_sense.tools.tools.quickactions

import android.widget.ImageButton
import androidx.fragment.app.Fragment
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.QuickActionButton
import com.kylecorry.trail_sense.shared.requireMainActivity

class QuickActionToolWidgets(button: ImageButton, fragment: Fragment) : QuickActionButton(
    button,
    fragment
) {

    override fun onCreate() {
        super.onCreate()
        setIcon(R.drawable.ic_summary)
    }

    override fun onClick() {
        super.onClick()
        fragment.requireMainActivity().openWidgets()
    }

}