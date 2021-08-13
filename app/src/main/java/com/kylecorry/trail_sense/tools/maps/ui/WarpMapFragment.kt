package com.kylecorry.trail_sense.tools.maps.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isInvisible
import androidx.lifecycle.lifecycleScope
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentMapsPerspectiveBinding
import com.kylecorry.trail_sense.tools.maps.infrastructure.MapRepo
import com.kylecorry.trail_sense.tools.maps.infrastructure.fixPerspective
import com.kylecorry.trail_sense.tools.maps.domain.Map
import com.kylecorry.trailsensecore.infrastructure.persistence.LocalFileService
import com.kylecorry.andromeda.fragments.BoundFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import java.io.IOException

class WarpMapFragment : BoundFragment<FragmentMapsPerspectiveBinding>() {

    private val mapRepo by lazy { MapRepo.getInstance(requireContext()) }
    private val localFileService by lazy { LocalFileService(requireContext()) }

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
                binding.perspectiveToggleBtn.text = getString(R.string.map_edit)
            } else {
                binding.perspectiveToggleBtn.text = getString(R.string.map_preview)
            }
        }

        binding.nextButton.setOnClickListener {
            lifecycleScope.launch {
                next()
            }
        }

        binding.nextButton.isInvisible = true
        lifecycleScope.launch {
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

    override fun onDestroyView() {
        super.onDestroyView()
        removeExclusionRects()
    }


    private fun onMapLoad(map: Map) {
        this.map = map
        binding.perspective.setImage(map.filename)
        binding.nextButton.isInvisible = false
        setExclusionRects()
    }

    fun setOnCompleteListener(listener: () -> Unit) {
        onDone = listener
    }

    private suspend fun next() {
        // TODO: Show loading indicator
        val map = map ?: return
        val percentBounds = binding.perspective.getPercentBounds() ?: return
        withContext(Dispatchers.IO) {
            val file = localFileService.getFile(map.filename, false)
            val bitmap = BitmapFactory.decodeFile(file.path)
            val bounds =
                percentBounds.toPixelBounds(bitmap.width.toFloat(), bitmap.height.toFloat())
            val warped = bitmap.fixPerspective(bounds)
            try {
                @Suppress("BlockingMethodInNonBlockingContext")
                FileOutputStream(file).use { out ->
                    warped.compress(Bitmap.CompressFormat.JPEG, 100, out)
                }
            } catch (e: IOException) {
                // TODO: Fix this
                return@withContext
            } finally {
                bitmap.recycle()
                warped.recycle()
            }

            mapRepo.addMap(map.copy(warped = true))
        }

        withContext(Dispatchers.Main) {
            onDone.invoke()
        }
    }

    private fun setExclusionRects() {
        if (Build.VERSION.SDK_INT < 29) return
        val exclusionRects = mutableListOf<Rect>()
        val rect = Rect(
            binding.perspective.left,
            binding.perspective.top,
            binding.perspective.right,
            binding.perspective.bottom
        )
        exclusionRects.add(rect)

        requireActivity().findViewById<View>(android.R.id.content).systemGestureExclusionRects =
            exclusionRects
    }

    private fun removeExclusionRects(){
        if (Build.VERSION.SDK_INT < 29) return
        requireActivity().findViewById<View>(android.R.id.content).systemGestureExclusionRects = mutableListOf()
    }
}