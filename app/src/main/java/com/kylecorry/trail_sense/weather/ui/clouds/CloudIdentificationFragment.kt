package com.kylecorry.trail_sense.weather.ui.clouds

import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.tabs.TabLayoutMediator
import com.kylecorry.andromeda.core.bitmap.BitmapUtils.resizeExact
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentTabsBinding
import com.kylecorry.trail_sense.shared.views.CustomViewPagerAdapter
import com.kylecorry.trail_sense.weather.domain.clouds.classification.TextureCloudClassifier

class CloudIdentificationFragment : BoundFragment<FragmentTabsBinding>() {

    private var image: Bitmap? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val camera = CloudCameraFragment()
        val results = CloudResultsFragment()

        camera.setOnImageListener {
            image?.recycle()
            image = it.resizeExact(TextureCloudClassifier.IMAGE_SIZE, TextureCloudClassifier.IMAGE_SIZE)
            it.recycle()
            image?.let {
                results.setImage(it)
            }
            binding.viewpager.setCurrentItem(1, true)
        }

        binding.viewpager.isUserInputEnabled = false

        val fragments = listOf(
            camera,
            results
        )
        val names = listOf(
            getString(R.string.capture),
            getString(R.string.results)
        )
        binding.viewpager.adapter = CustomViewPagerAdapter(this, fragments)

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

    override fun onDestroy() {
        super.onDestroy()
        image?.recycle()
    }
}