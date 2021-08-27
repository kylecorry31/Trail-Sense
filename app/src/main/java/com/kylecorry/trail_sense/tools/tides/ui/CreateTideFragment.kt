package com.kylecorry.trail_sense.tools.tides.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.core.time.Timer
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.pickers.Pickers
import com.kylecorry.trail_sense.databinding.FragmentCreateTideBinding
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.tools.tides.domain.TideEntity
import com.kylecorry.trail_sense.tools.tides.infrastructure.persistence.TideRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class CreateTideFragment : BoundFragment<FragmentCreateTideBinding>() {

    private val formatService by lazy { FormatService(requireContext()) }
    private var referenceDate: LocalDate? = null
    private var referenceTime: LocalTime? = null
    private var editingId: Long? = null
    private var editingTide: TideEntity? = null

    private val tideRepo by lazy { TideRepo.getInstance(requireContext()) }
    private val prefs by lazy { UserPreferences(requireContext()) }

    private val intervalometer = Timer {
        binding.createTideBtn.isVisible = formIsValid()
    }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentCreateTideBinding {
        return FragmentCreateTideBinding.inflate(layoutInflater, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        editingId = arguments?.getLong("edit_tide_id")

        if (editingId != null) {
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    editingTide = tideRepo.getTide(editingId!!)
                }
                withContext(Dispatchers.Main) {
                    if (editingTide != null) {
                        fillExistingTideValues(editingTide!!)
                    } else {
                        editingId = null
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        intervalometer.interval(20)
    }

    override fun onPause() {
        intervalometer.stop()
        super.onPause()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.datePicker.setOnClickListener {
            Pickers.date(requireContext(), referenceDate ?: LocalDate.now()) {
                if (it != null) {
                    referenceDate = it
                    binding.date.text = formatService.formatDate(
                        ZonedDateTime.of(
                            referenceDate,
                            LocalTime.NOON,
                            ZoneId.systemDefault()
                        ), false
                    )
                }
            }
        }

        binding.timePicker.setOnClickListener {
            Pickers.time(
                requireContext(),
                prefs.use24HourTime,
                referenceTime ?: LocalTime.now()
            ) {
                if (it != null) {
                    referenceTime = it
                    binding.time.text = formatService.formatTime(referenceTime!!, false)
                }
            }
        }

        binding.createTideBtn.setOnClickListener {
            val tide = getTide()
            if (tide != null){
                lifecycleScope.launch {
                    withContext(Dispatchers.IO){
                        tideRepo.addTide(tide)
                    }

                    withContext(Dispatchers.Main){
                        findNavController().popBackStack()
                    }
                }
            }
        }
    }


    private fun fillExistingTideValues(tide: TideEntity) {
        binding.tideName.setText(tide.name)
        binding.tideLocation.coordinate = tide.coordinate
        binding.date.text = formatService.formatDate(tide.reference, false)
        binding.time.text = formatService.formatTime(tide.reference.toLocalTime(), false)
        referenceDate = tide.reference.toLocalDate()
        referenceTime = tide.reference.toLocalTime()
    }

    private fun formIsValid(): Boolean {
        return getTide() != null
    }

    private fun getTide(): TideEntity? {
        if (referenceTime == null || referenceDate == null) {
            return null
        }

        val reference = ZonedDateTime.of(referenceDate!!, referenceTime!!, ZoneId.systemDefault())

        if (editingId != null && editingTide == null) {
            return null
        }

        val rawName = binding.tideName.text?.toString()
        val name = if (rawName.isNullOrBlank()) null else rawName
        val location = binding.tideLocation.coordinate

        return TideEntity(
            reference.toInstant().toEpochMilli(),
            name,
            location?.latitude,
            location?.longitude
        ).also {
            it.id = editingId ?: 0
        }
    }

}