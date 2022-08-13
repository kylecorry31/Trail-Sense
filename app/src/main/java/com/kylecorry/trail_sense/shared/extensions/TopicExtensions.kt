package com.kylecorry.trail_sense.shared.extensions

import com.kylecorry.andromeda.core.topics.generic.ITopic

fun <T> ITopic<T>.getOrNull(): T? {
    val current = value
    return if (current.isPresent) {
        current.get()
    } else {
        null
    }
}