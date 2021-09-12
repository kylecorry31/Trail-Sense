package com.kylecorry.trail_sense.tools.tides.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.list.ListView
import com.kylecorry.andromeda.pickers.Pickers
import com.kylecorry.sol.science.oceanography.OceanographyService
import com.kylecorry.sol.science.oceanography.TidalRange
import com.kylecorry.sol.science.oceanography.TideType
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentTideBinding
import com.kylecorry.trail_sense.databinding.ListItemTideBinding
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.tools.tides.domain.TideEntity
import com.kylecorry.trail_sense.tools.tides.domain.TideLoaderFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime


class TidesFragment : BoundFragment<FragmentTideBinding>() {

    private val oceanService = OceanographyService()
    private val formatService by lazy { FormatService(requireContext()) }
    private var displayDate = LocalDate.now()
    private lateinit var tideList: ListView<Pair<String, String>>
    private var referenceTide: TideEntity? = null

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentTideBinding {
        return FragmentTideBinding.inflate(layoutInflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        tideList = ListView(binding.tideList, R.layout.list_item_tide) { itemView, tide ->
            val tideBinding = ListItemTideBinding.bind(itemView)
            tideBinding.tideType.text = tide.first
            tideBinding.tideTime.text = tide.second
        }
        binding.tideCalibration.setOnClickListener {
            findNavController().navigate(R.id.action_tides_to_tideList)
        }
        binding.tideListDatePicker.setOnClickListener {
            Pickers.date(requireContext(), displayDate) {
                if (it != null) {
                    displayDate = it
                    onUpdate()
                }
            }
        }

        binding.loading.isVisible = true
        runInBackground {
            val loader = TideLoaderFactory().getTideLoader(requireContext())
            referenceTide = loader.getReferenceTide()
            withContext(Dispatchers.Main) {
                if (isBound) {
                    binding.loading.isVisible = false
                    if (referenceTide == null) {
                        Alerts.dialog(
                            requireContext(),
                            getString(R.string.no_tides),
                            getString(R.string.calibrate_new_tide)
                        ) { cancelled ->
                            if (!cancelled) {
                                findNavController().navigate(R.id.action_tides_to_tideList)
                            }
                        }
                    }
                    onUpdate()
                }
            }
        }

        scheduleUpdates(Duration.ofSeconds(15))
    }


    override fun onUpdate() {
        super.onUpdate()
        val reference = referenceTide?.reference ?: return
        binding.loading.isVisible = false
        binding.tideListDateText.text = formatService.formatRelativeDate(displayDate)
        binding.tideClock.time = ZonedDateTime.now()
        val next = oceanService.getNextTide(reference)
        binding.tideClock.nextTide = next
        binding.tideLocation.text = referenceTide?.name
            ?: if (referenceTide?.coordinate != null) formatService.formatLocation(referenceTide!!.coordinate!!) else getString(
                android.R.string.untitled
            )
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