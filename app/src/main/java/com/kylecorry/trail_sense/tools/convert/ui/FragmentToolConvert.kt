package com.kylecorry.trail_sense.tools.convert.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.tabs.TabLayoutMediator
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentToolConvertBinding
import com.kylecorry.trailsensecore.infrastructure.view.BoundFragment

class FragmentToolConvert : BoundFragment<FragmentToolConvertBinding>() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val convertTools = listOf(
            FragmentToolCoordinateConvert(),
            FragmentDistanceConverter(),
            FragmentTemperatureConverter(),
            FragmentVolumeConverter(),
            FragmentWeightConverter()
        )
        val convertNames = listOf(
            getString(R.string.coordinates_tab),
            getString(R.string.distance),
            getString(R.string.temperature),
            getString(R.string.volume),
            getString(R.string.weight)
        )
        binding.convertViewpager.adapter = CustomViewPagerAdapter(this, convertTools)

        TabLayoutMediator(binding.tabLayoutConvert, binding.convertViewpager) { tab, position ->
            tab.text = convertNames[position]
        }.attach()
    }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentToolConvertBinding {
        return FragmentToolConvertBinding.inflate(layoutInflater, container, false)
    }
}