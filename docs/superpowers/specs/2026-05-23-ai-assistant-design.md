# Trail-Sense AI 助手 — 设计文档

## 概述

为 Trail-Sense 添加设备端 AI 助手，帮助初学者理解传感器数据、识别图像（云、物种）、用自然语言回答问题、并提供上下文感知的安全提醒 — 全部通过 **LiteRT-LM + Gemma 4 E2B** 在本地运行。

### 目标

1. 降低初学者门槛 — 不懂传感器读数或工具使用的用户也能获得帮助
2. 增强现有工具（天气、云识别、导航）— 用 AI 生成自然语言解释
3. 保持隐私优先、设置后离线的原则
4. 与现有 Tool 注册系统无缝集成

### 非目标

- 云端推理或 API 调用（模型一次性下载除外）
- 替换现有 UI — AI 是增强，不是替代
- 支持多模型选择 — 仅推荐单一模型（Gemma 4 E2B）

---

## 架构

### 整体架构

```
┌──────────────────────────────────────────────────┐
│                  AI 助手工具                       │
│  ┌────────────┐  ┌────────────┐  ┌────────────┐  │
│  │  对话 UI   │  │ 快捷问题   │  │ 上下文卡片 │  │
│  └─────┬──────┘  └─────┬──────┘  └─────┬──────┘  │
│        └───────────┬───┘───────────────┘          │
│              AiPromptBuilder                      │
│                    │                              │
│           AiInferenceSubsystem                    │
│        ┌───────────┴───────────┐                  │
│        │     LiteRtLmEngine   │                   │
│        │  Engine → Conversation│                   │
│        │  → sendMessageAsync  │                   │
│        └───────────┬───────────┘                  │
│              ToolProviders                        │
│    ┌────────┬──────┴──────┬──────────┐            │
│    │ 天气   │   导航      │  天文    │ ...        │
│    │Context │  Context    │ Context  │            │
│    └────────┴─────────────┴──────────┘            │
└──────────────────────────────────────────────────┘
```

### 包结构

```
tools/ai_assistant/
├── AiAssistantToolRegistration.kt
├── ui/
│   ├── AiAssistantFragment.kt          // Compose fragment
│   ├── AiChatMessage.kt
│   ├── AiQuickQuestionChips.kt
│   └── AiContextCard.kt
├── domain/
│   ├── AiContextProvider.kt            // 工具接口
│   ├── AiPromptBuilder.kt
│   └── AiToolProviderRegistry.kt       // ToolProvider 注册
└── infrastructure/
    ├── AiInferenceSubsystem.kt         // 单例引擎管理
    ├── LiteRtLmEngine.kt              // LiteRT-LM 封装
    └── ModelManager.kt                 // 下载与存储
```

---

## 推理引擎

### 运行时：LiteRT-LM

