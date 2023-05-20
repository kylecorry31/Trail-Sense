package com.kylecorry.trail_sense.tools.maps.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import com.kylecorry.andromeda.alerts.dialog
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentMapCalibrationBinding
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.extensions.onMain
import com.kylecorry.trail_sense.shared.extensions.promptIfUnsavedChanges
import com.kylecorry.trail_sense.tools.maps.domain.MapCalibrationManager
import com.kylecorry.trail_sense.tools.maps.domain.PhotoMap
import com.kylecorry.trail_sense.tools.maps.infrastructure.MapRepo

class MapCalibrationFragment : BoundFragment<FragmentMapCalibrationBinding>() {

    private val mapRepo by lazy { MapRepo.getInstance(requireContext()) }

    private var mapId = 0L
    private var map: PhotoMap? = null

    private var calibrationIndex = 0
    private var maxPoints = 2
    private var onDone: () -> Unit = {}

    private lateinit var backCallback: OnBackPressedCallback

    private val manager = MapCalibrationManager(maxPoints) {
        updateMapCalibration()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mapId = requireArguments().getLong("mapId")
    }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentMapCalibrationBinding {
        return FragmentMapCalibrationBinding.inflate(layoutInflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        CustomUiUtils.disclaimer(
            requireContext(),
            getString(R.string.map_calibration),
            getString(R.string.map_calibration_instructions),
            getString(R.string.map_calibration_shown),
            cancelText = null
        )

        binding.mapCalibrationTitle.setOnClickListener {
            dialog(
                getString(R.string.map_calibration),
                getString(R.string.map_calibration_instructions),
                cancelText = null
            )
        }

        reloadMap()

        binding.calibrationNext.setOnClickListener {
            if (calibrationIndex == (maxPoints - 1)) {
                inBackground {
                    map = map?.let { save(it) }
                    manager.reset(map?.calibration?.calibrationPoints ?: emptyList())
                    backCallback.remove()
                    onDone()
                }
            } else {
                calibrationIndex++
                selectPoint(calibrationIndex)
            }
        }

        binding.calibrationPrev.setOnClickListener {
            calibrationIndex--
            selectPoint(calibrationIndex)
        }


        binding.mapCalibrationCoordinate.setOnCoordinateChangeListener {
            manager.calibrate(calibrationIndex, it)
        }

        binding.calibrationMap.onMapClick = {
            manager.calibrate(calibrationIndex, it)
        }

        CustomUiUtils.setButtonState(binding.zoomInBtn, false)
        CustomUiUtils.setButtonState(binding.zoomOutBtn, false)

        binding.zoomOutBtn.setOnClickListener {
            binding.calibrationMap.zoomBy(0.5f)
        }

        binding.zoomInBtn.setOnClickListener {
            binding.calibrationMap.zoomBy(2f)
        }

        backCallback = promptIfUnsavedChanges {
            hasChanges()
        }
    }

    fun setOnCompleteListener(listener: () -> Unit) {
        onDone = listener
    }

    fun reloadMap() {
        inBackground {
            map = mapRepo.getMap(mapId)
            onMain {
                map?.let(::onMapLoad)
            }
        }
    }

    private fun onMapLoad(map: PhotoMap) {
        this.map = map
        binding.calibrationMap.showMap(map)
        calibrateMap()
    }

    private fun updateMapCalibration() {
        map = map?.copy(calibration = map!!.calibration.copy(calibrationPoints = manager.getCalibration(false)))
        binding.calibrationMap.showMap(map!!)
        binding.calibrationMap.highlightedIndex = calibrationIndex
    }


    private fun calibrateMap() {
        map ?: return
        loadCalibrationPointsFromMap()

        // Find the first uncalibrated point
        calibrationIndex = manager.getNextUncalibratedIndex().coerceAtLeast(0)

        selectPoint(calibrationIndex)
    }

    fun recenter() {
        binding.calibrationMap.recenter()
    }

    private fun selectPoint(index: Int) {
        binding.mapCalibrationTitle.text =
            getString(R.string.calibrate_map_point, index + 1, maxPoints)
        binding.mapCalibrationCoordinate.coordinate = if (manager.isCalibrated(index)) {
            manager.getCalibrationPoint(index).location
        } else {
            null
        }
        binding.mapCalibrationBottomPanel.isVisible = true
        binding.calibrationNext.text =
            if (index == (maxPoints - 1)) getString(R.string.done) else getString(R.string.next)
        binding.calibrationPrev.isVisible = index == 1
    }

    private suspend fun save(map: PhotoMap): PhotoMap {
        var updated = mapRepo.getMap(map.id) ?: return map
        updated =
            updated.copy(calibration = updated.calibration.copy(calibrationPoints = manager.getCalibration()))
        mapRepo.addMap(updated)
        return updated
    }

    private fun loadCalibrationPointsFromMap() {
        val map = map ?: return
        manager.reset(map.calibration.calibrationPoints)
    }

    private fun hasChanges(): Boolean {
        return manager.hasChanges()
    }

    companion object {
        fun create(mapId: Long, onComplete: () -> Unit = {}): MapCalibrationFragment {
            return MapCalibrationFragment().apply {
                arguments = bundleOf("mapId" to mapId)
                setOnCompleteListener(onComplete)
            }
        }
    }

}