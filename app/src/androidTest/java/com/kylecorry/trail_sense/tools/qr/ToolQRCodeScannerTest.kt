package com.kylecorry.trail_sense.tools.qr

import com.kylecorry.andromeda.camera.Camera
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasText
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isTrue
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isVisible
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.string
import com.kylecorry.trail_sense.test_utils.TestUtils.context
import com.kylecorry.trail_sense.test_utils.TestUtils.isCameraInUse
import com.kylecorry.trail_sense.test_utils.ToolTestBase
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import org.junit.Assume.assumeTrue
import org.junit.Test

class ToolQRCodeScannerTest : ToolTestBase(Tools.QR_CODE_SCANNER) {

    @Test
    fun showsThatNoQRCodeIsDetected() {
        isVisible(R.id.camera)

        hasText(R.id.qr_text, string(R.string.no_qr_code_detected))
    }

    @Test
    fun scansWithTheBackCamera() {
        assumeTrue(Camera.hasBackCamera(context))

        isTrue(10000) {
            isCameraInUse(isBackFacing = true)
        }
    }
}
