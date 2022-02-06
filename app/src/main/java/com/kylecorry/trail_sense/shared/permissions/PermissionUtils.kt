package com.kylecorry.trail_sense.shared.permissions

import android.Manifest
import android.os.Build
import androidx.fragment.app.Fragment
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.camera.Camera
import com.kylecorry.andromeda.fragments.AndromedaFragment
import com.kylecorry.andromeda.fragments.IPermissionRequester
import com.kylecorry.andromeda.permissions.Permissions
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

fun <T> T.requestActivityRecognition(action: (hasPermission: Boolean) -> Unit) where T : IPermissionRequester, T: Fragment {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        requestPermissions(listOf(Manifest.permission.ACTIVITY_RECOGNITION)) {
            action(Permissions.canRecognizeActivity(requireContext()))
        }
    } else {
        action(true)
    }
}

fun AndromedaFragment.requestCamera(action: (hasPermission: Boolean) -> Unit) {
    requestPermissions(listOf(Manifest.permission.CAMERA)) {
        action(Camera.isAvailable(requireContext()))
    }
}