package com.kylecorry.trail_sense.navigation.ui

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.kylecorry.andromeda.camera.Camera
import com.kylecorry.andromeda.core.sensors.asLiveData
import com.kylecorry.andromeda.fragments.BoundBottomSheetDialogFragment
import com.kylecorry.andromeda.qr.QRService
import com.kylecorry.trail_sense.databinding.FragmentBeaconQrImportBinding
import com.kylecorry.trail_sense.navigation.domain.MyNamedCoordinate
import com.kylecorry.trailsensecore.infrastructure.images.BitmapUtils.toBitmap
import com.kylecorry.trailsensecore.infrastructure.system.GeoUriParser
import com.kylecorry.trailsensecore.infrastructure.system.UiUtils
import com.kylecorry.trailsensecore.infrastructure.system.tryOrNothing

class BeaconImportQRBottomSheet : BoundBottomSheetDialogFragment<FragmentBeaconQrImportBinding>() {

    private val qr = QRService()
    private val cameraSizePixels by lazy { UiUtils.dp(requireContext(), 250f).toInt() }
    private val camera by lazy {
        Camera(
            requireContext(),
            viewLifecycleOwner,
            previewView = binding.beaconQrScan,
            analyze = true,
            targetResolution = Size(cameraSizePixels, cameraSizePixels)
        )
    }

    var onBeaconScanned: ((beacon: MyNamedCoordinate) -> Unit)? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.cancelButton.setOnClickListener {
            dismiss()
        }

        camera.asLiveData().observe(viewLifecycleOwner) {
            onCameraUpdate()
        }
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun onCameraUpdate() {
        if (!isBound){
            return
        }
        var message: String? = null
        tryOrNothing {
            val bitmap = camera.image?.image?.toBitmap() ?: return@tryOrNothing
            message = qr.decode(bitmap)
            bitmap.recycle()
        }
        camera.image?.close()

        if (message != null) {
            onQRScanned(message!!)
        }
    }

    private fun onQRScanned(message: String) {
        val parsed = GeoUriParser().parse(Uri.parse(message)) ?: return
        onBeaconScanned?.invoke(MyNamedCoordinate.from(parsed))
    }


    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentBeaconQrImportBinding {
        return FragmentBeaconQrImportBinding.inflate(layoutInflater, container, false)
    }

}