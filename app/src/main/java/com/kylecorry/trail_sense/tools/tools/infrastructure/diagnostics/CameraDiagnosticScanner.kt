package com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import com.kylecorry.andromeda.permissions.Permissions
import com.kylecorry.trail_sense.R
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class CameraDiagnosticScanner : ToolDiagnosticScanner {
    @SuppressLint("UnsupportedChromeOsCameraSystemFeature")
    override fun quickScan(context: Context): List<ToolDiagnosticResult> {
        val issues = mutableListOf<ToolDiagnosticResult>()

        if (!Permissions.isCameraEnabled(context)) {
            issues.add(
                ToolDiagnosticResult(
                    "camera-no-permission",
                    ToolDiagnosticSeverity.Warning,
                    context.getString(R.string.camera),
                    context.getString(R.string.no_permission),
                    context.getString(
                        R.string.grant_permission,
                        context.getString(R.string.camera)
                    ),
                    ToolDiagnosticAction.permissions(context)
                )
            )
        }

        if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            issues.add(
                ToolDiagnosticResult(
                    "camera-unavailable",
                    ToolDiagnosticSeverity.Warning,
                    context.getString(R.string.camera),
                    context.getString(R.string.unavailable)
                )
            )
        }

        return issues
    }

    override fun fullScan(context: Context): Flow<List<ToolDiagnosticResult>> {
        return flowOf(quickScan(context))
    }
}
