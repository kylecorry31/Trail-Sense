package com.kylecorry.trail_sense.tools.tools.infrastructure

import android.content.Context

interface ToolRegistration {

    fun getTool(context: Context): Tool

}