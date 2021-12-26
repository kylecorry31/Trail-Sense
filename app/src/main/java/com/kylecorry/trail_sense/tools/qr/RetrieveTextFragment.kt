package com.kylecorry.trail_sense.tools.qr

import android.Manifest
import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.kylecorry.andromeda.buzz.Buzz
import com.kylecorry.andromeda.buzz.HapticFeedbackType
import com.kylecorry.andromeda.camera.Camera
import com.kylecorry.andromeda.core.bitmap.BitmapUtils.toBitmap
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.tryOrNothing
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.qr.QR
import com.kylecorry.trail_sense.databinding.FragmentScanTextBinding
import com.kylecorry.trail_sense.shared.alertNoCameraPermission

class RetrieveTextFragment : BoundFragment<FragmentScanTextBinding>() {

    private val cameraSizePixels by lazy { Resources.dp(requireContext(), 100f).toInt() }
    private val camera by lazy {
        Camera(
            requireContext(),
            viewLifecycleOwner,
            previewView = binding.qrScan,
            analyze = true,
            targetResolution = Size(cameraSizePixels, cameraSizePixels)
        )
    }

    private var text = ""

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.text.keyListener = null
    }

    override fun onResume() {
        super.onResume()
        requestPermissions(listOf(Manifest.permission.CAMERA)) {
            if (Camera.isAvailable(requireContext())) {
                camera.start(this::onCameraUpdate)
            } else {
                alertNoCameraPermission()
            }
        }
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun onCameraUpdate(): Boolean {
        if (!isBound) {
            return true
        }
        var message: String? = null
        tryOrNothing {
            val bitmap = camera.image?.image?.toBitmap() ?: return@tryOrNothing
            message = QR.decode(bitmap)
            bitmap.recycle()
        }
        camera.image?.close()

        if (message != null) {
            onQRScanned(message!!)
        }

        return true
    }

    private fun onQRScanned(message: String) {
        if (message.isNotEmpty() && text != message) {
            text = message
            binding.text.setText(message)
            Buzz.feedback(requireContext(), HapticFeedbackType.Click)
        }
    }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentScanTextBinding {
        return FragmentScanTextBinding.inflate(layoutInflater, container, false)
    }

    override fun onPause() {
        super.onPause()
        camera.stop(this::onCameraUpdate)
        Buzz.off(requireContext())
    }
}