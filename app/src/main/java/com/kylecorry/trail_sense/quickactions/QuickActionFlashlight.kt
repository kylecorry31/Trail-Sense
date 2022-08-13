package com.kylecorry.trail_sense.quickactions

import android.widget.ImageButton
import androidx.fragment.app.Fragment
import com.kylecorry.andromeda.core.topics.generic.ITopic
import com.kylecorry.andromeda.core.topics.generic.map
import com.kylecorry.andromeda.core.topics.generic.replay
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FeatureState
import com.kylecorry.trail_sense.tools.flashlight.domain.FlashlightState
import com.kylecorry.trail_sense.tools.flashlight.infrastructure.FlashlightSubsystem

class QuickActionFlashlight(btn: ImageButton, fragment: Fragment) :
    TopicQuickAction(btn, fragment, hideWhenUnavailable = true) {

    private val flashlight by lazy { FlashlightSubsystem.getInstance(context) }

    override fun onCreate() {
        super.onCreate()
        button.setImageResource(R.drawable.flashlight)
        button.setOnClickListener {
            flashlight.toggle()
        }
    }

    override val state: ITopic<FeatureState> = flashlight.state.map {
        if (flashlight.isAvailable()) {
            when (it) {
                FlashlightState.On -> FeatureState.On
                FlashlightState.SOS, FlashlightState.Strobe, FlashlightState.Off -> FeatureState.Off
            }
        } else {
            FeatureState.Unavailable
        }
    }.replay()

}