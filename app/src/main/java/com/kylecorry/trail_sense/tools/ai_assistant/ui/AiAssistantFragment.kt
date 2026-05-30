package com.kylecorry.trail_sense.tools.ai_assistant.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.clipboard.Clipboard
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.extensions.TrailSenseComposeFragment
import com.kylecorry.trail_sense.shared.extensions.compose.useState
import com.kylecorry.trail_sense.shared.navigateWithAnimation
import com.kylecorry.trail_sense.shared.sensors.LocationSubsystem
import com.kylecorry.trail_sense.tools.ai_assistant.domain.AiContext
import com.kylecorry.trail_sense.tools.ai_assistant.domain.AiPromptBuilder
import com.kylecorry.trail_sense.tools.ai_assistant.domain.AiToolCallCard
import com.kylecorry.trail_sense.tools.ai_assistant.domain.AiToolKnowledgeService
import com.kylecorry.trail_sense.tools.ai_assistant.domain.AiToolRunStatus
import com.kylecorry.trail_sense.tools.ai_assistant.domain.AiToolSkillEntry
import com.kylecorry.trail_sense.tools.ai_assistant.domain.CloudAiContextProvider
import com.kylecorry.trail_sense.tools.ai_assistant.domain.NavigationAiContextProvider
import com.kylecorry.trail_sense.tools.ai_assistant.domain.WeatherAiContextProvider
import com.kylecorry.trail_sense.tools.ai_assistant.infrastructure.AiAssistantTools
import com.kylecorry.trail_sense.tools.ai_assistant.infrastructure.AiInferenceSubsystem
import com.kylecorry.trail_sense.tools.ai_assistant.infrastructure.AiToolExecutionService
import com.kylecorry.trail_sense.tools.clouds.infrastructure.CloudDetailsService
import com.kylecorry.trail_sense.tools.clouds.infrastructure.persistence.CloudRepo
import com.kylecorry.trail_sense.tools.navigation.domain.NavigationService
import com.kylecorry.trail_sense.tools.navigation.infrastructure.Navigator
import com.kylecorry.trail_sense.tools.ai_assistant.infrastructure.persistence.AiChatRepo
import com.kylecorry.trail_sense.tools.ai_assistant.infrastructure.persistence.ChatSessionEntity
import com.kylecorry.trail_sense.tools.weather.infrastructure.subsystem.WeatherSubsystem
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.MessageCallback
import com.google.ai.edge.litertlm.ToolProvider
import com.google.ai.edge.litertlm.tool
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.roundToInt

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val isLoading: Boolean = false,
    val image: Bitmap? = null,
    val toolCalls: List<AiToolCallCard> = emptyList(),
    val id: Long? = null
)

class AiAssistantFragment : TrailSenseComposeFragment() {

