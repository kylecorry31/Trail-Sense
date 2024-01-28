package com.kylecorry.trail_sense.tools.clouds.ui

import com.kylecorry.sol.science.meteorology.clouds.CloudGenus

internal data class CloudSelection(
    val genus: CloudGenus?,
    val confidence: Float?,
    val isSelected: Boolean
)