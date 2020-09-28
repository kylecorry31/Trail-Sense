package com.kylecorry.trail_sense.tools.guide.infrastructure

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.guide.domain.UserGuide
import com.kylecorry.trail_sense.tools.guide.domain.UserGuideCategory

object Guides {

    fun guides(context: Context): List<UserGuideCategory> {

        val navigation = UserGuideCategory("Navigation", listOf(
            UserGuide("Example", "An example guide", R.raw.example)
        ))

        return listOf(
           navigation
        )
    }
}