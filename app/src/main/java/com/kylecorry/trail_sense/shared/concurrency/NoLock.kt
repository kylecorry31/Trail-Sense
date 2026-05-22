package com.kylecorry.trail_sense.shared.concurrency

class NoLock : Lock {
    override fun <T> withLock(key: Any?, block: () -> T): T {
        return block()
    }
}
