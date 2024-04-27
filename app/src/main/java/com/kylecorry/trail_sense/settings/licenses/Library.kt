package com.kylecorry.trail_sense.settings.licenses

data class Library(val name: String, val url: String, val license: () -> String)