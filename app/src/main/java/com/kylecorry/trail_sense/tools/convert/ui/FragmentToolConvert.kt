package com.kylecorry.trail_sense.tools.convert.ui

import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.extensions.TrailSenseReactiveFragment
import com.kylecorry.trail_sense.shared.views.CustomViewPagerAdapter

class FragmentToolConvert : TrailSenseReactiveFragment(R.layout.fragment_tabs) {

    override fun update() {
        val tabsView = useView<TabLayout>(R.id.tabs)
        val viewPagerView = useView<ViewPager2>(R.id.viewpager)

        useEffect(tabsView, viewPagerView) {
            val convertTools = listOf(
                FragmentToolCoordinateConvert(),
                FragmentDistanceConverter(),
                FragmentTemperatureConverter(),
                FragmentVolumeConverter(),
                FragmentWeightConverter(),
                FragmentTimeConverter()
            )
            val convertNames = listOf(
                getString(R.string.coordinates_tab),
                getString(R.string.distance),
                getString(R.string.temperature),
                getString(R.string.volume),
                getString(R.string.weight),
                getString(R.string.time)
            )
            viewPagerView.adapter = CustomViewPagerAdapter(this, convertTools)

            TabLayoutMediator(tabsView, viewPagerView) { tab, position ->
                tab.text = convertNames[position]
            }.attach()
        }
    }
}
