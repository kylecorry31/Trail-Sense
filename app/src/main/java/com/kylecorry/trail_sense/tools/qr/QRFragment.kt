package com.kylecorry.trail_sense.tools.qr

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.tabs.TabLayoutMediator
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentTabsBinding
import com.kylecorry.trail_sense.shared.views.CustomViewPagerAdapter

class QRFragment : BoundFragment<FragmentTabsBinding>() {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // TODO: Check arguments for send text, and switch to send tab if needed
        val textFragment = SendTextFragment()
        val tabs = listOf(
            RetrieveTextFragment(),
            textFragment
        )
        val names = listOf(
            getString(R.string.scan),
            getString(R.string.share_action_send),
        )

        binding.viewpager.isUserInputEnabled = false
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