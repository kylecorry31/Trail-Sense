package com.kylecorry.trail_sense.tools.text

import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.qr.QR
import com.kylecorry.trail_sense.databinding.FragmentSendTextBinding

class SendTextFragment : BoundFragment<FragmentSendTextBinding>() {

    private var text = ""
    private var image: Bitmap? = null

    fun show(text: String) {
        this.text = text
        if (!isBound) {
            return
        }

        binding.textEntry.setText(text)
        updateQR()
    }

    private fun updateQR(){
        binding.qr.setImageBitmap(null)
        image?.recycle()
        if (text.isNotEmpty()) {
            val width = Resources.dp(requireContext(), 250f).toInt()
            image = QR.encode(text, width, width)
            binding.qr.setImageBitmap(image)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        show(text)
        binding.textEntry.addTextChangedListener {
            this.text = binding.textEntry.text.toString()
            updateQR()
        }
    }


    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentSendTextBinding {
        return FragmentSendTextBinding.inflate(layoutInflater, container, false)
    }
}