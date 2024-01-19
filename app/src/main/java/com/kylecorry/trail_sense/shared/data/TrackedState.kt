package com.kylecorry.trail_sense.shared.data

data class TrackedState<T>(val initialState: T) {
    private var state: T = initialState
        set(value) {
            field = value
            hasChanges = true
        }

    /**
     * True if the state has changed since the last read
     */
    var hasChanges = false

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
}