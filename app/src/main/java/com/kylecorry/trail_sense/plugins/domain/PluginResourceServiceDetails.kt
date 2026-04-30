package com.kylecorry.trail_sense.plugins.domain

data class PluginResourceServiceDetails(
    val packageId: String,
    val name: String,
    val version: String?,
    val isOfficial: Boolean,
    val features: PluginResourceServiceFeatures,
    val allPermissions: List<String>,
    val grantedPermissions: List<String>
)
