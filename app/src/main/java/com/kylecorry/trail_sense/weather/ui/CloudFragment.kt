package com.kylecorry.trail_sense.weather.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.list.ListView
import com.kylecorry.sol.science.meteorology.clouds.CloudType
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentCloudsBinding
import com.kylecorry.trail_sense.databinding.ListItemCloudBinding
import com.kylecorry.trail_sense.weather.infrastructure.clouds.CloudRepo

class CloudFragment : BoundFragment<FragmentCloudsBinding>() {

    private val cloudRepo by lazy { CloudRepo(requireContext()) }
    private lateinit var listView: ListView<CloudType>

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        listView = ListView(binding.cloudList, R.layout.list_item_cloud) { itemView, item ->
            val itemBinding = ListItemCloudBinding.bind(itemView)
            CloudListItem(item, cloudRepo).display(itemBinding)
        }

        listView.addLineSeparator()
        listView.setData(cloudRepo.getClouds().sortedByDescending { it.height })
    }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentCloudsBinding {
        return FragmentCloudsBinding.inflate(layoutInflater, container, false)
    }


}