使用 `com.google.ai.edge:litertlm` 库，遵循 [Google AI Edge Gallery](https://github.com/google-ai-edge/gallery) 的相同模式。

### 模型：Gemma 4 E2B

| 规格 | 值 |
|------|-------|
| 模型 | Gemma 4 E2B (litert-community/gemma-4-E2B-it-litert-lm) |
| 磁盘大小 | 2.59 GB |
| 运行内存 (GPU) | ~676 MB |
| 运行内存 (CPU) | ~1.7 GB |
| 多模态 | 文本 + 图片 + 音频 |
| 上下文窗口 | 最高 32,768 tokens |
| Function Calling | 通过 ToolProvider 支持 |
| 推测解码 | 支持（2.2x 加速） |
| 许可证 | Apache 2.0 |
| 推理速度 | ~52 tokens/sec (GPU 后端) |

### 引擎生命周期

```kotlin
class AiInferenceSubsystem private constructor(context: Context) {
    private var engine: Engine? = null
    private var conversation: Conversation? = null

    fun isModelAvailable(): Boolean  // 检查模型是否已下载
    fun isEngineReady(): Boolean     // 检查引擎是否已初始化

    suspend fun initialize()          // 加载模型，创建引擎
    suspend fun createConversation(   // 创建带工具的对话
        systemInstruction: Contents,
        tools: List<ToolProvider>
    )
    suspend fun sendMessage(          // 流式推理
        input: String,
        images: List<Bitmap> = emptyList(),
        callback: MessageCallback
    )
    fun stopResponse()
    fun cleanup()
}
```

### 后端选择

自动检测优先级：GPU（首选，最快）→ CPU（兜底）。如设备支持 NPU 则优先使用。遵循 Gallery 应用的模式，`EngineConfig` 默认配置 `Backend.GPU()`。

---

## 模型管理

### 下载流程

```
设置 → AI 助手 → 下载模型
    → 显示模型信息（2.59 GB，Gemma 4 E2B）
    → 用户确认 → 从 HuggingFace 下载
    → 进度条 → 存储到应用内部存储
    → AI 功能在所有工具中可用
```

### 网络权限

在 AndroidManifest 中添加 `INTERNET` 权限。这偏离了当前的纯离线策略，但模型下载必须使用。该权限仅在模型下载时使用；所有推理均在设备端完成。

### ModelManager

```kotlin
class ModelManager(private val context: Context) {
    val modelInfo = AiModel(
        name = "Gemma 4 E2B",
        huggingFaceId = "litert-community/gemma-4-E2B-it-litert-lm",
        fileName = "gemma-4-E2B-it.litertlm",
        sizeBytes = 2_583_000_000L
    )

    fun isModelDownloaded(): Boolean
    fun getModelPath(): String?
    suspend fun downloadModel(onProgress: (Float) -> Unit)
    fun deleteModel()
    fun getModelSizeOnDisk(): Long
}
```

---

## 工具集成

### AiContextProvider 接口

每个需要 AI 支持的工具实现此接口：

```kotlin
interface AiContextProvider {
    val toolId: String
    suspend fun getAiContext(): AiContext
    fun getSuggestedQuestions(): List<String>
}

data class AiContext(
    val toolId: String,
    val toolName: String,
    val sensorData: Map<String, Any>,
    val historicalData: List<Any>?,
    val image: Bitmap?,
    val locale: Locale,
    val summary: String  // 供 prompt 使用的可读数据摘要
)
```

### ToolProvider 注册（Function Calling）

Gemma 4 通过 LiteRT-LM 的 `ToolProvider` 支持 function calling，允许 AI 主动查询应用数据：

```kotlin
object AiToolProviderRegistry {
    fun getToolProviders(context: Context): List<ToolProvider> = listOf(
        // 天气数据
        ToolProvider("get_current_weather") {
            val weather = WeatherSubsystem.getInstance(context).getWeather()
            mapOf(
                "pressure_hpa" to weather.pressure,
                "trend" to weather.trend.name,
                "change_3h" to weather.change3h,
                "forecast" to weather.forecast.name
            ).toJson()
        },
        // 导航状态
        ToolProvider("get_navigation_status") {
            val nav = NavigationSubsystem.getInstance(context).getStatus()
            mapOf(
                "bearing" to nav.bearing,
                "target_distance_m" to nav.targetDistance,
                "target_name" to nav.targetName,
                "off_course_m" to nav.offCourse
            ).toJson()
        },
        // 天文信息
        ToolProvider("get_astronomy_info") {
            val astro = AstronomySubsystem.getInstance(context).getInfo()
            mapOf(
                "sunrise" to astro.sunrise,
                "sunset" to astro.sunset,
                "moon_phase" to astro.moonPhase,
                "daylight_remaining" to astro.daylightRemaining
            ).toJson()
        },
        // 生存指南搜索
        ToolProvider("search_survival_guide") { params ->
            val query = params["query"] as String
            val results = SurvivalGuideSearch(context).search(query)
            results.map { it.toJson() }.toString()
        }
    )
}
```

### 第一阶段工具（MVP）

| 工具 | AiContextProvider | ToolProvider | AI 能力 |
|------|------------------|-------------|---------|
| 天气 | 当前气压、24h 趋势、预报 | `get_current_weather` | 解释气压变化、安全建议 |
| 云识别 | 相机图片 + 现有分类结果 | （通过图片输入） | 增强云类型识别、天气影响解读 |
| 导航 | 方位、距离、偏航 | `get_navigation_status` | 自然语言导航指引 |

### 第二阶段工具

| 工具 | AI 能力 |
|------|---------|
| 天文 | 解释天体事件、最佳观测时间 |
| 潮汐 | 解读潮汐数据、安全警告 |
| 物种图鉴 | AI 增强的拍照物种识别 |

### 第三阶段功能

- 通过后台服务集成主动提醒
- 多工具上下文（结合天气 + 导航 + 天文）
- 对话历史持久化

---

## UI 设计

### AI 助手 Fragment（Compose）

注册为新 Tool，在 `AiAssistantToolRegistration` 中注册，出现在工具列表的 "AI" 分类下。

```
┌─────────────────────────────┐
│ ← AI 助手                   │
├─────────────────────────────┤
│ ┌─────────────────────────┐ │
│ │ 上下文：天气             │ │
│ │ 气压：1008 hPa ↓        │ │
│ │ 趋势：下降中 (-4/3h)    │ │
│ └─────────────────────────┘ │
│                             │
│ ┌─AI─────────────────────┐  │
│ │ 气压在 3 小时内下降了   │  │
│ │ 4 hPa。这种快速下降通  │  │
│ │ 常预示恶劣天气即将到来。│  │
│ │ 建议在 1-2 小时内寻找   │  │
│ │ 避风处。                │  │
│ └─────────────────────────┘  │
│                             │
│ [安全吗?] [接下来怎么办?]   │  ← 快捷问题
│                             │
│ ┌─────────────────────┬───┐ │
│ │ 输入你的问题...      │ → │ │  ← 自由输入
│ └─────────────────────┴───┘ │
└─────────────────────────────┘
```

### 工具页面的 "Ask AI" 悬浮按钮

每个已集成的工具页面显示一个 FAB。点击后携带该工具的上下文跳转到 AI 助手 Fragment。

```kotlin
// 在 WeatherFragment 或任何已集成的工具中
AiAssistantFab(
    visible = aiInference.isModelAvailable(),
    onClick = {
        val context = weatherAiContextProvider.getAiContext()
        navigateToAiAssistant(context)
    }
)
```

未下载模型时，FAB 隐藏（不是灰显，而是不可见）。

### 模型下载 UI（设置页）

```
设置 → AI 助手
├── 模型：Gemma 4 E2B (2.59 GB)
│   ├── [下载] / [已下载 ✓]
│   └── [删除模型]
├── 说明："AI 完全在你的设备上运行..."
└── 免责声明："AI 建议仅供参考..."
```

---

## Prompt 工程

### System Instruction

```
You are a wilderness survival assistant built into the Trail Sense app.

Rules:
- Respond in the user's language ({locale}).
- Be concise: max 80 words per response.
- Prioritize safety-critical information.
- When interpreting sensor data, explain what it means in practical terms.
- Never fabricate sensor readings — use only data from tool calls.
- Always clarify that your advice is supplementary, not a replacement for
  proper training and judgment.

You have access to these tools:
- get_current_weather: Get current barometric pressure, trend, and forecast
- get_navigation_status: Get current bearing, distance to target, off-course info
- get_astronomy_info: Get sunrise/sunset, moon phase, daylight remaining
- search_survival_guide: Search the built-in survival reference guide
```

### 上下文注入

从特定工具启动时，初始上下文会被预注入：

```
[来自天气工具的上下文]
当前气压：1008.2 hPa
3 小时变化：-4.1 hPa（快速下降）
趋势：下降中
当前预报：可能有暴风雨
所在海拔：1,200m

用户正在查看天气工具，想要理解这些数据。
```

---

## 主动提醒（第三阶段）

与现有后台服务集成，生成 AI 增强的通知：

```kotlin
// 在 WeatherMonitorService 中
if (modelManager.isModelDownloaded() && weatherChange.isCritical()) {
    val insight = aiInference.generateOneShot(
        systemPrompt = "Generate a brief safety alert (under 30 words) for this weather change.",
        context = "Pressure dropped ${change}hPa in ${hours}h at ${altitude}m altitude."
    )
    notificationSubsystem.send(
        AiInsightNotification(
            title = getString(R.string.ai_weather_alert),
            body = insight
        )
    )
}
```

第三阶段需要引擎支持轻量级 "one-shot" 模式，快速初始化用于短文本生成，无需维护完整对话。

---

## 技术约束与风险

| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| 模型体积（2.59 GB 下载） | 用户可能不愿占用存储 | 下载前清晰展示大小信息，设置中提供删除选项 |
| CPU 占用 ~1.7 GB RAM / GPU ~676 MB | 低端设备可能吃力 | 检测可用 RAM，警告用户，优先使用 GPU 后端 |
| 首次推理冷启动（2-5 秒） | 体验感觉卡顿 | 显示加载动画，App 启动时预热引擎 |
| LiteRT-LM minSdk 兼容性 | Trail-Sense minSdk 是 23，LiteRT-LM 可能要求更高 | AI 功能通过运行时 SDK 检查门控；旧设备不可用 |
| 小模型幻觉 | 给出不正确的安全建议 | 所有 AI 输出标注免责声明；AI 解读旁同时显示原始数据 |
| 添加 INTERNET 权限 | 打破纯离线原则 | 仅用于模型下载；向用户清晰说明 |
| Gemma 4 E2B 精度限制 | 小众场景可能给出模糊回答 | 通过 system prompt 约束范围；利用 function calling 获取真实数据 |

---

## 分阶段交付

### 第一阶段 — MVP：AI 数据解读器

- AiInferenceSubsystem + LiteRtLmEngine
- ModelManager（从 HuggingFace 下载）
- AI 助手 Fragment（Compose）
- 天气 AiContextProvider + ToolProvider
- 天气工具页面的 "Ask AI" FAB
- 设置页面的模型管理
- 基础 system prompt + 上下文注入

### 第二阶段 — 图像识别

- 云识别工具：Gemma 4 多模态云类型识别
- 物种图鉴：拍照 → 物种识别
- 导航工具集成
- 每个工具的快捷问题 chips

### 第三阶段 — 全面集成 + 主动提醒

- 天文、潮汐及其余工具接入
- 通过后台服务的主动 AI 提醒
- 多工具上下文感知
- 对话历史持久化（可选）
- 生存指南 RAG（搜索 + AI 综合）

---

## 需要添加的依赖

```toml
# gradle/libs.versions.toml
[versions]
litertlm = "latest"  # 参见 https://github.com/google-ai-edge/LiteRT-LM

[libraries]
litertlm = { module = "com.google.ai.edge:litertlm", version.ref = "litertlm" }
```

```kotlin
// app/build.gradle.kts
dependencies {
    implementation(libs.litertlm)
}
```

AndroidManifest:
```xml
<uses-permission android:name="android.permission.INTERNET" />
```
