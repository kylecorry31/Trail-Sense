package com.kylecorry.trail_sense.shared.permissions

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.fragment.app.Fragment
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.alerts.toast
import com.kylecorry.andromeda.camera.Camera
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.andromeda.fragments.AndromedaFragment
import com.kylecorry.andromeda.fragments.IPermissionRequester
import com.kylecorry.andromeda.markdown.MarkdownService
import com.kylecorry.andromeda.permissions.PermissionRationale
import com.kylecorry.andromeda.permissions.Permissions
import com.kylecorry.andromeda.permissions.SpecialPermission
import com.kylecorry.trail_sense.R

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

fun Fragment.alertExactAlarmsDenied() {
    toast(getString(R.string.exact_alarm_permission_denied), short = false)
}

fun Fragment.requestActivityRecognition(action: (hasPermission: Boolean) -> Unit) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        requirePermissionRequester().requestPermissions(listOf(Manifest.permission.ACTIVITY_RECOGNITION)) {
            action(Permissions.canRecognizeActivity(requireContext()))
        }
    } else {
        action(true)
    }
}

fun Fragment.requestIgnoreBatteryOptimizations(
    action: (hasPermission: Boolean) -> Unit
) {
    requirePermissionRequester().requestPermission(
        SpecialPermission.IGNORE_BATTERY_OPTIMIZATIONS,
        PermissionRationale(
            getString(
                R.string.allow_ignore_battery_restrictions,
                getString(R.string.app_name)
            ),
            AppServiceRegistry.get<MarkdownService>().toMarkdown(
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
        if (!isGranted) {
            toast(getString(R.string.battery_usage_restricted), short = false)
        }
        action(isGranted)
    }
}

fun Fragment.getPermissionRequester(): IPermissionRequester? {
    return if (this is IPermissionRequester) {
        this
    } else if (requireActivity() is IPermissionRequester) {
        requireActivity() as IPermissionRequester
    } else {
        null
    }
}

fun Fragment.requirePermissionRequester(): IPermissionRequester {
    return getPermissionRequester()!!
}

fun <T> T.requestScheduleExactAlarms(action: (hasPermission: Boolean) -> Unit) where T : IPermissionRequester, T : Fragment {
    requestPermission(
        SpecialPermission.SCHEDULE_EXACT_ALARMS,
        PermissionRationale(
            getString(R.string.allow_schedule_exact_alarms, getString(R.string.app_name)),
            AppServiceRegistry.get<MarkdownService>().toMarkdown(
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
        if (!isGranted) {
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

fun Permissions.canStartLocationForgroundService(context: Context): Boolean {
    // Older API versions don't need foreground permission
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        return true
    }

    // The service can be started if it has background permission or if the system says it can get location
    return isBackgroundLocationEnabled(context) || canGetLocation(context, checkAppOps = true)
}

fun Permissions.canGetLocationCustom(context: Context): Boolean {
    return isBackgroundLocationEnabled(context) || canGetLocation(context, checkAppOps = true)
}

/**
 * Request location permission when absolutely required to start a foreground service (Android 14+)
 */
fun Fragment.requestBacktrackPermission(action: (hasPermission: Boolean) -> Unit) {
    if (Permissions.canStartLocationForgroundService(requireContext())) {
        action(true)
        return
    }

    requirePermissionRequester().requestPermissions(
        listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    ) {
        val hasPermission = Permissions.canStartLocationForgroundService(requireContext())
        if (!hasPermission) {
            toast(getString(R.string.backtrack_no_permission))
        }
        action(hasPermission)
    }
}