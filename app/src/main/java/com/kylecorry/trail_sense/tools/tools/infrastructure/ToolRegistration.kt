package com.kylecorry.trail_sense.tools.tools.infrastructure

import android.content.Context
import com.kylecorry.trail_sense.tools.tools.ui.Tool

interface ToolRegistration {

    fun getTool(context: Context): Tool

}