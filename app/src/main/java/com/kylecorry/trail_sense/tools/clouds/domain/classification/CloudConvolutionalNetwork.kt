package com.kylecorry.trail_sense.tools.clouds.domain.classification

import kotlin.math.exp

class CloudConvolutionalNetwork private constructor(private val weights: FloatArray) {

    fun probabilities(input: FloatArray): FloatArray {
        require(input.size == INPUT_SIZE * INPUT_SIZE * INPUT_CHANNELS)
        var values = input
        var size = INPUT_SIZE
        for (layer in layers) {
            values = convolve(values, size, layer)
            size = (size + 2 * layer.padding - layer.kernel) / layer.stride + 1
        }

        val area = size * size
        // Mean captures broad coverage; max retains localized cloud features.
        val features = FloatArray(FEATURE_COUNT)
        for (channel in 0 until FINAL_CHANNELS) {
            var sum = 0f
            var maximum = Float.NEGATIVE_INFINITY
            for (pixel in 0 until area) {
                val value = values[channel * area + pixel]
                sum += value
                maximum = maxOf(maximum, value)
            }
            features[channel] = sum / area
            features[FINAL_CHANNELS + channel] = maximum
        }
        val logits = FloatArray(OUTPUTS) { output ->
            var value = weights[DENSE_BIASES_OFFSET + output]
            for (channel in features.indices) {
                value += weights[DENSE_WEIGHTS_OFFSET + output * FEATURE_COUNT + channel] *
                    features[channel]
            }
            value
        }
        val maximum = logits.maxOrNull() ?: 0f
        val probabilities = FloatArray(OUTPUTS) { exp(logits[it] - maximum) }
        val sum = probabilities.sum()
        return FloatArray(OUTPUTS) { probabilities[it] / sum }
    }

    private fun convolve(input: FloatArray, size: Int, layer: Layer): FloatArray {
        val outputSize = (size + 2 * layer.padding - layer.kernel) / layer.stride + 1
        val output = FloatArray(layer.outputs * outputSize * outputSize)
        for (channel in 0 until layer.outputs) {
            for (y in 0 until outputSize) {
                for (x in 0 until outputSize) {
                    val value = convolvePixel(input, size, layer, channel, y, x)
                    output[(channel * outputSize + y) * outputSize + x] =
                        value.coerceAtLeast(0f)
                }
            }
        }
        return output
    }

    private fun convolvePixel(
        input: FloatArray,
        size: Int,
        layer: Layer,
        channel: Int,
        y: Int,
        x: Int
    ): Float {
        val channelsPerOutput = layer.inputs
        val biasOffset = layer.offset + layer.outputs * channelsPerOutput * layer.kernel * layer.kernel
        var value = weights[biasOffset + channel]
        for (inputChannel in 0 until channelsPerOutput) {
            for (ky in 0 until layer.kernel) {
                val iy = y * layer.stride + ky - layer.padding
                if (iy !in 0 until size) continue
                for (kx in 0 until layer.kernel) {
                    val ix = x * layer.stride + kx - layer.padding
                    if (ix !in 0 until size) continue
                    val weightIndex = layer.offset +
                        ((channel * channelsPerOutput + inputChannel) * layer.kernel + ky) *
                        layer.kernel + kx
                    value += input[(inputChannel * size + iy) * size + ix] * weights[weightIndex]
                }
            }
        }
        return value
    }

    private data class Layer(
        val inputs: Int,
        val outputs: Int,
        val kernel: Int,
        val stride: Int,
        val offset: Int
    ) {
        val padding = if (kernel == 3) 1 else 0
    }

    companion object {
        const val INPUT_SIZE = 128
        const val INPUT_CHANNELS = 3
        const val OUTPUTS = 10
        private const val FINAL_CHANNELS = 32
        private const val FEATURE_COUNT = FINAL_CHANNELS * 2
        private const val DENSE_WEIGHTS_OFFSET = 9072
        private const val DENSE_BIASES_OFFSET = DENSE_WEIGHTS_OFFSET + OUTPUTS * FEATURE_COUNT
        const val WEIGHT_COUNT = DENSE_BIASES_OFFSET + OUTPUTS

        // Batch normalization is folded into these OIHW weights and biases by Python.
        private val layers = listOf(
            Layer(3, 8, 3, 2, 0),
            Layer(8, 12, 3, 2, 224),
            Layer(12, 20, 3, 2, 1100),
            Layer(20, 32, 3, 2, 3280)
        )

        fun fromWeights(weights: FloatArray): CloudConvolutionalNetwork {
            require(weights.size == WEIGHT_COUNT)
            return CloudConvolutionalNetwork(weights.copyOf())
        }
    }
}
