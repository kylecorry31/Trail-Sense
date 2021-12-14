package com.kylecorry.trail_sense.tools.clinometer.ui

import android.Manifest
import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.camera.Camera
import com.kylecorry.andromeda.core.sensors.asLiveData
import com.kylecorry.andromeda.core.time.Throttle
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.sense.orientation.DeviceOrientation
import com.kylecorry.sol.math.InclinationService
import com.kylecorry.sol.math.SolMath
import com.kylecorry.sol.science.geology.AvalancheRisk
import com.kylecorry.sol.science.geology.GeologyService
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentClinometerBinding
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.tools.clinometer.infrastructure.CameraClinometer
import com.kylecorry.trail_sense.tools.clinometer.infrastructure.Clinometer
import com.kylecorry.trail_sense.tools.clinometer.infrastructure.SideClinometer
import java.time.Duration
import java.time.Instant
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min

class ClinometerFragment : BoundFragment<FragmentClinometerBinding>() {

    private val sensorService by lazy { SensorService(requireContext()) }
    private val cameraClinometer by lazy { CameraClinometer(requireContext()) }
    private val camera by lazy {
        Camera(
            requireContext(),
            viewLifecycleOwner,
            previewView = binding.cameraView,
            analyze = false
        )
    }
    private val sideClinometer by lazy { SideClinometer(requireContext()) }
    private val deviceOrientation by lazy { sensorService.getDeviceOrientationSensor() }
    private val prefs by lazy { UserPreferences(requireContext()) }
    private val geology = GeologyService()
    private val inclinationService = InclinationService()
    private val formatter by lazy { FormatService(requireContext()) }
    private val throttle = Throttle(20)

    private lateinit var clinometer: Clinometer

    private var slopeIncline: Float? = null
    private var slopeAngle: Float? = null
    private var startIncline: Float = 0f
    private var touchTime = Instant.now()

    private var lockState = ClinometerLockState.Unlocked
    private val holdDuration = Duration.ofMillis(200)

    private var distanceAway: Distance? = null

    private var useCamera = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        clinometer = getClinometer()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        CustomUiUtils.setButtonState(binding.clinometerLeftQuickAction, false)
        CustomUiUtils.setButtonState(binding.clinometerRightQuickAction, false)

        val units = if (prefs.distanceUnits == UserPreferences.DistanceUnits.Meters) {
            listOf(DistanceUnits.Meters, DistanceUnits.Feet)
        } else {
            listOf(DistanceUnits.Feet, DistanceUnits.Meters)
        }

        binding.cameraViewHolder.clipToOutline = true

        binding.clinometerLeftQuickAction.setOnClickListener {
            if (useCamera) {
                camera.stop(null)
                binding.clinometerLeftQuickAction.setImageResource(R.drawable.ic_camera)
                CustomUiUtils.setButtonState(binding.clinometerLeftQuickAction, false)
                useCamera = false
                clinometer = getClinometer()
            } else {
                requestPermissions(listOf(Manifest.permission.CAMERA)) {
                    if (Camera.isAvailable(requireContext())) {
                        useCamera = true
                        camera.start {
                            camera.setZoom(0.25f)
                            true
                        }
                        binding.clinometerLeftQuickAction.setImageResource(R.drawable.ic_screen_flashlight)
                        CustomUiUtils.setButtonState(binding.clinometerLeftQuickAction, false)
                        clinometer = getClinometer()
                    } else {
                        Alerts.toast(
                            requireContext(),
                            getString(R.string.camera_permission_denied),
                            short = false
                        )
                    }
                }
            }
        }

        binding.clinometerRightQuickAction.setOnClickListener {
            CustomUiUtils.pickDistance(
                requireContext(),
                units,
                distanceAway,
                getString(R.string.distance_away)
            ) {
                if (it != null) {
                    distanceAway = it
                    CustomUiUtils.setButtonState(binding.clinometerRightQuickAction, true)
                }
            }
        }

        // TODO: Make this discoverable by the user
        binding.root.setOnTouchListener { v, event ->
            when (lockState) {
                ClinometerLockState.Unlocked -> {
                    if (event.action == MotionEvent.ACTION_DOWN && isOrientationValid()) {
                        setStartAngle()
                        lockState = ClinometerLockState.PartiallyLocked
                    }
                }
                ClinometerLockState.PartiallyLocked -> {
                    if (event.action == MotionEvent.ACTION_UP) {
                        if (Duration.between(touchTime, Instant.now()) < holdDuration) {
                            // No sweep angle
                            clearStartAngle()
                        }

                        setEndAngle()

                        lockState = ClinometerLockState.Locked
                    }
                }
                ClinometerLockState.Locked -> {
                    if (event.action == MotionEvent.ACTION_DOWN && isOrientationValid()) {
                        setStartAngle()
                        clearEndAngle()
                        lockState = ClinometerLockState.PartiallyUnlocked
                    }
                }
                ClinometerLockState.PartiallyUnlocked -> {
                    if (event.action == MotionEvent.ACTION_UP) {
                        lockState = if (Duration.between(touchTime, Instant.now()) < holdDuration) {
                            // User wants to unlock
                            clearStartAngle()
                            clearEndAngle()
                            ClinometerLockState.Unlocked
                        } else {
                            // User wants to do another sweep angle
                            setEndAngle()
                            ClinometerLockState.Locked
                        }
                    }
                }
            }
            true
        }

