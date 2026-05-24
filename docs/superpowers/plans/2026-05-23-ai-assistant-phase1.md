# Trail-Sense AI 助手 Phase 1 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 Trail-Sense 添加基于 LiteRT-LM + Gemma 4 E2B 的设备端 AI 助手 MVP，首先集成天气工具的数据解读能力。

**Architecture:** 新增 `tools/ai_assistant/` 包，注册为 Tool ID 49。通过 `AiInferenceSubsystem` 单例管理 LiteRT-LM Engine 生命周期。天气工具实现 `AiContextProvider` 接口暴露数据给 AI。AI 助手使用 `TrailSenseComposeFragment` 提供对话 UI。模型通过 `ModelManager` 从 HuggingFace 下载后完全离线推理。

**Tech Stack:** Kotlin, Jetpack Compose (Material 3), LiteRT-LM (`com.google.ai.edge:litertlm`), AndroidX Navigation, Coroutines

**参考代码模式：**
- ToolRegistration: `MagnifierToolRegistration.kt` / `WeatherToolRegistration.kt`
- Compose Fragment: `ToolMagnifierFragment.kt` (extends `TrailSenseComposeFragment`)
- Subsystem 单例: `WeatherSubsystem.kt` (private constructor + `companion object getInstance`)
- Tool ID 分配: `Tools.kt` 中 `const val MAGNIFIER = 48L` 是当前最大值
- ToolCategory 可选: `Signaling, Distance, Location, Angles, Time, Power, Weather, Communication, Books, Other`

---

### Task 1: 添加 LiteRT-LM 依赖和 INTERNET 权限

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`
- Modify: `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: 在 version catalog 中添加 LiteRT-LM**

在 `gradle/libs.versions.toml` 的 `[versions]` 部分添加：

```toml
litertlm = "0.1.0"
```

在 `[libraries]` 部分添加：

```toml
litertlm = { module = "com.google.ai.edge:litertlm", version.ref = "litertlm" }
```

