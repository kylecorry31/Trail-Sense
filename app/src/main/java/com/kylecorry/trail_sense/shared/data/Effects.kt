package com.kylecorry.trail_sense.shared.data

/**
 * A class that allows for running effects only when the state changes
 */
class Effects {

    private val stateEffects = mutableMapOf<String, StateEffect>()
    private val lock = Any()

    /**
     * Run an effect only if the state changes
     * @param key The key for the effect (should be unique for each effect)
     * @param values The values that the effect depends on
     * @param action The action to run if the state changes
     */
    fun run(key: String, vararg values: Any?, action: () -> Unit) {
        val effect = synchronized(lock) {
            stateEffects.getOrPut(key) { StateEffect() }
        }
        effect.runIfChanged(*values, action = action)
    }

}