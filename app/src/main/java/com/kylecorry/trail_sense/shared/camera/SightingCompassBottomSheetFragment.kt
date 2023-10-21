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
import androidx.camera.core.ExperimentalZeroShutterLag
import androidx.camera.core.ImageCapture
import androidx.camera.view.PreviewView
import androidx.core.net.toUri
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import com.kylecorry.andromeda.camera.Camera
import com.kylecorry.andromeda.camera.ImageCaptureSettings
import com.kylecorry.andromeda.fragments.BoundFullscreenDialogFragment
import com.kylecorry.trail_sense.databinding.FragmentPhotoImportSheetBinding
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.sol.math.SolMath.toDegrees
import com.kylecorry.sol.units.Bearing
import com.kylecorry.trail_sense.databinding.FragmentSightingCompassSheetBinding
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.extensions.onIO
import com.kylecorry.trail_sense.shared.extensions.onMain
import com.kylecorry.trail_sense.shared.io.FileSubsystem

class SightingCompassBottomSheetFragment(
    private val onSelect: (bearing: Bearing?) -> Unit
) : BoundFullscreenDialogFragment<FragmentSightingCompassSheetBinding>() {

    private val formatter by lazy { FormatService.getInstance(requireContext()) }

    var bearing: Float? = null
        set(value) {
            field = value
            if (isBound) {
                updateUI()
            }
        }

    @SuppressLint("UnsafeOptInUsageError")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val volumeKeys = listOf(KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.KEYCODE_VOLUME_UP)
        dialog?.setOnKeyListener { _, keyCode, event ->
            if (!volumeKeys.contains(keyCode) || event.action != KeyEvent.ACTION_DOWN) {
                return@setOnKeyListener false
            }
            confirmBearing()
            true
        }

        dialog?.setOnCancelListener { onSelect(null) }

        binding.camera.setScaleType(PreviewView.ScaleType.FILL_CENTER)
        binding.camera.start(
            lifecycleOwner = this,
            readFrames = false
        )

        binding.toolTitle.rightButton.setOnClickListener {
            onSelect(null)
            dismiss()
        }

        binding.captureButton.setOnClickListener {
            confirmBearing()
        }

        updateUI()
    }

    private fun updateUI() {
        val minimumFOV = 5f
        binding.linearCompass.range = binding.camera.fov.first.coerceAtLeast(minimumFOV)

        binding.toolTitle.title.text = bearing?.let { formatter.formatDegrees(it) } ?: ""
        binding.linearCompass.azimuth = Bearing(bearing ?: 0f)
    }

    private fun confirmBearing() {
        onSelect(bearing?.let { Bearing(it) })
        dismiss()
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
    ): FragmentSightingCompassSheetBinding {
        return FragmentSightingCompassSheetBinding.inflate(layoutInflater, container, false)
    }
}