    private var aiContext: AiContext? = null
    private var imagePickerCallback: ((Uri?) -> Unit)? = null
    private val imagePicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        imagePickerCallback?.invoke(uri)
        imagePickerCallback = null
    }

    private fun pickImage(onPick: (Uri?) -> Unit) {
        imagePickerCallback = onPick
        imagePicker.launch("image/*")
    }

    private suspend fun loadBitmapFromUri(uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(requireContext().contentResolver, uri)) { decoder, info, _ ->
                    val size = getScaledImageSize(info.size.width, info.size.height)
                    decoder.setTargetSize(size.first, size.second)
                }
            } else {
                loadScaledBitmapLegacy(uri)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun loadScaledBitmapLegacy(uri: Uri): Bitmap? {
        val resolver = requireContext().contentResolver
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        }

        val (width, height) = getScaledImageSize(options.outWidth, options.outHeight)
        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = calculateSampleSize(options.outWidth, options.outHeight)
        }
        val sampled = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, decodeOptions)
        } ?: return null

        if (sampled.width == width && sampled.height == height) {
            return sampled
        }

        val scaled = Bitmap.createScaledBitmap(sampled, width, height, true)
        sampled.recycle()
        return scaled
    }

    private fun calculateSampleSize(width: Int, height: Int): Int {
        var sampleSize = 1
        while (width / (sampleSize * 2) >= MAX_ATTACHED_IMAGE_SIZE ||
            height / (sampleSize * 2) >= MAX_ATTACHED_IMAGE_SIZE
        ) {
            sampleSize *= 2
        }
        return sampleSize
    }

    private fun getScaledImageSize(width: Int, height: Int): Pair<Int, Int> {
        val largestSide = width.coerceAtLeast(height)
        if (largestSide <= MAX_ATTACHED_IMAGE_SIZE || largestSide <= 0) {
            return width.coerceAtLeast(1) to height.coerceAtLeast(1)
        }

        val scale = MAX_ATTACHED_IMAGE_SIZE.toFloat() / largestSide.toFloat()
        return (width * scale).roundToInt().coerceAtLeast(1) to
            (height * scale).roundToInt().coerceAtLeast(1)
    }

    @Composable
    override fun FragmentContent() {
        val aiSubsystem = AiInferenceSubsystem.getInstance(requireContext())
        val toolKnowledgeService = remember {
            AiToolKnowledgeService(requireContext().applicationContext)
        }
        val toolExecutionService = remember {
            AiToolExecutionService(requireContext().applicationContext)
        }
        val aiAssistantTools = remember {
            AiAssistantTools(toolExecutionService)
        }
        val aiToolProviders = remember {
            listOf(tool(aiAssistantTools))
        }
        val availableSkills = remember { toolKnowledgeService.getSkills() }
        val chatRepo = AiChatRepo.getInstance(requireContext())
        val (messages, setMessages) = useState(listOf<ChatMessage>())
        val (inputText, setInputText) = useState("")
        val (isGenerating, setIsGenerating) = useState(false)
        val (isInitializing, setIsInitializing) = useState(false)
        val (error, setError) = useState<String?>(null)
        val (suggestedQuestions, setSuggestedQuestions) = useState(emptyList<String>())
        val (attachedImage, setAttachedImage) = useState<Bitmap?>(null)
        val (currentSessionId, setCurrentSessionId) = useState<Long?>(null)
        val (showHistory, setShowHistory) = useState(false)
        val (sessions, setSessions) = useState(emptyList<ChatSessionEntity>())
        val (showSkillPicker, setShowSkillPicker) = useState(false)
        val (selectedSkillIds, setSelectedSkillIds) = useState(availableSkills.map { it.id }.toSet())
        val listState = rememberLazyListState()
        val scope = rememberCoroutineScope()

        LaunchedEffect(Unit) {
            val toolId = arguments?.getString("tool_id")
            val imageUri = arguments?.getString("image_uri")?.let { Uri.parse(it) }
            val shouldRestoreLatestSession = imageUri == null
            var hasContextSuggestions = false
            val existingSessions = chatRepo.getAllSessions()
            setSessions(existingSessions)

            when (toolId) {
                "weather" -> {
                    try {
                        val weatherSubsystem = WeatherSubsystem.getInstance(requireContext())
                        val provider = WeatherAiContextProvider { weatherSubsystem.getWeather() }
                        aiContext = provider.getAiContext()
                        setSuggestedQuestions(provider.getSuggestedQuestions())
                        hasContextSuggestions = true
                    } catch (_: Exception) {}
                }
                "clouds" -> {
                    try {
                        val image = imageUri?.let { loadBitmapFromUri(it) }
                        val provider = CloudAiContextProvider(
                            CloudRepo.getInstance(requireContext()),
                            CloudDetailsService(requireContext()),
                            image
                        )
                        aiContext = provider.getAiContext()
                        setSuggestedQuestions(provider.getSuggestedQuestions())
                        hasContextSuggestions = true
                        if (image != null) setAttachedImage(image)
                    } catch (_: Exception) {}
                }
                "navigation" -> {
                    try {
                        val nav = Navigator.getInstance(requireContext())
                        val loc = LocationSubsystem.getInstance(requireContext())
                        val provider = NavigationAiContextProvider(
                            nav, NavigationService(), loc.location
                        )
                        aiContext = provider.getAiContext()
                        setSuggestedQuestions(provider.getSuggestedQuestions())
                        hasContextSuggestions = true
                    } catch (_: Exception) {}
                }
            }

            var restoredLatestSession = false
            if (shouldRestoreLatestSession) {
                val latestSession = existingSessions.firstOrNull()
                if (latestSession != null) {
                    val msgs = loadChatMessages(chatRepo, latestSession.id)
                    setMessages(msgs)
                    setCurrentSessionId(latestSession.id)
                    setSuggestedQuestions(emptyList())
                    restoredLatestSession = true
                }
            }

            if (!restoredLatestSession && !hasContextSuggestions) {
                setSuggestedQuestions(getDefaultSkillQuestions())
            }

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
                        systemInstruction = Contents.of(listOf(Content.Text(systemPrompt))),
                        tools = aiToolProviders
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "AI initialization failed", e)
                    setError(getString(R.string.ai_initialization_failed))
                }
                setIsInitializing(false)
            }
        }

        fun sendMessage(text: String) {
            if (text.isBlank() || isGenerating) return
            val messageImage = attachedImage ?: aiContext?.image
            val userMessage = ChatMessage(text, isUser = true, image = messageImage)
            val loadingMessage = ChatMessage("", isUser = false, isLoading = true)
            val priorMessages = messages
            aiAssistantTools.configure(selectedSkillIds)
            toolExecutionService.configure(selectedSkillIds)
            val selectedSkill = toolExecutionService.selectSkill(text, selectedSkillIds)
            selectedSkill?.let { toolExecutionService.activateSkill(it) }
            setMessages(priorMessages + userMessage + loadingMessage)
            setInputText("")
            setIsGenerating(true)
            setSuggestedQuestions(emptyList())

            val hasImage = messageImage != null
            val images = listOfNotNull(messageImage)
            val currentAiContext = aiContext
            aiContext = null
            setAttachedImage(null)

            val responseBuilder = StringBuilder()

            suspend fun saveConversation(
                response: String,
                toolCardMessages: List<ChatMessage>
            ): List<ChatMessage> {
                val sessionId = currentSessionId ?: chatRepo.createSession(
                    text.take(50)
                ).also { setCurrentSessionId(it) }
                val imagePath = messageImage?.let { chatRepo.saveMessageImage(it) }
                val userMessageId = chatRepo.addMessage(
                    sessionId,
                    text,
                    isUser = true,
                    imagePath = imagePath
                )
                val savedToolMessages = toolCardMessages
                    .filter { it.toolCalls.isNotEmpty() }
                    .map {
                        val toolMessageId = chatRepo.addMessage(
                            sessionId,
                            "",
                            isUser = false,
                            toolCallsJson = AiToolCallCard.toJson(it.toolCalls)
                        )
                        it.copy(id = toolMessageId)
                    }
                val responseMessageId = chatRepo.addMessage(
                    sessionId,
                    response,
                    isUser = false
                )

                return priorMessages +
                    userMessage.copy(id = userMessageId) +
                    savedToolMessages +
                    ChatMessage(response, isUser = false, id = responseMessageId)
            }

            scope.launch {
                val completedToolMessages = mutableListOf<ChatMessage>()
                try {
                    val skillRun = selectedSkill?.let {
                        toolExecutionService.executeStepByStep(
                            skill = it,
                            onToolStarted = { runningCard ->
                                setMessages(
                                    priorMessages +
                                        userMessage +
                                        completedToolMessages +
                                        ChatMessage(
                                            text = "",
                                            isUser = false,
                                            toolCalls = listOf(runningCard)
                                        ) +
                                        loadingMessage
                                )
                            },
                            onToolFinished = { completedCard ->
                                completedToolMessages.add(
                                    ChatMessage(
                                        text = "",
                                        isUser = false,
                                        toolCalls = listOf(completedCard)
                                    )
                                )
                                setMessages(
                                    priorMessages +
                                        userMessage +
                                        completedToolMessages +
                                        loadingMessage
                                )
                            }
                        )
                    }
                    val toolResults = skillRun?.toPromptContext()

                    val toolKnowledge = toolKnowledgeService.getPromptContext(
                        text,
                        currentAiContext?.toolId,
                        enabledSkillIds = selectedSkillIds
                    )
                    val chatHistory = if (hasImage) null else buildChatHistory(priorMessages)
                    val prompt = AiPromptBuilder.buildUserPrompt(
                        currentAiContext,
                        text,
                        toolKnowledge,
                        chatHistory,
                        hasImage = hasImage,
                        toolResults = toolResults
                    )

                    if (hasImage) {
                        recreateConversation(aiSubsystem, aiToolProviders)
                    }
                    aiSubsystem.sendMessage(
                        input = prompt,
                        images = images,
                        callback = object : MessageCallback {
                            override fun onMessage(message: Message) {
                                val partialResponse = message.toString()
                                if (partialResponse.isEmpty()) {
                                    return
                                }
                                responseBuilder.append(partialResponse)
                                val response = formatAiResponse(responseBuilder.toString())
                                if (response.isBlank()) {
                                    return
                                }
                                scope.launch {
                                    val updated = priorMessages +
                                        userMessage +
                                        completedToolMessages +
                                        ChatMessage(response, isUser = false)
                                    setMessages(updated)
                                }
                            }

                            override fun onDone() {
                                scope.launch {
                                    val response = formatAiResponse(responseBuilder.toString())
                                        .ifBlank { getString(R.string.ai_inference_error) }
                                    setIsGenerating(false)
                                    setMessages(saveConversation(response, completedToolMessages.toList()))
                                    setSessions(chatRepo.getAllSessions())
                                }
                            }

                            override fun onError(throwable: Throwable) {
                                Log.e(TAG, "AI response failed", throwable)
                                scope.launch {
                                    val errorMsg = if (throwable is java.util.concurrent.CancellationException) {
                                        formatAiResponse(responseBuilder.toString())
                                            .ifBlank { getString(R.string.ai_inference_error) }
                                    } else {
                                        getString(R.string.ai_inference_error)
                                    }
                                    val response = formatAiResponse(responseBuilder.toString())
                                        .ifBlank { errorMsg }
                                    setIsGenerating(false)
                                    setMessages(saveConversation(response, completedToolMessages.toList()))
                                    setSessions(chatRepo.getAllSessions())
                                }
                            }
                        }
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "AI message send failed", e)
                    val errorMsg = getString(R.string.ai_inference_error)
                    setIsGenerating(false)
                    setMessages(saveConversation(errorMsg, completedToolMessages.toList()))
                    setSessions(chatRepo.getAllSessions())
                }
            }
        }

        LaunchedEffect(messages.size) {
            if (messages.isNotEmpty()) {
                listState.animateScrollToItem(messages.size - 1)
            }
        }

        if (showHistory) {
            ChatHistoryScreen(
                sessions = sessions,
                onSessionClick = { session ->
                    scope.launch {
                        val msgs = loadChatMessages(chatRepo, session.id)
                        setMessages(msgs)
                        setCurrentSessionId(session.id)
                        setShowHistory(false)
                        setSuggestedQuestions(emptyList())
                    }
                },
                onDeleteSession = { session ->
                    scope.launch {
                        chatRepo.deleteSession(session.id)
                        val remainingSessions = chatRepo.getAllSessions()
                        setSessions(remainingSessions)
                        if (currentSessionId == session.id) {
                            val latestSession = remainingSessions.firstOrNull()
                            if (latestSession == null) {
                                setMessages(emptyList())
                                setCurrentSessionId(null)
                                setSuggestedQuestions(getDefaultSkillQuestions())
                            } else {
                                val msgs = loadChatMessages(chatRepo, latestSession.id)
                                setMessages(msgs)
                                setCurrentSessionId(latestSession.id)
                                setSuggestedQuestions(emptyList())
                            }
                        }
                    }
                },
                onBack = { setShowHistory(false) }
            )
        } else {
            AiAssistantContent(
                messages = messages,
                inputText = inputText,
                isGenerating = isGenerating,
                isInitializing = isInitializing,
                error = error,
                suggestedQuestions = suggestedQuestions,
                attachedImage = attachedImage,
                onInputChanged = setInputText,
                onSend = ::sendMessage,
                onStop = { aiSubsystem.stopResponse() },
                onCameraClick = {
                    scope.launch {
                        val uri = CustomUiUtils.takePhoto(this@AiAssistantFragment)
                        uri?.let { setAttachedImage(loadBitmapFromUri(it)) }
                    }
                },
                onPickImageClick = {
                    pickImage { uri ->
                        uri ?: return@pickImage
                        scope.launch {
                            setAttachedImage(loadBitmapFromUri(uri))
                        }
                    }
                },
                onPickScreenshotClick = {
                    pickImage { uri ->
                        uri ?: return@pickImage
                        scope.launch {
                            setAttachedImage(loadBitmapFromUri(uri))
                        }
                    }
                },
                onRemoveImage = { setAttachedImage(null) },
                skills = availableSkills,
                selectedSkillIds = selectedSkillIds,
                showSkillPicker = showSkillPicker,
                onSkillsClick = { setShowSkillPicker(true) },
                onDismissSkills = { setShowSkillPicker(false) },
                onSkillSelectedChange = { skill, selected ->
                    setSelectedSkillIds(
                        if (selected) {
                            selectedSkillIds + skill.id
                        } else {
                            selectedSkillIds - skill.id
                        }
                    )
                },
                onSelectAllSkills = { setSelectedSkillIds(availableSkills.map { it.id }.toSet()) },
                onClearSkills = { setSelectedSkillIds(emptySet()) },
                onHistoryClick = {
                    scope.launch {
                        setSessions(chatRepo.getAllSessions())
                        setShowHistory(true)
                    }
                },
                onNewChat = {
                    setMessages(emptyList())
                    setCurrentSessionId(null)
                    aiContext = null
                    aiAssistantTools.configure(selectedSkillIds)
                    setSuggestedQuestions(getDefaultSkillQuestions())
                    scope.launch {
                        aiSubsystem.createConversation(
                            systemInstruction = Contents.of(listOf(Content.Text(
                                AiPromptBuilder.buildSystemPrompt(
                                    resources.configuration.locales[0] ?: Locale.ENGLISH
                                )
                            ))),
                            tools = aiToolProviders
                        )
                    }
                },
                onDeleteMessage = { message ->
                    setMessages(messages.filterNot { it === message })
                    message.id?.let { id ->
                        scope.launch {
                            chatRepo.deleteMessage(id)
                            setSessions(chatRepo.getAllSessions())
                        }
                    }
                },
                listState = listState,
                contextCard = aiContext?.summary,
                showAgentSkillsIntro = messages.isEmpty() && aiContext == null,
                onOpenTool = { navAction ->
                    findNavController().navigateWithAnimation(navAction)
                }
            )
        }
    }

    private companion object {
        private const val TAG = "AiAssistantFragment"
        private const val MAX_ATTACHED_IMAGE_SIZE = 1024
        private const val IMAGE_ATTACHMENT_PREFIX = "[Image attached]"
    }

    private suspend fun loadChatMessages(
        chatRepo: AiChatRepo,
        sessionId: Long
    ): List<ChatMessage> {
        return chatRepo.getMessages(sessionId).map {
            ChatMessage(
                text = it.text.removePrefix("$IMAGE_ATTACHMENT_PREFIX\n"),
                isUser = it.isUser,
                image = it.imagePath?.let { path -> chatRepo.loadMessageImage(path) },
                toolCalls = AiToolCallCard.listFromJson(it.toolCallsJson),
                id = it.id
            )
        }
    }

    private fun buildChatHistory(messages: List<ChatMessage>): String? {
        val history = messages
            .filter { !it.isLoading && it.text.isNotBlank() }
            .takeLast(6)
            .joinToString("\n") {
                val sender = if (it.isUser) "User" else "Assistant"
                "$sender: ${it.text}"
            }

        return history.takeIf { it.isNotBlank() }
    }

    private fun formatAiResponse(response: String): String {
        return response.replace("\\n", "\n")
    }

    private fun getDefaultSkillQuestions(): List<String> {
        return listOf(
            getString(R.string.ai_skill_prompt_avalanche),
            getString(R.string.ai_skill_prompt_storm),
            getString(R.string.ai_skill_prompt_navigate_back),
            getString(R.string.ai_skill_prompt_cold_elevation)
        )
    }

    private suspend fun recreateConversation(
        aiSubsystem: AiInferenceSubsystem,
        tools: List<ToolProvider>
    ) {
        val systemPrompt = AiPromptBuilder.buildSystemPrompt(
            resources.configuration.locales[0] ?: Locale.ENGLISH
        )
        aiSubsystem.createConversation(
            systemInstruction = Contents.of(listOf(Content.Text(systemPrompt))),
            tools = tools
        )
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun AiAssistantContent(
    messages: List<ChatMessage>,
    inputText: String,
    isGenerating: Boolean,
    isInitializing: Boolean,
    error: String?,
    suggestedQuestions: List<String>,
    attachedImage: Bitmap?,
    onInputChanged: (String) -> Unit,
    onSend: (String) -> Unit,
    onStop: () -> Unit,
    onCameraClick: () -> Unit,
    onPickImageClick: () -> Unit,
    onPickScreenshotClick: () -> Unit,
    onRemoveImage: () -> Unit,
    skills: List<AiToolSkillEntry>,
    selectedSkillIds: Set<String>,
    showSkillPicker: Boolean,
    onSkillsClick: () -> Unit,
    onDismissSkills: () -> Unit,
    onSkillSelectedChange: (AiToolSkillEntry, Boolean) -> Unit,
    onSelectAllSkills: () -> Unit,
    onClearSkills: () -> Unit,
    onHistoryClick: () -> Unit,
    onNewChat: () -> Unit,
    onDeleteMessage: (ChatMessage) -> Unit,
    listState: androidx.compose.foundation.lazy.LazyListState,
    contextCard: String?,
    showAgentSkillsIntro: Boolean,
    onOpenTool: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val (showImageMenu, setShowImageMenu) = useState(false)

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onHistoryClick, modifier = Modifier.testTag("history_button")) {
                Icon(painterResource(R.drawable.ic_tool_notes), contentDescription = stringResource(R.string.ai_chat_history))
            }
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painterResource(R.drawable.ic_ai_assistant),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(R.string.ai_agent_skills_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            IconButton(onClick = onNewChat, modifier = Modifier.testTag("new_chat_button")) {
                Icon(painterResource(R.drawable.ic_add), contentDescription = stringResource(R.string.ai_new_chat))
            }
        }

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

        if (showAgentSkillsIntro) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth().testTag("agent_skills_intro"),
                contentAlignment = Alignment.Center
            ) {
                AgentSkillsIntro()
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth().testTag("chat_list"),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages) { message ->
                    ChatBubble(
                        message = message,
                        onDelete = { onDeleteMessage(message) },
                        onOpenTool = onOpenTool
                    )
                }
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

        if (attachedImage != null) {
            Row(
                modifier = Modifier.padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    bitmap = attachedImage.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp))
                )
                IconButton(onClick = onRemoveImage) {
                    Icon(painterResource(R.drawable.ic_cancel), contentDescription = null)
                }
            }
        }

        Text(
            text = stringResource(R.string.ai_disclaimer),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 4.dp)
        )

        PromptInputCard(
            inputText = inputText,
            isGenerating = isGenerating,
            enabled = error == null,
            selectedSkillCount = selectedSkillIds.size,
            onInputChanged = onInputChanged,
            onSend = onSend,
            onStop = onStop,
            onSkillsClick = onSkillsClick,
            onShowImageMenu = { setShowImageMenu(true) }
        )

        DropdownMenu(
            expanded = showImageMenu,
            onDismissRequest = { setShowImageMenu(false) }
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.ai_take_photo)) },
                onClick = {
                    setShowImageMenu(false)
                    onCameraClick()
                },
                leadingIcon = {
                    Icon(painterResource(R.drawable.ic_camera), contentDescription = null)
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.ai_choose_image)) },
                onClick = {
                    setShowImageMenu(false)
                    onPickImageClick()
                },
                leadingIcon = {
                    Icon(painterResource(R.drawable.ic_file), contentDescription = null)
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.ai_choose_screenshot)) },
                onClick = {
                    setShowImageMenu(false)
                    onPickScreenshotClick()
                },
                leadingIcon = {
                    Icon(painterResource(R.drawable.ic_phone), contentDescription = null)
                }
            )
        }

        if (showSkillPicker) {
            SkillPickerBottomSheet(
                skills = skills,
                selectedSkillIds = selectedSkillIds,
                onDismiss = onDismissSkills,
                onSkillSelectedChange = onSkillSelectedChange,
                onSelectAll = onSelectAllSkills,
                onClear = onClearSkills
            )
        }
    }
}

