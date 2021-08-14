package com.kylecorry.trail_sense.navigation.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.kylecorry.andromeda.clipboard.Clipboard
import com.kylecorry.andromeda.fragments.BoundBottomSheetDialogFragment
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentLocationBinding
import com.kylecorry.trail_sense.databinding.ListItemPlainMenuBinding
import com.kylecorry.trail_sense.navigation.infrastructure.share.LocationCopy
import com.kylecorry.trail_sense.navigation.infrastructure.share.LocationGeoSender
import com.kylecorry.trail_sense.navigation.infrastructure.share.LocationSharesheet
import com.kylecorry.trail_sense.shared.FormatServiceV2
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trailsensecore.domain.geo.CoordinateFormat
import com.kylecorry.trailsensecore.domain.geo.GeoService
import com.kylecorry.trailsensecore.domain.units.Distance
import com.kylecorry.trailsensecore.infrastructure.sensors.gps.IGPS
import com.kylecorry.andromeda.core.time.Timer
import com.kylecorry.trailsensecore.infrastructure.view.ListView
import java.time.Duration
import java.time.Instant

class LocationBottomSheet : BoundBottomSheetDialogFragment<FragmentLocationBinding>() {

    var gps: IGPS? = null

    private val formatService by lazy { FormatServiceV2(requireContext()) }
    private val prefs by lazy { UserPreferences(requireContext()) }
    private val geoService = GeoService()
    private val clipboard by lazy { Clipboard(requireContext()) }

    private lateinit var coordinateList: ListView<CoordinateDisplay>

    private val intervalometer = Timer {
        updateUI()
    }

    private val listIntervalometer = Timer {
        updateList()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        coordinateList =
            ListView(
                binding.coordinateFormats,
                R.layout.list_item_plain_menu
            ) { itemView, coordinate ->
                val itemBinding = ListItemPlainMenuBinding.bind(itemView)
                itemBinding.title.text = coordinate.coordinate
                itemBinding.description.text = coordinate.format
                itemBinding.menuBtn.setImageResource(R.drawable.ic_copy)
                itemBinding.menuBtn.setOnClickListener {
                    clipboard.copy(
                        coordinate.coordinate,
                        getString(R.string.copied_to_clipboard_toast)
                    )
                }
            }
        coordinateList.addLineSeparator()

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

        binding.location.setOnLongClickListener {
            val locationSender = LocationCopy(requireContext(), clipboard)
            gps?.location?.let {
                locationSender.send(it)
            }
            true
        }
    }

    override fun onResume() {
        super.onResume()
        intervalometer.interval(100)
        listIntervalometer.interval(2000)
    }

    override fun onPause() {
        super.onPause()
        intervalometer.stop()
        listIntervalometer.stop()
    }

    private fun updateList() {
        if (!isBound) {
            return
        }

        val gps = this.gps ?: return

        val formats = CoordinateFormat.values().mapNotNull {
            val formatted = formatService.formatLocation(gps.location, it, false)
            if (formatted == "?") {
                return@mapNotNull null
            }
            val formatName = formatService.formatCoordinateType(it)
            CoordinateDisplay(formatted, formatName)
        }

        coordinateList.setData(formats)
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

        val timeAgo = Duration.between(gps.time, Instant.now())
        binding.time.text = getString(R.string.time_ago, formatService.formatDuration(timeAgo))
    }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentLocationBinding {
        return FragmentLocationBinding.inflate(layoutInflater, container, false)
    }

    private data class CoordinateDisplay(val coordinate: String, val format: String)
}