package com.kylecorry.trail_sense.plugins.domain

data class Plugin(
    val packageId: String,
    val name: String,
    val version: String?,
    val signatures: List<String>
)
