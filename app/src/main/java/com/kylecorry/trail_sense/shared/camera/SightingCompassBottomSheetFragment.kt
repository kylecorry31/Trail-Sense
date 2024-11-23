package com.kylecorry.trail_sense.shared.camera

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.camera.view.PreviewView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import com.kylecorry.andromeda.fragments.BoundFullscreenDialogFragment
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.luna.coroutines.CoroutineQueueRunner
import com.kylecorry.sol.units.Bearing
import com.kylecorry.trail_sense.databinding.FragmentSightingCompassSheetBinding
import com.kylecorry.trail_sense.shared.FormatService
import kotlinx.coroutines.Dispatchers

class SightingCompassBottomSheetFragment(
    private val onSelect: (bearing: Float?) -> Unit
) : BoundFullscreenDialogFragment<FragmentSightingCompassSheetBinding>() {

    private val formatter by lazy { FormatService.getInstance(requireContext()) }

    var bearing: Float? = null
        set(value) {
            field = value
            if (isBound) {
                updateUI()
            }
        }

    private val fovRunner = CoroutineQueueRunner(1, dispatcher = Dispatchers.Default)

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

        // TODO: Width should always match parent, height can be cropped
        binding.camera.setScaleType(PreviewView.ScaleType.FIT_CENTER)
        binding.camera.start(
            lifecycleOwner = this,
            readFrames = false,
            shouldStabilizePreview = false
        )

        binding.toolTitle.rightButton.setOnClickListener {
            onSelect(null)
            dismiss()
        }

        binding.captureButton.setOnClickListener {
            confirmBearing()
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

        updateUI()
    }

    private fun updateUI() {
        inBackground {
            fovRunner.enqueue {
                if (!isBound) {
                    return@enqueue
                }
                val minimumFOV = 5f
                binding.linearCompass.range = binding.camera.fov.first.coerceAtLeast(minimumFOV)
            }
        }

        binding.toolTitle.title.text = bearing?.let { formatter.formatDegrees(it) } ?: ""
        binding.linearCompass.azimuth = Bearing(bearing ?: 0f)
    }

    private fun confirmBearing() {
        onSelect(bearing)
        dismiss()
    }

    override fun onDestroyView() {
        if (isBound) {
            binding.camera.stop()
        }
        fovRunner.cancel()
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