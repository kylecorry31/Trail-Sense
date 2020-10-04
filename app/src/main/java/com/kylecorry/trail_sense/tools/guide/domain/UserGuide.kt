package com.kylecorry.trail_sense.tools.guide.domain

import androidx.annotation.RawRes

data class UserGuide(val name: String, val description: String? = null, @RawRes val contents: Int)