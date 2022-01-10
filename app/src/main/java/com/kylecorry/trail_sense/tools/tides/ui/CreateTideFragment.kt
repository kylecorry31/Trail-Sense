package com.kylecorry.trail_sense.tools.tides.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.textfield.TextInputEditText
import com.kylecorry.andromeda.core.math.DecimalFormatter
import com.kylecorry.andromeda.core.time.Timer
import com.kylecorry.andromeda.core.toFloatCompat
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.list.ListView
import com.kylecorry.sol.science.oceanography.Tide
import com.kylecorry.sol.science.oceanography.TideType
import com.kylecorry.sol.time.Time.toZonedDateTime
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentCreateTideBinding
import com.kylecorry.trail_sense.databinding.ListItemTideEntryBinding
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.tools.tides.domain.TideTable
import com.kylecorry.trail_sense.tools.tides.infrastructure.persistence.TideTableRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.ZonedDateTime

class CreateTideFragment : BoundFragment<FragmentCreateTideBinding>() {

    private val formatService by lazy { FormatService(requireContext()) }
    private var editingId: Long? = null
    private var editingTide: TideTable? = null

    private lateinit var tideTimesList: ListView<TideEntry>
    private var tides = mutableListOf<TideEntry>()

    private val tideRepo by lazy { TideTableRepo.getInstance(requireContext()) }
    private val prefs by lazy { UserPreferences(requireContext()) }
    private val units by lazy { prefs.baseDistanceUnits }

    private val intervalometer = Timer {
        binding.createTideBtn.isVisible = formIsValid()
    }

    private val watchers = mutableMapOf<TextInputEditText, TextWatcher>()

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentCreateTideBinding {
        return FragmentCreateTideBinding.inflate(layoutInflater, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val editingIdValue = arguments?.getLong("edit_tide_id")
        editingId = if (editingIdValue != null && editingIdValue != 0L) editingIdValue else null
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

        tideTimesList = ListView(binding.tideTimes, R.layout.list_item_tide_entry) { view, tide ->
            val itemBinding = ListItemTideEntryBinding.bind(view)
            itemBinding.tideHeight.removeTextChangedListener(
                watchers.getOrDefault(
                    itemBinding.tideHeight,
                    null
                )
            )
            watchers.remove(itemBinding.tideHeight)

            itemBinding.tideType.text =
                if (tide.isHigh) getString(R.string.high_tide_letter) else getString(
                    R.string.low_tide_letter
                )
            itemBinding.tideType.setOnClickListener {
                tide.isHigh = !tide.isHigh
                itemBinding.tideType.text =
                    if (tide.isHigh) getString(R.string.high_tide_letter) else getString(
                        R.string.low_tide_letter
                    )
            }

            itemBinding.delete.setOnClickListener {
                tides.remove(tide)
                tideTimesList.setData(tides)
            }

            itemBinding.tideTime.text = null
            itemBinding.tideHeight.text = null

            tide.time?.let {
                itemBinding.tideTime.setText(
                    formatService.formatDateTime(
                        it,
                        false,
                        abbreviateMonth = true
                    )
                )
            }

            itemBinding.tideTime.setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    CustomUiUtils.pickDatetime(
                        requireContext(),
                        prefs.use24HourTime,
                        tide.time?.toLocalDateTime() ?: LocalDateTime.now()
                    ) {
                        if (it != null) {
                            tide.time = it.toZonedDateTime()
                            itemBinding.tideTime.setText(
                                formatService.formatDateTime(
                                    it.toZonedDateTime(),
                                    false,
                                    abbreviateMonth = true
                                )
                            )
                        }
                    }
                }
                true
            }

            itemBinding.tideHeightHolder.hint = formatService.getDistanceUnitName(units)

            tide.height?.let {
                itemBinding.tideHeight.setText(DecimalFormatter.format(it.distance, 2))
            }

            val watcher = itemBinding.tideHeight.addTextChangedListener {
                val height = it?.toString()?.trim()?.toFloatCompat()
                if (height != null) {
                    tide.height = Distance(height, units)
                } else {
                    tide.height = null
                }
            }

            watchers[itemBinding.tideHeight] = watcher
        }

        tides.clear()
        if (editingId != null) {
            runInBackground {
                withContext(Dispatchers.IO) {
                    editingTide = tideRepo.getTideTable(editingId!!)
                }
                withContext(Dispatchers.Main) {
                    if (editingTide != null) {
                        fillExistingTideValues(editingTide!!)
                    } else {
                        editingId = null
                    }
                }
            }
        } else {
            tides.add(TideEntry(true, null, null))
            tideTimesList.setData(tides)
        }

        binding.addTideEntry.setOnClickListener {
            tides.add(TideEntry(true, null, null))
            tideTimesList.setData(tides)
        }

        binding.createTideBtn.setOnClickListener {
            val tide = getTide()
            if (tide != null) {
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        tideRepo.addTideTable(tide)
                    }

                    withContext(Dispatchers.Main) {
                        findNavController().popBackStack()
                    }
                }
            }
        }
    }


    private fun fillExistingTideValues(tide: TideTable) {
        binding.tideName.setText(tide.name)
        binding.tideLocation.coordinate = tide.location
        tides.addAll(tide.tides.map {
            TideEntry(
                it.type == TideType.High,
                it.time,
                Distance.meters(it.height).convertTo(units)
            )
        })
        tideTimesList.setData(tides)
    }

    private fun formIsValid(): Boolean {
        return getTide() != null
    }

    private fun getTide(): TideTable? {
        val tides = tides.mapNotNull {
            val time = it.time ?: return@mapNotNull null
            Tide(
                time,
                if (it.isHigh) TideType.High else TideType.Low,
                it.height?.meters()?.distance ?: 0f
            )
        }

        if (editingId != null && editingTide == null) {
            return null
        }

        if (tides.isEmpty()) {
            return null
        }

        val rawName = binding.tideName.text?.toString()
        val name = if (rawName.isNullOrBlank()) null else rawName
        val location = binding.tideLocation.coordinate

        return TideTable(
            editingId ?: 0,
            tides,
            name,
            location
        )
    }


    private data class TideEntry(
        var isHigh: Boolean,
        var time: ZonedDateTime?,
        var height: Distance?
    )

}