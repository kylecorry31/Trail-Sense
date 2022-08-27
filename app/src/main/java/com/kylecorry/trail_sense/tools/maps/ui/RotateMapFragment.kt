package com.kylecorry.trail_sense.tools.maps.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isInvisible
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.trail_sense.databinding.FragmentMapsRotateBinding
import com.kylecorry.trail_sense.shared.extensions.inBackground
import com.kylecorry.trail_sense.tools.maps.domain.Map
import com.kylecorry.trail_sense.tools.maps.infrastructure.MapRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
        binding.rotateView.angle = map.rotation.toFloat()
        binding.rotateView.setImage(map.filename)
        binding.nextButton.isInvisible = false
    }

    fun setOnCompleteListener(listener: () -> Unit) {
        onDone = listener
    }

    private suspend fun next() {
        val map = map ?: return
        val rotation = binding.rotateView.angle
        withContext(Dispatchers.IO) {
            mapRepo.addMap(map.copy(rotated = true, rotation = rotation.toInt()))
        }

        withContext(Dispatchers.Main) {
            onDone.invoke()
        }
    }
}