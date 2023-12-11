package com.kylecorry.trail_sense.tools.guide.infrastructure

import android.content.Context
import androidx.annotation.RawRes

class UserGuideService(private val context: Context) {

    fun load(@RawRes resource: Int): String {
        return context.resources.openRawResource(resource).use {
            it.bufferedReader().readText()
        }
    }

}