package com.kylecorry.trail_sense.tools.diagnostics.infrastructure

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import com.kylecorry.andromeda.permissions.Permissions
import com.kylecorry.trail_sense.tools.diagnostics.domain.DiagnosticCode
import com.kylecorry.trail_sense.tools.diagnostics.domain.IDiagnostic

class CameraDiagnostic(private val context: Context) : IDiagnostic {

    @SuppressLint("UnsupportedChromeOsCameraSystemFeature")
    override fun scan(): List<DiagnosticCode> {
        val issues = mutableListOf<DiagnosticCode>()

        if (!Permissions.isCameraEnabled(context)) {
            issues.add(DiagnosticCode.CameraNoPermission)
        }

        if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            issues.add(DiagnosticCode.CameraUnavailable)
        }

        return issues
    }
}