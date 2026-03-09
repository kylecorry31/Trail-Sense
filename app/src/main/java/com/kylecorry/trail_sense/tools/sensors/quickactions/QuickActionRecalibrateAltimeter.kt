package com.kylecorry.trail_sense.tools.sensors.quickactions

import android.widget.ImageButton
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.alerts.toast
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.andromeda.sense.altitude.FusedAltimeter
import com.kylecorry.andromeda.sense.readAll
import com.kylecorry.sol.science.meteorology.Meteorology
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.Pressure
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.QuickActionButton
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.extensions.withCancelableLoading
import com.kylecorry.trail_sense.shared.navigateWithAnimation
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.sensors.SensorService
import kotlinx.coroutines.launch

class QuickActionRecalibrateAltimeter(btn: ImageButton, fragment: Fragment) :
    QuickActionButton(btn, fragment) {

    private val prefs by lazy { UserPreferences(fragment.requireContext()) }

    override fun onCreate() {
        super.onCreate()
        setIcon(R.drawable.refresh_altitude)
    }

    override fun onClick() {
        super.onClick()
        val mode = prefs.altimeterMode
        when (mode) {
            UserPreferences.AltimeterMode.Barometer,
            UserPreferences.AltimeterMode.Override -> {
                val distanceUnits = prefs.baseDistanceUnits
                CustomUiUtils.pickElevation(
                    fragment.requireContext(),
                    Distance.meters(prefs.altitudeOverride).convertTo(distanceUnits),
                    fragment.getString(R.string.quick_action_recalibrate_altimeter)
                ) { elevation, cancelled ->
                    if (!cancelled && elevation != null) {
                        fragment.inBackground {
                            val sensorService = SensorService(fragment.requireContext())
                            if (mode == UserPreferences.AltimeterMode.Barometer) {
                                val barometer = sensorService.getBarometer()
                                barometer.read()
                                val seaLevelPressure = Meteorology.getSeaLevelPressure(
                                    Pressure.hpa(barometer.pressure),
                                    elevation.meters()
                                )
                                prefs.altitudeOverride = elevation.meters().value
                                prefs.seaLevelPressureOverride = seaLevelPressure.value
                            } else {
                                prefs.altitudeOverride = elevation.meters().value
                            }
                            fragment.toast(fragment.getString(R.string.elevation_override_updated_toast))
                        }
                    }
                }
            }

            UserPreferences.AltimeterMode.GPSBarometer,
            UserPreferences.AltimeterMode.DigitalElevationModelBarometer -> {
                fragment.inBackground {
                    val sensorService = SensorService(fragment.requireContext())
                    val altimeter = sensorService.getAltimeter()

                    val job = launch {
                        FusedAltimeter.clearCachedCalibration(
                            getAppService<PreferencesSubsystem>().preferences
                        )
                        readAll(listOf(altimeter))
                    }

                    Alerts.withCancelableLoading(
                        fragment.requireContext(),
                        fragment.getString(R.string.recalibrating),
                        onCancel = { job.cancel() }
                    ) {
                        job.join()
                        fragment.toast(fragment.getString(R.string.done))
                    }
                }
            }

            else -> {
                fragment.findNavController().navigateWithAnimation(R.id.calibrateAltimeterFragment)
            }
        }
    }

    override fun onLongClick(): Boolean {
        super.onLongClick()
        fragment.findNavController().navigateWithAnimation(R.id.calibrateAltimeterFragment)
        return true
    }
}