@Composable
private fun PromptInputCard(
    inputText: String,
    isGenerating: Boolean,
    enabled: Boolean,
    selectedSkillCount: Int,
    onInputChanged: (String) -> Unit,
    onSend: (String) -> Unit,
    onStop: () -> Unit,
    onSkillsClick: () -> Unit,
    onShowImageMenu: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag("prompt_input_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
            BasicTextField(
                value = inputText,
                onValueChange = onInputChanged,
                enabled = !isGenerating && enabled,
                maxLines = 3,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .testTag("input_field"),
                decorationBox = { innerTextField ->
                    if (inputText.isBlank()) {
                        Text(
                            text = stringResource(R.string.ai_input_hint),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    innerTextField()
                }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = onShowImageMenu,
                        enabled = !isGenerating && enabled,
                        modifier = Modifier.testTag("image_button")
                    ) {
                        Icon(
                            painterResource(R.drawable.ic_add),
                            contentDescription = stringResource(R.string.ai_attach_image)
                        )
                    }
                    OutlinedButton(
                        onClick = onSkillsClick,
                        enabled = !isGenerating && enabled,
                        modifier = Modifier.testTag("skills_button")
                    ) {
                        Text(stringResource(R.string.ai_skills))
                        Spacer(modifier = Modifier.width(6.dp))
                        SkillCountBadge(selectedSkillCount)
                    }
                }

                IconButton(
                    onClick = {
                        if (isGenerating) onStop() else onSend(inputText)
                    },
                    enabled = enabled,
                    modifier = Modifier.testTag("send_button")
                ) {
                    if (isGenerating) {
                        Icon(painterResource(R.drawable.ic_cancel), contentDescription = null)
                    } else {
                        Icon(painterResource(R.drawable.ic_send), contentDescription = null)
                    }
                }
            }
        }
    }
}

