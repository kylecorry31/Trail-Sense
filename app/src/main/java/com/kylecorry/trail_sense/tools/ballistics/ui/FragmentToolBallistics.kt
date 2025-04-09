package com.kylecorry.trail_sense.tools.ballistics.ui

import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.extensions.TrailSenseReactiveFragment
import com.kylecorry.trail_sense.shared.views.CustomViewPagerAdapter

class FragmentToolBallistics : TrailSenseReactiveFragment(R.layout.fragment_tabs) {


    override fun update() {
        val tabsView = useView<TabLayout>(R.id.tabs)
        val viewPagerView = useView<ViewPager2>(R.id.viewpager)

        val pages = useMemo {
            listOf(
                FragmentScopeAdjustment(),
                FragmentBallisticsCalculator()
            )
        }

        val pageNames = useMemo {
            listOf(
                getString(R.string.scope),
                getString(R.string.ballistics)
            )
        }

        useEffect(tabsView, viewPagerView) {
            viewPagerView.adapter = CustomViewPagerAdapter(this, pages)
            // Disable view pager scroll
            viewPagerView.isUserInputEnabled = false
            TabLayoutMediator(tabsView, viewPagerView) { tab, position ->
                tab.text = pageNames[position]
            }.attach()
        }
    }
}