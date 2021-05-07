package com.kylecorry.trail_sense.tools.convert.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager.widget.ViewPager
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentToolConvertBinding
import com.kylecorry.trail_sense.tools.coordinateconvert.ui.FragmentToolCoordinateConvert
import com.kylecorry.trail_sense.tools.distanceconvert.ui.FragmentDistanceConverter
import com.kylecorry.trailsensecore.infrastructure.view.BoundFragment

class FragmentToolConvert : BoundFragment<FragmentToolConvertBinding>() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val convertTools = ArrayList<Fragment>()
        convertTools.add(FragmentToolCoordinateConvert())
        convertTools.add(FragmentDistanceConverter())
        val convertToolsNames = ArrayList<Int>()
        convertToolsNames.add(R.string.distance_hint)
        convertToolsNames.add(R.string.coordinates_tab)

        val viewPager: ViewPager = binding.convertViewpager
        viewPager.adapter = ConvertViewPagerAdapter(childFragmentManager, convertTools, convertToolsNames, requireContext())

        val tabLayout = binding.tabLayoutConvert
        tabLayout.setupWithViewPager(viewPager)
    }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentToolConvertBinding {
        return FragmentToolConvertBinding.inflate(layoutInflater, container, false)
    }
}