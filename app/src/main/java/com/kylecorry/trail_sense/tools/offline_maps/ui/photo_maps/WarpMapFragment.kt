package com.kylecorry.trail_sense.tools.offline_maps.ui.photo_maps

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isInvisible
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.alerts.dialog
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.luna.concurrency.onDefault
import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.luna.concurrency.onMain
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentPhotoMapsPerspectiveBinding
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.extensions.withCancelableLoading
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.PhotoMap
import com.kylecorry.trail_sense.tools.offline_maps.domain.OfflineMapService
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.photo_maps.calibration.MapCornerDetector
import kotlinx.coroutines.launch

class WarpMapFragment : BoundFragment<FragmentPhotoMapsPerspectiveBinding>() {

    private val service = getAppService<OfflineMapService>()

    private val cornerDetector = MapCornerDetector()

    private var mapId = 0L
    private var map: PhotoMap? = null

    private var onDone: () -> Unit = {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mapId = requireArguments().getLong("mapId")
    }

    override fun onDestroyView() {
        if (isBound) {
            binding.perspective.clearImage()
        }
        super.onDestroyView()
    }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentPhotoMapsPerspectiveBinding {
        return FragmentPhotoMapsPerspectiveBinding.inflate(layoutInflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.instructionsText.setOnClickListener {
            dialog(
                getString(R.string.map_perspective_instructions),
                getString(R.string.crop_map_instructions_long),
                cancelText = null
            )
        }

        binding.resetPerspectiveBtn.setOnClickListener {
            binding.perspective.resetCorners()
        }

        binding.perspectiveToggleBtn.setOnClickListener {
            binding.perspective.isPreview = !binding.perspective.isPreview

            if (binding.perspective.isPreview) {
                binding.perspectiveToggleBtn.text = getString(R.string.edit)
            } else {
                binding.perspectiveToggleBtn.text = getString(R.string.preview)
            }
        }

        binding.nextButton.setOnClickListener {
            inBackground {
                next()
            }
        }

        binding.nextButton.isInvisible = true
        binding.perspective.onLoad = { image ->
            inBackground {
                val job = launch {
                    val detected = onDefault { cornerDetector.detect(image) }
                    onMain {
                        if (isBound && detected != null) {
                            binding.perspective.setBounds(detected)
                        }
                    }
                }

                Alerts.withCancelableLoading(
                    requireContext(),
                    getString(R.string.loading),
                    onCancel = { job.cancel() }) {
                    job.join()
                }
            }
        }
        inBackground {
            onIO {
                map = service.getPhotoMap(mapId)
            }
            onMain {
                map?.let {
                    onMapLoad(it)
                }
            }
        }
    }


    private fun onMapLoad(map: PhotoMap) {
        this.map = map
        binding.perspective.mapRotation = map.georeference.rotation
        binding.perspective.setImage(map.imageFile.path)
        binding.nextButton.isInvisible = false
    }

    fun setOnCompleteListener(listener: () -> Unit) {
        onDone = listener
    }

    private suspend fun next() {
        val map = map ?: return
        val percentBounds = if (binding.perspective.hasChanges) {
            binding.perspective.getPercentBounds() ?: return
        } else {
            null
        }
        val loading = onMain {
            Alerts.loading(requireContext(), getString(R.string.saving))
        }
        onIO {
            service.warp(map, percentBounds)
        }

        onMain {
            loading.dismiss()
            binding.perspective.clearImage()
            onDone.invoke()
        }
    }
}
