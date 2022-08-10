package com.kylecorry.trail_sense.tools.qr.ui

import android.content.DialogInterface
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.kylecorry.andromeda.core.tryOrNothing
import com.kylecorry.andromeda.fragments.BoundBottomSheetDialogFragment
import com.kylecorry.andromeda.qr.QR
import com.kylecorry.trail_sense.databinding.FragmentQrImportSheetBinding
import com.kylecorry.trail_sense.shared.haptics.HapticSubsystem

class ScanQRBottomSheet(
    private val title: String,
    private val onTextScanned: (text: String?) -> Boolean
) :
    BoundBottomSheetDialogFragment<FragmentQrImportSheetBinding>() {

    private val cameraSize = Size(200, 200)
    private val haptics by lazy { HapticSubsystem.getInstance(requireContext()) }

    private var lastMessage: String? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.camera.clipToOutline = true
        binding.camera.start(cameraSize) {
            onCameraUpdate(it)
        }

        binding.scanQrSheetTitle.text = title

        binding.cancelButton.setOnClickListener {
            onTextScanned(null)
            dismiss()
        }
    }

    override fun onDestroyView() {
        if (isBound) {
            binding.camera.stop()
        }
        super.onDestroyView()
    }

    private fun onCameraUpdate(bitmap: Bitmap) {
        if (!isBound) {
            bitmap.recycle()
            return
        }
        var message: String? = null
        tryOrNothing {
            message = QR.decode(bitmap)
            bitmap.recycle()
        }
        if (message != null && lastMessage != message) {
            haptics.click()
            lastMessage = message
            if (!onTextScanned(message)) {
                dismiss()
            }
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        binding.camera.stop()
        haptics.off()
    }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentQrImportSheetBinding {
        return FragmentQrImportSheetBinding.inflate(layoutInflater, container, false)
    }

}