package com.kylecorry.trail_sense.tools.tides.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.time.Timer
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.list.ListView
import com.kylecorry.andromeda.pickers.Pickers
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentTideBinding
import com.kylecorry.trail_sense.databinding.ListItemTideBinding
import com.kylecorry.trail_sense.shared.FormatServiceV2
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.tools.tides.domain.TideEntity
import com.kylecorry.trail_sense.tools.tides.infrastructure.persistence.TideRepo
import com.kylecorry.trailsensecore.domain.oceanography.OceanographyService
import com.kylecorry.trailsensecore.domain.oceanography.TidalRange
import com.kylecorry.trailsensecore.domain.oceanography.TideType
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime


class TidesFragment : BoundFragment<FragmentTideBinding>() {

    private val oceanService = OceanographyService()
    private val formatService by lazy { FormatServiceV2(requireContext()) }
    private val prefs by lazy { UserPreferences(requireContext()) }
    private var displayDate = LocalDate.now()
    private lateinit var tideList: ListView<Pair<String, String>>
    private val intervalometer = Timer {
        update()
    }
    private val tideRepo by lazy { TideRepo.getInstance(requireContext()) }
    private var referenceTide: TideEntity? = null

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentTideBinding {
        return FragmentTideBinding.inflate(layoutInflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        tideList = ListView(binding.tideList, R.layout.list_item_tide) { view, tide ->
            val tideView = ListItemTideBinding.bind(view)
            tideView.tideType.text = tide.first
            tideView.tideTime.text = tide.second
        }
        binding.tideCalibration.setOnClickListener {
            findNavController().navigate(R.id.action_tides_to_tideList)
        }
        binding.tideListDatePicker.setOnClickListener {
            Pickers.date(requireContext(), displayDate){
                if (it != null){
                    displayDate = it
                    update()
                }
            }
        }

        tideRepo.getTides().observe(viewLifecycleOwner, {
            // TODO: Allow auto tide choosing based on location
            val lastTide = prefs.lastTide
            referenceTide = it.firstOrNull { tide -> tide.id == lastTide } ?: it.firstOrNull()
            if (referenceTide == null) {
                Alerts.dialog(requireContext(), getString(R.string.no_tides), getString(R.string.calibrate_new_tide)){ cancelled ->
                    if (!cancelled){
                        findNavController().navigate(R.id.action_tides_to_tideList)
                    }
                }
            }
            update()
        })
    }

    override fun onResume() {
        super.onResume()
        // TODO: Add check if reference is too old
        intervalometer.interval(Duration.ofSeconds(15))
    }

    override fun onPause() {
        super.onPause()
        intervalometer.stop()
    }

    private fun update() {
        context ?: return
        val reference = referenceTide?.reference ?: return
        binding.tideListDateText.text = formatService.formatRelativeDate(displayDate)
        binding.tideClock.time = ZonedDateTime.now()
        val next = oceanService.getNextTide(reference)
        binding.tideClock.nextTide = next
        binding.tideLocation.text = referenceTide?.name ?: if (referenceTide?.coordinate != null) formatService.formatLocation(referenceTide!!.coordinate!!) else getString(R.string.untitled_tide)
        binding.tideHeight.text = getTideTypeName(oceanService.getTideType(reference))
        val tides = oceanService.getTides(reference, displayDate)
        val tideStrings = tides.map {
            val type = if (it.type == TideType.High) {
                getString(R.string.high_tide)
            } else {
                getString(R.string.low_tide)
            }
            val time = formatService.formatTime(it.time.toLocalTime(), false)
            type to time
        }.toMutableList()
        tideStrings.add(
            getString(R.string.tidal_range) to getTidalRangeName(
                oceanService.getTidalRange(
                    displayDate.atStartOfDay(
                        ZoneId.systemDefault()
                    )
                )
            )
        )
        tideList.setData(tideStrings)
    }

    private fun getTidalRangeName(range: TidalRange): String {
        return when (range) {
            TidalRange.Neap -> getString(R.string.tide_neap)
            TidalRange.Spring -> getString(R.string.tide_spring)
            TidalRange.Normal -> getString(R.string.tide_normal)
        }
    }

    private fun getTideTypeName(tideType: TideType): String {
        return when (tideType) {
            TideType.High -> getString(R.string.high_tide)
            TideType.Low -> getString(R.string.low_tide)
            TideType.Half -> getString(R.string.half_tide)
        }
    }

}