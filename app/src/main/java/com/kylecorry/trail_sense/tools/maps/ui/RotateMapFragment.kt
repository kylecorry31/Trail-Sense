package com.kylecorry.trail_sense.tools.maps.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isInvisible
import androidx.lifecycle.lifecycleScope
import com.kylecorry.andromeda.files.LocalFiles
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.trail_sense.databinding.FragmentMapsRotateBinding
import com.kylecorry.trail_sense.tools.maps.domain.Map
import com.kylecorry.trail_sense.tools.maps.infrastructure.MapRepo
import com.kylecorry.trail_sense.tools.maps.infrastructure.rotate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import java.io.IOException

class RotateMapFragment : BoundFragment<FragmentMapsRotateBinding>() {

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
    ): FragmentMapsRotateBinding {
        return FragmentMapsRotateBinding.inflate(layoutInflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rotateClockwise.setOnClickListener {
            binding.rotateView.angle += 90
        }

        binding.rotateCounterClockwise.setOnClickListener {
            binding.rotateView.angle -= 90
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


    private fun onMapLoad(map: Map) {
        this.map = map
        binding.rotateView.setImage(map.filename)
        binding.nextButton.isInvisible = false
    }

    fun setOnCompleteListener(listener: () -> Unit) {
        onDone = listener
    }

    private suspend fun next() {
        // TODO: Show loading indicator
        val map = map ?: return
        val rotation = binding.rotateView.angle
        withContext(Dispatchers.IO) {
            if (rotation != 0f) {
                val file = LocalFiles.getFile(requireContext(), map.filename, false)
                val bitmap = BitmapFactory.decodeFile(file.path)
                val rotated = bitmap.rotate(rotation)

                try {
                    @Suppress("BlockingMethodInNonBlockingContext")
                    FileOutputStream(file).use { out ->
                        rotated.compress(Bitmap.CompressFormat.JPEG, 100, out)
                    }
                } catch (e: IOException) {
                    // TODO: Fix this
                    return@withContext
                } finally {
                    bitmap.recycle()
                    rotated.recycle()
                }
            }

            mapRepo.addMap(map.copy(rotated = true))
        }

        withContext(Dispatchers.Main) {
            onDone.invoke()
        }
    }
}