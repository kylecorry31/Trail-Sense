package com.kylecorry.trail_sense.weather.ui.clouds

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.core.tryOrNothing
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.list.ListView
import com.kylecorry.sol.science.meteorology.clouds.CloudGenus
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentCloudsBinding
import com.kylecorry.trail_sense.databinding.ListItemCloudBinding
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.weather.infrastructure.clouds.CloudRepo

class CloudFragment : BoundFragment<FragmentCloudsBinding>() {

    private val cloudRepo by lazy { CloudRepo(requireContext()) }
    private lateinit var listView: ListView<CloudGenus>

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.cloudListTitle.subtitle.isVisible = false

        listView = ListView(binding.cloudList, R.layout.list_item_cloud) { itemView, item ->
            val itemBinding = ListItemCloudBinding.bind(itemView)
            CloudListItem(item, cloudRepo).display(itemBinding)
        }

        listView.addLineSeparator()
        listView.setData(cloudRepo.getClouds().sortedByDescending { it.level })

        CustomUiUtils.setButtonState(binding.cloudListTitle.rightQuickAction, false)
        binding.cloudListTitle.rightQuickAction.isVisible = UserPreferences(requireContext()).weather.showCloudScanner
        binding.cloudListTitle.rightQuickAction.setOnClickListener {
            tryOrNothing {
                findNavController().navigate(R.id.action_cloud_to_cloud_scan)
            }
        }
    }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentCloudsBinding {
        return FragmentCloudsBinding.inflate(layoutInflater, container, false)
    }


}