package com.kylecorry.trail_sense.tools.qr.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.kylecorry.andromeda.alerts.toast
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.fragments.BoundBottomSheetDialogFragment
import com.kylecorry.andromeda.qr.QR
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentQrShareSheetBinding

class ViewQRBottomSheet(
    private val title: String,
    private val text: String
) : BoundBottomSheetDialogFragment<FragmentQrShareSheetBinding>() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.qrCode.clipToOutline = true
        updateUI()
    }

    private fun updateUI() {
        if (!isBound) {
            return
        }
        binding.qrTitle.text = title
        val size = Resources.dp(requireContext(), 250f).toInt()

        if (text.length > MAX_LENGTH){
            toast(getString(R.string.qr_text_too_long))
        }

        val bitmap = QR.encode(text.take(MAX_LENGTH), size, size)
        binding.qrCode.setImageBitmap(bitmap)
    }


    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentQrShareSheetBinding {
        return FragmentQrShareSheetBinding.inflate(layoutInflater, container, false)
    }

    companion object {
        const val MAX_LENGTH = 1000
    }

}