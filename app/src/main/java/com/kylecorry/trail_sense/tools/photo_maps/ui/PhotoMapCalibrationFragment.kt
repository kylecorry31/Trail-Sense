package com.kylecorry.trail_sense.tools.photo_maps.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import com.kylecorry.andromeda.alerts.dialog
import com.kylecorry.andromeda.core.coroutines.onMain
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.ui.Colors
import com.kylecorry.andromeda.core.ui.setCompoundDrawables
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.andromeda.fragments.observe
import com.kylecorry.sol.math.SolMath
import com.kylecorry.sol.math.SolMath.roundNearestAngle
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentPhotoMapCalibrationBinding
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.CustomUiUtils.getPrimaryMarkerColor
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.declination.GPSDeclinationStrategy
import com.kylecorry.trail_sense.shared.extensions.promptIfUnsavedChanges
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.tools.photo_maps.domain.MapCalibrationManager
import com.kylecorry.trail_sense.tools.photo_maps.domain.PhotoMap
import com.kylecorry.trail_sense.tools.photo_maps.infrastructure.MapRepo
import com.kylecorry.trail_sense.tools.photo_maps.infrastructure.calibration.MapRotationCalculator
import com.kylecorry.trail_sense.tools.photo_maps.infrastructure.layers.BeaconLayerManager
import com.kylecorry.trail_sense.tools.photo_maps.infrastructure.layers.ILayerManager
import com.kylecorry.trail_sense.tools.photo_maps.infrastructure.layers.MultiLayerManager
import com.kylecorry.trail_sense.tools.photo_maps.infrastructure.layers.MyAccuracyLayerManager
import com.kylecorry.trail_sense.tools.photo_maps.infrastructure.layers.MyLocationLayerManager
import com.kylecorry.trail_sense.tools.photo_maps.infrastructure.layers.PathLayerManager
import com.kylecorry.trail_sense.tools.navigation.ui.layers.BeaconLayer
import com.kylecorry.trail_sense.tools.navigation.ui.layers.MyAccuracyLayer
import com.kylecorry.trail_sense.tools.navigation.ui.layers.MyLocationLayer
import com.kylecorry.trail_sense.tools.navigation.ui.layers.PathLayer

class PhotoMapCalibrationFragment : BoundFragment<FragmentPhotoMapCalibrationBinding>() {

    private val mapRepo by lazy { MapRepo.getInstance(requireContext()) }

    private var mapId = 0L
    private var map: PhotoMap? = null

    private var calibrationIndex = 0
    private var maxPoints = 2
    private var onDone: () -> Unit = {}
    private var showRotation: (Float) -> Unit = {}
    private val rotationCalculator = MapRotationCalculator()
    private var originalRotation = 0f

    private lateinit var backCallback: OnBackPressedCallback

    private val prefs by lazy { UserPreferences(requireContext()) }

    private val manager = MapCalibrationManager(maxPoints) {
        updateMapCalibration()
    }

    // Layers
    private var layerManager: ILayerManager? = null
    private val beaconLayer = BeaconLayer()
    private val pathLayer = PathLayer()
    private val myLocationLayer = MyLocationLayer()
    private val myAccuracyLayer = MyAccuracyLayer()

    // Sensors
    private val sensorService by lazy { SensorService(requireContext()) }
    private val gps by lazy { sensorService.getGPS() }
    private val compass by lazy { sensorService.getCompass() }

    private val declinationStrategy by lazy {
        GPSDeclinationStrategy(gps)
    }

    private var showPreview = false

    override fun onCreate(savedInstanceState: Bundle?) {
        pathLayer.setShouldRenderWithDrawLines(prefs.navigation.useFastPathRendering)
        super.onCreate(savedInstanceState)
        mapId = requireArguments().getLong("mapId")
    }

    override fun onResume() {
        super.onResume()
        layerManager = MultiLayerManager(
            listOf(
                PathLayerManager(requireContext(), pathLayer),
                MyAccuracyLayerManager(myAccuracyLayer, Resources.getPrimaryMarkerColor(requireContext())),
                MyLocationLayerManager(myLocationLayer, Resources.getPrimaryMarkerColor(requireContext())),
                BeaconLayerManager(requireContext(), beaconLayer)
            )
        )

        // Populate the last known location
        layerManager?.onLocationChanged(gps.location, gps.horizontalAccuracy)

        observe(gps) {
            layerManager?.onLocationChanged(gps.location, gps.horizontalAccuracy)
            compass.declination = declinationStrategy.getDeclination()
        }

        observe(compass) {
            layerManager?.onBearingChanged(compass.rawBearing)
        }
    }

