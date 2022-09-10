package com.kylecorry.trail_sense.navigation.beacons.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.topics.asLiveData
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.pickers.Pickers
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentBeaconDetailsBinding
import com.kylecorry.trail_sense.navigation.beacons.domain.Beacon
import com.kylecorry.trail_sense.navigation.beacons.infrastructure.persistence.BeaconEntity
import com.kylecorry.trail_sense.navigation.beacons.infrastructure.persistence.BeaconRepo
import com.kylecorry.trail_sense.navigation.beacons.infrastructure.share.BeaconSender
import com.kylecorry.trail_sense.shared.*
import com.kylecorry.trail_sense.shared.extensions.inBackground
import com.kylecorry.trail_sense.shared.sensors.SensorService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BeaconDetailsFragment : BoundFragment<FragmentBeaconDetailsBinding>() {

    private val beaconRepo by lazy { BeaconRepo.getInstance(requireContext()) }
    private val formatService by lazy { FormatService(requireContext()) }
    private val prefs by lazy { UserPreferences(requireContext()) }
    private val gps by lazy { SensorService(requireContext()).getGPS(false) }

    private var beacon: Beacon? = null
    private var beaconId: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        beaconId = requireArguments().getLong("beacon_id")
    }

    private fun loadBeacon(id: Long) {
        inBackground {
            withContext(Dispatchers.IO) {
                beacon = beaconRepo.getBeacon(id)?.toBeacon()
            }

            withContext(Dispatchers.Main) {
                beacon?.apply {
                    binding.beaconTitle.title.text = this.name
                    binding.beaconTitle.subtitle.text = formatService.formatLocation(this.coordinate)

                    if (this.elevation != null) {
                        val d = Distance.meters(this.elevation).convertTo(prefs.baseDistanceUnits)
                        binding.beaconAltitude.title =
                            formatService.formatDistance(d, Units.getDecimalPlaces(d.units), false)
                    } else {
                        binding.beaconAltitude.isVisible = false
                    }

                    if (!this.comment.isNullOrEmpty()) {
                        binding.commentText.text = this.comment
                    }

                    binding.navigateBtn.setOnClickListener {
                        val bundle = bundleOf("destination" to id)
                        findNavController().navigate(
                            R.id.action_beaconDetailsFragment_to_action_navigation,
                            bundle
                        )
                    }

                    binding.editBtn.isVisible = !temporary

                    binding.editBtn.setOnClickListener {
                        val bundle = bundleOf("edit_beacon" to id)
                        findNavController().navigate(
                            R.id.action_beacon_details_to_beacon_edit,
                            bundle
                        )
                    }

                    binding.beaconTitle.rightButton.setOnClickListener {
                        Pickers.menu(
                            it,
                            listOf(getString(R.string.share_ellipsis), getString(R.string.delete))
                        ) { idx ->
                            when (idx) {
                                0 -> {
                                    BeaconSender(this@BeaconDetailsFragment).send(this)
                                }
                                1 -> {
                                    Alerts.dialog(
                                        requireContext(),
                                        getString(R.string.delete),
                                        name
                                    ) { cancelled ->
                                        if (!cancelled) {
                                            inBackground {
                                                withContext(Dispatchers.IO) {
                                                    beaconRepo.deleteBeacon(BeaconEntity.from(this@apply))
                                                }

                                                withContext(Dispatchers.Main) {
                                                    findNavController().navigateUp()
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            true
                        }
                    }
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.beaconTitle.rightButton.flatten()
        if (beaconId != null) {
            loadBeacon(beaconId!!)
        }
        gps.asLiveData().observe(viewLifecycleOwner) {
            val beacon = beacon
            if (isBound && beacon != null) {
                val distance = Distance.meters(beacon.coordinate.distanceTo(gps.location))
                    .convertTo(prefs.baseDistanceUnits).toRelativeDistance()
                binding.beaconDistance.title =
                    formatService.formatDistance(
                        distance,
                        Units.getDecimalPlaces(distance.units),
                        false
                    )
            }
        }
    }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentBeaconDetailsBinding {
        return FragmentBeaconDetailsBinding.inflate(layoutInflater, container, false)
    }
}

