package com.kylecorry.trail_sense.tools.guide.infrastructure

import android.content.Context
import com.kylecorry.trail_sense.tools.guide.domain.UserGuide

class UserGuideService(private val context: Context) {

    fun load(guide: UserGuide): String {
        val input = context.resources.openRawResource(guide.contents)
        val text = input.bufferedReader().readText()
        input.close()
        return text
    }

}