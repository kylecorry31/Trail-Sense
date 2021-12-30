package com.kylecorry.trail_sense.tools.qr.ui

import android.Manifest
import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.buzz.Buzz
import com.kylecorry.andromeda.buzz.HapticFeedbackType
import com.kylecorry.andromeda.camera.Camera
import com.kylecorry.andromeda.clipboard.Clipboard
import com.kylecorry.andromeda.core.bitmap.BitmapUtils.toBitmap
import com.kylecorry.andromeda.core.system.GeoUriParser
import com.kylecorry.andromeda.core.system.Intents
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.tryOrNothing
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.qr.QR
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentScanTextBinding
import com.kylecorry.trail_sense.navigation.beacons.infrastructure.persistence.BeaconService
import com.kylecorry.trail_sense.shared.*
import com.kylecorry.trail_sense.tools.notes.infrastructure.NoteRepo
import com.kylecorry.trail_sense.tools.qr.infrastructure.BeaconQREncoder
import com.kylecorry.trail_sense.tools.qr.infrastructure.NoteQREncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ScanQRFragment : BoundFragment<FragmentScanTextBinding>() {

    private val cameraSizePixels by lazy { Resources.dp(requireContext(), 100f).toInt() }
    private var camera: Camera? = null

    private var text = ""
    private var torchOn = false

    private val beaconQREncoder = BeaconQREncoder()
    private val noteQREncoder = NoteQREncoder()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.text.keyListener = null
        text = ""

        camera?.stop(this::onCameraUpdate)
        camera = Camera(
            requireContext(),
            viewLifecycleOwner,
            previewView = binding.qrScan,
            analyze = true,
            targetResolution = Size(cameraSizePixels, cameraSizePixels)
        )

        binding.qrCameraHolder.clipToOutline = true

        binding.qrTorchState.setOnClickListener {
            torchOn = !torchOn
            binding.qrTorchState.setImageResource(if (torchOn) R.drawable.ic_torch_on else R.drawable.ic_torch_off)
            camera?.setTorch(torchOn)
        }

        binding.qrZoom.setOnProgressChangeListener { progress, _ ->
            camera?.setZoom(progress / 100f)
        }

        binding.qrCopy.setOnClickListener {
            Clipboard.copy(requireContext(), text, getString(R.string.copied_to_clipboard_toast))
        }

        binding.qrSaveNote.setOnClickListener {
            runInBackground {
                val id = withContext(Dispatchers.IO) {
                    NoteRepo.getInstance(requireContext())
                        .addNote(noteQREncoder.decode(text))
                }
                withContext(Dispatchers.Main) {
                    CustomUiUtils.snackbar(
                        binding.root,
                        getString(R.string.saved_to_note),
                        action = getString(R.string.edit)
                    ) {
                        findNavController().navigate(
                            R.id.fragmentToolNotesCreate,
                            bundleOf("edit_note_id" to id)
                        )
                    }
                }
            }
        }

        binding.qrLocation.setOnClickListener {
            val intent = Intents.url(text)
            Intents.openChooser(requireContext(), intent, text)
        }

        binding.qrBeacon.setOnClickListener {
            val beacon = beaconQREncoder.decode(text) ?: return@setOnClickListener
            runInBackground {
                val id = BeaconService(requireContext()).addBeacon(beacon)
                CustomUiUtils.snackbar(
                    binding.root,
                    getString(R.string.beacon_created),
                    action = getString(R.string.edit)
                ) {
                    findNavController().navigate(
                        R.id.place_beacon,
                        bundleOf("edit_beacon" to id)
                    )
                }
            }
        }

        binding.qrWeb.setOnClickListener {
            val intent = Intents.url(text)
            Intents.openChooser(requireContext(), intent, text)
        }
    }

    override fun onResume() {
        super.onResume()
        torchOn = false
        binding.qrZoom.progress = 0
        binding.qrTorchState.setImageResource(R.drawable.ic_torch_off)
        requestPermissions(listOf(Manifest.permission.CAMERA)) {
            if (Camera.isAvailable(requireContext())) {
                camera?.start(this::onCameraUpdate)
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
            val bitmap = camera?.image?.image?.toBitmap() ?: return@tryOrNothing
            message = QR.decode(bitmap)
            bitmap.recycle()
        }
        camera?.image?.close()

        if (message != null) {
            onQRScanned(message!!)
        }

        return true
    }

    private fun onQRScanned(message: String) {
        if (message.isNotEmpty() && text != message) {
            text = message

            val type = when {
                isURL(text) -> ScanType.Url
                isLocation(text) -> ScanType.Geo
                else -> ScanType.Text
            }

            binding.text.text = message
            binding.qrWeb.isVisible = type == ScanType.Url
            binding.qrLocation.isVisible = type == ScanType.Geo
            binding.qrBeacon.isVisible = type == ScanType.Geo
            binding.qrSaveNote.isVisible = type == ScanType.Text
            binding.qrCopy.isVisible = true
            binding.qrActions.isVisible = true

            binding.qrMessageType.setImageResource(
                when (type) {
                    ScanType.Text -> R.drawable.ic_note
                    ScanType.Url -> R.drawable.ic_link
                    ScanType.Geo -> R.drawable.ic_location
                }
            )

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
        camera?.stop(this::onCameraUpdate)
        Buzz.off(requireContext())
    }

    private fun isLocation(text: String): Boolean {
        return GeoUriParser.parse(Uri.parse(text)) != null
    }

    private fun isURL(text: String): Boolean {
        return android.util.Patterns.WEB_URL.matcher(text).matches()
    }

    private enum class ScanType {
        Text,
        Url,
        Geo
    }


}