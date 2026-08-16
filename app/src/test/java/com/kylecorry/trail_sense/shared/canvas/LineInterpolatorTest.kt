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
            1.5f, 1.5f,

            1.5f, 1.5f,
            2f, 2f
        )
        val expectedZ = floatArrayOf(
            1f,
            1.5f,

            1.5f,
            2f
        )
        assertArrayEquals(expectedLine, lineOutput.toFloatArray(), 0.001f)
        assertArrayEquals(expectedZ, zOutput.toFloatArray(), 0.001f)
    }

    @Test
    fun increaseResolutionTwoPointsLongSegment(){
        val interpolator = LineInterpolator()
        val line = mutableListOf(0f, 0f, 0f, 10f)
        val lineOutput = mutableListOf<Float>()
        val z = mutableListOf(0f, 20f)
        val zOutput = mutableListOf<Float>()
        interpolator.increaseResolution(line, lineOutput, 1f, z, zOutput)
        val expectedLine = floatArrayOf(
            0f, 0f,
            0f, 1f,

            0f, 1f,
            0f, 2f,

            0f, 2f,
            0f, 3f,

            0f, 3f,
            0f, 4f,

            0f, 4f,
            0f, 5f,

            0f, 5f,
            0f, 6f,

            0f, 6f,
            0f, 7f,

            0f, 7f,
            0f, 8f,

            0f, 8f,
            0f, 9f,

            0f, 9f,
            0f, 10f
        )
        val expectedZ = floatArrayOf(
            0f, 2f,
            2f, 4f,
            4f, 6f,
            6f, 8f,
            8f, 10f,
            10f, 12f,
            12f, 14f,
            14f, 16f,
            16f, 18f,
            18f, 20f
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
            1.5f, 1.25f,

            1.5f, 1.25f,
            2f, 1.5f
        )
        val expectedZ = floatArrayOf(
            0f,
            1f,

            1f,
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
            1.5f, 1.25f,

            1.5f, 1.25f,
            2f, 1.5f,

            2f, 1.5f,
            2.5f, 1.75f,

            2.5f, 1.75f,
            3f, 2f
        )
        val expectedZ = floatArrayOf(
            0f,
            1f,

            1f,
            2f,

            2f,
            2.5f,

            2.5f,
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
            1.5f, 1.5f,

            1.5f, 1.5f,
            2f, 2f
        )
        assertArrayEquals(expectedLine, lineOutput.toFloatArray(), 0.001f)
    }

}
