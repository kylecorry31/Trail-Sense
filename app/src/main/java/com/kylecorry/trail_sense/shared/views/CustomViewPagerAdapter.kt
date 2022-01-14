package com.kylecorry.trail_sense.shared.views

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class CustomViewPagerAdapter(parent: Fragment, val fragments: List<Fragment>): FragmentStateAdapter(parent) {
    override fun getItemCount(): Int {
        return fragments.size
    }

    override fun createFragment(position: Int): Fragment {
        return fragments[position]
    }
}