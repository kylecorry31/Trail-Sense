package com.kylecorry.trail_sense.tools.convert.ui

import android.content.res.ColorStateList
import android.graphics.Color
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.extensions.TrailSenseReactiveBottomSheetFragment
import com.kylecorry.trail_sense.shared.views.CustomViewPagerAdapter

class ConvertBottomSheetFragment :
    TrailSenseReactiveBottomSheetFragment(R.layout.fragment_tabs) {

    override fun update() {
        val viewpager = useView<ViewPager2>(R.id.viewpager)
        val tabs = useView<TabLayout>(R.id.tabs)

        val convertTools = useMemo {
            listOf(
                FragmentToolCoordinateConvert(),
                FragmentDistanceConverter(),
                FragmentTemperatureConverter()
            )
        }

        val convertNames = useMemo {
            listOf(
                getString(R.string.coordinates_tab),
                getString(R.string.distance),
                getString(R.string.temperature)
            )
        }

        // View effects
        useEffect(tabs, viewpager, convertNames) {
            viewpager.adapter = CustomViewPagerAdapter(this, convertTools)

            tabs.backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)

            TabLayoutMediator(tabs, viewpager) { tab, position ->
                tab.text = convertNames[position]
            }.attach()
        }
    }
}
