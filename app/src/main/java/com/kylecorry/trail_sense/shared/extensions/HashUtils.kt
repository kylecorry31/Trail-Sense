package com.kylecorry.trail_sense.shared.extensions

object HashUtils {

    fun hash(vararg values: Any?): Int {
        var hash = 0
        for (value in values) {
            hash = hash * 31 + (value?.hashCode() ?: 0)
        }
        return hash
    }

}