    override fun onPause() {
        super.onPause()
        layerManager?.stop()
        layerManager = null
    }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentPhotoMapCalibrationBinding {
        return FragmentPhotoMapCalibrationBinding.inflate(layoutInflater, container, false)
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

        binding.previewButton.setOnClickListener {
            setPreviewMode(!showPreview)
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

    fun setOnRotationListener(listener: (Float) -> Unit) {
        showRotation = listener
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
        originalRotation = map.calibration.rotation
        binding.calibrationMap.mapAzimuth = 0f
        binding.calibrationMap.keepMapUp = true
        binding.calibrationMap.showMap(map)
        calibrateMap()
        layerManager?.onBoundsChanged(map.boundary())
    }

    private fun updateMapCalibration() {
        if (!isBound){
            return
        }

        val isCalibrated = isFullyCalibrated()
        if (binding.previewButton.isVisible != isCalibrated) {
            binding.previewButton.isVisible = isCalibrated
            CustomUiUtils.setButtonState(binding.previewButton, showPreview)
        }

        if (!isCalibrated && showPreview){
            setPreviewMode(false)
        }

        map = map?.copy(
            calibration = map!!.calibration.copy(
                calibrationPoints = manager.getCalibration(false)
            )
        )

        map = map?.copy(
            calibration = map!!.calibration.copy(
                rotation = if (showPreview) rotationCalculator.calculate(map!!) else originalRotation
            )
        )
        layerManager?.onBoundsChanged(map?.boundary())
        val map = map ?: return
        binding.calibrationMap.mapAzimuth = 0f
        binding.calibrationMap.keepMapUp = true
        binding.calibrationMap.showMap(map)
        binding.calibrationMap.highlightedIndex = calibrationIndex
        updateCompletionState()
        updateRotation()
    }

    private fun updateRotation() {
        val map = map ?: return
        val rotation = if (map.isCalibrated) {
            rotationCalculator.calculate(map)
        } else {
            map.baseRotation().toFloat()
        }

        val baseRotation = rotation.roundNearestAngle(90f)

        showRotation(SolMath.deltaAngle(baseRotation, rotation))
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

        updateCompletionState()
    }

    private fun updateCompletionState() {
        // If it is calibrated, replace the info icon with a green checkmark
        if (manager.isCalibrated(calibrationIndex)) {
            binding.mapCalibrationTitle.setCompoundDrawables(
                Resources.dp(requireContext(), 24f).toInt(),
                left = R.drawable.ic_check_outline
            )
            Colors.setImageColor(binding.mapCalibrationTitle, AppColor.Green.color)
        } else {
            binding.mapCalibrationTitle.setCompoundDrawables(
                Resources.dp(requireContext(), 24f).toInt(),
                left = R.drawable.ic_info
            )
            Colors.setImageColor(
                binding.mapCalibrationTitle,
                Resources.androidTextColorSecondary(requireContext())
            )
        }
    }

    private fun setPreviewMode(enabled: Boolean) {
        if (showPreview == enabled) {
            return
        }

        CustomUiUtils.setButtonState(binding.previewButton, enabled)

        showPreview = enabled
        val layers = if (enabled) listOf(
            pathLayer,
            myAccuracyLayer,
            myLocationLayer,
            beaconLayer
        ) else emptyList()
        binding.calibrationMap.setLayers(layers)
        updateMapCalibration()

        if (showPreview){
            layerManager?.start()
        } else {
            layerManager?.stop()
        }

    }

    private fun isFullyCalibrated(): Boolean {
        return manager.getCalibration(true).size == maxPoints
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
        fun create(
            mapId: Long,
            showRotation: (rotation: Float) -> Unit = {},
            onComplete: () -> Unit = {}
        ): PhotoMapCalibrationFragment {
            return PhotoMapCalibrationFragment().apply {
                arguments = bundleOf("mapId" to mapId)
                setOnRotationListener(showRotation)
                setOnCompleteListener(onComplete)
            }
        }
    }

}