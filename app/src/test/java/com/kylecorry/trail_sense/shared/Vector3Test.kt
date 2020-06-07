package com.kylecorry.trail_sense.shared

import com.kylecorry.trail_sense.shared.domain.Vector3
import org.junit.Test

import org.junit.Assert.*
import kotlin.math.sqrt

class Vector3Test {

    @Test
    fun cross() {
        val vec1 = Vector3(1f, 2f, 3f)
        val vec2 = Vector3(2f, 3f, 4f)
        val expected =
            Vector3(-1f, 2f, -1f)

        val cross = vec1.cross(vec2)

        assertEquals(expected, cross)
    }

    @Test
    fun minus() {
        val vec1 = Vector3(1f, 3f, 3f)
        val vec2 = Vector3(2f, 2f, 6f)
        val expected =
            Vector3(-1f, 1f, -3f)

        assertEquals(expected, vec1 - vec2)
    }

    @Test
    fun plus() {
        val vec1 = Vector3(1f, 3f, 3f)
        val vec2 = Vector3(2f, 2f, 6f)
        val expected =
            Vector3(3f, 5f, 9f)

        assertEquals(expected, vec1 + vec2)
    }

    @Test
    fun times() {
        val vec = Vector3(1f, 3f, 4f)
        val expected =
            Vector3(2f, 6f, 8f)

        assertEquals(expected, vec * 2f)
    }

    @Test
    fun toFloatArray() {
        val vec = Vector3(1f, 3f, 4f)
        val expected = floatArrayOf(1f, 3f, 4f)

        assertArrayEquals(expected, vec.toFloatArray(), 0.001f)
    }

    @Test
    fun dot() {
        val vec1 = Vector3(1f, 3f, 3f)
        val vec2 = Vector3(2f, 2f, 6f)
        val expected = 26f

        assertEquals(expected, vec1.dot(vec2))
    }

    @Test
    fun magnitude() {
        val vec = Vector3(1f, 3f, 4f)
        val expected = sqrt(26f)

        assertEquals(expected, vec.magnitude(), 0.001f)
    }

    @Test
    fun normalize() {
        val vec = Vector3(3f, 4f, 12f)
        val magnitude = 13f
        val expected = Vector3(
            3 / magnitude,
            4 / magnitude,
            12 / magnitude
        )

        assertEquals(expected, vec.normalize())
    }
}