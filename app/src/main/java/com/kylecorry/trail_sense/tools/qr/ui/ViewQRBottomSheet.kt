package com.kylecorry.trail_sense.tools.qr.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.fragments.BoundBottomSheetDialogFragment
import com.kylecorry.andromeda.qr.QR
import com.kylecorry.trail_sense.databinding.FragmentBeaconQrShareBinding

class ViewQRBottomSheet(
    private val title: String,
    private val text: String
) : BoundBottomSheetDialogFragment<FragmentBeaconQrShareBinding>() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateUI()
    }

    private fun updateUI() {
        if (!isBound) {
            return
        }
        binding.beaconName.text = title
        val size = Resources.dp(requireContext(), 250f).toInt()
        val bitmap = QR.encode(text, size, size)
        binding.beaconQr.setImageBitmap(bitmap)
    }


    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentBeaconQrShareBinding {
        return FragmentBeaconQrShareBinding.inflate(layoutInflater, container, false)
    }

}