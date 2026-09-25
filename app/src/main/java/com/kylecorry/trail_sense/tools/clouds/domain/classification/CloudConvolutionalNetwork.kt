package com.kylecorry.trail_sense.tools.clouds.domain.classification

import kotlin.math.exp

class CloudConvolutionalNetwork private constructor(private val weights: FloatArray) {

    fun probabilities(input: FloatArray): FloatArray {
        return forward(input)
    }

    private fun forward(input: FloatArray): FloatArray {
        require(input.size == INPUT_SIZE * INPUT_SIZE * INPUT_CHANNELS)

        val conv1 = FloatArray(CONV1_CHANNELS * CONV1_SIZE * CONV1_SIZE)
        for (outputChannel in 0 until CONV1_CHANNELS) {
            for (y in 0 until CONV1_SIZE) {
                for (x in 0 until CONV1_SIZE) {
                    var value = weights[CONV1_BIASES_OFFSET + outputChannel]
                    for (inputChannel in 0 until INPUT_CHANNELS) {
                        for (kernelY in 0 until KERNEL_SIZE) {
                            for (kernelX in 0 until KERNEL_SIZE) {
                                val inputIndex =
                                    (inputChannel * INPUT_SIZE + y + kernelY) * INPUT_SIZE + x + kernelX
                                val weightIndex =
                                    conv1WeightIndex(outputChannel, inputChannel, kernelY, kernelX)
                                value += input[inputIndex] * weights[weightIndex]
                            }
                        }
                    }
                    conv1[(outputChannel * CONV1_SIZE + y) * CONV1_SIZE + x] = value.coerceAtLeast(0f)
                }
            }
        }

        val pool1 = FloatArray(CONV1_CHANNELS * POOL1_SIZE * POOL1_SIZE)
        maxPool(conv1, CONV1_CHANNELS, CONV1_SIZE, pool1)

        val conv2 = FloatArray(CONV2_CHANNELS * CONV2_SIZE * CONV2_SIZE)
        for (outputChannel in 0 until CONV2_CHANNELS) {
            for (y in 0 until CONV2_SIZE) {
                for (x in 0 until CONV2_SIZE) {
                    var value = weights[CONV2_BIASES_OFFSET + outputChannel]
                    for (inputChannel in 0 until CONV1_CHANNELS) {
                        for (kernelY in 0 until KERNEL_SIZE) {
                            for (kernelX in 0 until KERNEL_SIZE) {
                                val inputIndex =
                                    (inputChannel * POOL1_SIZE + y + kernelY) * POOL1_SIZE + x + kernelX
                                val weightIndex =
                                    conv2WeightIndex(outputChannel, inputChannel, kernelY, kernelX)
                                value += pool1[inputIndex] * weights[weightIndex]
                            }
                        }
                    }
                    conv2[(outputChannel * CONV2_SIZE + y) * CONV2_SIZE + x] = value.coerceAtLeast(0f)
                }
            }
        }

        val pool2 = FloatArray(CONV2_CHANNELS * POOL2_SIZE * POOL2_SIZE)
        maxPool(conv2, CONV2_CHANNELS, CONV2_SIZE, pool2)

        val features = FloatArray(CONV2_CHANNELS)
        for (channel in 0 until CONV2_CHANNELS) {
            for (y in 0 until POOL2_SIZE) {
                for (x in 0 until POOL2_SIZE) {
                    features[channel] +=
                        pool2[(channel * POOL2_SIZE + y) * POOL2_SIZE + x] / POOL2_AREA
                }
            }
        }

        val logits = FloatArray(OUTPUTS)
        for (output in 0 until OUTPUTS) {
            var value = weights[DENSE_BIASES_OFFSET + output]
            for (channel in 0 until CONV2_CHANNELS) {
                value += weights[DENSE_WEIGHTS_OFFSET + output * CONV2_CHANNELS + channel] *
                    features[channel]
            }
            logits[output] = value
        }

        val maxLogit = logits.maxOrNull() ?: 0f
        val probabilities = FloatArray(OUTPUTS) { exp(logits[it] - maxLogit) }
        val sum = probabilities.sum().coerceAtLeast(1e-12f)
        for (index in probabilities.indices) {
            probabilities[index] /= sum
        }

        return probabilities
    }

    private fun maxPool(
        input: FloatArray,
        channels: Int,
        inputSize: Int,
        output: FloatArray
    ) {
        val outputSize = inputSize / 2
        for (channel in 0 until channels) {
            for (y in 0 until outputSize) {
                for (x in 0 until outputSize) {
                    var maxIndex = (channel * inputSize + y * 2) * inputSize + x * 2
                    for (dy in 0..1) {
                        for (dx in 0..1) {
                            val index = (channel * inputSize + y * 2 + dy) * inputSize + x * 2 + dx
                            if (input[index] > input[maxIndex]) {
                                maxIndex = index
                            }
                        }
                    }
                    val outputIndex = (channel * outputSize + y) * outputSize + x
                    output[outputIndex] = input[maxIndex]
                }
            }
        }
    }

    private fun conv1WeightIndex(output: Int, input: Int, y: Int, x: Int): Int {
        return CONV1_WEIGHTS_OFFSET + ((output * INPUT_CHANNELS + input) * KERNEL_SIZE + y) *
            KERNEL_SIZE + x
    }

    private fun conv2WeightIndex(output: Int, input: Int, y: Int, x: Int): Int {
        return CONV2_WEIGHTS_OFFSET + ((output * CONV1_CHANNELS + input) * KERNEL_SIZE + y) *
            KERNEL_SIZE + x
    }

    companion object {
        const val INPUT_SIZE = 16
        const val INPUT_CHANNELS = 3
        const val OUTPUTS = 11
        private const val KERNEL_SIZE = 3
        private const val CONV1_CHANNELS = 12
        private const val CONV2_CHANNELS = 12
        private const val CONV1_SIZE = INPUT_SIZE - KERNEL_SIZE + 1
        private const val POOL1_SIZE = CONV1_SIZE / 2
        private const val CONV2_SIZE = POOL1_SIZE - KERNEL_SIZE + 1
        private const val POOL2_SIZE = CONV2_SIZE / 2
        private const val POOL2_AREA = (POOL2_SIZE * POOL2_SIZE).toFloat()
        private const val CONV1_WEIGHTS_OFFSET = 0
        private const val CONV1_BIASES_OFFSET = CONV1_CHANNELS * INPUT_CHANNELS * KERNEL_SIZE * KERNEL_SIZE
        private const val CONV2_WEIGHTS_OFFSET = CONV1_BIASES_OFFSET + CONV1_CHANNELS
        private const val CONV2_BIASES_OFFSET =
            CONV2_WEIGHTS_OFFSET + CONV2_CHANNELS * CONV1_CHANNELS * KERNEL_SIZE * KERNEL_SIZE
        private const val DENSE_WEIGHTS_OFFSET = CONV2_BIASES_OFFSET + CONV2_CHANNELS
        private const val DENSE_BIASES_OFFSET = DENSE_WEIGHTS_OFFSET + OUTPUTS * CONV2_CHANNELS
        const val WEIGHT_COUNT = DENSE_BIASES_OFFSET + OUTPUTS

        fun fromWeights(weights: FloatArray): CloudConvolutionalNetwork {
            require(weights.size == WEIGHT_COUNT)
            return CloudConvolutionalNetwork(weights.copyOf())
        }

    }
}