package com.kylecorry.trail_sense.tools.maps.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import com.kylecorry.andromeda.alerts.dialog
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentMapCalibrationBinding
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.extensions.onMain
import com.kylecorry.trail_sense.tools.maps.domain.MapCalibrationPoint
import com.kylecorry.trail_sense.tools.maps.domain.PercentCoordinate
import com.kylecorry.trail_sense.tools.maps.domain.PhotoMap
import com.kylecorry.trail_sense.tools.maps.infrastructure.MapRepo

class MapCalibrationFragment : BoundFragment<FragmentMapCalibrationBinding>() {

    private val mapRepo by lazy { MapRepo.getInstance(requireContext()) }

    private var mapId = 0L
    private var map: PhotoMap? = null

    private var points = mutableListOf<MapCalibrationPoint>()
    private var calibratedPoints = mutableSetOf<Int>()
    private var calibrationIndex = 0
    private var maxPoints = 2
    private var onDone: () -> Unit = {}

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

        fillCalibrationPoints()
        reloadMap()

        binding.calibrationNext.setOnClickListener {
            if (calibrationIndex == (maxPoints - 1)) {
                inBackground {
                    map = map?.let { save(it) }
                    onDone()
                }
            } else {
                calibrationIndex++
                calibratePoint(calibrationIndex)
            }
        }

        binding.calibrationPrev.setOnClickListener {
            calibrationIndex--
            calibratePoint(calibrationIndex)
        }


        binding.mapCalibrationCoordinate.setOnCoordinateChangeListener {
            points[calibrationIndex] =
                MapCalibrationPoint(it ?: Coordinate.zero, points[calibrationIndex].imageLocation)
            if (it == null) {
                calibratedPoints.remove(calibrationIndex)
            } else {
                calibratedPoints.add(calibrationIndex)
            }
            updateMapCalibration()
        }

        binding.calibrationMap.onMapClick = {
            points[calibrationIndex] = MapCalibrationPoint(points[calibrationIndex].location, it)
            updateMapCalibration()
        }

        CustomUiUtils.setButtonState(binding.zoomInBtn, false)
        CustomUiUtils.setButtonState(binding.zoomOutBtn, false)

        binding.zoomOutBtn.setOnClickListener {
            binding.calibrationMap.zoomBy(0.5f)
        }

        binding.zoomInBtn.setOnClickListener {
            binding.calibrationMap.zoomBy(2f)
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
        map = map?.copy(calibration = map!!.calibration.copy(calibrationPoints = points))
        binding.calibrationMap.showMap(map!!)
        binding.calibrationMap.highlightedIndex = calibrationIndex
    }


    private fun calibrateMap() {
        map ?: return
        loadCalibrationPointsFromMap()

        // Find the first uncalibrated point
        calibrationIndex =
            points.indexOfFirst { !calibratedPoints.contains(points.indexOf(it)) }.coerceAtLeast(0)

        calibratePoint(calibrationIndex)
    }

    fun recenter() {
        binding.calibrationMap.recenter()
    }

    private fun calibratePoint(index: Int) {
        binding.mapCalibrationTitle.text =
            getString(R.string.calibrate_map_point, index + 1, maxPoints)
        binding.mapCalibrationCoordinate.coordinate = if (calibratedPoints.contains(index)) {
            points[index].location
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
        val newPoints = map.calibration.calibrationPoints.filterIndexed { index, _ ->
            calibratedPoints.contains(index)
        }
        updated =
            updated.copy(calibration = updated.calibration.copy(calibrationPoints = newPoints))
        mapRepo.addMap(updated)
        return updated
    }

    private fun loadCalibrationPointsFromMap() {
        val map = map ?: return
        points = map.calibration.calibrationPoints.toMutableList()
        calibratedPoints.clear()
        calibratedPoints.addAll(points.indices)
        fillCalibrationPoints()
    }

    private fun fillCalibrationPoints() {
        while (points.size < maxPoints) {
            points.add(MapCalibrationPoint(Coordinate.zero, PercentCoordinate(0.5f, 0.5f)))
        }
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