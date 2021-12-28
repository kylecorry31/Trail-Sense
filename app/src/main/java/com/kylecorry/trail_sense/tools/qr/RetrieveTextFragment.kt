package com.kylecorry.trail_sense.tools.qr

import android.Manifest
import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.alerts.toast
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
import com.kylecorry.trail_sense.navigation.domain.MyNamedCoordinate
import com.kylecorry.trail_sense.navigation.infrastructure.share.LocationGeoSender
import com.kylecorry.trail_sense.shared.AppUtils
import com.kylecorry.trail_sense.shared.alertNoCameraPermission
import com.kylecorry.trail_sense.shared.setOnProgressChangeListener
import com.kylecorry.trail_sense.tools.notes.domain.Note
import com.kylecorry.trail_sense.tools.notes.infrastructure.NoteRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant

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
    private var torchOn = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.text.keyListener = null

        binding.qrCameraHolder.clipToOutline = true

        binding.qrTorchState.setOnClickListener {
            torchOn = !torchOn
            binding.qrTorchState.setImageResource(if (torchOn) R.drawable.ic_torch_on else R.drawable.ic_torch_off)
            camera.setTorch(torchOn)
        }

        binding.qrZoom.setOnProgressChangeListener { progress, _ ->
            camera.setZoom(progress / 100f)
        }

        binding.qrCopy.setOnClickListener {
            Clipboard.copy(requireContext(), text, getString(R.string.copied_to_clipboard_toast))
        }

        binding.qrSaveNote.setOnClickListener {
            Alerts.dialog(requireContext(), getString(R.string.create_note), text) {
                if (!it) {
                    runInBackground {
                        withContext(Dispatchers.IO) {
                            NoteRepo.getInstance(requireContext())
                                .addNote(Note(null, text, Instant.now().toEpochMilli()))
                        }
                        withContext(Dispatchers.Main) {
                            toast(getString(R.string.saved_to_note))
                        }
                    }
                }
            }
        }

        binding.qrLocation.setOnClickListener {
            val location = GeoUriParser.parse(Uri.parse(text)) ?: return@setOnClickListener
            val sender = LocationGeoSender(requireContext())
            sender.send(location.coordinate)
        }

        binding.qrBeacon.setOnClickListener {
            val location = GeoUriParser.parse(Uri.parse(text)) ?: return@setOnClickListener
            AppUtils.placeBeacon(requireContext(), MyNamedCoordinate.from(location))
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

            val type = when {
                isURL(text) -> ScanType.Url
                isLocation(text) -> ScanType.Geo
                else -> ScanType.Text
            }

            binding.text.text = message
            binding.qrWeb.isVisible = type == ScanType.Url
            binding.qrLocation.isVisible = type == ScanType.Geo
            binding.qrBeacon.isVisible = type == ScanType.Geo

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
        camera.stop(this::onCameraUpdate)
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