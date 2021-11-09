package com.kylecorry.trail_sense.shared

class ObjectPool<T>(
    private val maxSize: Int? = null,
    private val factory: () -> T
) {

    private val inUse = mutableListOf<T>()
    private val available = mutableSetOf<T>()
    private val lock = Object()

    val size: Int
        get() = inUse.size + available.size

    /**
     * Gets an object from the pool, or creates a new one if the pool is empty
     */
    fun get(): T {
        synchronized(lock) {
            val existing = available.firstOrNull()
            if (existing != null) {
                available.remove(existing)
                inUse.add(existing)
                return existing
            }

            if (maxSize != null && size >= maxSize) {
                throw RuntimeException("The pool is already at the maximum size")
            }

            val obj = factory.invoke()
            inUse.add(obj)
            return obj
        }
    }

    /**
     * Releases an object back into the pool
     */
    fun release(obj: T) {
        synchronized(lock) {
            val removed = inUse.remove(obj)
            if (removed) {
                available.add(obj)
            }
        }
    }

    /**
     * Moves all objects back into the pool
     */
    fun reset() {
        synchronized(lock) {
            for (obj in inUse) {
                inUse.remove(obj)
                available.add(obj)
            }
        }
    }

    /**
     * Releases all objects and empties the pool
     */
    fun clear() {
        synchronized(lock) {
            inUse.clear()
            available.clear()
        }
    }

}