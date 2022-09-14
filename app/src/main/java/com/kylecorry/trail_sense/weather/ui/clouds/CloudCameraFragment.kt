package com.kylecorry.trail_sense.weather.ui.clouds

import android.graphics.Bitmap
import android.os.Bundle
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.camera.view.PreviewView
import androidx.core.view.isVisible
import com.kylecorry.andromeda.camera.Camera
import com.kylecorry.andromeda.core.bitmap.BitmapUtils
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentCameraInputBinding
import com.kylecorry.trail_sense.shared.extensions.inBackground
import com.kylecorry.trail_sense.shared.extensions.onIO
import com.kylecorry.trail_sense.shared.extensions.onMain
import com.kylecorry.trail_sense.shared.io.DeleteTempFilesCommand
import com.kylecorry.trail_sense.shared.io.Files
import com.kylecorry.trail_sense.shared.io.FragmentUriPicker
import com.kylecorry.trail_sense.shared.permissions.requestCamera

class CloudCameraFragment : BoundFragment<FragmentCameraInputBinding>() {

    private var onImage: ((Bitmap) -> Unit) = { it.recycle() }
    private val uriPicker = FragmentUriPicker(this)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.camera.setShowTorch(false)
        binding.camera.setScaleType(PreviewView.ScaleType.FIT_CENTER)

        binding.ok.setOnClickListener {
            binding.camera.quickCapture {
                onImage(it)
            }
        }

        binding.upload.setOnClickListener {
            inBackground {
                val uri = uriPicker.open(listOf("image/*"))
                uri?.let {
                    onIO {
                        // TODO: Let the user know the file couldn't be opened
                        val temp = Files.copyToTemp(requireContext(), uri) ?: return@onIO
                        val bp = BitmapUtils.decodeBitmapScaled(temp.path, CloudIdentificationFragment.IMAGE_SIZE, CloudIdentificationFragment.IMAGE_SIZE)
                        DeleteTempFilesCommand(requireContext()).execute()
                        onMain {
                            onImage.invoke(bp)
                        }
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestCamera { }
    }

    override fun onResume() {
        super.onResume()
        if (Camera.isAvailable(requireContext())) {
            try {
                binding.camera.start(Size(CloudIdentificationFragment.IMAGE_SIZE, CloudIdentificationFragment.IMAGE_SIZE))
                showCamera()
            } catch (e: Exception) {
                e.printStackTrace()
                hideCamera()
            }
        } else {
            hideCamera()
        }
    }

    private fun showCamera() {
        binding.orText.text = getString(R.string.or).uppercase()
        binding.camera.isVisible = true
        binding.ok.isVisible = true
    }

    private fun hideCamera() {
        binding.orText.text = getString(R.string.no_camera_access)
        binding.camera.isVisible = false
        binding.ok.isVisible = false
    }

    override fun onPause() {
        super.onPause()
        binding.camera.stop()
    }

    fun setOnImageListener(listener: (image: Bitmap) -> Unit) {
        onImage = listener
    }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentCameraInputBinding {
        return FragmentCameraInputBinding.inflate(layoutInflater, container, false)
    }

}