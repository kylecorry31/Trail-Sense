package com.kylecorry.trail_sense.shared.quickactions

import android.widget.ImageButton
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.kylecorry.trail_sense.shared.QuickActionButton
import com.kylecorry.trail_sense.shared.navigateWithAnimation

class QuickActionOpenTool(
    button: ImageButton,
    fragment: Fragment,
    @IdRes private val navId: Int,
    @DrawableRes private val icon: Int
) :
    QuickActionButton(button, fragment) {

    override fun onCreate() {
        super.onCreate()
        setIcon(icon)
    }

    override fun onClick() {
        super.onClick()
        fragment.findNavController().navigateWithAnimation(navId)
    }

}