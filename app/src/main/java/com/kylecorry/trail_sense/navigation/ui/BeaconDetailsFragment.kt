package com.kylecorry.trail_sense.navigation.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentBeaconDetailsBinding
import com.kylecorry.trail_sense.navigation.infrastructure.persistence.BeaconRepo
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trailsensecore.domain.navigation.Beacon
import com.kylecorry.trailsensecore.infrastructure.view.BoundFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BeaconDetailsFragment : BoundFragment<FragmentBeaconDetailsBinding>() {

    private val beaconRepo by lazy { BeaconRepo.getInstance(requireContext()) }
    private val formatService by lazy { FormatService(requireContext()) }

    private var beacon: Beacon? = null
    private var beaconId: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        beaconId = requireArguments().getLong("beacon_id")
    }

    private fun loadBeacon(id: Long) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                beacon = beaconRepo.getBeacon(id)?.toBeacon()
            }

            withContext(Dispatchers.Main) {
                beacon?.apply {
                    binding.beaconName.text = this.name
                    binding.locationText.text = formatService.formatLocation(this.coordinate)

                    if (this.elevation != null) {
                        binding.altitudeText.text =
                            formatService.formatSmallDistance(this.elevation!!)
                    } else {
                        binding.altitudeText.visibility = View.GONE
                        binding.altitudeIcon.visibility = View.GONE
                    }

                    if (!this.comment.isNullOrEmpty()) {
                        binding.commentText.text = this.comment
                    } else {
                        binding.commentText.visibility = View.GONE
                        binding.commentIcon.visibility = View.GONE
                    }

                    binding.navigateBtn.setOnClickListener {
                        val bundle = bundleOf("destination" to (beacon?.id ?: 0L))
                        findNavController().navigate(
                            R.id.action_beaconDetailsFragment_to_action_navigation,
                            bundle
                        )
                    }

                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (beaconId != null) {
            loadBeacon(beaconId!!)
        }
    }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentBeaconDetailsBinding {
        return FragmentBeaconDetailsBinding.inflate(layoutInflater, container, false)
    }
}

