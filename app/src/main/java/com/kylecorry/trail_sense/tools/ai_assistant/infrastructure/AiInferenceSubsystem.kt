package com.kylecorry.trail_sense.tools.ai_assistant.infrastructure

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.MessageCallback
import com.google.ai.edge.litertlm.SamplerConfig
import com.google.ai.edge.litertlm.ToolProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.roundToInt

class AiInferenceSubsystem private constructor(private val context: Context) {

    private val modelManager = ModelManager(context)
    private var engine: Engine? = null
    private var conversation: Conversation? = null
    private var initializedModelId: String? = null

    fun isModelAvailable(): Boolean = modelManager.isModelDownloaded()

    fun isEngineReady(): Boolean {
        return engine != null &&
            conversation != null &&
            initializedModelId == modelManager.selectedModel.id
    }

    suspend fun initialize() = withContext(Dispatchers.IO) {
        val model = modelManager.selectedModel
        val modelPath = modelManager.getModelPath(model)
            ?: throw IllegalStateException("Model not downloaded")

        if (initializedModelId != model.id) {
            cleanup()
        }

        try {
            val gpuConfig = EngineConfig(
                modelPath = modelPath,
                backend = Backend.GPU(),
                visionBackend = Backend.GPU(),
                maxNumTokens = MAX_TOKENS
            )
            val gpuEngine = Engine(gpuConfig)
            gpuEngine.initialize()
            engine = gpuEngine
            initializedModelId = model.id
        } catch (_: Exception) {
            val cpuConfig = EngineConfig(
                modelPath = modelPath,
                backend = Backend.CPU(),
                visionBackend = Backend.CPU(),
                maxNumTokens = MAX_TOKENS
            )
            val cpuEngine = Engine(cpuConfig)
            cpuEngine.initialize()
            engine = cpuEngine
            initializedModelId = model.id
        }
    }

    suspend fun createConversation(
        systemInstruction: Contents? = null,
        tools: List<ToolProvider> = emptyList()
    ) = withContext(Dispatchers.IO) {
        val eng = engine ?: throw IllegalStateException("Engine not initialized")
        conversation?.close()
        conversation = eng.createConversation(
            ConversationConfig(
                samplerConfig = SamplerConfig(
                    topK = DEFAULT_TOP_K,
                    topP = DEFAULT_TOP_P,
                    temperature = DEFAULT_TEMPERATURE
                ),
                systemInstruction = systemInstruction,
                tools = tools
            )
        )
    }

    suspend fun sendMessage(
        input: String,
        images: List<Bitmap> = emptyList(),
        callback: MessageCallback
    ) = withContext(Dispatchers.Default) {
        val conv = conversation ?: throw IllegalStateException("Conversation not created")
        val contents = mutableListOf<Content>()
        for (image in images) {
            val stream = ByteArrayOutputStream()
            val resized = resizeImageForModel(image)
            resized.compress(Bitmap.CompressFormat.JPEG, IMAGE_QUALITY, stream)
            contents.add(Content.ImageBytes(stream.toByteArray()))
        }
        if (input.isNotBlank()) {
            contents.add(Content.Text(input))
        }
        conv.sendMessageAsync(Contents.of(contents), callback, emptyMap())
    }

    private fun resizeImageForModel(image: Bitmap): Bitmap {
        val largestSide = image.width.coerceAtLeast(image.height)
        if (largestSide <= MAX_IMAGE_SIZE) {
            return image
        }

        val scale = MAX_IMAGE_SIZE.toFloat() / largestSide.toFloat()
        return Bitmap.createScaledBitmap(
            image,
            (image.width * scale).roundToInt().coerceAtLeast(1),
            (image.height * scale).roundToInt().coerceAtLeast(1),
            true
        )
    }

    fun stopResponse() {
        conversation?.cancelProcess()
    }

    fun cleanup() {
        try { conversation?.close() } catch (_: Exception) {}
        try { engine?.close() } catch (_: Exception) {}
        conversation = null
        engine = null
        initializedModelId = null
    }

    companion object {
        private const val MAX_TOKENS = 4096
        private const val MAX_IMAGE_SIZE = 1024
        private const val IMAGE_QUALITY = 85
        private const val DEFAULT_TOP_K = 64
        private const val DEFAULT_TOP_P = 0.95
        private const val DEFAULT_TEMPERATURE = 1.0

        @SuppressLint("StaticFieldLeak")
        private var instance: AiInferenceSubsystem? = null

        @Synchronized
        fun getInstance(context: Context): AiInferenceSubsystem {
            if (instance == null) {
                instance = AiInferenceSubsystem(context.applicationContext)
            }
            return instance!!
        }
    }
}
