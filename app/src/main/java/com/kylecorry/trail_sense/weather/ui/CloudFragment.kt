package com.kylecorry.trail_sense.weather.ui

import android.Manifest
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.camera.Camera
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.list.ListView
import com.kylecorry.sol.science.meteorology.clouds.CloudType
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentCloudsBinding
import com.kylecorry.trail_sense.databinding.ListItemCloudBinding
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.UserPreferences
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

        CustomUiUtils.setButtonState(binding.cloudScanBtn, false)
        binding.cloudScanBtn.isVisible = UserPreferences(requireContext()).weather.showCloudScanner
        binding.cloudScanBtn.setOnClickListener {
            requestPermissions(
                listOf(Manifest.permission.CAMERA)
            ) {
                if (Camera.isAvailable(requireContext())) {
                    findNavController().navigate(R.id.action_cloud_to_cloud_scan)
                } else {
                    Alerts.toast(
                        requireContext(),
                        getString(R.string.camera_permission_denied),
                        short = false
                    )
                }
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