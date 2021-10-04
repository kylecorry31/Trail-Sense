package com.kylecorry.trail_sense.shared.camera

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.kylecorry.andromeda.camera.Camera
import com.kylecorry.andromeda.core.bitmap.BitmapUtils.toBitmap
import com.kylecorry.andromeda.core.sensors.asLiveData
import com.kylecorry.andromeda.fragments.BoundBottomSheetDialogFragment
import com.kylecorry.trail_sense.databinding.FragmentCameraInputBinding

class CameraInputBottomSheet : BoundBottomSheetDialogFragment<FragmentCameraInputBinding>() {

    private var onImage: ((Bitmap?) -> Unit) = { it?.recycle() }
    private var captureNextImage = false

    private val camera by lazy {
        Camera(
            requireContext(),
            viewLifecycleOwner,
            previewView = binding.preview,
            analyze = true
        )
    }

    // TODO: Allow zoom
//    val scaleListener = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
//        override fun onScale(detector: ScaleGestureDetector): Boolean {
//            val zoom = SolMath.clamp(cloudSensor.zoom * detector.scaleFactor, 1f, 2f)
//            cloudSensor.zoom = zoom
//            return true
//        }
//    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.ok.setOnClickListener {
            captureNextImage = true
        }

        camera.asLiveData().observe(viewLifecycleOwner) {
            onCameraUpdate()
        }
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun onCameraUpdate() {
        if (!isBound || !captureNextImage) {
            camera.image?.close()
            return
        }
        try {
            val bitmap = camera.image?.image?.toBitmap()
            bitmap?.let(onImage)
            captureNextImage = false
        } finally {
            camera.image?.close()
        }
    }

    fun setOnImageListener(listener: (image: Bitmap?) -> Unit) {
        onImage = listener
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        onImage.invoke(null)
    }


    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentCameraInputBinding {
        return FragmentCameraInputBinding.inflate(layoutInflater, container, false)
    }

}