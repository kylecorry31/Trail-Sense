package com.kylecorry.trail_sense.weather.ui

import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.tabs.TabLayoutMediator
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentTabsBinding
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.views.CustomViewPagerAdapter
import com.kylecorry.trail_sense.tools.maps.infrastructure.resize

class CloudIdentificationFragment : BoundFragment<FragmentTabsBinding>() {

    private var image: Bitmap? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        CustomUiUtils.disclaimer(
            requireContext(),
            getString(R.string.experimental),
            "Cloud identification is experimental and is using a very simple (and error prone) method of identifying clouds.\n\nI'm currently looking for feedback on the calibration, crashes, and ease of use only. There are already plans to improve the result accuracy in a future release.",
            getString(R.string.disclaimer_experimental_clouds_key)
        )

        val camera = CloudCameraFragment()
        val calibration = CloudCalibrationFragment()
        val results = CloudResultsFragment()

        camera.setOnImageListener {
            image?.recycle()
            image = it.resize(500, 500)
            it.recycle()
            image?.let {
                calibration.setImage(it)
            }
            binding.viewpager.setCurrentItem(1, true)
        }

        calibration.setOnResultsListener {
            results.setResult(it)
        }

        calibration.setOnDoneListener {
            binding.viewpager.setCurrentItem(2, true)
        }

        binding.viewpager.isUserInputEnabled = false

        val fragments = listOf(
            camera,
            calibration,
            results
        )
        val names = listOf(
            getString(R.string.capture),
            getString(R.string.calibrate),
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