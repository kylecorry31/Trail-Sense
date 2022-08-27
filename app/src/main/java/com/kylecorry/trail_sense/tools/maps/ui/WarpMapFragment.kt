package com.kylecorry.trail_sense.tools.maps.ui

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isInvisible
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.files.LocalFiles
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentMapsPerspectiveBinding
import com.kylecorry.trail_sense.shared.extensions.inBackground
import com.kylecorry.trail_sense.tools.maps.domain.Map
import com.kylecorry.trail_sense.tools.maps.infrastructure.ImageSaver
import com.kylecorry.trail_sense.tools.maps.infrastructure.MapRepo
import com.kylecorry.trail_sense.tools.maps.infrastructure.fixPerspective
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import java.io.IOException

class WarpMapFragment : BoundFragment<FragmentMapsPerspectiveBinding>() {

    private val mapRepo by lazy { MapRepo.getInstance(requireContext()) }

    private var mapId = 0L
    private var map: Map? = null

    private var onDone: () -> Unit = {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mapId = requireArguments().getLong("mapId")
    }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentMapsPerspectiveBinding {
        return FragmentMapsPerspectiveBinding.inflate(layoutInflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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
        inBackground {
            withContext(Dispatchers.IO) {
                map = mapRepo.getMap(mapId)
            }
            withContext(Dispatchers.Main) {
                map?.let {
                    onMapLoad(it)
                }
            }
        }
    }


    private fun onMapLoad(map: Map) {
        this.map = map
        binding.perspective.mapRotation = map.rotation.toFloat()
        binding.perspective.setImage(map.filename)
        binding.nextButton.isInvisible = false
    }

    fun setOnCompleteListener(listener: () -> Unit) {
        onDone = listener
    }

    private suspend fun next() {
        val map = map ?: return
        val percentBounds = binding.perspective.getPercentBounds() ?: return
        val loading = withContext(Dispatchers.Main) {
            Alerts.loading(requireContext(), getString(R.string.saving))
        }
        withContext(Dispatchers.IO) {
            if (binding.perspective.hasChanges) {
                val file = LocalFiles.getFile(requireContext(), map.filename, false)
                val bitmap = BitmapFactory.decodeFile(file.path)
                val bounds =
                    percentBounds.toPixelBounds(bitmap.width.toFloat(), bitmap.height.toFloat())
                val warped = bitmap.fixPerspective(bounds)
                try {
                    @Suppress("BlockingMethodInNonBlockingContext")
                    FileOutputStream(file).use { out ->
                        ImageSaver().save(warped, out)
                    }
                } catch (e: IOException) {
                    withContext(Dispatchers.Main) {
                        loading.dismiss()
                    }
                    return@withContext
                } finally {
                    bitmap.recycle()
                    warped.recycle()
                }
            }
            mapRepo.addMap(map.copy(warped = true))
        }

        withContext(Dispatchers.Main) {
            loading.dismiss()
            onDone.invoke()
        }
    }
}