package com.kylecorry.trail_sense.tools.convert.ui

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter

class ConvertViewPagerAdapter(fm: FragmentManager, val convertTools: ArrayList<Fragment>, val convertToolsNames: ArrayList<Int>, val context: Context) : FragmentStatePagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    override fun getCount(): Int  = convertTools.size

    override fun getItem(i: Int): Fragment {
        return convertTools[i]
    }

    override fun getPageTitle(position: Int): CharSequence {
        return context.getText(convertToolsNames[position])
    }
}
