package com.kylecorry.trail_sense.tools.navigation.ui.errors

import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.core.sensors.Quality
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.diagnostics.domain.DiagnosticCode
import com.kylecorry.trail_sense.tools.navigation.ui.NavigatorFragment
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.ErrorBannerReason
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.alerts.MissingSensorAlert
import com.kylecorry.trail_sense.shared.requireMainActivity
import com.kylecorry.trail_sense.shared.views.UserError
import java.util.Locale

class NavigatorUserErrors(private val fragment: NavigatorFragment) {

    private val banner = fragment.requireMainActivity().errorBanner
    private val formatter = FormatService.getInstance(fragment.requireContext())

    private val possibleErrors = listOf(
        ErrorBannerReason.NoGPS,
        ErrorBannerReason.LocationNotSet,
        ErrorBannerReason.GPSTimeout,
        ErrorBannerReason.NoCompass,
        ErrorBannerReason.CompassPoor
    )

    private val errorMap = mapOf(
        DiagnosticCode.GPSUnavailable to UserError(
            ErrorBannerReason.NoGPS,
            fragment.getString(R.string.location_disabled),
            R.drawable.satellite
        ),
        DiagnosticCode.LocationUnset to UserError(
            ErrorBannerReason.LocationNotSet,
            fragment.getString(R.string.location_not_set),
            R.drawable.satellite,
            fragment.getString(R.string.set)
        ) {
            val navController = fragment.findNavController()
            banner.dismiss(ErrorBannerReason.LocationNotSet)
            navController.navigate(R.id.calibrateGPSFragment)
        },
        DiagnosticCode.GPSTimedOut to UserError(
            ErrorBannerReason.GPSTimeout,
            fragment.getString(R.string.gps_signal_lost),
            R.drawable.satellite
        ),
        DiagnosticCode.MagnetometerPoor to UserError(
            ErrorBannerReason.CompassPoor,
            fragment.getString(
                R.string.compass_calibrate_toast,
                formatter.formatQuality(Quality.Poor).lowercase(Locale.getDefault())
            ),
            R.drawable.ic_compass_icon,
            fragment.getString(R.string.how)
        ) {
            fragment.displayAccuracyTips()
            banner.dismiss(ErrorBannerReason.CompassPoor)
        },
        DiagnosticCode.MagnetometerUnavailable to UserError(
            ErrorBannerReason.NoCompass,
            MissingSensorAlert.getMissingSensorTitle(
                fragment.requireContext(),
                fragment.getString(R.string.pref_compass_sensor_title)
            ),
            R.drawable.ic_compass_icon
        ) {
            MissingSensorAlert(fragment.requireContext()).alert(fragment.getString(R.string.pref_compass_sensor_title))
        }
    )

    private var isTimedOut = false
    private var isGPSErrorShown = false
    private var isCompassErrorShown = false
    private var isPoorCompassShown = false

    fun reset() {
        isTimedOut = false
        isGPSErrorShown = false
        isCompassErrorShown = false
        isPoorCompassShown = false
        possibleErrors.forEach {
            banner.dismiss(it)
        }
    }

    // TODO: Improve architecture using the strategy pattern and remove this class all together
    // TODO: Each error condition should be responsible for its own display and dismissal
    fun update(codes: List<DiagnosticCode>) {

        // Location unset
        if (!isGPSErrorShown && codes.contains(DiagnosticCode.LocationUnset)) {
            show(DiagnosticCode.LocationUnset)
            isGPSErrorShown = true
        } else if (!codes.contains(DiagnosticCode.LocationUnset)) {
            banner.dismiss(ErrorBannerReason.LocationNotSet)
            // Do not reset isGPSErrorShown
        }

        // GPS unavailable
        if (!isGPSErrorShown && codes.contains(DiagnosticCode.GPSUnavailable)) {
            show(DiagnosticCode.GPSUnavailable)
            isGPSErrorShown = true
        } else if (!codes.contains(DiagnosticCode.GPSUnavailable)) {
            banner.dismiss(ErrorBannerReason.NoGPS)
            // Do not reset isGPSErrorShown
        }

        // GPS timed out
        if (!isTimedOut && codes.contains(DiagnosticCode.GPSTimedOut)) {
            show(DiagnosticCode.GPSTimedOut)
            isTimedOut = true
        } else if (!codes.contains(DiagnosticCode.GPSTimedOut)) {
            banner.dismiss(ErrorBannerReason.GPSTimeout)
            isTimedOut = false
        }

        // Compass unavailable
        if (!isCompassErrorShown && codes.contains(DiagnosticCode.MagnetometerUnavailable)) {
            show(DiagnosticCode.MagnetometerUnavailable)
            isCompassErrorShown = true

            // Show a one time dialog containing the missing compass text
            CustomUiUtils.disclaimer(
                fragment.requireContext(),
                MissingSensorAlert.getMissingSensorTitle(
                    fragment.requireContext(),
                    fragment.getString(R.string.pref_compass_sensor_title)
                ),
                MissingSensorAlert.getMissingSensorMessage(
                    fragment.requireContext(),
                    fragment.getString(R.string.pref_compass_sensor_title)
                ),
                "no_compass_message_shown",
                cancelText = null
            )
        } else if (!codes.contains(DiagnosticCode.MagnetometerUnavailable)) {
            banner.dismiss(ErrorBannerReason.NoCompass)
            // Do not reset isCompassErrorShown
        }

        // Compass poor
        if (!isPoorCompassShown && codes.contains(DiagnosticCode.MagnetometerPoor)) {
            show(DiagnosticCode.MagnetometerPoor)
            isPoorCompassShown = true
        } else if (!codes.contains(DiagnosticCode.MagnetometerPoor)) {
            banner.dismiss(ErrorBannerReason.CompassPoor)
            isPoorCompassShown = false
        }

    }

    private fun show(code: DiagnosticCode) {
        val error = errorMap[code]
        if (error != null) {
            banner.report(error)
        }
    }
}