@Composable
private fun SkillCountBadge(count: Int) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .height(18.dp)
            .widthIn(min = 18.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SkillPickerBottomSheet(
    skills: List<AiToolSkillEntry>,
    selectedSkillIds: Set<String>,
    onDismiss: () -> Unit,
    onSkillSelectedChange: (AiToolSkillEntry, Boolean) -> Unit,
    onSelectAll: () -> Unit,
    onClear: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.ai_skills),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onSelectAll) {
                    Text(stringResource(R.string.ai_skills_turn_on_all))
                }
                TextButton(onClick = onClear) {
                    Text(stringResource(R.string.ai_skills_turn_off_all))
                }
            }

            Text(
                text = stringResource(R.string.ai_skills_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(skills, key = { it.id }) { skill ->
                    SkillPickerRow(
                        skill = skill,
                        selected = skill.id in selectedSkillIds,
                        onSelectedChange = { selected -> onSkillSelectedChange(skill, selected) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SkillPickerRow(
    skill: AiToolSkillEntry,
    selected: Boolean,
    onSelectedChange: (Boolean) -> Unit
) {
    val language = LocalContext.current.resources.configuration.locales[0]?.language ?: "en"
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = skill.name(language),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = skill.summary(language),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Checkbox(
                checked = selected,
                onCheckedChange = onSelectedChange
            )
        }
    }
}

@Composable
private fun AgentSkillsIntro() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 24.dp)
    ) {
        Text(
            text = stringResource(R.string.ai_agent_skills_intro_label),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = stringResource(R.string.ai_agent_skills_title),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(
            text = stringResource(R.string.ai_agent_skills_description),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 24.dp)
        )
        Text(
            text = stringResource(R.string.ai_agent_skills_try_prompt),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 32.dp)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChatBubble(
    message: ChatMessage,
    onDelete: () -> Unit,
    onOpenTool: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!message.isLoading && message.text.isBlank() && message.image == null && message.toolCalls.isEmpty()) {
        return
    }

    val context = LocalContext.current
    val (showMenu, setShowMenu) = useState(false)
    val containerColor = if (message.isUser) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }
    val canShowActions = !message.isLoading
    val actionModifier = if (canShowActions) {
        Modifier.combinedClickable(
            onClick = {},
            onLongClick = { setShowMenu(true) }
        )
    } else {
        Modifier
    }

    val isToolOnlyMessage = !message.isUser &&
        !message.isLoading &&
        message.text.isBlank() &&
        message.image == null &&
        message.toolCalls.isNotEmpty()

    if (isToolOnlyMessage) {
        ToolCallBubble(
            message = message,
            onDelete = onDelete,
            onOpenTool = onOpenTool,
            modifier = modifier
        )
        return
    }

    Box(modifier = modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier
                .align(if (message.isUser) Alignment.CenterEnd else Alignment.CenterStart)
                .fillMaxWidth(0.85f)
                .then(actionModifier)
                .testTag(if (message.isUser) "user_message" else "ai_message"),
            colors = CardDefaults.cardColors(containerColor = containerColor)
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                if (message.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(12.dp).testTag("message_loading"),
                        strokeWidth = 2.dp
                    )
                } else {
                    Column(
                        modifier = Modifier.padding(
                            start = 12.dp,
                            top = 12.dp,
                            end = if (canShowActions) 44.dp else 12.dp,
                            bottom = 12.dp
                        )
                    ) {
                        if (message.image != null) {
                            Image(
                                bitmap = message.image.asImageBitmap(),
                                contentDescription = stringResource(R.string.ai_attached_image),
                                modifier = Modifier
                                    .size(160.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .testTag("message_image")
                            )
                        }
                        if (message.toolCalls.isNotEmpty()) {
                            ToolCallPanel(
                                toolCalls = message.toolCalls,
                                onOpenTool = onOpenTool,
                                modifier = if (message.image == null) {
                                    Modifier
                                } else {
                                    Modifier.padding(top = 8.dp)
                                }
                            )
                        }
                        if (message.text.isNotBlank()) {
                            Text(
                                text = markdownToAnnotatedString(message.text),
                                modifier = if (message.image == null && message.toolCalls.isEmpty()) {
                                    Modifier
                                } else {
                                    Modifier.padding(top = 8.dp)
                                },
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    if (canShowActions) {
                        IconButton(
                            onClick = { setShowMenu(true) },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 4.dp, end = 4.dp)
                                .size(32.dp)
                                .testTag("message_menu_button")
                        ) {
                            Icon(
                                painterResource(R.drawable.ic_menu_dots),
                                contentDescription = null
                            )
                        }
                    }
                }
            }
        }
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { setShowMenu(false) }
        ) {
            if (message.text.isNotBlank()) {
                DropdownMenuItem(
                    text = { Text(stringResource(androidx.preference.R.string.copy)) },
                    onClick = {
                        setShowMenu(false)
                        Clipboard.copy(
                            context,
                            markdownToPlainText(message.text),
                            context.getString(R.string.copied_to_clipboard_toast)
                        )
                    },
                    leadingIcon = {
                        Icon(painterResource(R.drawable.ic_copy), contentDescription = null)
                    }
                )
            }
            DropdownMenuItem(
                text = { Text(stringResource(R.string.delete)) },
                onClick = {
                    setShowMenu(false)
                    onDelete()
                },
                leadingIcon = {
                    Icon(painterResource(R.drawable.ic_delete), contentDescription = null)
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ToolCallBubble(
    message: ChatMessage,
    onDelete: () -> Unit,
    onOpenTool: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val (showMenu, setShowMenu) = useState(false)
    val actionModifier = Modifier.combinedClickable(
        onClick = {},
        onLongClick = { setShowMenu(true) }
    )

    Box(modifier = modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxWidth(0.82f)
                .then(actionModifier)
                .testTag("tool_message"),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            ToolCallPanel(
                toolCalls = message.toolCalls,
                onOpenTool = onOpenTool,
                onMenuClick = { setShowMenu(true) },
                modifier = Modifier.fillMaxWidth()
            )
        }
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { setShowMenu(false) }
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.delete)) },
                onClick = {
                    setShowMenu(false)
                    onDelete()
                },
                leadingIcon = {
                    Icon(painterResource(R.drawable.ic_delete), contentDescription = null)
                }
            )
        }
    }
}

