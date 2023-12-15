package com.kylecorry.trail_sense.tools.augmented_reality

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.camera.view.PreviewView
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import com.kylecorry.andromeda.core.ui.Colors.withAlpha
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.fragments.observeFlow
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentAugmentedRealityBinding
import com.kylecorry.trail_sense.navigation.beacons.domain.Beacon
import com.kylecorry.trail_sense.navigation.beacons.infrastructure.persistence.BeaconRepo
import com.kylecorry.trail_sense.navigation.infrastructure.Navigator
import com.kylecorry.trail_sense.shared.DistanceUtils.toRelativeDistance
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.permissions.alertNoCameraPermission
import com.kylecorry.trail_sense.shared.permissions.requestCamera
import com.kylecorry.trail_sense.tools.augmented_reality.position.GeographicARPoint
import java.time.ZonedDateTime
import kotlin.math.hypot

// TODO: Support arguments for default layer visibility (ex. coming from astronomy, enable only sun/moon)
class AugmentedRealityFragment : BoundFragment<FragmentAugmentedRealityBinding>() {

    private val userPrefs by lazy { UserPreferences(requireContext()) }
    private val beaconRepo by lazy {
        BeaconRepo.getInstance(requireContext())
    }

    private val formatter by lazy { FormatService.getInstance(requireContext()) }

    private val beaconLayer by lazy {
        ARBeaconLayer(
            Distance.meters(userPrefs.navigation.maxBeaconDistance),
            onFocus = this::onBeaconFocused
        ) {
            if (navigator.getDestinationId() != it.id) {
                navigator.navigateTo(it)
            } else {
                navigator.cancelNavigation()
            }
            true
        }
    }

    private val astronomyLayer by lazy {
        ARAstronomyLayer(
            drawLines = true,
            drawBelowHorizon = false,
            onSunFocus = this::onSunFocused,
            onMoonFocus = this::onMoonFocused
        )
    }

    private val navigator by lazy { Navigator.getInstance(requireContext()) }

    private val gridLayer by lazy {
        ARGridLayer(
            30,
            northColor = AppColor.Orange.color,
            horizonColor = Color.WHITE,
            labelColor = Color.WHITE,
            color = Color.WHITE.withAlpha(100),
            useTrueNorth = userPrefs.compass.useTrueNorth
        )
    }

    private var isCameraEnabled = true

    // TODO: Draw an indicator around the focused marker

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeFlow(beaconRepo.getBeacons()) {
            beaconLayer.setBeacons(it)
        }

        observeFlow(navigator.destination) {
            if (it == null) {
                binding.arView.clearGuide()
            } else {
                binding.arView.guideTo(GeographicARPoint(it.coordinate, it.elevation)) {
                    // Do nothing when reached
                }
            }
            beaconLayer.destination = it
        }

        binding.camera.setScaleType(PreviewView.ScaleType.FIT_CENTER)
        binding.camera.setShowTorch(false)

        binding.arView.bind(binding.camera)

        binding.arView.setLayers(listOf(gridLayer, astronomyLayer, beaconLayer))

        binding.cameraToggle.setOnClickListener {
            if (isCameraEnabled) {
                stopCamera()
            } else {
                startCamera()
            }
        }
    }

    override fun onResume() {
        super.onResume()

        binding.arView.start()
        if (isCameraEnabled) {
            startCamera()
        } else {
            stopCamera()
        }
    }

    // TODO: Move this to the AR view
    private fun startCamera() {
        isCameraEnabled = true
        binding.cameraToggle.setImageResource(R.drawable.ic_camera)
        binding.arView.backgroundFillColor = Color.TRANSPARENT
        requestCamera {
            if (it) {
                binding.camera.isVisible = true
                binding.camera.start(
                    readFrames = false, shouldStabilizePreview = false
                )
            } else {
                binding.camera.isInvisible = true
                isCameraEnabled = false
                binding.cameraToggle.setImageResource(R.drawable.ic_camera_off)
                binding.arView.backgroundFillColor = Color.BLACK
                alertNoCameraPermission()
            }
        }
    }

    private fun stopCamera() {
        binding.cameraToggle.setImageResource(R.drawable.ic_camera_off)
        isCameraEnabled = false
        binding.arView.backgroundFillColor = Color.BLACK
        binding.camera.stop()
        binding.camera.isInvisible = true
    }

    override fun onPause() {
        super.onPause()
        binding.camera.stop()
        binding.arView.stop()
    }

    private fun onSunFocused(time: ZonedDateTime): Boolean {
        binding.arView.focusText =
            getString(R.string.sun) + "\n" + formatter.formatRelativeDateTime(
                time,
                includeSeconds = false
            )
        return true
    }

    private fun onMoonFocused(time: ZonedDateTime): Boolean {
        binding.arView.focusText =
            getString(R.string.moon) + "\n" + formatter.formatRelativeDateTime(
                time,
                includeSeconds = false
            )
        return true
    }

    private fun onBeaconFocused(beacon: Beacon): Boolean {
        val distance = hypot(
            binding.arView.location.distanceTo(beacon.coordinate),
            (beacon.elevation ?: binding.arView.altitude) - binding.arView.altitude
        )
        val userDistance = Distance.meters(distance).convertTo(userPrefs.baseDistanceUnits)
            .toRelativeDistance()
        val formattedDistance = formatter.formatDistance(
            userDistance,
            Units.getDecimalPlaces(userDistance.units),
            strict = false
        )
        binding.arView.focusText = beacon.name + "\n" + formattedDistance
        return true
    }

    override fun generateBinding(
        layoutInflater: LayoutInflater, container: ViewGroup?
    ): FragmentAugmentedRealityBinding {
        return FragmentAugmentedRealityBinding.inflate(layoutInflater, container, false)
    }
}