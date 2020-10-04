package com.kylecorry.trail_sense.tools.guide.infrastructure

import android.content.Context
import androidx.annotation.RawRes

class UserGuideService(private val context: Context) {

    fun load(@RawRes resource: Int): String {
        val input = context.resources.openRawResource(resource)
        val text = input.bufferedReader().readText()
        input.close()
        return text
    }

}