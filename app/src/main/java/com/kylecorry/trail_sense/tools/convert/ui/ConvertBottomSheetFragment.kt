package com.kylecorry.trail_sense.tools.convert.ui

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.tabs.TabLayoutMediator
import com.kylecorry.andromeda.fragments.BoundBottomSheetDialogFragment
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentTabsBinding
import com.kylecorry.trail_sense.shared.views.CustomViewPagerAdapter

class ConvertBottomSheetFragment :
    BoundBottomSheetDialogFragment<FragmentTabsBinding>() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val convertTools = listOf(
            FragmentToolCoordinateConvert(),
            FragmentDistanceConverter(),
            FragmentTemperatureConverter()
        )
        val convertNames = listOf(
            getString(R.string.coordinates_tab),
            getString(R.string.distance),
            getString(R.string.temperature)
        )
        binding.viewpager.adapter = CustomViewPagerAdapter(this, convertTools)

        binding.tabs.backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)

        TabLayoutMediator(binding.tabs, binding.viewpager) { tab, position ->
            tab.text = convertNames[position]
        }.attach()
    }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentTabsBinding {
        return FragmentTabsBinding.inflate(layoutInflater, container, false)
    }
}
