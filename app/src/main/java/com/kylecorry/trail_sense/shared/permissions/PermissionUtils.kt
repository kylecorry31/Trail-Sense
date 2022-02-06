package com.kylecorry.trail_sense.shared.permissions

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.fragment.app.Fragment
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.fragments.AndromedaFragment
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

fun AndromedaFragment.requestActivityRecognition(action: (hasPermission: Boolean) -> Unit){
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        requestPermissions(listOf(Manifest.permission.ACTIVITY_RECOGNITION)){
            action(Permissions.canRecognizeActivity(requireContext()))
        }
    } else {
        action(true)
    }
}

// TODO: Add better support for the settings fragment in Andromeda
fun requestActivityRecognition(context: Context, requestPermissions: (permissions: List<String>, action: () -> Unit) -> Unit, action: (hasPermission: Boolean) -> Unit){
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        requestPermissions(listOf(Manifest.permission.ACTIVITY_RECOGNITION)){
            action(Permissions.canRecognizeActivity(context))
        }
    } else {
        action(true)
    }
}