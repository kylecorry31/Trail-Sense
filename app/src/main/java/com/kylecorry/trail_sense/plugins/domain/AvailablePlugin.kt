package com.kylecorry.trail_sense.plugins.domain

data class AvailablePlugin(
    val packageId: String,
    val name: String,
    val version: String?,
    val signatures: List<String>,
    val allPermissions: List<String>,
    val grantedPermissions: List<String>
)
