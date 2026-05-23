package com.kylecorry.trail_sense.tools.ai_assistant.infrastructure

import android.content.Context
import java.io.File
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

class ModelManager(private val modelDir: File) {

    constructor(context: Context) : this(
        File(context.filesDir, MODEL_DIR)
    )

    fun isModelDownloaded(): Boolean {
        return getModelFile().exists()
    }

    fun getModelPath(): String? {
        val file = getModelFile()
        return if (file.exists()) file.absolutePath else null
    }

    fun getModelSizeOnDisk(): Long {
        val file = getModelFile()
        return if (file.exists()) file.length() else 0L
    }

    suspend fun downloadModel(onProgress: (Float) -> Unit) {
        modelDir.mkdirs()
        val tempFile = File(modelDir, "$MODEL_FILE_NAME.tmp")
        val targetFile = getModelFile()

        try {
            val url = URL(MODEL_DOWNLOAD_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 30_000
            connection.readTimeout = 30_000
            val totalBytes = connection.contentLengthLong
            var downloadedBytes = 0L

            connection.inputStream.use { input: InputStream ->
                tempFile.outputStream().use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead
                        if (totalBytes > 0) {
                            onProgress(downloadedBytes.toFloat() / totalBytes)
                        }
                    }
                }
            }
            tempFile.renameTo(targetFile)
        } catch (e: Exception) {
            tempFile.delete()
            throw e
        }
    }

    fun deleteModel() {
        getModelFile().delete()
    }

    private fun getModelFile(): File {
        return File(modelDir, MODEL_FILE_NAME)
    }

    companion object {
        const val MODEL_FILE_NAME = "gemma-4-E2B-it.litertlm"
        const val MODEL_DIR = "ai_models"
        const val MODEL_DISPLAY_NAME = "Gemma 4 E2B"
        const val MODEL_SIZE_BYTES = 2_583_000_000L
        const val MODEL_DOWNLOAD_URL =
            "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it.litertlm"
    }
}
