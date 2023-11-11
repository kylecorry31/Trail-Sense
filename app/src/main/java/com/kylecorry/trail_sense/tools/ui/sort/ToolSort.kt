package com.kylecorry.trail_sense.tools.ui.sort

import com.kylecorry.trail_sense.tools.ui.Tool

interface ToolSort {

    fun sort(tools: List<Tool>): List<CategorizedTools>

}