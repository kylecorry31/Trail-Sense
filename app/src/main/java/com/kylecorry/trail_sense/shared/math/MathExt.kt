package com.kylecorry.trail_sense.shared.math

import kotlin.math.pow
import kotlin.math.sqrt

fun FloatArray.normalize(): FloatArray {
    val mag = this.magnitude()
    return this.map { it / mag }.toFloatArray()
}

fun FloatArray.magnitude(): Float {
    return sqrt(this.reduce { acc, value -> acc + value.pow(2) })
}

fun FloatArray.dot(other: FloatArray): Float {
    if (this.size != other.size) {
        throw Exception("Arrays must be the same size to calculate the dot product")
    }

    return this.mapIndexed { index, value -> value * other[index] }.sum()
}

fun FloatArray.cross(other: FloatArray): FloatArray {
    if (this.size != other.size) {
        throw Exception("Arrays must be the same size to calculate the cross product")
    }

    if (this.size != 3) {
        throw Exception("Arrays must be the a length 3 vector")
    }

    return floatArrayOf(
        this[1] * other[2] - this[2] * other[1],
        this[2] * other[0] - this[0] * other[2],
        this[0] * other[1] - this[1] * other[0]
    )
}

fun FloatArray.minus(other: FloatArray): FloatArray {
    if (this.size != other.size) {
        throw Exception("Arrays must be the same size to minus")
    }

    return this.mapIndexed { index, value -> value - other[index] }.toFloatArray()
}

fun FloatArray.scale(by: Float): FloatArray {
    return this.map { it * by }.toFloatArray()
}