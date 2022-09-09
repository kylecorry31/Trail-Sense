package com.kylecorry.trail_sense.tools.qr.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.camera.Camera
import com.kylecorry.andromeda.clipboard.Clipboard
import com.kylecorry.andromeda.core.system.GeoUri
import com.kylecorry.andromeda.core.system.Intents
import com.kylecorry.andromeda.core.tryOrNothing
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.list.ListView
import com.kylecorry.andromeda.qr.QR
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentScanTextBinding
import com.kylecorry.trail_sense.databinding.ListItemQrResultBinding
import com.kylecorry.trail_sense.navigation.beacons.infrastructure.persistence.BeaconService
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.extensions.inBackground
import com.kylecorry.trail_sense.shared.haptics.HapticSubsystem
import com.kylecorry.trail_sense.shared.permissions.alertNoCameraPermission
import com.kylecorry.trail_sense.shared.permissions.requestCamera
import com.kylecorry.trail_sense.tools.notes.infrastructure.NoteRepo
import com.kylecorry.trail_sense.tools.qr.infrastructure.BeaconQREncoder
import com.kylecorry.trail_sense.tools.qr.infrastructure.NoteQREncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ScanQRFragment : BoundFragment<FragmentScanTextBinding>() {

    private val cameraSize = Size(200, 200)

    private var history = mutableListOf<String>()
    private lateinit var qrHistoryList: ListView<String>

    private val beaconQREncoder = BeaconQREncoder()
    private val noteQREncoder = NoteQREncoder()

    private val haptics by lazy { HapticSubsystem.getInstance(requireContext()) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        qrHistoryList =
            ListView(binding.qrHistory, R.layout.list_item_qr_result) { itemView, text ->
                val itemBinding = ListItemQrResultBinding.bind(itemView)

                val type = when {
                    isURL(text) -> ScanType.Url
                    isLocation(text) -> ScanType.Geo
                    else -> ScanType.Text
                }

                itemBinding.text.text = text.ifEmpty { null }
                itemBinding.qrWeb.isVisible = type == ScanType.Url
                itemBinding.qrLocation.isVisible = type == ScanType.Geo
                itemBinding.qrBeacon.isVisible = type == ScanType.Geo
                itemBinding.qrSaveNote.isVisible = type == ScanType.Text
                itemBinding.qrCopy.isVisible = true
                itemBinding.qrDelete.isVisible = true
                itemBinding.qrActions.isVisible = text.isNotEmpty()

                itemBinding.qrMessageType.setImageResource(
                    when (type) {
                        ScanType.Text -> R.drawable.ic_tool_notes
                        ScanType.Url -> R.drawable.ic_link
                        ScanType.Geo -> R.drawable.ic_location
                    }
                )

                itemBinding.qrCopy.setOnClickListener {
                    copy(text)
                }

                itemBinding.qrSaveNote.setOnClickListener {
                    createNote(text)
                }

                itemBinding.qrLocation.setOnClickListener {
                    openMap(text)
                }

                itemBinding.qrBeacon.setOnClickListener {
                    createBeacon(text)
                }

                itemBinding.qrWeb.setOnClickListener {
                    openUrl(text)
                }

                itemBinding.qrDelete.setOnClickListener {
                    deleteResult(text)
                }
            }
        binding.camera.clipToOutline = true
    }


    private fun copy(text: String) {
        Clipboard.copy(requireContext(), text, getString(R.string.copied_to_clipboard_toast))
    }

    private fun createNote(text: String) {
        inBackground {
            val id = withContext(Dispatchers.IO) {
                NoteRepo.getInstance(requireContext())
                    .addNote(noteQREncoder.decode(text))
            }
            withContext(Dispatchers.Main) {
                CustomUiUtils.snackbar(
                    this@ScanQRFragment,
                    getString(R.string.saved_to_note),
                    action = getString(R.string.view)
                ) {
                    findNavController().navigate(
                        R.id.fragmentToolNotesCreate,
                        bundleOf("edit_note_id" to id)
                    )
                }
            }
        }
    }

    private fun openMap(text: String) {
        val intent = Intents.url(text)
        Intents.openChooser(requireContext(), intent, text)
    }

    private fun openUrl(text: String) {
        val protocols = listOf("http", "https", "rtsp", "ftp")
        val url = if (protocols.none { text.lowercase().startsWith(it) }){
            // Default to HTTPS
            "https://$text"
        } else {
            text
        }
        val intent = Intents.url(url)
        Intents.openChooser(requireContext(), intent, text)
    }

    private fun createBeacon(text: String) {
        val beacon = beaconQREncoder.decode(text) ?: return
        inBackground {
            val id = BeaconService(requireContext()).add(beacon)
            CustomUiUtils.snackbar(
                this@ScanQRFragment,
                getString(R.string.beacon_created),
                action = getString(R.string.view)
            ) {
                findNavController().navigate(
                    R.id.beaconDetailsFragment,
                    bundleOf("beacon_id" to id)
                )
            }
        }
    }

    private fun deleteResult(text: String) {
        val idx = history.indexOf(text)
        history.remove(text)
        updateHistoryList(false)
        CustomUiUtils.snackbar(
            this@ScanQRFragment,
            getString(R.string.result_deleted),
            action = getString(R.string.undo)
        ) {
            if (!history.contains(text)) {
                if (idx <= history.size) {
                    history.add(idx, text)
                } else {
                    history.add(text)
                }
                updateHistoryList(false)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestCamera { hasPermission ->
            if (hasPermission){
                startCamera()
            } else {
                alertNoCameraPermission()
            }
        }
    }


    override fun onResume() {
        super.onResume()
        updateHistoryList()
        if (Camera.isAvailable(requireContext())) {
            startCamera()
        }
    }

    private fun startCamera() {
        if (!isBound) return
        binding.camera.start(cameraSize) {
            onCameraUpdate(it)
        }
    }


    @SuppressLint("UnsafeOptInUsageError")
    private fun onCameraUpdate(bitmap: Bitmap): Boolean {
        if (!isBound) {
            bitmap.recycle()
            return true
        }
        var message: String? = null
        tryOrNothing {
            message = QR.decode(bitmap)
            bitmap.recycle()
        }

        if (message != null) {
            onQRScanned(message!!)
        }

        return true
    }

    private fun addReading(text: String) {
        if (history.contains(text)) {
            history.remove(text)
        }

        history.add(0, text)

        while (history.size > 10) {
            history.removeLast()
        }

        updateHistoryList()
    }

    private fun updateHistoryList(scrollToTop: Boolean = true) {
        qrHistoryList.setData(if (history.isEmpty()) listOf("") else history)
        if (scrollToTop) {
            qrHistoryList.scrollToPosition(0, true)
        }
    }

    private fun onQRScanned(message: String) {
        val lastMessage = history.firstOrNull()
        if (message.isNotEmpty() && lastMessage != message) {
            addReading(message)
            haptics.click()
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
        binding.camera.stop()
        haptics.off()
    }

    private fun isLocation(text: String): Boolean {
        return GeoUri.from(Uri.parse(text)) != null
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