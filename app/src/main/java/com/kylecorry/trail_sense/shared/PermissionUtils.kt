package com.kylecorry.trail_sense.shared

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.kylecorry.trail_sense.R
import com.kylecorry.trailsensecore.infrastructure.system.UiUtils

object PermissionUtils {

    fun isBackgroundLocationEnabled(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            isLocationEnabled(context)
        } else {
            hasPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
    }

    fun isLocationEnabled(context: Context): Boolean {
        return hasPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
    }

    fun hasPermission(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun requestPermissions(activity: Activity, permissions: List<String>, requestCode: Int) {
        if (permissions.isEmpty()) {
            activity.onRequestPermissionsResult(
                requestCode,
                permissions.toTypedArray(),
                intArrayOf()
            )
            return
        }
        ActivityCompat.requestPermissions(activity, permissions.toTypedArray(), requestCode)
    }

    fun requestPermissionsWithRationale(
        activity: Activity,
        permissions: List<String>,
        rationale: PermissionRationale,
        requestCode: Int
    ) {
        UiUtils.alertWithCancel(
            activity,
            rationale.title,
            rationale.reason,
            R.string.dialog_grant,
            R.string.dialog_deny
        ) { cancelled ->
            if (!cancelled) {
                requestPermissions(activity, permissions, requestCode)
            } else {
                activity.onRequestPermissionsResult(
                    requestCode,
                    permissions.toTypedArray(),
                    intArrayOf(PackageManager.PERMISSION_DENIED)
                )
            }
        }
    }

}