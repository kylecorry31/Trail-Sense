package com.kylecorry.trail_sense.tools.qr.ui

import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.pickers.Pickers
import com.kylecorry.andromeda.qr.QR
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentSendTextBinding
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.tools.notes.infrastructure.NoteRepo
import com.kylecorry.trail_sense.tools.qr.infrastructure.BeaconQREncoder
import com.kylecorry.trail_sense.tools.qr.infrastructure.NoteQREncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SendTextFragment : BoundFragment<FragmentSendTextBinding>() {

    private var text = ""
    private var image: Bitmap? = null
    private val gps by lazy { SensorService(requireContext()).getGPS(false) }

    private val beaconQREncoder = BeaconQREncoder()
    private val noteQREncoder = NoteQREncoder()

    fun show(text: String) {
        this.text = text
        if (!isBound) {
            return
        }

        binding.textEntry.setText(text)
        updateQR()
    }

    private fun updateQR() {
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
        binding.qr.clipToOutline = true
        binding.textEntry.addTextChangedListener {
            this.text = binding.textEntry.text.toString()
            updateQR()
        }

        binding.qrSendBeacon.setOnClickListener {
            CustomUiUtils.pickBeacon(requireContext(), getString(R.string.beacon), gps.location) {
                if (it != null) {
                    show(beaconQREncoder.encode(it))
                }
            }
        }

        binding.qrSendNote.setOnClickListener {
            // TODO: Use a better UI for this
            runInBackground {
                // TODO: Only load note titles and dates (notes should be revamped to use the first line of text as a title)
                val notes = withContext(Dispatchers.IO) {
                    NoteRepo.getInstance(requireContext()).getNotesSync()
                }

                val titles = notes.map { it.title ?: getString(android.R.string.untitled) }

                Pickers.item(requireContext(), getString(R.string.note), titles) {
                    if (it != null) {
                        val note = notes[it]
                        show(noteQREncoder.encode(note))
                    }
                }


            }
        }

    }


    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentSendTextBinding {
        return FragmentSendTextBinding.inflate(layoutInflater, container, false)
    }
}