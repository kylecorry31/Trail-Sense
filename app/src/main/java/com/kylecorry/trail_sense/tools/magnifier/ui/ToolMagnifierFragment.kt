package com.kylecorry.trail_sense.tools.magnifier.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.keepScreenOn
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.extensions.TrailSenseComposeFragment
import com.kylecorry.trail_sense.shared.extensions.compose.useEffect
import com.kylecorry.trail_sense.shared.extensions.compose.useEffectWithCleanup
import com.kylecorry.trail_sense.shared.extensions.compose.usePauseEffect
import com.kylecorry.trail_sense.shared.extensions.compose.useResumeEffect
import com.kylecorry.trail_sense.shared.extensions.compose.useState
import com.kylecorry.trail_sense.shared.permissions.alertNoCameraPermission
import com.kylecorry.trail_sense.shared.permissions.requestCamera
import com.kylecorry.trail_sense.shared.views.CameraView

class ToolMagnifierFragment : TrailSenseComposeFragment() {

    @Composable
    override fun FragmentContent() {
        val (cameraView, setCameraView) = useState<CameraView?>(null)
        val (isCameraEnabled, setIsCameraEnabled) = useState(false)
        val (frozenFrame, setFrozenFrame) = useState<Bitmap?>(null)
        val (isCloseUpFocus, setIsCloseUpFocus) = useState(false)

        useResumeEffect {
            requestCamera {
                setIsCameraEnabled(it)
                if (!it) {
                    alertNoCameraPermission()
                }
            }
        }

        useEffect(cameraView, isCameraEnabled, resumedCount) {
            if (isCameraEnabled) {
                cameraView?.start(
                    readFrames = false,
                    preferBackCamera = true,
                    shouldStabilizePreview = false
                )
            } else {
                cameraView?.stop()
            }
        }

        usePauseEffect(cameraView) {
            cameraView?.stop()
        }

        useEffectWithCleanup(cameraView) {
            return@useEffectWithCleanup {
                cameraView?.stop()
            }
        }

        useEffect(cameraView, isCloseUpFocus, isCameraEnabled) {
            cameraView?.setFocus(if (isCloseUpFocus) 0f else null)
        }

        MagnifierContent(
            frozenFrame = frozenFrame,
            isCloseUpFocus = isCloseUpFocus,
            onCameraChanged = setCameraView,
            onFreezeToggle = {
                if (frozenFrame == null) {
                    setFrozenFrame(cameraView?.previewImage)
                } else {
                    setFrozenFrame(null)
                }
            },
            onFocusToggle = {
                setIsCloseUpFocus(!isCloseUpFocus)
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun MagnifierContent(
    frozenFrame: Bitmap?,
    isCloseUpFocus: Boolean,
    onCameraChanged: (CameraView?) -> Unit,
    onFreezeToggle: () -> Unit,
    onFocusToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        AndroidView(
            factory = { context ->
                CameraView(context, null).also {
                    it.id = R.id.camera
                    it.defaultZoomRatio = 2f
                    it.minZoomRatio = 1f
                    it.setShowTorch(true)
                    onCameraChanged(it)
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .keepScreenOn()
                .testTag("camera")
        )

        if (frozenFrame != null) {
            Image(
                bitmap = frozenFrame.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("frozen_frame")
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 56.dp)
                .testTag("magnifier_controls")
        ) {
            IconButton(
                onClick = onFocusToggle,
                modifier = Modifier
                    .padding(8.dp)
                    .size(48.dp)
                    .testTag("focus_toggle_btn")
            ) {
                Icon(
                    painter = painterResource(
                        if (isCloseUpFocus) R.drawable.ic_magnifier else R.drawable.ic_focus_auto
                    ),
                    contentDescription = null,
                    tint = Color.White
                )
            }

            IconButton(
                onClick = onFreezeToggle,
                modifier = Modifier
                    .padding(8.dp)
                    .size(48.dp)
                    .testTag("freeze_btn")
            ) {
                Icon(
                    painter = painterResource(
                        if (frozenFrame != null) {
                            R.drawable.ic_baseline_play_arrow_24
                        } else {
                            R.drawable.ic_pause
                        }
                    ),
                    contentDescription = null,
                    tint = Color.White
                )
            }
        }
    }
}

@Preview
@Composable
private fun MagnifierPreview() {
    MaterialTheme {
        MagnifierContent(
            frozenFrame = null,
            isCloseUpFocus = false,
            onCameraChanged = {},
            onFreezeToggle = {},
            onFocusToggle = {},
            modifier = Modifier.fillMaxSize()
        )
    }
}
