package com.kylecorry.trail_sense.tools.metaldetector.ui

import com.kylecorry.trailsensecore.domain.math.Vector3
import kotlin.math.sqrt

data class Quaternion(val x: Float, val y: Float, val z: Float, val w: Float) {

    fun toFloatArray(): FloatArray {
        return floatArrayOf(x, y, z, w)
    }

    operator fun times(other: Quaternion): Quaternion {
        val out = FloatArray(4)
        QuaternionMath.multiply(toFloatArray(), other.toFloatArray(), out)
        return from(out)
    }

    operator fun plus(other: Quaternion): Quaternion {
        val out = FloatArray(4)
        QuaternionMath.add(toFloatArray(), other.toFloatArray(), out)
        return from(out)
    }

    operator fun minus(other: Quaternion): Quaternion {
        val out = FloatArray(4)
        QuaternionMath.subtract(toFloatArray(), other.toFloatArray(), out)
        return from(out)
    }

    fun rotate(vector: Vector3): Vector3 {
        val out = FloatArray(3)
        QuaternionMath.rotate(vector.toFloatArray(), toFloatArray(), out)
        return Vector3(out[0], out[1], out[2])
    }

    companion object {
        val zero = Quaternion(0f, 0f, 0f, 1f)

        fun from(arr: FloatArray): Quaternion {
            return Quaternion(arr[0], arr[1], arr[2], arr[3])
        }
    }

}


object QuaternionMath {
    const val X = 0
    const val Y = 1
    const val Z = 2
    const val W = 3

    fun rotate(point: FloatArray, quat: FloatArray, out: FloatArray) {
        val u = floatArrayOf(quat[X], quat[Y], quat[Z])
        val s = quat[W]
        val first = Vector3Utils.times(u, Vector3Utils.dot(u, point) * 2f)
        val second = Vector3Utils.times(point, s * s - Vector3Utils.dot(u, u))
        val third = Vector3Utils.times(Vector3Utils.cross(u, point), 2f * s)

        val sum = Vector3Utils.plus(first, Vector3Utils.plus(second, third))
        out[0] = sum[0]
        out[1] = sum[1]
        out[2] = sum[2]
    }

    fun multiply(a: FloatArray, b: FloatArray, out: FloatArray) {
        val x = a[W] * b[X] + a[X] * b[W] + a[Y] * b[Z] - a[Z] * b[Y]
        val y = a[W] * b[Y] - a[X] * b[Z] + a[Y] * b[W] + a[Z] * b[X]
        val z = a[W] * b[Z] + a[X] * b[Y] - a[Y] * b[X] + a[Z] * b[W]
        val w = a[W] * b[W] - a[X] * b[X] - a[Y] * b[Y] - a[Z] * b[Z]
        out[X] = x
        out[Y] = y
        out[Z] = z
        out[W] = w
    }

    fun add(a: FloatArray, b: FloatArray, out: FloatArray) {
        out[X] = a[X] + b[X]
        out[Y] = a[Y] + b[Y]
        out[Z] = a[Z] + b[Z]
        out[W] = a[W] + b[W]
    }

    fun subtract(a: FloatArray, b: FloatArray, out: FloatArray) {
        out[X] = a[X] - b[X]
        out[Y] = a[Y] - b[Y]
        out[Z] = a[Z] - b[Z]
        out[W] = a[W] - b[W]
    }

    fun magnitude(quat: FloatArray): Float {
        return sqrt(quat[X] * quat[X] + quat[Y] * quat[Y] + quat[Z] * quat[Z] + quat[W] * quat[W])
    }

    fun normalize(quat: FloatArray, out: FloatArray) {
        val mag = magnitude(quat)
        divide(quat, mag, out)
    }

    fun divide(quat: FloatArray, divisor: Float, out: FloatArray) {
        out[X] = quat[X] / divisor
        out[Y] = quat[Y] / divisor
        out[Z] = quat[Z] / divisor
        out[W] = quat[W] / divisor
    }

    fun multiply(quat: FloatArray, scale: Float, out: FloatArray) {
        out[X] = quat[X] * scale
        out[Y] = quat[Y] * scale
        out[Z] = quat[Z] * scale
        out[W] = quat[W] * scale
    }

    fun conjugate(quat: FloatArray, out: FloatArray) {
        out[X] = -quat[X]
        out[Y] = -quat[Y]
        out[Z] = -quat[Z]
        out[W] = quat[W]
    }
}