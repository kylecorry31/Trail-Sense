package com.kylecorry.trail_sense.navigation.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentBeaconDetailsBinding
import com.kylecorry.trail_sense.navigation.infrastructure.database.BeaconRepo
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trailsensecore.domain.navigation.Beacon

class BeaconDetailsFragment : Fragment() {

    private val beaconRepo by lazy { BeaconRepo.getInstance(requireContext()) }
    private val formatService by lazy { FormatService(requireContext()) }

    private var _binding: FragmentBeaconDetailsBinding? = null
    private val binding get() = _binding!!

    private var beacon: Beacon? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val id = requireArguments().getLong("beacon_id")
        beacon = beaconRepo.get(id)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentBeaconDetailsBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        beacon?.apply {
            binding.beaconName.text = this.name
            binding.locationText.text = formatService.formatLocation(this.coordinate)

            if (this.elevation != null) {
                binding.altitudeText.text = formatService.formatSmallDistance(this.elevation!!)
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
                findNavController().navigate(R.id.action_beaconDetailsFragment_to_action_navigation, bundle)
            }

        }
    }
}

