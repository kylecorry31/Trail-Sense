package com.kylecorry.trail_sense.shared

class ObjectPool<T>(initialSize: Int = 0, private val factory: () -> T) {

    private val inUse = mutableListOf<T>()
    private val available = MutableList(initialSize) { factory.invoke() }
    private val lock = Object()

    /**
     * Gets an object from the pool, or creates a new one if the pool is empty
     */
    fun get(): T {
        synchronized(lock) {
            val last = available.removeLastOrNull()
            if (last != null) {
                inUse.add(last)
                return last
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
            inUse.remove(obj)
            if (!available.contains(obj)) {
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
                if (!available.contains(obj)) {
                    available.add(obj)
                }
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