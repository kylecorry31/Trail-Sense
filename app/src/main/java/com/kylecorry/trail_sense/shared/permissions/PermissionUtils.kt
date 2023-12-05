package com.kylecorry.trail_sense.shared.permissions

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.fragment.app.Fragment
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.alerts.toast
import com.kylecorry.andromeda.camera.Camera
import com.kylecorry.andromeda.fragments.AndromedaFragment
import com.kylecorry.andromeda.fragments.IPermissionRequester
import com.kylecorry.andromeda.sense.location.GPS
import com.kylecorry.andromeda.markdown.MarkdownService
import com.kylecorry.andromeda.permissions.PermissionRationale
import com.kylecorry.andromeda.permissions.Permissions
import com.kylecorry.andromeda.permissions.SpecialPermission
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.sensors.SensorService

fun Fragment.alertNoCameraPermission() {
    Alerts.toast(
        requireContext(),
        getString(R.string.camera_permission_denied),
        short = false
    )
}

fun Fragment.alertNoActivityRecognitionPermission() {
    Alerts.toast(
        requireContext(),
        getString(R.string.activity_recognition_permission_denied),
        short = false
    )
}

fun Fragment.alertBatteryUsageRestricted(){
    toast(getString(R.string.battery_usage_restricted), short = false)
}

fun Fragment.alertExactAlarmsDenied(){
    toast(getString(R.string.exact_alarm_permission_denied), short = false)
}

fun <T> T.requestActivityRecognition(action: (hasPermission: Boolean) -> Unit) where T : IPermissionRequester, T : Fragment {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        requestPermissions(listOf(Manifest.permission.ACTIVITY_RECOGNITION)) {
            action(Permissions.canRecognizeActivity(requireContext()))
        }
    } else {
        action(true)
    }
}

fun <T> T.requestIgnoreBatteryOptimizations(action: (hasPermission: Boolean) -> Unit) where T : IPermissionRequester, T : Fragment {
    requestPermission(
        SpecialPermission.IGNORE_BATTERY_OPTIMIZATIONS,
        PermissionRationale(
            getString(R.string.allow_ignore_battery_restrictions, getString(R.string.app_name)),
            MarkdownService(requireContext()).toMarkdown(
                getString(
                    R.string.allow_ignore_battery_restrictions_instructions,
                    getString(R.string.settings)
                )
            ),
            ok = getString(R.string.settings),
        )
    ) {
        val isGranted = Permissions.hasPermission(
            requireContext(),
            SpecialPermission.IGNORE_BATTERY_OPTIMIZATIONS
        )
        if (!isGranted){
            alertBatteryUsageRestricted()
        }
        action(isGranted)
    }
}

fun <T> T.requestScheduleExactAlarms(action: (hasPermission: Boolean) -> Unit) where T : IPermissionRequester, T : Fragment {
    requestPermission(
        SpecialPermission.SCHEDULE_EXACT_ALARMS,
        PermissionRationale(
            getString(R.string.allow_schedule_exact_alarms, getString(R.string.app_name)),
            MarkdownService(requireContext()).toMarkdown(
                getString(
                    R.string.allow_schedule_exact_alarms_instructions,
                    getString(R.string.app_name),
                    getString(R.string.settings)
                )
            ),
            ok = getString(R.string.settings),
        )
    ) {
        val isGranted = Permissions.hasPermission(
            requireContext(),
            SpecialPermission.SCHEDULE_EXACT_ALARMS
        )
        if (!isGranted){
            alertExactAlarmsDenied()
        }
        action(isGranted)
    }
}

fun AndromedaFragment.requestCamera(action: (hasPermission: Boolean) -> Unit) {
    requestPermissions(listOf(Manifest.permission.CAMERA)) {
        action(Camera.isAvailable(requireContext()))
    }
}

fun Permissions.canRunLocationForegroundService(context: Context, isInBackground: Boolean = false): Boolean {
    // Older API versions don't need foreground permission
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        return true
    }

    // If it is not in the background, just check if it has location permissions
    if (!isInBackground){
        return canGetLocation(context)
    }

    // The app is in the background, some restrictions apply

    // This is not a restriction, but appears to be a bug: https://issuetracker.google.com/issues/294408576
    if (!GPS.isAvailable(context)){
        return false
    }

    // If background location is granted, we can start the service
    if (isBackgroundLocationEnabled(context)){
        return true
    }

    // Otherwise, we can only start the service if it is ignoring battery optimizations and has location permission
    return isIgnoringBatteryOptimizations(context) && canGetLocation(context)
}

/**
 * Request location permission when absolutely required to start a foreground service (Android 14+)
 */
fun <T> T.requestLocationForegroundServicePermission(action: (hasPermission: Boolean) -> Unit) where T : IPermissionRequester, T : Fragment {
    if (Permissions.canRunLocationForegroundService(requireContext())) {
        action(true)
        return
    }

    requestPermissions(listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) {
        val hasPermission = Permissions.canRunLocationForegroundService(requireContext())
        if (!hasPermission){
            toast(getString(R.string.backtrack_no_permission))
        }
        action(hasPermission)
    }
}