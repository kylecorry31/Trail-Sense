package com.kylecorry.trail_sense.tools.magnifier.ui

import android.widget.ImageButton
import android.widget.ImageView
import androidx.core.view.isVisible
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.extensions.TrailSenseReactiveFragment
import com.kylecorry.trail_sense.shared.extensions.usePauseEffect
import com.kylecorry.trail_sense.shared.extensions.useResumeEffect
import com.kylecorry.trail_sense.shared.permissions.alertNoCameraPermission
import com.kylecorry.trail_sense.shared.permissions.requestCamera
import com.kylecorry.trail_sense.shared.views.CameraView

class ToolMagnifierFragment : TrailSenseReactiveFragment(R.layout.fragment_tool_magnifier) {

    override fun update() {
        val cameraView = useView<CameraView>(R.id.camera)
        val frozenFrameView = useView<ImageView>(R.id.frozen_frame)
        val freezeBtn = useView<ImageButton>(R.id.freeze_btn)
        val focusToggleBtn = useView<ImageButton>(R.id.focus_toggle_btn)

        val (isCameraEnabled, setIsCameraEnabled) = useState(false)
        val (isFrozen, setIsFrozen) = useState(false)
        val (isCloseUpFocus, setIsCloseUpFocus) = useState(false)

        useResumeEffect {
            requestCamera {
                setIsCameraEnabled(it)
                if (!it) {
                    alertNoCameraPermission()
                }
            }
        }

        // Start / stop camera
        useEffect(isCameraEnabled, resetOnResume) {
            if (isCameraEnabled) {
                cameraView.defaultZoomRatio = 2f
                cameraView.minZoomRatio = 1f
                cameraView.setShowTorch(true)
                cameraView.start(
                    readFrames = false,
                    preferBackCamera = true,
                    shouldStabilizePreview = false
                )
            } else {
                cameraView.stop()
            }
        }

        usePauseEffect(cameraView) {
            cameraView.stop()
        }

        // Focus mode
        useEffect(isCloseUpFocus, isCameraEnabled) {
            if (isCloseUpFocus) {
                cameraView.setFocus(0f)
            } else {
                cameraView.setFocus(null)
            }
            focusToggleBtn.setImageResource(
                if (isCloseUpFocus) R.drawable.ic_magnifier else R.drawable.ic_focus_auto
            )
        }

        // Freeze frame
        useEffect(isFrozen) {
            if (isFrozen) {
                val bitmap = cameraView.previewImage
                if (bitmap != null) {
                    frozenFrameView.setImageBitmap(bitmap)
                    frozenFrameView.isVisible = true
                } else {
                    setIsFrozen(false)
                }
            } else {
                frozenFrameView.isVisible = false
                frozenFrameView.setImageBitmap(null)
            }
            freezeBtn.setImageResource(
                if (isFrozen) R.drawable.ic_baseline_play_arrow_24 else R.drawable.ic_pause
            )
        }

        // Button click listeners
        useEffect(freezeBtn, isFrozen) {
            freezeBtn.setOnClickListener {
                setIsFrozen(!isFrozen)
            }
        }

        useEffect(focusToggleBtn, isCloseUpFocus) {
            focusToggleBtn.setOnClickListener {
                setIsCloseUpFocus(!isCloseUpFocus)
            }
        }
    }
}
