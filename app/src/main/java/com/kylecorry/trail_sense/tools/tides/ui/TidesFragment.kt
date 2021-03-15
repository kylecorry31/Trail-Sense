package com.kylecorry.trail_sense.tools.tides.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.TimePicker
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentTideBinding
import com.kylecorry.trail_sense.databinding.ListItemPlainBinding
import com.kylecorry.trail_sense.shared.BoundFragment
import com.kylecorry.trail_sense.shared.FormatServiceV2
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.tools.tides.domain.TideService
import com.kylecorry.trail_sense.tools.tides.domain.TideType
import com.kylecorry.trailsensecore.domain.astronomy.tides.Tide
import com.kylecorry.trailsensecore.infrastructure.system.UiUtils
import com.kylecorry.trailsensecore.infrastructure.time.Intervalometer
import com.kylecorry.trailsensecore.infrastructure.view.ListView
import java.time.*


class TidesFragment : BoundFragment<FragmentTideBinding>() {

    private val tideService = TideService()
    private val formatService by lazy { FormatServiceV2(requireContext()) }
    private val prefs by lazy { UserPreferences(requireContext()) }
    private var displayDate = LocalDate.now()
    private lateinit var tideList: ListView<com.kylecorry.trail_sense.tools.tides.domain.Tide>
    private val intervalometer = Intervalometer {
        update()
    }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentTideBinding {
        return FragmentTideBinding.inflate(layoutInflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        tideList = ListView(binding.tideList, R.layout.list_item_plain) { view, tide ->
            val tideView = ListItemPlainBinding.bind(view)
            tideView.title.text = if (tide.isHigh) {
                getString(R.string.high_tide)
            } else {
                getString(R.string.low_tide)
            }
            tideView.description.text = formatService.formatTime(tide.time.toLocalTime(), false)
        }
        binding.tideCalibration.setOnClickListener {
            calibrateTides()
        }
        binding.tideListDatePicker.setOnClickListener {
            val datePickerDialog = DatePickerDialog(
                requireContext(),
                { view, year, month, dayOfMonth ->
                    displayDate = LocalDate.of(year, month + 1, dayOfMonth)
                    update()
                },
                displayDate.year,
                displayDate.monthValue - 1,
                displayDate.dayOfMonth
            )
            datePickerDialog.show()
        }
    }

    override fun onResume() {
        super.onResume()
        // TODO: Add check if reference is too old
        intervalometer.interval(Duration.ofSeconds(15))
        if (prefs.referenceHighTide == null) {
            calibrateTides()
        }
    }

    override fun onPause() {
        super.onPause()
        intervalometer.stop()
    }

    private fun calibrateTides() {
        var referenceTime = prefs.referenceHighTide?.toLocalTime()
        var referenceDate = prefs.referenceHighTide?.toLocalDate()
        val now = LocalDateTime.now()
        val dialogView = View.inflate(activity, R.layout.view_tide_time_picker, null)
        val alertDialog = UiUtils.alertViewWithCancel(
            requireContext(),
            "Tide Calibration",
            dialogView,
            getString(R.string.dialog_ok),
            getString(R.string.dialog_cancel)
        ) { cancelled ->
            if (!cancelled) {
                if (referenceTime != null && referenceDate != null) {
                    prefs.referenceHighTide =
                        ZonedDateTime.of(referenceDate!!, referenceTime!!, ZoneId.systemDefault())
                    update()
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
            val timePickerDialog = TimePickerDialog(
                requireContext(),
                { timePicker: TimePicker, hour: Int, minute: Int ->
                    referenceTime = LocalTime.of(hour, minute)
                    referenceTimeTxt.text = if (referenceTime != null) {
                        formatService.formatTime(referenceTime!!, false)
                    } else {
                        getString(R.string.time_not_set)
                    }
                },
                referenceTime?.hour ?: now.hour,
                referenceTime?.minute ?: now.minute,
                prefs.use24HourTime
            )
            timePickerDialog.show()
        }
        dialogView.findViewById<Button>(R.id.date_picker).setOnClickListener {
            val datePickerDialog = DatePickerDialog(
                requireContext(),
                { view, year, month, dayOfMonth ->
                    referenceDate = LocalDate.of(year, month + 1, dayOfMonth)
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
                },
                referenceDate?.year ?: now.year,
                (referenceDate?.monthValue ?: now.monthValue) - 1,
                referenceDate?.dayOfMonth ?: now.dayOfMonth
            )
            datePickerDialog.show()
        }

        alertDialog.show()
    }

    private fun update() {
        val reference = prefs.referenceHighTide ?: return
        binding.tideListDateText.text = formatService.formatRelativeDate(displayDate)
        binding.tideClock.time = ZonedDateTime.now()
        val next = tideService.getNextSimpleTide(reference)
        binding.tideClock.nextTide = next
        binding.tidalRange.text = getTidalRangeName(tideService.getTidalRange(LocalDate.now()))
        binding.tideHeight.text = getTideTypeName(tideService.getSimpleTideType(reference))
        tideList.setData(
            tideService.getSimpleTides(
                reference,
                displayDate
            )
        )
    }

    private fun getTidalRangeName(range: Tide): String {
        return when (range) {
            Tide.Neap -> getString(R.string.tide_neap)
            Tide.Spring -> getString(R.string.tide_spring)
            Tide.Normal -> getString(R.string.tide_normal)
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