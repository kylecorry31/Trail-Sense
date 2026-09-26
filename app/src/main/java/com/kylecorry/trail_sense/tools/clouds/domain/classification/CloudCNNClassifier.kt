package com.kylecorry.trail_sense.tools.clouds.domain.classification

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.kylecorry.sol.science.meteorology.clouds.CloudGenus
import java.nio.ByteBuffer
import java.nio.ByteOrder

class CloudCNNClassifier(
    private val context: Context,
    private val onPredictionsCalculated: (List<Float>) -> Unit = {}
) : ICloudClassifier {

    private val network by lazy {
        CloudConvolutionalNetwork.fromWeights(loadWeights())
    }

    override suspend fun classify(bitmap: Bitmap): List<ClassificationResult<CloudGenus?>> {
        val predictions = network.probabilities(bitmapToInput(bitmap)).toList()
        onPredictionsCalculated(predictions)

        return CLOUD_GENUSES.mapIndexed { index, genus ->
            ClassificationResult<CloudGenus?>(genus, predictions[index])
        }.sortedByDescending { it.confidence }
    }

    companion object {
        const val IMAGE_SIZE = 400
        private const val WEIGHTS_ASSET = "cloud_cnn_weights.webp"
        private const val MODEL_VERSION = 3
        private val MODEL_MAGIC = byteArrayOf(0x54, 0x53, 0x43, 0x4c)

        val CLOUD_GENUSES = listOf(
            CloudGenus.Cirrus,
            CloudGenus.Cirrocumulus,
            CloudGenus.Cirrostratus,
            CloudGenus.Altostratus,
            CloudGenus.Altocumulus,
            CloudGenus.Nimbostratus,
            CloudGenus.Stratocumulus,
            CloudGenus.Cumulus,
            CloudGenus.Stratus,
            CloudGenus.Cumulonimbus
        )

        fun bitmapToInput(bitmap: Bitmap): FloatArray {
            val size = CloudConvolutionalNetwork.INPUT_SIZE
            val resized = Bitmap.createScaledBitmap(bitmap, size, size, true)
            val pixels = IntArray(size * size)
            try {
                resized.getPixels(pixels, 0, size, 0, 0, size, size)
                val planeSize = size * size
                val input = FloatArray(planeSize * CloudConvolutionalNetwork.INPUT_CHANNELS)
                for (index in pixels.indices) {
                    input[index] = ((pixels[index] shr 16) and 0xff) / 255f
                    input[planeSize + index] = ((pixels[index] shr 8) and 0xff) / 255f
                    input[planeSize * 2 + index] = (pixels[index] and 0xff) / 255f
                }
                return input
            } finally {
                if (resized !== bitmap) {
                    resized.recycle()
                }
            }
        }
    }

    private fun loadWeights(): FloatArray {
        val bitmap = context.assets.open(WEIGHTS_ASSET).use { stream ->
            BitmapFactory.decodeStream(stream)
        } ?: error("Unable to decode CNN weights")

        val pixels = IntArray(bitmap.width * bitmap.height)
        try {
            bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        } finally {
            bitmap.recycle()
        }

        val bytes = ByteArray(pixels.size * 3)
        pixels.forEachIndexed { index, pixel ->
            val byteIndex = index * 3
            bytes[byteIndex] = ((pixel shr 16) and 0xff).toByte()
            bytes[byteIndex + 1] = ((pixel shr 8) and 0xff).toByte()
            bytes[byteIndex + 2] = (pixel and 0xff).toByte()
        }

        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val magic = ByteArray(MODEL_MAGIC.size)
        buffer.get(magic)
        require(magic.contentEquals(MODEL_MAGIC)) { "Invalid CNN weights asset" }
        require((buffer.get().toInt() and 0xff) == MODEL_VERSION) {
            "Unsupported CNN weights version"
        }
        val weightCount = buffer.short.toInt() and 0xffff
        require(weightCount == CloudConvolutionalNetwork.WEIGHT_COUNT)
        require(buffer.remaining() >= weightCount * 4)
        val weights = FloatArray(weightCount) { buffer.float }
        require(weights.all { it.isFinite() })
        return weights
    }
}
