package com.kylecorry.trail_sense.tools.tides.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentTideBinding
import com.kylecorry.trail_sense.databinding.ListItemTideBinding
import com.kylecorry.trail_sense.shared.BoundFragment
import com.kylecorry.trail_sense.shared.FormatServiceV2
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.tools.tides.domain.TideEntity
import com.kylecorry.trail_sense.tools.tides.infrastructure.persistence.TideRepo
import com.kylecorry.trailsensecore.domain.oceanography.OceanographyService
import com.kylecorry.trailsensecore.domain.oceanography.TidalRange
import com.kylecorry.trailsensecore.domain.oceanography.TideType
import com.kylecorry.trailsensecore.infrastructure.system.UiUtils
import com.kylecorry.trailsensecore.infrastructure.time.Intervalometer
import com.kylecorry.trailsensecore.infrastructure.view.ListView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.*


class TidesFragment : BoundFragment<FragmentTideBinding>() {

    private val oceanService = OceanographyService()
    private val formatService by lazy { FormatServiceV2(requireContext()) }
    private val prefs by lazy { UserPreferences(requireContext()) }
    private var displayDate = LocalDate.now()
    private lateinit var tideList: ListView<Pair<String, String>>
    private val intervalometer = Intervalometer {
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
            calibrateTides()
        }
        binding.tideListDatePicker.setOnClickListener {
            UiUtils.pickDate(requireContext(), displayDate){
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
                calibrateTides()
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

    private fun calibrateTides() {
        var referenceTime = referenceTide?.reference?.toLocalTime()
        var referenceDate = referenceTide?.reference?.toLocalDate()
        val now = LocalDateTime.now()
        val dialogView = View.inflate(activity, R.layout.view_tide_time_picker, null)
        val alertDialog = UiUtils.alertViewWithCancel(
            requireContext(),
            getString(R.string.tide_calibration),
            dialogView,
            getString(R.string.dialog_ok),
            getString(R.string.dialog_cancel)
        ) { cancelled ->
            if (!cancelled) {
                if (referenceTime != null && referenceDate != null) {
                    val newReference =
                        ZonedDateTime.of(referenceDate!!, referenceTime!!, ZoneId.systemDefault())

                    val newTide = if (referenceTide == null) {
                        TideEntity(newReference.toInstant().toEpochMilli(), null, null, null)
                    } else {
                        referenceTide!!.copy(
                            referenceHighTide = newReference.toInstant().toEpochMilli()
                        ).also {
                            it.id = referenceTide!!.id
                        }
                    }

                    lifecycleScope.launch {
                        withContext(Dispatchers.IO) {
                            tideRepo.addTide(newTide)
                        }
                        withContext(Dispatchers.Main) {
                            update()
                        }
                    }

                }
            }
        }

        val referenceTimeTxt = dialogView.findViewById<TextView>(R.id.time)
        val referenceDateTxt = dialogView.findViewById<TextView>(R.id.date)

        referenceTimeTxt.text = if (referenceTime != null) {
            formatService.formatTime(referenceTime, false)
        } else {
            getString(R.string.time_not_set)
        }

        referenceDateTxt.text = if (referenceDate != null) {
            formatService.formatDate(
                ZonedDateTime.of(
                    referenceDate,
                    LocalTime.NOON,
                    ZoneId.systemDefault()
                ), false
            )
        } else {
            getString(R.string.date_not_set)
        }

        dialogView.findViewById<Button>(R.id.time_picker).setOnClickListener {
            UiUtils.pickTime(requireContext(), prefs.use24HourTime, referenceTime ?: now.toLocalTime()){
                if (it != null) {
                    referenceTime = it
                    referenceTimeTxt.text = formatService.formatTime(referenceTime!!, false)
                }
            }
        }
        dialogView.findViewById<Button>(R.id.date_picker).setOnClickListener {
            UiUtils.pickDate(requireContext(), referenceDate ?: now.toLocalDate()){
                if (it != null){
                    referenceDate = it
                    referenceDateTxt.text = formatService.formatDate(
                        ZonedDateTime.of(
                            referenceDate,
                            LocalTime.NOON,
                            ZoneId.systemDefault()
                        ), false
                    )
                }
            }
        }

        alertDialog.show()
    }

    private fun update() {
        context ?: return
        val reference = referenceTide?.reference ?: return
        binding.tideListDateText.text = formatService.formatRelativeDate(displayDate)
        binding.tideClock.time = ZonedDateTime.now()
        val next = oceanService.getNextTide(reference)
        binding.tideClock.nextTide = next
        binding.tideLocation.text = referenceTide?.name
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