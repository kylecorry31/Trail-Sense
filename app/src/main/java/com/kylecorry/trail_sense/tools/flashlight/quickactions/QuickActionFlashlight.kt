package com.kylecorry.trail_sense.tools.flashlight.quickactions

import android.widget.ImageButton
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.core.topics.generic.ITopic
import com.kylecorry.andromeda.core.topics.generic.map
import com.kylecorry.andromeda.core.topics.generic.replay
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FeatureState
import com.kylecorry.trail_sense.shared.openTool
import com.kylecorry.trail_sense.shared.quickactions.TopicQuickAction
import com.kylecorry.trail_sense.tools.flashlight.domain.FlashlightMode
import com.kylecorry.trail_sense.tools.flashlight.infrastructure.FlashlightSubsystem
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

class QuickActionFlashlight(btn: ImageButton, fragment: Fragment) :
    TopicQuickAction(btn, fragment, hideWhenUnavailable = true) {

    private val flashlight by lazy { FlashlightSubsystem.getInstance(context) }

    override fun onCreate() {
        super.onCreate()
        setIcon(R.drawable.flashlight)
    }

    override fun onClick() {
        super.onClick()
        flashlight.toggle()
    }

    override fun onLongClick(): Boolean {
        super.onLongClick()
        fragment.findNavController().openTool(Tools.FLASHLIGHT)
        return true
    }

    override val state: ITopic<FeatureState> = flashlight.mode.map {
        if (flashlight.isAvailable()) {
            when (it) {
                FlashlightMode.Torch -> FeatureState.On
                else -> FeatureState.Off
            }
        } else {
            FeatureState.Unavailable
        }
    }.replay()

}