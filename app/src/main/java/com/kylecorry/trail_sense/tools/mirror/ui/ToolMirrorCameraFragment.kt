package com.kylecorry.trail_sense.tools.mirror.ui

import android.graphics.Color
import androidx.camera.view.PreviewView
import com.kylecorry.andromeda.permissions.Permissions
import com.kylecorry.andromeda.torch.ScreenTorch
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.extensions.TrailSenseReactiveFragment
import com.kylecorry.trail_sense.shared.extensions.usePauseEffect
import com.kylecorry.trail_sense.shared.extensions.useResumeEffect
import com.kylecorry.trail_sense.shared.permissions.alertNoCameraPermission
import com.kylecorry.trail_sense.shared.permissions.requestCamera
import com.kylecorry.trail_sense.shared.views.CameraView

class ToolMirrorCameraFragment : TrailSenseReactiveFragment(R.layout.fragment_tool_mirror_camera) {

    override fun update() {
        val context = useAndroidContext()
        val cameraView = useView<CameraView>(R.id.camera)
        val screenTorch = useMemo { ScreenTorch(requireActivity().window) }

        val (isCameraEnabled, setIsCameraEnabled) = useState(false)
        val wasPermissionRequested = useRef(false)

        useEffect(cameraView) {
            cameraView.setScaleType(PreviewView.ScaleType.FIT_CENTER)
            cameraView.setShowTorch(false)
            cameraView.setPreviewBackgroundColor(Color.WHITE)
        }

        useResumeEffect(screenTorch) {
            screenTorch.on()
        }

        useResumeEffect(context) {
            setIsCameraEnabled(Permissions.isCameraEnabled(context))

            // Only ask once so returning from the permission dialog doesn't re-prompt
            if (!wasPermissionRequested.current) {
                wasPermissionRequested.current = true
                requestCamera {
                    setIsCameraEnabled(it)
                    if (!it) {
                        alertNoCameraPermission()
                    }
                }
            }
        }

        useEffect(cameraView, isCameraEnabled, resetOnResume) {
            if (isCameraEnabled) {
                cameraView.start(
                    readFrames = false,
                    preferBackCamera = false,
                    shouldStabilizePreview = false
                )
            } else {
                cameraView.stop()
            }
        }

        usePauseEffect(cameraView, screenTorch) {
            cameraView.stop()
            screenTorch.off()
        }
    }
}
