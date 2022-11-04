package com.kylecorry.trail_sense.shared.camera

import android.content.DialogInterface
import android.net.Uri
import android.os.Bundle
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.camera.core.ImageCapture
import androidx.camera.view.PreviewView
import androidx.core.net.toUri
import androidx.core.view.isInvisible
import com.kylecorry.andromeda.camera.ImageCaptureSettings
import com.kylecorry.andromeda.fragments.BoundFullscreenDialogFragment
import com.kylecorry.trail_sense.databinding.FragmentPhotoImportSheetBinding
import com.kylecorry.trail_sense.shared.extensions.inBackground
import com.kylecorry.trail_sense.shared.extensions.onIO
import com.kylecorry.trail_sense.shared.extensions.onMain
import com.kylecorry.trail_sense.shared.io.FileSubsystem

class PhotoImportBottomSheetFragment(
    private val resolution: Size? = null,
    private val onCapture: (uri: Uri?) -> Unit
) : BoundFullscreenDialogFragment<FragmentPhotoImportSheetBinding>() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.camera.setScaleType(PreviewView.ScaleType.FIT_CENTER)
        binding.camera.clipToOutline = true
        binding.camera.start(
            resolution,
            lifecycleOwner = this,
            readFrames = false,
            captureSettings = ImageCaptureSettings(captureMode = ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
        )

        binding.toolTitle.rightButton.setOnClickListener {
            onCapture(null)
            dismiss()
        }

        binding.captureButton.setOnClickListener {
            binding.captureButton.isInvisible = true
            inBackground {
                val file = FileSubsystem.getInstance(requireContext()).createTemp("jpg")
                val success = onIO {
                    binding.camera.capture(file)
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