> 注意：版本号需要在实现时查 [LiteRT-LM releases](https://github.com/google-ai-edge/LiteRT-LM) 确认最新稳定版。

- [ ] **Step 2: 在 app build.gradle.kts 中添加依赖**

在 `app/build.gradle.kts` 的 `dependencies` 块中添加：

```kotlin
implementation(libs.litertlm)
```

- [ ] **Step 3: 在 AndroidManifest.xml 中添加 INTERNET 权限**

在 `app/src/main/AndroidManifest.xml` 的 `<manifest>` 标签内、现有权限声明附近添加：

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

- [ ] **Step 4: Sync Gradle 并验证编译通过**

Run: `cd /Users/zhangjiantao/Documents/GitHub/Trail-Sense && ./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add gradle/libs.versions.toml app/build.gradle.kts app/src/main/AndroidManifest.xml
git commit -m "feat(ai): add LiteRT-LM dependency and INTERNET permission"
```

---

### Task 2: 创建 ModelManager — 模型下载与存储管理

**Files:**
- Create: `app/src/main/java/com/kylecorry/trail_sense/tools/ai_assistant/infrastructure/ModelManager.kt`
- Test: `app/src/test/java/com/kylecorry/trail_sense/tools/ai_assistant/infrastructure/ModelManagerTest.kt`

- [ ] **Step 1: 写失败的测试**

```kotlin
package com.kylecorry.trail_sense.tools.ai_assistant.infrastructure

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class ModelManagerTest {

    @TempDir
    lateinit var tempDir: File

    @Test
    fun `isModelDownloaded returns false when model file does not exist`() {
        val manager = ModelManager(tempDir)
        assertFalse(manager.isModelDownloaded())
    }

    @Test
    fun `isModelDownloaded returns true when model file exists`() {
        val manager = ModelManager(tempDir)
        File(tempDir, ModelManager.MODEL_FILE_NAME).createNewFile()
        assertTrue(manager.isModelDownloaded())
    }

    @Test
    fun `getModelPath returns null when model not downloaded`() {
        val manager = ModelManager(tempDir)
        assertNull(manager.getModelPath())
    }

    @Test
    fun `getModelPath returns path when model exists`() {
        val manager = ModelManager(tempDir)
        val modelFile = File(tempDir, ModelManager.MODEL_FILE_NAME)
        modelFile.createNewFile()
        assertEquals(modelFile.absolutePath, manager.getModelPath())
    }

    @Test
    fun `deleteModel removes the model file`() {
        val manager = ModelManager(tempDir)
        val modelFile = File(tempDir, ModelManager.MODEL_FILE_NAME)
        modelFile.writeText("fake model data")
        assertTrue(modelFile.exists())
        manager.deleteModel()
        assertFalse(modelFile.exists())
    }

    @Test
    fun `deleteModel does nothing when no model exists`() {
        val manager = ModelManager(tempDir)
        manager.deleteModel()
        assertFalse(manager.isModelDownloaded())
    }
}
```

- [ ] **Step 2: 运行测试验证失败**

Run: `./gradlew :app:testDebugUnitTest --tests "com.kylecorry.trail_sense.tools.ai_assistant.infrastructure.ModelManagerTest"`
Expected: FAIL — 类不存在

- [ ] **Step 3: 实现 ModelManager**

```kotlin
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
```

- [ ] **Step 4: 运行测试验证通过**

Run: `./gradlew :app:testDebugUnitTest --tests "com.kylecorry.trail_sense.tools.ai_assistant.infrastructure.ModelManagerTest"`
Expected: 6 tests PASSED

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/kylecorry/trail_sense/tools/ai_assistant/infrastructure/ModelManager.kt app/src/test/java/com/kylecorry/trail_sense/tools/ai_assistant/infrastructure/ModelManagerTest.kt
git commit -m "feat(ai): add ModelManager for model download and storage"
```

---

### Task 3: 创建 AiInferenceSubsystem — LiteRT-LM 引擎封装

**Files:**
- Create: `app/src/main/java/com/kylecorry/trail_sense/tools/ai_assistant/infrastructure/AiInferenceSubsystem.kt`

- [ ] **Step 1: 实现 AiInferenceSubsystem**

```kotlin
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
```

- [ ] **Step 2: 验证编译通过**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

> 注意：此类依赖 LiteRT-LM 的 API。如果 API 签名与 Gallery 源码中观察到的不同，需在此步骤根据实际 SDK 调整。编译成功即表示 API 匹配。

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/kylecorry/trail_sense/tools/ai_assistant/infrastructure/AiInferenceSubsystem.kt
git commit -m "feat(ai): add AiInferenceSubsystem wrapping LiteRT-LM engine"
```

---

### Task 4: 创建 AiContextProvider 接口和天气实现

**Files:**
- Create: `app/src/main/java/com/kylecorry/trail_sense/tools/ai_assistant/domain/AiContextProvider.kt`
- Create: `app/src/main/java/com/kylecorry/trail_sense/tools/ai_assistant/domain/AiContext.kt`
- Create: `app/src/main/java/com/kylecorry/trail_sense/tools/ai_assistant/domain/WeatherAiContextProvider.kt`
- Test: `app/src/test/java/com/kylecorry/trail_sense/tools/ai_assistant/domain/WeatherAiContextProviderTest.kt`

- [ ] **Step 1: 创建 AiContext 数据类**

```kotlin
package com.kylecorry.trail_sense.tools.ai_assistant.domain

import android.graphics.Bitmap
import java.util.Locale

data class AiContext(
    val toolId: String,
    val toolName: String,
    val sensorData: Map<String, Any>,
    val image: Bitmap?,
    val summary: String
)
```

- [ ] **Step 2: 创建 AiContextProvider 接口**

```kotlin
package com.kylecorry.trail_sense.tools.ai_assistant.domain

interface AiContextProvider {
    val toolId: String
    suspend fun getAiContext(): AiContext
    fun getSuggestedQuestions(): List<String>
}
```

- [ ] **Step 3: 写 WeatherAiContextProvider 的失败测试**

```kotlin
package com.kylecorry.trail_sense.tools.ai_assistant.domain

import com.kylecorry.sol.science.meteorology.PressureCharacteristic
import com.kylecorry.sol.science.meteorology.PressureTendency
import com.kylecorry.sol.science.meteorology.WeatherCondition
import com.kylecorry.sol.units.Pressure
import com.kylecorry.sol.units.Temperature
import com.kylecorry.trail_sense.tools.weather.domain.CurrentWeather
import com.kylecorry.trail_sense.tools.weather.domain.WeatherObservation
import com.kylecorry.trail_sense.tools.weather.domain.WeatherPrediction
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.Instant

class WeatherAiContextProviderTest {

    @Test
    fun `getAiContext includes pressure and tendency in sensorData`() = runBlocking {
        val weather = CurrentWeather(
            prediction = WeatherPrediction(
                hourly = listOf(WeatherCondition.Storm),
                daily = listOf(WeatherCondition.Storm),
                front = null,
                hourlyArrival = null,
                temperature = null,
                alerts = emptyList()
            ),
            pressureTendency = PressureTendency(
                PressureCharacteristic.FallingFast,
                -4.1f
            ),
            observation = WeatherObservation(
                id = 1L,
                time = Instant.now(),
                pressure = Pressure.hpa(1008.2f),
                temperature = Temperature.celsius(20f),
                humidity = 65f
            ),
            clouds = null
        )

        val provider = WeatherAiContextProvider { weather }
        val context = provider.getAiContext()

        assertEquals("weather", context.toolId)
        assertEquals(1008.2f, context.sensorData["pressure_hpa"])
        assertEquals("FallingFast", context.sensorData["pressure_characteristic"])
        assertEquals(-4.1f, context.sensorData["pressure_change_hpa"])
        assertNull(context.image)
        assertTrue(context.summary.contains("1008.2"))
    }

    @Test
    fun `getSuggestedQuestions returns non-empty list`() {
        val provider = WeatherAiContextProvider { error("unused") }
        val questions = provider.getSuggestedQuestions()
        assertTrue(questions.isNotEmpty())
    }
}
```

- [ ] **Step 4: 运行测试验证失败**

Run: `./gradlew :app:testDebugUnitTest --tests "com.kylecorry.trail_sense.tools.ai_assistant.domain.WeatherAiContextProviderTest"`
Expected: FAIL — 类不存在

- [ ] **Step 5: 实现 WeatherAiContextProvider**

```kotlin
package com.kylecorry.trail_sense.tools.ai_assistant.domain

import com.kylecorry.trail_sense.tools.weather.domain.CurrentWeather

class WeatherAiContextProvider(
    private val weatherProvider: suspend () -> CurrentWeather
) : AiContextProvider {

    override val toolId: String = "weather"

    override suspend fun getAiContext(): AiContext {
        val weather = weatherProvider()
        val obs = weather.observation
        val tendency = weather.pressureTendency
        val prediction = weather.prediction

        val sensorData = mutableMapOf<String, Any>()
        if (obs != null) {
            sensorData["pressure_hpa"] = obs.pressure.hpa().pressure
            sensorData["temperature_c"] = obs.temperature.celsius().temperature
            obs.humidity?.let { sensorData["humidity_percent"] = it }
        }
        sensorData["pressure_characteristic"] = tendency.characteristic.name
        sensorData["pressure_change_hpa"] = tendency.amount

        val hourlyConditions = prediction.primaryHourly?.name ?: "Unknown"
        val dailyConditions = prediction.primaryDaily?.name ?: "Unknown"
        sensorData["forecast_hourly"] = hourlyConditions
        sensorData["forecast_daily"] = dailyConditions

        val summary = buildString {
            append("Weather Tool Data:\n")
            if (obs != null) {
                append("- Pressure: ${obs.pressure.hpa().pressure} hPa\n")
                append("- Temperature: ${obs.temperature.celsius().temperature}°C\n")
                obs.humidity?.let { append("- Humidity: ${it}%\n") }
            }
            append("- Pressure trend: ${tendency.characteristic.name} (${tendency.amount} hPa)\n")
            append("- Hourly forecast: $hourlyConditions\n")
            append("- Daily forecast: $dailyConditions\n")
        }

        return AiContext(
            toolId = toolId,
            toolName = "Weather",
            sensorData = sensorData,
            image = null,
            summary = summary
        )
    }

    override fun getSuggestedQuestions(): List<String> {
        return listOf(
            "What does this pressure trend mean?",
            "Is it safe to stay outdoors?",
            "What weather should I expect in the next few hours?"
        )
    }
}
```

- [ ] **Step 6: 运行测试验证通过**

Run: `./gradlew :app:testDebugUnitTest --tests "com.kylecorry.trail_sense.tools.ai_assistant.domain.WeatherAiContextProviderTest"`
Expected: 2 tests PASSED

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/kylecorry/trail_sense/tools/ai_assistant/domain/ app/src/test/java/com/kylecorry/trail_sense/tools/ai_assistant/domain/
git commit -m "feat(ai): add AiContextProvider interface and weather implementation"
```

---

### Task 5: 创建 AiPromptBuilder

**Files:**
- Create: `app/src/main/java/com/kylecorry/trail_sense/tools/ai_assistant/domain/AiPromptBuilder.kt`
- Test: `app/src/test/java/com/kylecorry/trail_sense/tools/ai_assistant/domain/AiPromptBuilderTest.kt`

- [ ] **Step 1: 写失败的测试**

```kotlin
package com.kylecorry.trail_sense.tools.ai_assistant.domain

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.Locale

class AiPromptBuilderTest {

    @Test
    fun `buildSystemPrompt includes locale`() {
        val prompt = AiPromptBuilder.buildSystemPrompt(Locale.CHINESE)
        assertTrue(prompt.contains("zh"))
    }

    @Test
    fun `buildSystemPrompt includes safety instructions`() {
        val prompt = AiPromptBuilder.buildSystemPrompt(Locale.ENGLISH)
        assertTrue(prompt.contains("safety"))
        assertTrue(prompt.contains("supplementary"))
    }

    @Test
    fun `buildUserPrompt includes context summary and question`() {
        val context = AiContext(
            toolId = "weather",
            toolName = "Weather",
            sensorData = mapOf("pressure_hpa" to 1008.2f),
            image = null,
            summary = "Pressure: 1008.2 hPa"
        )
        val prompt = AiPromptBuilder.buildUserPrompt(context, "Is it safe?")
        assertTrue(prompt.contains("Pressure: 1008.2 hPa"))
        assertTrue(prompt.contains("Is it safe?"))
    }

    @Test
    fun `buildUserPrompt without context only includes question`() {
        val prompt = AiPromptBuilder.buildUserPrompt(null, "Hello")
        assertTrue(prompt.contains("Hello"))
        assertFalse(prompt.contains("Context"))
    }
}
```

- [ ] **Step 2: 运行测试验证失败**

Run: `./gradlew :app:testDebugUnitTest --tests "com.kylecorry.trail_sense.tools.ai_assistant.domain.AiPromptBuilderTest"`
Expected: FAIL

- [ ] **Step 3: 实现 AiPromptBuilder**

```kotlin
package com.kylecorry.trail_sense.tools.ai_assistant.domain

import java.util.Locale

object AiPromptBuilder {

    fun buildSystemPrompt(locale: Locale): String {
        return """
            You are a wilderness survival assistant built into the Trail Sense app.
            
            Rules:
            - Respond in the user's language (${locale.language}).
            - Be concise: max 80 words per response.
            - Prioritize safety-critical information first.
            - When interpreting sensor data, explain what it means in practical terms.
            - Never fabricate sensor readings — use only the data provided.
            - Always clarify that your advice is supplementary, not a replacement for proper training and judgment.
        """.trimIndent()
    }

    fun buildUserPrompt(context: AiContext?, question: String): String {
        if (context == null) {
            return question
        }
        return buildString {
            append("[Context from ${context.toolName} tool]\n")
            append(context.summary)
            append("\n")
            append("User question: $question")
        }
    }
}
```

- [ ] **Step 4: 运行测试验证通过**

Run: `./gradlew :app:testDebugUnitTest --tests "com.kylecorry.trail_sense.tools.ai_assistant.domain.AiPromptBuilderTest"`
Expected: 4 tests PASSED

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/kylecorry/trail_sense/tools/ai_assistant/domain/AiPromptBuilder.kt app/src/test/java/com/kylecorry/trail_sense/tools/ai_assistant/domain/AiPromptBuilderTest.kt
git commit -m "feat(ai): add AiPromptBuilder for system and user prompts"
```

---

### Task 6: 创建 AI 助手 Compose Fragment UI

**Files:**
- Create: `app/src/main/java/com/kylecorry/trail_sense/tools/ai_assistant/ui/AiAssistantFragment.kt`

- [ ] **Step 1: 创建 ChatMessage 数据类和 Fragment**

```kotlin
package com.kylecorry.trail_sense.tools.ai_assistant.ui

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.extensions.TrailSenseComposeFragment
import com.kylecorry.trail_sense.shared.extensions.compose.useState
import com.kylecorry.trail_sense.tools.ai_assistant.domain.AiContext
import com.kylecorry.trail_sense.tools.ai_assistant.domain.AiPromptBuilder
import com.kylecorry.trail_sense.tools.ai_assistant.infrastructure.AiInferenceSubsystem
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.MessageCallback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val isLoading: Boolean = false
)

class AiAssistantFragment : TrailSenseComposeFragment() {

    private var aiContext: AiContext? = null

    @Composable
    override fun FragmentContent() {
        val aiSubsystem = AiInferenceSubsystem.getInstance(requireContext())
        val (messages, setMessages) = useState(listOf<ChatMessage>())
        val (inputText, setInputText) = useState("")
        val (isGenerating, setIsGenerating) = useState(false)
        val (isInitializing, setIsInitializing) = useState(false)
        val (error, setError) = useState<String?>(null)
        val (suggestedQuestions, setSuggestedQuestions) = useState(emptyList<String>())
        val listState = rememberLazyListState()

        // TODO: Retrieve aiContext from navigation arguments in a future step

        LaunchedEffect(Unit) {
            if (!aiSubsystem.isModelAvailable()) {
                setError(getString(R.string.ai_model_not_downloaded))
                return@LaunchedEffect
            }
            if (!aiSubsystem.isEngineReady()) {
                setIsInitializing(true)
                try {
                    aiSubsystem.initialize()
                    val systemPrompt = AiPromptBuilder.buildSystemPrompt(
                        resources.configuration.locales[0] ?: Locale.ENGLISH
                    )
                    aiSubsystem.createConversation(
                        systemInstruction = Contents.of(listOf(Content.Text(systemPrompt)))
                    )
                } catch (e: Exception) {
                    setError(getString(R.string.ai_initialization_failed))
                }
                setIsInitializing(false)
            }
        }

        fun sendMessage(text: String) {
            if (text.isBlank() || isGenerating) return
            val userMessage = ChatMessage(text, isUser = true)
            val loadingMessage = ChatMessage("", isUser = false, isLoading = true)
            setMessages(messages + userMessage + loadingMessage)
            setInputText("")
            setIsGenerating(true)
            setSuggestedQuestions(emptyList())

            val prompt = AiPromptBuilder.buildUserPrompt(aiContext, text)
            aiContext = null // Only inject context on first message

            val responseBuilder = StringBuilder()
            aiSubsystem.sendMessage(
                input = prompt,
                callback = object : MessageCallback {
                    override fun onMessage(message: Message) {
                        responseBuilder.append(message.toString())
                        val updated = messages + userMessage + ChatMessage(
                            responseBuilder.toString(),
                            isUser = false
                        )
                        setMessages(updated)
                    }

                    override fun onDone() {
                        val final = messages + userMessage + ChatMessage(
                            responseBuilder.toString(),
                            isUser = false
                        )
                        setMessages(final)
                        setIsGenerating(false)
                    }

                    override fun onError(throwable: Throwable) {
                        val errorMsg = if (throwable is java.util.concurrent.CancellationException) {
                            responseBuilder.toString()
                        } else {
                            getString(R.string.ai_inference_error)
                        }
                        val final = messages + userMessage + ChatMessage(errorMsg, isUser = false)
                        setMessages(final)
                        setIsGenerating(false)
                    }
                }
            )
        }

        LaunchedEffect(messages.size) {
            if (messages.isNotEmpty()) {
                listState.animateScrollToItem(messages.size - 1)
            }
        }

        AiAssistantContent(
            messages = messages,
            inputText = inputText,
            isGenerating = isGenerating,
            isInitializing = isInitializing,
            error = error,
            suggestedQuestions = suggestedQuestions,
            onInputChanged = setInputText,
            onSend = ::sendMessage,
            onStop = { aiSubsystem.stopResponse() },
            listState = listState,
            contextCard = aiContext?.summary
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AiAssistantContent(
    messages: List<ChatMessage>,
    inputText: String,
    isGenerating: Boolean,
    isInitializing: Boolean,
    error: String?,
    suggestedQuestions: List<String>,
    onInputChanged: (String) -> Unit,
    onSend: (String) -> Unit,
    onStop: () -> Unit,
    listState: androidx.compose.foundation.lazy.LazyListState,
    contextCard: String?,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        if (error != null) {
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 8.dp).testTag("error_text")
            )
        }

        if (isInitializing) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(modifier = Modifier.testTag("loading_indicator"))
                    Text(
                        text = stringResource(R.string.ai_loading_model),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
            return
        }

        if (contextCard != null && messages.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).testTag("context_card"),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(
                    text = contextCard,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth().testTag("chat_list"),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { message ->
                ChatBubble(message)
            }
        }

        AnimatedVisibility(visible = suggestedQuestions.isNotEmpty() && messages.isEmpty()) {
            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                suggestedQuestions.forEach { question ->
                    AssistChip(
                        onClick = { onSend(question) },
                        label = { Text(question) },
                        modifier = Modifier.testTag("suggested_question")
                    )
                }
            }
        }

        Text(
            text = stringResource(R.string.ai_disclaimer),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 4.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = onInputChanged,
                modifier = Modifier.weight(1f).testTag("input_field"),
                placeholder = { Text(stringResource(R.string.ai_input_hint)) },
                enabled = !isGenerating && error == null,
                singleLine = false,
                maxLines = 3
            )
            IconButton(
                onClick = {
                    if (isGenerating) onStop() else onSend(inputText)
                },
                enabled = error == null,
                modifier = Modifier.testTag("send_button")
            ) {
                if (isGenerating) {
                    Icon(Icons.Default.Stop, contentDescription = null)
                } else {
                    Icon(Icons.AutoMirrored.Default.Send, contentDescription = null)
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessage, modifier: Modifier = Modifier) {
    val alignment = if (message.isUser) Alignment.End else Alignment.Start
    val containerColor = if (message.isUser) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }

    Box(modifier = modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier
                .align(if (message.isUser) Alignment.CenterEnd else Alignment.CenterStart)
                .fillMaxWidth(0.85f)
                .testTag(if (message.isUser) "user_message" else "ai_message"),
            colors = CardDefaults.cardColors(containerColor = containerColor)
        ) {
            if (message.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(12.dp).testTag("message_loading"),
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = message.text,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
```

- [ ] **Step 2: 验证编译通过**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/kylecorry/trail_sense/tools/ai_assistant/ui/
git commit -m "feat(ai): add AI assistant Compose fragment with chat UI"
```

---

### Task 7: 添加字符串资源

**Files:**
- Modify: `app/src/main/res/values/strings.xml`

- [ ] **Step 1: 在 strings.xml 末尾（`</resources>` 之前）添加 AI 相关字符串**

```xml
<!-- AI Assistant -->
<string name="tool_ai_assistant_title">AI Assistant</string>
<string name="ai_model_not_downloaded">AI model not downloaded. Go to Settings to download.</string>
<string name="ai_initialization_failed">Failed to initialize AI model.</string>
<string name="ai_loading_model">Loading AI model…</string>
<string name="ai_input_hint">Ask a question…</string>
<string name="ai_disclaimer">AI suggestions are for reference only, not a replacement for proper training and judgment.</string>
<string name="ai_inference_error">An error occurred while generating a response.</string>
<string name="ai_settings_title">AI Assistant</string>
<string name="ai_model_name">Gemma 4 E2B</string>
<string name="ai_model_size">2.59 GB</string>
<string name="ai_download_model">Download model</string>
<string name="ai_delete_model">Delete model</string>
<string name="ai_model_downloaded">Model downloaded</string>
<string name="ai_downloading">Downloading…</string>
<string name="ai_about">AI runs entirely on your device. An internet connection is only needed to download the model.</string>
```

- [ ] **Step 2: 验证编译通过**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/res/values/strings.xml
git commit -m "feat(ai): add string resources for AI assistant"
```

---

### Task 8: 添加 drawable 资源（AI 图标）

**Files:**
- Create: `app/src/main/res/drawable/ic_ai_assistant.xml`

- [ ] **Step 1: 创建 AI 助手图标（使用 Material "auto_awesome" 图标）**

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24"
    android:tint="?attr/colorControlNormal">
    <path
        android:fillColor="@android:color/white"
        android:pathData="M19,9l1.25,-2.75L23,5l-2.75,-1.25L19,1l-1.25,2.75L15,5l2.75,1.25L19,9zM11.5,9.5L9,4 6.5,9.5 1,12l5.5,2.5L9,20l2.5,-5.5L17,12l-5.5,-2.5zM19,15l-1.25,2.75L15,19l2.75,1.25L19,23l1.25,-2.75L23,19l-2.75,-1.25L19,15z"/>
</vector>
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/res/drawable/ic_ai_assistant.xml
git commit -m "feat(ai): add AI assistant icon"
```

---

### Task 9: 注册 AI 助手为 Tool 并添加到导航

**Files:**
- Create: `app/src/main/java/com/kylecorry/trail_sense/tools/ai_assistant/AiAssistantToolRegistration.kt`
- Modify: `app/src/main/java/com/kylecorry/trail_sense/tools/tools/infrastructure/Tools.kt`
- Modify: `app/src/main/res/navigation/nav_graph.xml`

- [ ] **Step 1: 创建 AiAssistantToolRegistration**

```kotlin
package com.kylecorry.trail_sense.tools.ai_assistant

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.ai_assistant.infrastructure.AiInferenceSubsystem
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tool
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolCategory
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolRegistration
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

object AiAssistantToolRegistration : ToolRegistration {
    override fun getTool(context: Context): Tool {
        return Tool(
            Tools.AI_ASSISTANT,
            context.getString(R.string.tool_ai_assistant_title),
            R.drawable.ic_ai_assistant,
            R.id.aiAssistantFragment,
            ToolCategory.Other,
            isAvailable = {
                AiInferenceSubsystem.getInstance(it).isModelAvailable()
            }
        )
    }
}
```

- [ ] **Step 2: 在 Tools.kt 中注册**

在 `Tools.kt` 中添加：

1. 在 Tool IDs 部分（`const val MAGNIFIER = 48L` 之后）添加：
```kotlin
const val AI_ASSISTANT = 49L
```

2. 在 import 部分添加：
```kotlin
import com.kylecorry.trail_sense.tools.ai_assistant.AiAssistantToolRegistration
```

3. 在 `registry` 列表中（`MagnifierToolRegistration` 之后）添加：
```kotlin
AiAssistantToolRegistration
```

- [ ] **Step 3: 在 nav_graph.xml 中添加 Fragment 条目**

在 `nav_graph.xml` 中（其他 fragment 条目附近）添加：

```xml
<fragment
    android:id="@+id/aiAssistantFragment"
    android:name="com.kylecorry.trail_sense.tools.ai_assistant.ui.AiAssistantFragment"
    android:label="AiAssistantFragment" />
```

- [ ] **Step 4: 验证编译通过**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/kylecorry/trail_sense/tools/ai_assistant/AiAssistantToolRegistration.kt app/src/main/java/com/kylecorry/trail_sense/tools/tools/infrastructure/Tools.kt app/src/main/res/navigation/nav_graph.xml
git commit -m "feat(ai): register AI assistant as tool with navigation"
```

---

### Task 10: 创建 AI 设置页面（模型下载管理）

**Files:**
- Create: `app/src/main/java/com/kylecorry/trail_sense/tools/ai_assistant/ui/AiSettingsFragment.kt`
- Modify: `app/src/main/res/navigation/nav_graph.xml`

- [ ] **Step 1: 创建 AiSettingsFragment**

```kotlin
package com.kylecorry.trail_sense.tools.ai_assistant.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.extensions.TrailSenseComposeFragment
import com.kylecorry.trail_sense.shared.extensions.compose.useState
import com.kylecorry.trail_sense.tools.ai_assistant.infrastructure.ModelManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AiSettingsFragment : TrailSenseComposeFragment() {

    @Composable
    override fun FragmentContent() {
        val modelManager = ModelManager(requireContext())
        val (isDownloaded, setIsDownloaded) = useState(modelManager.isModelDownloaded())
        val (isDownloading, setIsDownloading) = useState(false)
        val (progress, setProgress) = useState(0f)
        val (error, setError) = useState<String?>(null)
        val scope = rememberCoroutineScope()

        AiSettingsContent(
            isDownloaded = isDownloaded,
            isDownloading = isDownloading,
            progress = progress,
            error = error,
            onDownload = {
                scope.launch {
                    setIsDownloading(true)
                    setError(null)
                    try {
                        withContext(Dispatchers.IO) {
                            modelManager.downloadModel { p ->
                                setProgress(p)
                            }
                        }
                        setIsDownloaded(true)
                    } catch (e: Exception) {
                        setError(e.message ?: getString(R.string.ai_inference_error))
                    }
                    setIsDownloading(false)
                }
            },
            onDelete = {
                modelManager.deleteModel()
                setIsDownloaded(false)
            }
        )
    }
}

@Composable
private fun AiSettingsContent(
    isDownloaded: Boolean,
    isDownloading: Boolean,
    progress: Float,
    error: String?,
    onDownload: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = stringResource(R.string.ai_settings_title),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.testTag("ai_settings_title")
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.ai_model_name),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = stringResource(R.string.ai_model_size),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (isDownloading) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().testTag("download_progress")
                    )
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                } else if (isDownloaded) {
                    Text(
                        text = stringResource(R.string.ai_model_downloaded),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.testTag("downloaded_label")
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.testTag("delete_button")
                    ) {
                        Text(stringResource(R.string.ai_delete_model))
                    }
                } else {
                    Button(
                        onClick = onDownload,
                        modifier = Modifier.fillMaxWidth().testTag("download_button")
                    ) {
                        Text(stringResource(R.string.ai_download_model))
                    }
                }

                if (error != null) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp).testTag("error_text")
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.ai_about),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.ai_disclaimer),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
```

- [ ] **Step 2: 在 nav_graph.xml 中添加设置 Fragment**

```xml
<fragment
    android:id="@+id/aiSettingsFragment"
    android:name="com.kylecorry.trail_sense.tools.ai_assistant.ui.AiSettingsFragment"
    android:label="AiSettingsFragment" />
```

- [ ] **Step 3: 在 AiAssistantToolRegistration 中添加 settingsNavAction**

将 `AiAssistantToolRegistration.kt` 中 Tool 构造更新为：

```kotlin
return Tool(
    Tools.AI_ASSISTANT,
    context.getString(R.string.tool_ai_assistant_title),
    R.drawable.ic_ai_assistant,
    R.id.aiAssistantFragment,
    ToolCategory.Other,
    settingsNavAction = R.id.aiSettingsFragment,
    isAvailable = { true }  // 改为始终可见，未下载模型时在 Fragment 内提示
)
```

> 注意：`isAvailable` 改为 `{ true }` 使工具始终可见（便于用户发现），下载提示在 Fragment 内显示。

- [ ] **Step 4: 验证编译通过**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/kylecorry/trail_sense/tools/ai_assistant/ui/AiSettingsFragment.kt app/src/main/res/navigation/nav_graph.xml app/src/main/java/com/kylecorry/trail_sense/tools/ai_assistant/AiAssistantToolRegistration.kt
git commit -m "feat(ai): add AI settings page for model download management"
```

---

### Task 11: 集成天气工具 — 在 WeatherFragment 上下文传递 AI 助手

**Files:**
- Modify: `app/src/main/java/com/kylecorry/trail_sense/tools/ai_assistant/ui/AiAssistantFragment.kt`
- Modify: `app/src/main/res/navigation/nav_graph.xml`

此任务让 AI 助手 Fragment 能接收来自其他工具的上下文数据。

- [ ] **Step 1: 给 aiAssistantFragment 添加导航参数**

在 `nav_graph.xml` 中更新 aiAssistantFragment 条目：

```xml
<fragment
    android:id="@+id/aiAssistantFragment"
    android:name="com.kylecorry.trail_sense.tools.ai_assistant.ui.AiAssistantFragment"
    android:label="AiAssistantFragment">
    <argument
        android:name="tool_id"
        app:argType="string"
        app:nullable="true"
        android:defaultValue="@null" />
</fragment>
```

- [ ] **Step 2: 在 AiAssistantFragment 中读取参数并加载上下文**

在 `AiAssistantFragment` 类中添加上下文加载逻辑。在 `FragmentContent()` 的 `LaunchedEffect(Unit)` 开始处添加：

```kotlin
val toolId = arguments?.getString("tool_id")
if (toolId == "weather" && aiContext == null) {
    try {
        val weatherSubsystem = com.kylecorry.trail_sense.tools.weather.infrastructure.subsystem.WeatherSubsystem.getInstance(requireContext())
        val provider = com.kylecorry.trail_sense.tools.ai_assistant.domain.WeatherAiContextProvider {
            weatherSubsystem.getWeather()
        }
        aiContext = provider.getAiContext()
        setSuggestedQuestions(provider.getSuggestedQuestions())
    } catch (_: Exception) {}
}
```

- [ ] **Step 3: 验证编译通过**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/kylecorry/trail_sense/tools/ai_assistant/ui/AiAssistantFragment.kt app/src/main/res/navigation/nav_graph.xml
git commit -m "feat(ai): support receiving weather context via navigation arguments"
```

---

### Task 12: 端到端验证

**Files:** 无新文件

- [ ] **Step 1: 运行全部单元测试**

Run: `./gradlew :app:testDebugUnitTest`
Expected: 全部 PASS（无回归）

- [ ] **Step 2: 验证完整编译**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 手动测试清单**

在设备或模拟器上安装 debug APK，验证以下场景：

1. 打开工具列表 → 能看到 "AI Assistant" 工具
2. 点击 AI Assistant → 显示"模型未下载"提示
3. 进入 Settings → AI Assistant → 点击"下载模型" → 显示进度条 → 下载完成
4. 返回 AI Assistant → 模型加载中动画 → 加载完成后可输入问题
5. 输入问题 → AI 流式返回回答
6. 删除模型 → 确认 AI 功能显示未下载提示

> 注意：需要在有足够存储空间（>3 GB）和 RAM（>2 GB）的设备上测试。GPU 加速需要真机。

- [ ] **Step 4: Commit 最终状态（如有修复）**

```bash
git add -A
git commit -m "fix(ai): address issues found during manual testing"
```
