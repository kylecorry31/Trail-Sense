package com.kylecorry.trail_sense.shared.extensions

import com.kylecorry.andromeda.core.tryOrLog
import com.kylecorry.trail_sense.BuildConfig

fun isDebug(): Boolean {
    return BuildConfig.DEBUG
}

fun ifDebug(fn: () -> Unit){
    if (isDebug()){
        tryOrLog(fn)
    }
}