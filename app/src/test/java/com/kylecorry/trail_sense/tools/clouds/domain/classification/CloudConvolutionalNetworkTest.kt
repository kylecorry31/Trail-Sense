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
}