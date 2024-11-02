package com.kylecorry.trail_sense.shared.debugging

import com.kylecorry.andromeda.core.tryOrLog
import com.kylecorry.trail_sense.BuildConfig

fun isDebug(): Boolean {
    return BuildConfig.DEBUG
}

fun ifDebug(fn: () -> Unit) {
    if (isDebug()) {
        tryOrLog(fn)
    }
}

fun getBuildType(): String {
    return BuildConfig.BUILD_TYPE
}

fun isPlayStoreBuild(): Boolean {
    return getBuildType() == "playStore"
}