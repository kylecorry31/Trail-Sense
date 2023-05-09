package com.kylecorry.trail_sense.tools.maps.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentMapCalibrationBinding
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.extensions.onIO
import com.kylecorry.trail_sense.tools.maps.domain.MapCalibrationPoint
import com.kylecorry.trail_sense.tools.maps.domain.PercentCoordinate
import com.kylecorry.trail_sense.tools.maps.domain.PhotoMap
import com.kylecorry.trail_sense.tools.maps.infrastructure.MapRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MapCalibrationFragment : BoundFragment<FragmentMapCalibrationBinding>() {

    private val mapRepo by lazy { MapRepo.getInstance(requireContext()) }

    private var mapId = 0L
    private var map: PhotoMap? = null

    private var calibrationPoint1Percent: PercentCoordinate? = null
    private var calibrationPoint2Percent: PercentCoordinate? = null
    private var calibrationPoint1: Coordinate? = null
    private var calibrationPoint2: Coordinate? = null
    private var calibrationIndex = 0
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

        reloadMap()

        binding.calibrationNext.setOnClickListener {
            if (calibrationIndex == 1) {
                inBackground {
                    // Save the new calibration
                    onIO {
                        map?.let {
                            var updated = mapRepo.getMap(it.id) ?: return@let
                            updated = updated.copy(
                                calibration = updated.calibration.copy(calibrationPoints = it.calibration.calibrationPoints)
                            )
                            mapRepo.addMap(updated)
                            map = updated
                        }
                        onDone()
                    }
                }
            } else {
                calibratePoint(++calibrationIndex)
            }
        }

        binding.calibrationPrev.setOnClickListener {
            calibratePoint(--calibrationIndex)
        }


        binding.mapCalibrationCoordinate.setOnCoordinateChangeListener {
            if (calibrationIndex == 0) {
                calibrationPoint1 = it
            } else {
                calibrationPoint2 = it
            }

            updateMapCalibration()
        }

        binding.calibrationMap.onMapClick = {
            if (calibrationIndex == 0) {
                calibrationPoint1Percent = it
            } else {
                calibrationPoint2Percent = it
            }
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
            withContext(Dispatchers.IO) {
                map = mapRepo.getMap(mapId)
            }
            withContext(Dispatchers.Main) {
                map?.let {
                    onMapLoad(it)
                }
            }
        }
    }

    private fun onMapLoad(map: PhotoMap) {
        this.map = map
        binding.calibrationMap.showMap(map)
        calibrateMap()
    }

    private fun updateMapCalibration() {
        val points = mutableListOf<MapCalibrationPoint>()
        if (calibrationPoint1Percent != null) {
            points.add(
                MapCalibrationPoint(
                    calibrationPoint1 ?: Coordinate.zero,
                    calibrationPoint1Percent!!
                )
            )
        }

        if (calibrationPoint2Percent != null) {
            points.add(
                MapCalibrationPoint(
                    calibrationPoint2 ?: Coordinate.zero,
                    calibrationPoint2Percent!!
                )
            )
        }

        map = map?.copy(calibration = map!!.calibration.copy(calibrationPoints = points))
        binding.calibrationMap.showMap(map!!)
        binding.calibrationMap.highlightedIndex = calibrationIndex
    }


    fun calibrateMap() {
        map ?: return
        loadCalibrationPointsFromMap()

        calibrationIndex = if (calibrationPoint1 == null || calibrationPoint1Percent == null) {
            0
        } else {
            1
        }

        calibratePoint(calibrationIndex)
    }

    fun recenter() {
        binding.calibrationMap.recenter()
    }

    private fun calibratePoint(index: Int) {
        loadCalibrationPointsFromMap()
        binding.mapCalibrationTitle.text = getString(R.string.calibrate_map_point, index + 1, 2)
        binding.mapCalibrationCoordinate.coordinate =
            if (index == 0) calibrationPoint1 else calibrationPoint2
        binding.mapCalibrationBottomPanel.isVisible = true
        binding.calibrationNext.text =
            if (index == 0) getString(R.string.next) else getString(R.string.done)
        binding.calibrationPrev.isEnabled = index == 1
    }

    private fun loadCalibrationPointsFromMap() {
        map ?: return
        val first =
            if (map!!.calibration.calibrationPoints.isNotEmpty()) map!!.calibration.calibrationPoints[0] else null
        val second =
            if (map!!.calibration.calibrationPoints.size > 1) map!!.calibration.calibrationPoints[1] else null
        calibrationPoint1 = first?.location
        calibrationPoint2 = second?.location
        calibrationPoint1Percent = first?.imageLocation
        calibrationPoint2Percent = second?.imageLocation
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