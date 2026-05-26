package com.kylecorry.trail_sense.tools.ai_assistant.infrastructure

import android.content.Context
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

data class AiModel(
    val id: String,
    val displayName: String,
    val fileName: String,
    val sizeBytes: Long,
    val downloadUrl: String
)

class ModelManager(
    private val modelDir: File,
    private val getSelectedModelId: () -> String = { DEFAULT_MODEL_ID },
    private val setSelectedModelId: (String) -> Unit = {}
) {

    constructor(context: Context) : this(
        File(context.filesDir, MODEL_DIR),
        getSelectedModelId = {
            PreferencesSubsystem.getInstance(context).preferences.getString(PREF_SELECTED_MODEL_ID)
                ?: DEFAULT_MODEL_ID
        },
        setSelectedModelId = {
            PreferencesSubsystem.getInstance(context).preferences.putString(
                PREF_SELECTED_MODEL_ID,
                it
            )
        }
    )

    val models: List<AiModel>
        get() = MODELS

    var selectedModel: AiModel
        get() = getModel(getSelectedModelId())
        set(value) = setSelectedModelId(value.id)

    fun getModel(id: String): AiModel {
        return MODELS.firstOrNull { it.id == id } ?: DEFAULT_MODEL
    }

    fun isModelDownloaded(model: AiModel = selectedModel): Boolean {
        return getModelFile(model).exists()
    }

    fun getModelPath(model: AiModel = selectedModel): String? {
        val file = getModelFile(model)
        return if (file.exists()) file.absolutePath else null
    }

    fun getModelSizeOnDisk(model: AiModel = selectedModel): Long {
        val file = getModelFile(model)
        return if (file.exists()) file.length() else 0L
    }

    suspend fun downloadModel(model: AiModel = selectedModel, onProgress: (Float) -> Unit) {
        modelDir.mkdirs()
        val tempFile = File(modelDir, "${model.fileName}.tmp")
        val targetFile = getModelFile(model)

        val existingBytes = if (tempFile.exists()) tempFile.length() else 0L

        val url = URL(model.downloadUrl)
        val connection = url.openConnection() as HttpURLConnection
        connection.connectTimeout = 30_000
        connection.readTimeout = 30_000
        if (existingBytes > 0) {
            connection.setRequestProperty("Range", "bytes=$existingBytes-")
        }

        try {
            val responseCode = connection.responseCode
            val totalBytes = if (responseCode == 206) {
                existingBytes + connection.contentLengthLong
            } else {
                connection.contentLengthLong
            }
            val append = responseCode == 206
            var downloadedBytes = if (append) existingBytes else 0L

            if (!append && existingBytes > 0) {
                tempFile.delete()
            }

            connection.inputStream.use { input: InputStream ->
                FileOutputStream(tempFile, append).use { output ->
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
            // 保留 tempFile 以支持断点续传
            throw e
        }
    }

    fun getDownloadProgress(model: AiModel = selectedModel): Float {
        val tempFile = File(modelDir, "${model.fileName}.tmp")
        if (!tempFile.exists()) return 0f
        return tempFile.length().toFloat() / model.sizeBytes
    }

    fun deleteModel(model: AiModel = selectedModel) {
        getModelFile(model).delete()
        File(modelDir, "${model.fileName}.tmp").delete()
    }

    private fun getModelFile(model: AiModel): File {
        return File(modelDir, model.fileName)
    }

    companion object {
        const val MODEL_DIR = "ai_models"
        const val DEFAULT_MODEL_ID = "gemma-4-e2b-it"
        const val PREF_SELECTED_MODEL_ID = "pref_ai_selected_model_id"

        val MODELS = listOf(
            AiModel(
                id = DEFAULT_MODEL_ID,
                displayName = "Gemma-4-E2B-it",
                fileName = "gemma-4-E2B-it.litertlm",
                sizeBytes = 2_583_085_056L,
                downloadUrl = "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it.litertlm"
            ),
            AiModel(
                id = "gemma-4-e4b-it",
                displayName = "Gemma-4-E4B-it",
                fileName = "gemma-4-E4B-it.litertlm",
                sizeBytes = 3_654_467_584L,
                downloadUrl = "https://huggingface.co/litert-community/gemma-4-E4B-it-litert-lm/resolve/main/gemma-4-E4B-it.litertlm"
            )
        )

        val DEFAULT_MODEL = MODELS.first()

        const val MODEL_FILE_NAME = "gemma-4-E2B-it.litertlm"
        const val MODEL_DISPLAY_NAME = "Gemma-4-E2B-it"
        const val MODEL_SIZE_BYTES = 2_583_085_056L
        const val MODEL_DOWNLOAD_URL =
            "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it.litertlm"
    }
}
