package com.kylecorry.trail_sense.shared.canvas

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class LineInterpolatorTest {

    @Test
    fun increaseResolutionNoPoints(){
        val interpolator = LineInterpolator()
        val line = mutableListOf<Float>()
        val lineOutput = mutableListOf<Float>()
        val z = mutableListOf<Float>()
        val zOutput = mutableListOf<Float>()
        interpolator.increaseResolution(line, lineOutput, 1f, z, zOutput)
        assertEquals(0, lineOutput.size)
        assertEquals(0, zOutput.size)
    }

    @Test
    fun increaseResolutionOnePoint(){
        val interpolator = LineInterpolator()
        val line = mutableListOf(1f, 1f)
        val lineOutput = mutableListOf<Float>()
        val z = mutableListOf(1f)
        val zOutput = mutableListOf<Float>()
        interpolator.increaseResolution(line, lineOutput, 1f, z, zOutput)
        assertArrayEquals(line.toFloatArray(), lineOutput.toFloatArray(), 0.001f)
        assertArrayEquals(z.toFloatArray(), zOutput.toFloatArray(), 0.001f)
    }

    @Test
    fun increaseResolutionThreePoints(){
        val interpolator = LineInterpolator()
        val line = mutableListOf(1f, 1f, 2f, 2f, 3f, 3f)
        val lineOutput = mutableListOf<Float>()
        val z = mutableListOf(1f, 2f, 3f)
        val zOutput = mutableListOf<Float>()
        interpolator.increaseResolution(line, lineOutput, 1f, z, zOutput)
        assertArrayEquals(line.toFloatArray(), lineOutput.toFloatArray(), 0.001f)
        assertArrayEquals(z.toFloatArray(), zOutput.toFloatArray(), 0.001f)
    }

    @Test
    fun increaseResolutionTwoPointsAlreadySpaced(){
        val interpolator = LineInterpolator()
        val line = mutableListOf(0f, 1f, 0f, 2f)
        val lineOutput = mutableListOf<Float>()
        val z = mutableListOf(1f, 2f)
        val zOutput = mutableListOf<Float>()
        interpolator.increaseResolution(line, lineOutput, 1f, z, zOutput)
        assertArrayEquals(line.toFloatArray(), lineOutput.toFloatArray(), 0.001f)
        assertArrayEquals(z.toFloatArray(), zOutput.toFloatArray(), 0.001f)
    }

    @Test
    fun increaseResolutionTwoPointsTooShort(){
        val interpolator = LineInterpolator()
        val line = mutableListOf(0f, 1f, 0f, 1.5f)
        val lineOutput = mutableListOf<Float>()
        val z = mutableListOf(1f, 1.5f)
        val zOutput = mutableListOf<Float>()
        interpolator.increaseResolution(line, lineOutput, 1f, z, zOutput)
        assertArrayEquals(line.toFloatArray(), lineOutput.toFloatArray(), 0.001f)
        assertArrayEquals(z.toFloatArray(), zOutput.toFloatArray(), 0.001f)
    }

    @Test
    fun increaseResolutionTwoPoints45(){
        val interpolator = LineInterpolator()
        val line = mutableListOf(1f, 1f, 2f, 2f)
        val lineOutput = mutableListOf<Float>()
        val z = mutableListOf(1f, 2f)
        val zOutput = mutableListOf<Float>()
        interpolator.increaseResolution(line, lineOutput, 0.5f, z, zOutput)
        val expectedLine = floatArrayOf(
            1f, 1f,
            1.125f, 1.125f,

            1.125f, 1.125f,
            1.25f, 1.25f,

            1.25f, 1.25f,
            1.375f, 1.375f,

            1.375f, 1.375f,
            1.5f, 1.5f,

            1.5f, 1.5f,
            1.625f, 1.625f,

            1.625f, 1.625f,
            1.75f, 1.75f,

            1.75f, 1.75f,
            1.875f, 1.875f,

            1.875f, 1.875f,
            2f, 2f
        )
        val expectedZ = floatArrayOf(
            1f,
            1.125f,

            1.125f,
            1.25f,

            1.25f,
            1.375f,

            1.375f,
            1.5f,

            1.5f,
            1.625f,

            1.625f,
            1.75f,

            1.75f,
            1.875f,

            1.875f,
            2f
        )
        assertArrayEquals(expectedLine, lineOutput.toFloatArray(), 0.001f)
        assertArrayEquals(expectedZ, zOutput.toFloatArray(), 0.001f)
    }

    @Test
    fun increaseResolutionTwoPointsUneven(){
        val interpolator = LineInterpolator()
        val line = mutableListOf(1f, 1f, 2f, 1.5f)
        val lineOutput = mutableListOf<Float>()
        val z = mutableListOf(0f, 2f)
        val zOutput = mutableListOf<Float>()
        interpolator.increaseResolution(line, lineOutput, 0.5f, z, zOutput)
        val expectedLine = floatArrayOf(
            1f, 1f,
            1.2f, 1.1f,

            1.2f, 1.1f,
            1.4f, 1.2f,

            1.4f, 1.2f,
            1.6f, 1.3f,

            1.6f, 1.3f,
            1.8f, 1.4f,

            1.8f, 1.4f,
            2f, 1.5f
        )
        val expectedZ = floatArrayOf(
            0f,
            0.4f,

            0.4f,
            0.8f,

            0.8f,
            1.2f,

            1.2f,
            1.6f,

            1.6f,
            2f
        )
        assertArrayEquals(expectedLine, lineOutput.toFloatArray(), 0.001f)
        assertArrayEquals(expectedZ, zOutput.toFloatArray(), 0.001f)
    }

    @Test
    fun increaseResolutionMultiplePoints(){
        val interpolator = LineInterpolator()
        val line = mutableListOf(1f, 1f, 2f, 1.5f, 2f, 1.5f, 3f, 2f)
        val lineOutput = mutableListOf<Float>()
        val z = mutableListOf(0f, 2f, 3f)
        val zOutput = mutableListOf<Float>()
        interpolator.increaseResolution(line, lineOutput, 0.5f, z, zOutput)
        val expectedLine = floatArrayOf(
            1f, 1f,
            1.2f, 1.1f,

            1.2f, 1.1f,
            1.4f, 1.2f,

            1.4f, 1.2f,
            1.6f, 1.3f,

            1.6f, 1.3f,
            1.8f, 1.4f,

            1.8f, 1.4f,
            2f, 1.5f,

            2f, 1.5f,
            2.2f, 1.6f,

            2.2f, 1.6f,
            2.4f, 1.7f,

            2.4f, 1.7f,
            2.6f, 1.8f,

            2.6f, 1.8f,
            2.8f, 1.9f,

            2.8f, 1.9f,
            3f, 2f
        )
        val expectedZ = floatArrayOf(
            0f,
            0.4f,

            0.4f,
            0.8f,

            0.8f,
            1.2f,

            1.2f,
            1.6f,

            1.6f,
            2f,

            2f,
            2.2f,

            2.2f,
            2.4f,

            2.4f,
            2.6f,

            2.6f,
            2.8f,

            2.8f,
            3f
        )
        assertArrayEquals(expectedLine, lineOutput.toFloatArray(), 0.001f)
        assertArrayEquals(expectedZ, zOutput.toFloatArray(), 0.001f)
    }

    @Test
    fun increaseResolutionNoZ(){
        val interpolator = LineInterpolator()
        val line = mutableListOf(1f, 1f, 2f, 2f)
        val lineOutput = mutableListOf<Float>()
        interpolator.increaseResolution(line, lineOutput, 0.5f)
        val expectedLine = floatArrayOf(
            1f, 1f,
            1.125f, 1.125f,

            1.125f, 1.125f,
            1.25f, 1.25f,

            1.25f, 1.25f,
            1.375f, 1.375f,

            1.375f, 1.375f,
            1.5f, 1.5f,

            1.5f, 1.5f,
            1.625f, 1.625f,

            1.625f, 1.625f,
            1.75f, 1.75f,

            1.75f, 1.75f,
            1.875f, 1.875f,

            1.875f, 1.875f,
            2f, 2f
        )
        assertArrayEquals(expectedLine, lineOutput.toFloatArray(), 0.001f)
    }

}