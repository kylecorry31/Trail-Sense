package com.kylecorry.trail_sense.shared.data

class TrackedState<T>(initialState: T) {
    private var state: T = initialState
        set(value) {
            if (value != field) {
                hasChanges = true
            }
            field = value
        }

    /**
     * True if the state has changed since the last read
     */
    var hasChanges = true

    /**
     * Read the state
     * @return the state
     */
    fun read(): T {
        hasChanges = false
        return state
    }

    /**
     * Write the state
     * @param value the value to write
     */
    fun write(value: T) {
        state = value
    }

    fun peek(): T {
        return state
    }
}