package com.kylecorry.trail_sense.shared.concurrency

class StripedLock(stripes: Int = DEFAULT_STRIPES) : Lock {

    private val locks: Array<Any>

    init {
        require(stripes > 0) { "stripes must be greater than 0" }
        locks = Array(stripes) { Any() }
    }

    override fun <T> withLock(key: Any?, block: () -> T): T {
        return synchronized(getLock(key)) {
            block()
        }
    }

    private fun getLock(key: Any?): Any {
        val hash = key?.hashCode() ?: 0
        return locks[Math.floorMod(hash, locks.size)]
    }

    companion object {
        private const val DEFAULT_STRIPES = 64
    }
}
