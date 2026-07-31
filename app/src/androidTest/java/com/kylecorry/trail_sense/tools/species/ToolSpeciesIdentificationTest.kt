package com.kylecorry.trail_sense.tools.species

import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isTrue
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isVisible
import com.kylecorry.trail_sense.test_utils.TestUtils.context
import com.kylecorry.trail_sense.test_utils.TestUtils.isCameraInUse
import com.kylecorry.trail_sense.test_utils.ToolTestBase
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import org.junit.Test

class ToolSpeciesIdentificationTest : ToolTestBase(Tools.SPECIES_IDENTIFICATION) {

    @Test
    fun verifyBasicFunctionality() {
        if (!Tools.isToolAvailable(context, Tools.SPECIES_IDENTIFICATION)) {
            return
        }

        isVisible(R.id.camera)
        isVisible(R.id.species_frame)
        isVisible(R.id.species_result_card)
        isTrue(20000) {
            isCameraInUse(isBackFacing = true)
        }
    }
}
