package com.kylecorry.trail_sense.tools.mirror

import android.content.Context
import com.kylecorry.andromeda.camera.Camera
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.volume.SystemVolumeAction
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tool
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolCategory
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolRegistration
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolVolumeAction
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolVolumeActionPriority
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics.ToolDiagnosticFactory

object MirrorCameraToolRegistration : ToolRegistration {
    override fun getTool(context: Context): Tool {
        return Tool(
            Tools.MIRROR_CAMERA,
            context.getString(R.string.mirror_camera),
            R.drawable.ic_mirror_camera,
            R.id.mirrorCameraFragment,
            ToolCategory.Other,
            guideId = R.raw.guide_tool_mirror_camera,
            isAvailable = { Camera.hasFrontCamera(it) },
            volumeActions = listOf(
                ToolVolumeAction(
                    ToolVolumeActionPriority.Normal,
                    { _, isOpen, _ -> isOpen },
                    ::SystemVolumeAction
                )
            ),
            diagnostics = listOf(
                ToolDiagnosticFactory.camera(context),
            )
        )
    }
}