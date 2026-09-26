package com.kylecorry.trail_sense.tools.clouds.domain.classification

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CloudConvolutionalNetworkTest {

    @Test
    fun inferenceProducesNormalizedProbabilities() {
        val network = CloudConvolutionalNetwork.fromWeights(
            FloatArray(CloudConvolutionalNetwork.WEIGHT_COUNT) { index ->
                (index % 13 - 6) * 0.001f
            }
        )
        val input = FloatArray(
            CloudConvolutionalNetwork.INPUT_SIZE *
                CloudConvolutionalNetwork.INPUT_SIZE *
                CloudConvolutionalNetwork.INPUT_CHANNELS
        ) { index -> if (index % 7 < 3) 1f else 0f }

        val probabilities = network.probabilities(input)
        assertEquals(CloudConvolutionalNetwork.OUTPUTS, probabilities.size)
        assertTrue(probabilities.all { it.isFinite() && it in 0f..1f })
        assertEquals(1f, probabilities.sum(), 0.0001f)
    }

    @Test
    fun matchesPythonNetwork() {
        // Generated with folded CloudCNN in scripts/experiments/cloud_cnn.py (PyTorch).
        val weights = FloatArray(CloudConvolutionalNetwork.WEIGHT_COUNT) {
            (it % 23 - 11) * 0.03f
        }
        val input = FloatArray(3 * 128 * 128) { (it % 251) / 250f }
        val expected = floatArrayOf(
            0.00415386166f, 0.63144803f, 0.0178515408f, 0.00464494666f, 0.00957910717f,
            0.120537125f, 0.194114f, 0.0104779107f, 0.0034853369f, 0.0037079975f
        )
        val actual = CloudConvolutionalNetwork.fromWeights(weights).probabilities(input)
        expected.indices.forEach { assertEquals(expected[it], actual[it], 0.000001f) }
    }
}
