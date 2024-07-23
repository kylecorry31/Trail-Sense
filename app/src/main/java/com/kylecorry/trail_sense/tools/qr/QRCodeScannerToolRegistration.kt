package com.kylecorry.trail_sense.tools.qr

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tool
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolCategory
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolRegistration
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics.ToolDiagnosticFactory

object QRCodeScannerToolRegistration : ToolRegistration {
    override fun getTool(context: Context): Tool {
        return Tool(
            Tools.QR_CODE_SCANNER,
            context.getString(R.string.qr_code_scanner),
            R.drawable.ic_qr_code,
            R.id.scanQrFragment,
            ToolCategory.Other,
            guideId = R.raw.guide_tool_qr_code_scanner,
            diagnostics = listOf(
                ToolDiagnosticFactory.camera(context)
            )
        )
    }
}