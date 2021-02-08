package com.kylecorry.trail_sense.shared.math

object MathExtensions {

    fun String.toFloatCompat2(): Float? {
        val asFloat = try {
            this.replace(",", ".").toFloatOrNull()
        } catch (e: Exception){
            null
        }
        asFloat ?: return null
        if (asFloat.isNaN() || asFloat.isInfinite()) {
            return null
        }
        return asFloat
    }
}