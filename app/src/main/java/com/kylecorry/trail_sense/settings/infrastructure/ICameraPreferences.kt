package com.kylecorry.trail_sense.settings.infrastructure

interface ICameraPreferences {
    val useZeroShutterLag: Boolean
    val projectionType: CameraPreferences.ProjectionType
}