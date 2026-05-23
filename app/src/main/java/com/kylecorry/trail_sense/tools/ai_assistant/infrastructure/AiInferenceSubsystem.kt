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
import java.util.concurrent.CancellationException

class AiInferenceSubsystem private constructor(private val context: Context) {

    private val modelManager = ModelManager(context)
    private var engine: Engine? = null
    private var conversation: Conversation? = null

    fun isModelAvailable(): Boolean = modelManager.isModelDownloaded()

    fun isEngineReady(): Boolean = engine != null && conversation != null

    suspend fun initialize() = withContext(Dispatchers.IO) {
        val modelPath = modelManager.getModelPath()
            ?: throw IllegalStateException("Model not downloaded")

        val engineConfig = EngineConfig(
            modelPath = modelPath,
            backend = Backend.GPU(),
            visionBackend = Backend.GPU(),
            maxNumTokens = MAX_TOKENS
        )

        val newEngine = Engine(engineConfig)
        newEngine.initialize()
        engine = newEngine
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

    fun sendMessage(
        input: String,
        images: List<Bitmap> = emptyList(),
        callback: MessageCallback
    ) {
        val conv = conversation ?: throw IllegalStateException("Conversation not created")
        val contents = mutableListOf<Content>()
        for (image in images) {
            val stream = ByteArrayOutputStream()
            image.compress(Bitmap.CompressFormat.PNG, 100, stream)
            contents.add(Content.ImageBytes(stream.toByteArray()))
        }
        if (input.isNotBlank()) {
            contents.add(Content.Text(input))
        }
        conv.sendMessageAsync(Contents.of(contents), callback, emptyMap())
    }

    fun stopResponse() {
        conversation?.cancelProcess()
    }

    fun cleanup() {
        try { conversation?.close() } catch (_: Exception) {}
        try { engine?.close() } catch (_: Exception) {}
        conversation = null
        engine = null
    }

    companion object {
        private const val MAX_TOKENS = 4096
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
