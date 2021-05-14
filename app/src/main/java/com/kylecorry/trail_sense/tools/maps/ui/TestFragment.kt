package com.kylecorry.trail_sense.tools.maps.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.kylecorry.trail_sense.databinding.TestFragmentBinding
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trailsensecore.infrastructure.sensors.asLiveData
import com.kylecorry.trailsensecore.infrastructure.view.BoundFragment

class TestFragment : BoundFragment<TestFragmentBinding>() {

    private val gps by lazy { SensorService(requireContext()).getGPS() }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): TestFragmentBinding {
        return TestFragmentBinding.inflate(layoutInflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        gps.asLiveData().observe(viewLifecycleOwner, {
            binding.heightMap.setMyLocation(gps.location, gps.altitude)
        })
    }

}