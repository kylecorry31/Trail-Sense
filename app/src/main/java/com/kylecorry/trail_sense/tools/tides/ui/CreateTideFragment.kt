package com.kylecorry.trail_sense.tools.tides.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.core.time.Timer
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.sol.time.Time.toZonedDateTime
import com.kylecorry.trail_sense.databinding.FragmentCreateTideBinding
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.tools.tides.domain.TideEntity
import com.kylecorry.trail_sense.tools.tides.infrastructure.persistence.TideRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

class CreateTideFragment : BoundFragment<FragmentCreateTideBinding>() {

    private val formatService by lazy { FormatService(requireContext()) }
    private var referenceDatetime: LocalDateTime? = null
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

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tideTime.inputType = InputType.TYPE_NULL

        binding.tideTime.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                CustomUiUtils.pickDatetime(
                    requireContext(),
                    prefs.use24HourTime,
                    referenceDatetime ?: LocalDateTime.now()
                ) {
                    if (it != null) {
                        referenceDatetime = it
                        binding.tideTime.setText(
                            formatService.formatDateTime(
                                it.toZonedDateTime(),
                                false
                            )
                        )
                    }
                }
            }
            true
        }

        binding.createTideBtn.setOnClickListener {
            val tide = getTide()
            if (tide != null) {
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        tideRepo.addTide(tide)
                    }

                    withContext(Dispatchers.Main) {
                        findNavController().popBackStack()
                    }
                }
            }
        }
    }


    private fun fillExistingTideValues(tide: TideEntity) {
        binding.tideName.setText(tide.name)
        binding.tideLocation.coordinate = tide.coordinate
        binding.tideTime.setText(formatService.formatDateTime(tide.reference, false))
        referenceDatetime = tide.reference.toLocalDateTime()
        binding.diurnal.isChecked = tide.diurnal
    }

    private fun formIsValid(): Boolean {
        return getTide() != null
    }

    private fun getTide(): TideEntity? {
        val reference = referenceDatetime?.toZonedDateTime() ?: return null

        if (editingId != null && editingTide == null) {
            return null
        }

        val rawName = binding.tideName.text?.toString()
        val name = if (rawName.isNullOrBlank()) null else rawName
        val location = binding.tideLocation.coordinate
        val diurnal = binding.diurnal.isChecked

        return TideEntity(
            reference.toInstant().toEpochMilli(),
            name,
            location?.latitude,
            location?.longitude,
            diurnal = diurnal
        ).also {
            it.id = editingId ?: 0
        }
    }

}