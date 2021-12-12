package com.kylecorry.trail_sense.tools.clinometer.ui

import android.Manifest
import android.os.Bundle
import android.view.LayoutInflater
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
import com.kylecorry.sol.math.SolMath
import com.kylecorry.sol.science.geology.AvalancheRisk
import com.kylecorry.sol.science.geology.GeologyService
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentClinometerBinding
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.tools.clinometer.infrastructure.CameraClinometer
import com.kylecorry.trail_sense.tools.clinometer.infrastructure.Clinometer
import com.kylecorry.trail_sense.tools.clinometer.infrastructure.SideClinometer

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
    private val geology = GeologyService()
    private val formatter by lazy { FormatService(requireContext()) }
    private val throttle = Throttle(20)

    private lateinit var clinometer: Clinometer

    private var slopeIncline: Float? = null
    private var slopeAngle: Float? = null

    private var useCamera = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        clinometer = getClinometer()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        CustomUiUtils.setButtonState(binding.clinometerLeftQuickAction, false)
        CustomUiUtils.setButtonState(binding.clinometerRightQuickAction, false)

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

        binding.root.setOnClickListener {
            if (slopeIncline == null && isOrientationValid()) {
                slopeAngle = clinometer.angle
                slopeIncline = clinometer.incline
            } else {
                slopeAngle = null
                slopeIncline = null
            }
        }

        sideClinometer.asLiveData().observe(viewLifecycleOwner, { updateUI() })
        cameraClinometer.asLiveData().observe(viewLifecycleOwner, { updateUI() })
        deviceOrientation.asLiveData().observe(viewLifecycleOwner, { updateUI() })

    }

    override fun onPause() {
        super.onPause()
        if (useCamera) {
            camera.stop(null)
            useCamera = false
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

        if (!isOrientationValid() && slopeIncline == null) {
            binding.clinometerInstructions.text = getString(R.string.clinometer_rotate_device)
            return
        }

        binding.cameraView.isVisible = useCamera
        binding.viewCameraLine.isVisible = useCamera
        binding.clinometer.isInvisible = useCamera

        binding.clinometerInstructions.text = getString(R.string.set_inclination_instructions)

        val angle = slopeAngle ?: clinometer.angle
        val incline = slopeIncline ?: clinometer.incline

        val avalancheRisk = geology.getAvalancheRisk(incline)

        binding.clinometer.angle = angle
        binding.lock.isVisible = slopeAngle != null

        binding.inclination.text = formatter.formatDegrees(incline)
        binding.avalancheRisk.title = getAvalancheRiskString(avalancheRisk)

        binding.inclinationDescription.text =
            getString(R.string.slope_amount, formatter.formatPercentage(getSlopePercent(incline)))

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

}