@Composable
private fun ToolCallPanel(
    toolCalls: List<AiToolCallCard>,
    onOpenTool: (Int) -> Unit,
    onMenuClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val (expanded, setExpanded) = useState(true)
    val skillName = toolCalls.firstOrNull()?.let { it.skillName ?: formatSkillId(it.skillId) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp)
            .testTag("tool_call_card")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.ai_tool_calls),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                if (skillName != null) {
                    Text(
                        text = skillName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { setExpanded(!expanded) }) {
                    Text(stringResource(if (expanded) R.string.hide else R.string.show))
                }
                if (onMenuClick != null) {
                    IconButton(
                        onClick = onMenuClick,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("message_menu_button")
                    ) {
                        Icon(
                            painterResource(R.drawable.ic_menu_dots),
                            contentDescription = null
                        )
                    }
                }
            }
        }

        AnimatedVisibility(visible = expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                toolCalls.forEach { call ->
                    ToolCallRow(call, onOpenTool)
                }
            }
        }
    }
}

@Composable
private fun ToolCallRow(
    call: AiToolCallCard,
    onOpenTool: (Int) -> Unit
) {
    val context = LocalContext.current
    val statusText = when (call.status) {
        AiToolRunStatus.Running -> R.string.ai_tool_status_running
        AiToolRunStatus.Succeeded -> R.string.ai_tool_status_succeeded
        AiToolRunStatus.Unavailable -> R.string.ai_tool_status_unavailable
        AiToolRunStatus.Failed -> R.string.ai_tool_status_failed
    }
    val statusColor = when (call.status) {
        AiToolRunStatus.Succeeded -> MaterialTheme.colorScheme.primary
        AiToolRunStatus.Running -> MaterialTheme.colorScheme.onSurfaceVariant
        AiToolRunStatus.Unavailable -> MaterialTheme.colorScheme.tertiary
        AiToolRunStatus.Failed -> MaterialTheme.colorScheme.error
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = call.toolName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = stringResource(statusText),
                style = MaterialTheme.typography.labelSmall,
                color = statusColor
            )
        }
        Text(
            text = call.summary,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp)
        )
        if (!call.details.isNullOrBlank()) {
            Text(
                text = call.details,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        if (call.openedNavAction != null && Tools.isToolAvailable(context, call.toolId)) {
            OutlinedButton(
                onClick = { onOpenTool(call.openedNavAction) },
                modifier = Modifier.padding(top = 6.dp)
            ) {
                Text(stringResource(R.string.ai_open_tool))
            }
        }
    }
}