        sideClinometer.asLiveData().observe(viewLifecycleOwner, { updateUI() })
        cameraClinometer.asLiveData().observe(viewLifecycleOwner, { updateUI() })
        deviceOrientation.asLiveData().observe(viewLifecycleOwner, { updateUI() })

    }

    private fun clearStartAngle() {
        startIncline = 0f
        binding.cameraClinometer.startInclination = null
        binding.clinometer.startAngle = null
    }

    private fun setStartAngle() {
        touchTime = Instant.now()
        startIncline = clinometer.incline
        binding.cameraClinometer.startInclination = startIncline
        binding.clinometer.startAngle = clinometer.angle
    }

    private fun setEndAngle() {
        slopeAngle = clinometer.angle
        slopeIncline = clinometer.incline
    }

    private fun clearEndAngle() {
        slopeAngle = null
        slopeIncline = null
    }

    override fun onPause() {
        super.onPause()
        if (useCamera) {
            camera.stop(null)
            useCamera = false
            clinometer = getClinometer()
        }
    }

    private fun getClinometer(): Clinometer {
        return if (useCamera) {
            cameraClinometer
        } else {
            sideClinometer
        }
    }

    private fun updateUI() {

        if (throttle.isThrottled()) {
            return
        }

        binding.lock.isVisible = slopeAngle != null

        if (!isOrientationValid() && slopeAngle == null) {
            binding.clinometerInstructions.text = getString(R.string.clinometer_rotate_device)
            return
        }

        binding.cameraViewHolder.isVisible = useCamera
        binding.clinometer.isInvisible = useCamera

        binding.clinometerInstructions.text = getString(R.string.set_inclination_instructions)

        val angle = slopeAngle ?: clinometer.angle
        val incline = slopeIncline ?: clinometer.incline

        val avalancheRisk = geology.getAvalancheRisk(incline)

        binding.clinometer.angle = angle
        binding.cameraClinometer.inclination = incline

        binding.inclination.text = formatter.formatDegrees(incline)
        binding.avalancheRisk.title = getAvalancheRiskString(avalancheRisk)

        binding.inclinationDescription.text =
            getString(R.string.slope_amount, formatter.formatPercentage(getSlopePercent(incline)))

        val distance = distanceAway
        binding.estimatedHeight.title = if (distance != null) {
            formatter.formatDistance(
                getHeight(
                    distance,
                    min(startIncline, incline),
                    max(startIncline, incline)
                )
            )
        } else {
            getString(R.string.distance_unset)
        }

    }

    private fun getSlopePercent(incline: Float): Float {
        return SolMath.tanDegrees(incline) * 100
    }

    private fun getAvalancheRiskString(risk: AvalancheRisk): String {
        return when (risk) {
            AvalancheRisk.Low -> getString(R.string.low)
            AvalancheRisk.Moderate -> getString(R.string.moderate)
            AvalancheRisk.High -> getString(R.string.high)
        }
    }

    private fun isOrientationValid(): Boolean {
        val invalidOrientations = if (useCamera) {
            listOf(
                DeviceOrientation.Orientation.Landscape,
                DeviceOrientation.Orientation.LandscapeInverse
            )
        } else {
            listOf(DeviceOrientation.Orientation.Flat, DeviceOrientation.Orientation.FlatInverse)
        }

        return !invalidOrientations.contains(deviceOrientation.orientation)
    }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentClinometerBinding {
        return FragmentClinometerBinding.inflate(layoutInflater, container, false)
    }

    private fun getHeight(distanceAway: Distance, bottom: Float, top: Float): Distance {
        return Distance.meters(
            inclinationService.estimateHeightAngles(
                distanceAway.meters().distance,
                if ((top - bottom).absoluteValue < 3f) 0f else bottom,
                top
            )
        ).convertTo(distanceAway.units)
    }

    private enum class ClinometerLockState {
        PartiallyUnlocked,
        Unlocked,
        PartiallyLocked,
        Locked
    }


}
