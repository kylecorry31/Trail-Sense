package com.kylecorry.trail_sense.shared.camera

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.net.Uri
import android.os.Bundle
import android.util.Size
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.camera.core.ImageCapture
import androidx.camera.view.PreviewView
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.exifinterface.media.ExifInterface
import com.kylecorry.andromeda.camera.ImageCaptureSettings
import com.kylecorry.andromeda.core.coroutines.onIO
import com.kylecorry.andromeda.core.coroutines.onMain
import com.kylecorry.andromeda.core.tryOrLog
import com.kylecorry.andromeda.fragments.BoundFullscreenDialogFragment
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.andromeda.fragments.observe
import com.kylecorry.andromeda.sense.orientation.DeviceOrientation
import com.kylecorry.trail_sense.databinding.FragmentPhotoImportSheetBinding
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.shared.sensors.SensorService

class PhotoImportBottomSheetFragment(
    private val resolution: Size? = null,
    private val useZeroShutterLag: Boolean = false,
    private val onCapture: (uri: Uri?) -> Unit
) : BoundFullscreenDialogFragment<FragmentPhotoImportSheetBinding>() {

    private var isCapturing = false

    private var orientation = DeviceOrientation.Orientation.Portrait

    @SuppressLint("UnsafeOptInUsageError")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val orientationSensor = SensorService(requireContext()).getDeviceOrientationSensor()
        orientation = DeviceOrientation.Orientation.Portrait
        observe(orientationSensor) {
            val newOrientation = orientationSensor.orientation
            if (newOrientation == DeviceOrientation.Orientation.Landscape || newOrientation == DeviceOrientation.Orientation.LandscapeInverse) {
                orientation = newOrientation
            } else if (newOrientation == DeviceOrientation.Orientation.Portrait || newOrientation == DeviceOrientation.Orientation.PortraitInverse) {
                orientation = newOrientation
            }
        }

        val volumeKeys = listOf(KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.KEYCODE_VOLUME_UP)
        dialog?.setOnKeyListener { _, keyCode, event ->
            if (!volumeKeys.contains(keyCode) || event.action != KeyEvent.ACTION_DOWN) {
                return@setOnKeyListener false
            }
            takePhoto()
            true
        }

        dialog?.setOnCancelListener { onCapture(null) }

        binding.camera.setScaleType(PreviewView.ScaleType.FIT_CENTER)
        binding.camera.start(
            resolution,
            lifecycleOwner = this,
            readFrames = false,
            captureSettings = ImageCaptureSettings(
                captureMode = if (useZeroShutterLag) {
                    ImageCapture.CAPTURE_MODE_ZERO_SHUTTER_LAG
                } else if (resolution == null) {
                    ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY
                } else {
                    ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY
                }
            )
        )

        binding.toolTitle.rightButton.setOnClickListener {
            onCapture(null)
            dismiss()
        }

        binding.captureButton.setOnClickListener {
            takePhoto()
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = insets.top
                bottomMargin = insets.bottom
                leftMargin = insets.left
                rightMargin = insets.right
            }
            windowInsets
        }
    }

    private fun takePhoto() {
        if (isCapturing) {
            return
        }
        isCapturing = true
        binding.captureButton.isInvisible = true
        binding.loadingIndicator.isVisible = true
        inBackground {
            val file = FileSubsystem.getInstance(requireContext()).createTemp("jpg")
            val success = onIO {
                val saved = binding.camera.capture(file)

                // Set the orientation EXIF
                tryOrLog {
                    if (orientation != DeviceOrientation.Orientation.Portrait) {
                        val exif = ExifInterface(file)
                        exif.rotate(
                            when (orientation) {
                                DeviceOrientation.Orientation.PortraitInverse -> 180
                                DeviceOrientation.Orientation.Landscape -> 270
                                DeviceOrientation.Orientation.LandscapeInverse -> 90
                                else -> 0
                            }
                        )
                        exif.saveAttributes()
                    }
                }

                saved
            }
            onMain {
                if (!success) {
                    file.delete()
                    onCapture(null)
                } else {
                    onCapture(file.toUri())
                }
                dismiss()
            }
        }
    }

    override fun onDestroyView() {
        if (isBound) {
            binding.camera.stop()
        }
        super.onDestroyView()
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        binding.camera.stop()
    }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentPhotoImportSheetBinding {
        return FragmentPhotoImportSheetBinding.inflate(layoutInflater, container, false)
    }
}