private fun formatSkillId(skillId: String): String {
    return skillId.split('_')
        .filter { it.isNotBlank() }
        .joinToString(" ") {
            it.replaceFirstChar { char -> char.uppercase(Locale.getDefault()) }
        }
}

private fun markdownToAnnotatedString(text: String): AnnotatedString {
    return buildAnnotatedString {
        var currentIndex = 0
        var boldStart = text.indexOf("**")
        while (boldStart >= 0) {
            val boldEnd = text.indexOf("**", startIndex = boldStart + 2)
            if (boldEnd < 0) {
                break
            }

            append(text.substring(currentIndex, boldStart))
            val contentStart = length
            append(text.substring(boldStart + 2, boldEnd))
            addStyle(
                SpanStyle(fontWeight = FontWeight.Bold),
                contentStart,
                length
            )
            currentIndex = boldEnd + 2
            boldStart = text.indexOf("**", startIndex = currentIndex)
        }
        append(text.substring(currentIndex))
    }
}

private fun markdownToPlainText(text: String): String {
    return markdownToAnnotatedString(text).text
}

@Composable
private fun ChatHistoryScreen(
    sessions: List<ChatSessionEntity>,
    onSessionClick: (ChatSessionEntity) -> Unit,
    onDeleteSession: (ChatSessionEntity) -> Unit,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(painterResource(R.drawable.ic_cancel), contentDescription = null)
            }
            Text(
                text = stringResource(R.string.ai_chat_history),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f).padding(start = 8.dp)
            )
        }

        if (sessions.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.ai_no_chat_history),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(sessions) { session ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onSessionClick(session) }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = session.title ?: stringResource(R.string.ai_new_chat),
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1
                                )
                                Text(
                                    text = formatTimestamp(session.updatedOn),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { onDeleteSession(session) }) {
                                Icon(
                                    painterResource(R.drawable.ic_delete),
                                    contentDescription = stringResource(R.string.ai_delete_chat)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatTimestamp(millis: Long): String {
    val formatter = java.text.SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())
    return formatter.format(java.util.Date(millis))
}
