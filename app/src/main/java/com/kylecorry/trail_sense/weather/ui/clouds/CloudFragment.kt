package com.kylecorry.trail_sense.weather.ui.clouds

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.alerts.CoroutineAlerts
import com.kylecorry.andromeda.core.tryOrNothing
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentCloudsBinding
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.extensions.inBackground
import com.kylecorry.trail_sense.shared.extensions.onMain
import com.kylecorry.trail_sense.weather.infrastructure.clouds.CloudDetailsService
import com.kylecorry.trail_sense.weather.infrastructure.persistence.CloudObservation
import com.kylecorry.trail_sense.weather.infrastructure.persistence.CloudRepo

class CloudFragment : BoundFragment<FragmentCloudsBinding>() {

    private val mapper by lazy { CloudReadingListItemMapper(requireContext(), this::handleAction) }
    private val repo by lazy { CloudRepo.getInstance(requireContext()) }
    private val cloudDetailsService by lazy { CloudDetailsService(requireContext()) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        repo.getAllLive().observe(viewLifecycleOwner) {
            binding.cloudList.setItems(it.sortedByDescending { it.time }, mapper)
        }

        CustomUiUtils.setButtonState(binding.cloudListTitle.rightButton, false)
        // TODO: Add FAB menu
        binding.cloudList.emptyView = binding.cloudEmptyText
        binding.cloudListTitle.rightButton.setOnClickListener {
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

    private fun handleAction(action: CloudReadingAction, reading: Reading<CloudObservation>) {
        when (action) {
            CloudReadingAction.Delete -> delete(reading)
        }
    }

    private fun delete(reading: Reading<CloudObservation>) {
        inBackground {
            val cancelled = onMain {
                CoroutineAlerts.dialog(
                    requireContext(),
                    getString(R.string.delete),
                    cloudDetailsService.getCloudName(reading.value.genus)
                )
            }
            if (!cancelled) {
                repo.delete(reading)
            }
        }
    }

}