package com.kylecorry.trail_sense.tools.text

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.tabs.TabLayoutMediator
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentTabsBinding
import com.kylecorry.trail_sense.shared.views.CustomViewPagerAdapter

class TextTransmissionFragment : BoundFragment<FragmentTabsBinding>() {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val textFragment = SendTextFragment()
        val tabs = listOf(
            textFragment,
            RetrieveTextFragment()
        )
        val names = listOf(
            getString(R.string.share_action_send),
            getString(R.string.scan)
        )

        binding.viewpager.adapter = CustomViewPagerAdapter(this, tabs)

        TabLayoutMediator(binding.tabs, binding.viewpager) { tab, position ->
            tab.text = names[position]
        }.attach()
    }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentTabsBinding {
        return FragmentTabsBinding.inflate(layoutInflater, container, false)
    }
}