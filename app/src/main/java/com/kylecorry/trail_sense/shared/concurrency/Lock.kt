package com.kylecorry.trail_sense.shared.concurrency

interface Lock {
    fun <T> withLock(key: Any?, block: () -> T): T
}
