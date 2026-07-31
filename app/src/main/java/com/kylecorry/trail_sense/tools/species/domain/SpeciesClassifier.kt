package com.kylecorry.trail_sense.tools.species.domain

import android.content.Context
import android.content.res.AssetManager
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import java.io.Closeable
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class SpeciesClassifier(context: Context) : Closeable {

    private val taxonomy = context.assets.open(TAXONOMY_PATH).use(SpeciesTaxonomy::load)
    private val interpreter = Interpreter(
        loadModel(context.assets),
        Interpreter.Options().apply { setNumThreads(INFERENCE_THREADS) }
    )
    private val input = ByteBuffer.allocateDirect(INPUT_SIZE * INPUT_SIZE * CHANNELS * Float.SIZE_BYTES)
        .order(ByteOrder.nativeOrder())
    private val pixels = IntArray(INPUT_SIZE * INPUT_SIZE)
    private val output = Array(1) { FloatArray(interpreter.getOutputTensor(0).shape().last()) }

    init {
        check(output[0].size == taxonomy.modelSize) {
            "Species model output size (${output[0].size}) does not match taxonomy (${taxonomy.modelSize})"
        }
    }

    @Synchronized
    fun classify(bitmap: Bitmap, limit: Int = 3): List<SpeciesPrediction> {
        val prepared = prepareBitmap(bitmap)
        prepared.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)
        input.rewind()
        for (x in 0 until INPUT_SIZE) {
            for (y in 0 until INPUT_SIZE) {
                val color = pixels[y * INPUT_SIZE + x]
                input.putFloat(((color shr 16) and 0xff).toFloat())
                input.putFloat(((color shr 8) and 0xff).toFloat())
                input.putFloat((color and 0xff).toFloat())
            }
        }
        input.rewind()
        interpreter.run(input, output)
        if (prepared !== bitmap) {
            prepared.recycle()
        }
        return taxonomy.getPredictions(output[0], limit)
    }

    @Synchronized
    override fun close() {
        interpreter.close()
    }

    private fun prepareBitmap(bitmap: Bitmap): Bitmap {
        if (bitmap.width == INPUT_SIZE && bitmap.height == INPUT_SIZE) {
            return bitmap
        }
        val cropSize = minOf(bitmap.width, bitmap.height)
        val x = (bitmap.width - cropSize) / 2
        val y = (bitmap.height - cropSize) / 2
        val cropped = Bitmap.createBitmap(bitmap, x, y, cropSize, cropSize)
        val scaled = Bitmap.createScaledBitmap(cropped, INPUT_SIZE, INPUT_SIZE, true)
        if (cropped !== bitmap && cropped !== scaled) {
            cropped.recycle()
        }
        return scaled
    }

    companion object {
        const val INPUT_SIZE = 299
        private const val CHANNELS = 3
        private const val INFERENCE_THREADS = 2
        private const val MODEL_PATH = "species/INatVision_Small_2_fact256_8bit.tflite"
        private const val TAXONOMY_PATH = "species/taxonomy.csv"

        private fun loadModel(assets: AssetManager): MappedByteBuffer {
            assets.openFd(MODEL_PATH).use { descriptor ->
                descriptor.createInputStream().channel.use { channel ->
                    return channel.map(
                        FileChannel.MapMode.READ_ONLY,
                        descriptor.startOffset,
                        descriptor.declaredLength
                    )
                }
            }
        }
    }
}
