package com.kylecorry.trail_sense.tools.qr.ui

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.os.Bundle
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.kylecorry.andromeda.buzz.Buzz
import com.kylecorry.andromeda.buzz.HapticFeedbackType
import com.kylecorry.andromeda.camera.Camera
import com.kylecorry.andromeda.core.bitmap.BitmapUtils.toBitmap
import com.kylecorry.andromeda.core.sensors.asLiveData
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.tryOrNothing
import com.kylecorry.andromeda.fragments.BoundBottomSheetDialogFragment
import com.kylecorry.andromeda.qr.QR
import com.kylecorry.trail_sense.databinding.FragmentQrImportSheetBinding

class ScanQRBottomSheet(private val title: String, private val onTextScanned: (text: String?) -> Boolean) :
    BoundBottomSheetDialogFragment<FragmentQrImportSheetBinding>() {

    private val cameraSizePixels = 200
    private val camera by lazy {
        Camera(
            requireContext(),
            viewLifecycleOwner,
            previewView = binding.qrScanner,
            analyze = true,
            targetResolution = Size(cameraSizePixels, cameraSizePixels)
        )
    }

    private var lastMessage: String? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.qrScanner.clipToOutline = true

        binding.scanQrSheetTitle.text = title

        binding.cancelButton.setOnClickListener {
            onTextScanned(null)
            dismiss()
        }

        camera.asLiveData().observe(viewLifecycleOwner) {
            onCameraUpdate()
        }
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun onCameraUpdate() {
        if (!isBound) {
            return
        }
        var message: String? = null
        tryOrNothing {
            val bitmap = camera.image?.image?.toBitmap() ?: return@tryOrNothing
            message = QR.decode(bitmap)
            bitmap.recycle()
        }
        camera.image?.close()

        if (message != null && lastMessage != message) {
            Buzz.feedback(requireContext(), HapticFeedbackType.Click)
            lastMessage = message
            if (!onTextScanned(message)) {
                dismiss()
            }
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        Buzz.off(requireContext())
    }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentQrImportSheetBinding {
        return FragmentQrImportSheetBinding.inflate(layoutInflater, container, false)
    }

}