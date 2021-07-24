package com.kylecorry.trail_sense.navigation.ui

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.viewbinding.ViewBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentLocationBinding
import com.kylecorry.trail_sense.navigation.infrastructure.share.LocationGeoSender
import com.kylecorry.trail_sense.navigation.infrastructure.share.LocationSharesheet
import com.kylecorry.trail_sense.shared.FormatServiceV2
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trailsensecore.domain.geo.GeoService
import com.kylecorry.trailsensecore.domain.units.Distance
import com.kylecorry.trailsensecore.infrastructure.sensors.gps.IGPS
import com.kylecorry.trailsensecore.infrastructure.time.Intervalometer
import java.time.Duration
import java.time.Instant

// TODO: Make bottom sheet dialog utils like alert dialogs
class LocationBottomSheet : BoundBottomSheetDialogFragment<FragmentLocationBinding>() {

    var gps: IGPS? = null

    private val formatService by lazy { FormatServiceV2(requireContext()) }
    private val prefs by lazy { UserPreferences(requireContext()) }
    private val geoService = GeoService()
    private val intervalometer = Intervalometer {
        updateUI()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.locationShare.setOnClickListener {
            val locationSender = LocationSharesheet(requireContext())
            gps?.location?.let {
                locationSender.send(it)
            }
        }

        binding.locationMap.setOnClickListener {
            val locationSender = LocationGeoSender(requireContext())
            gps?.location?.let {
                locationSender.send(it)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        intervalometer.interval(100)
    }

    override fun onPause() {
        super.onPause()
        intervalometer.stop()
    }

    private fun updateUI() {
        if (!isBound) {
            return
        }

        val gps = this.gps ?: return

        binding.location.text = formatService.formatLocation(gps.location)
        binding.satellites.text = getString(R.string.num_satellites, gps.satellites)

        val accuracy = gps.horizontalAccuracy
        binding.accuracy.isVisible = accuracy != null
        if (accuracy != null) {
            val accuracyStr = formatService.formatDistance(
                Distance.meters(accuracy).convertTo(prefs.baseDistanceUnits)
            )
            binding.accuracy.text = getString(R.string.accuracy_distance_format, accuracyStr)
        }

        binding.climateZone.text = formatService.formatRegion(geoService.getRegion(gps.location))

        // TODO: Show GPS time instead
        val timeAgo = Duration.between(gps.time, Instant.now())
        binding.time.text = getString(R.string.time_ago, formatService.formatDuration(timeAgo))
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
    }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentLocationBinding {
        return FragmentLocationBinding.inflate(layoutInflater, container, false)
    }

}

// TODO: Move this to TS Core
abstract class BoundBottomSheetDialogFragment<T : ViewBinding> : BottomSheetDialogFragment() {

    abstract fun generateBinding(layoutInflater: LayoutInflater, container: ViewGroup?): T

    protected val binding: T
        get() = _binding!!

    protected val isBound: Boolean
        get() = context != null && _binding != null

    private var _binding: T? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = generateBinding(inflater, container)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}