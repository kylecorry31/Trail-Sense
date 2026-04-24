package com.kylecorry.trail_sense.tools.flashlight.quickactions

import android.hardware.camera2.CameraManager
import android.widget.ImageButton
import androidx.core.content.getSystemService
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

    private var isCameraInUse = false

    private val cameraManager by lazy { context.getSystemService<CameraManager>() }
    private val cameraCallback = object : CameraManager.AvailabilityCallback() {
        override fun onCameraAvailable(cameraId: String) {
            super.onCameraAvailable(cameraId)
            isCameraInUse = false
        }

        override fun onCameraUnavailable(cameraId: String) {
            super.onCameraUnavailable(cameraId)
            isCameraInUse = true
        }
    }

    override fun onCreate() {
        super.onCreate()
        setIcon(R.drawable.flashlight)
    }

    override fun onResume() {
        super.onResume()
        cameraManager?.registerAvailabilityCallback(cameraCallback, null)
    }

    override fun onPause() {
        super.onPause()
        cameraManager?.unregisterAvailabilityCallback(cameraCallback)
    }

    override fun onClick() {
        super.onClick()
        if (!isCameraInUse) {
            flashlight.toggle()
        }
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
