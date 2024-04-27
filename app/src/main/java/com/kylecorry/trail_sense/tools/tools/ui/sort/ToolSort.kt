package com.kylecorry.trail_sense.tools.tools.ui.sort

import com.kylecorry.trail_sense.tools.tools.infrastructure.Tool

interface ToolSort {

    fun sort(tools: List<Tool>): List<CategorizedTools>

}