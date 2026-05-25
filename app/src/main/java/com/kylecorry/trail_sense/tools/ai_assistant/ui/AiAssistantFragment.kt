package com.kylecorry.trail_sense.tools.ai_assistant.ui

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.extensions.TrailSenseComposeFragment
import com.kylecorry.trail_sense.shared.extensions.compose.useState
import com.kylecorry.trail_sense.shared.sensors.LocationSubsystem
import com.kylecorry.trail_sense.tools.ai_assistant.domain.AiContext
import com.kylecorry.trail_sense.tools.ai_assistant.domain.AiPromptBuilder
import com.kylecorry.trail_sense.tools.ai_assistant.domain.AiToolKnowledgeService
import com.kylecorry.trail_sense.tools.ai_assistant.domain.CloudAiContextProvider
import com.kylecorry.trail_sense.tools.ai_assistant.domain.NavigationAiContextProvider
import com.kylecorry.trail_sense.tools.ai_assistant.domain.WeatherAiContextProvider
import com.kylecorry.trail_sense.tools.ai_assistant.infrastructure.AiInferenceSubsystem
import com.kylecorry.trail_sense.tools.clouds.infrastructure.CloudDetailsService
import com.kylecorry.trail_sense.tools.clouds.infrastructure.persistence.CloudRepo
import com.kylecorry.trail_sense.tools.navigation.domain.NavigationService
import com.kylecorry.trail_sense.tools.navigation.infrastructure.Navigator
import com.kylecorry.trail_sense.tools.ai_assistant.infrastructure.persistence.AiChatRepo
import com.kylecorry.trail_sense.tools.ai_assistant.infrastructure.persistence.ChatSessionEntity
import com.kylecorry.trail_sense.tools.weather.infrastructure.subsystem.WeatherSubsystem
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.MessageCallback
import kotlinx.coroutines.launch
import java.util.Locale

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val isLoading: Boolean = false
)

class AiAssistantFragment : TrailSenseComposeFragment() {

    private var aiContext: AiContext? = null

    private fun loadBitmapFromUri(uri: Uri): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(requireContext().contentResolver, uri))
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(requireContext().contentResolver, uri)
            }
        } catch (_: Exception) {
            null
        }
    }

    @Composable
    override fun FragmentContent() {
        val aiSubsystem = AiInferenceSubsystem.getInstance(requireContext())
        val toolKnowledgeService = remember {
            AiToolKnowledgeService(requireContext().applicationContext)
        }
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
        val listState = rememberLazyListState()
        val scope = rememberCoroutineScope()

        LaunchedEffect(Unit) {
            val toolId = arguments?.getString("tool_id")
            val imageUri = arguments?.getString("image_uri")?.let { Uri.parse(it) }
            val hasIncomingContext = toolId != null || imageUri != null
            val existingSessions = chatRepo.getAllSessions()
            setSessions(existingSessions)

            when (toolId) {
                "weather" -> {
                    try {
                        val weatherSubsystem = WeatherSubsystem.getInstance(requireContext())
                        val provider = WeatherAiContextProvider { weatherSubsystem.getWeather() }
                        aiContext = provider.getAiContext()
                        setSuggestedQuestions(provider.getSuggestedQuestions())
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
                    } catch (_: Exception) {}
                }
            }

            if (!hasIncomingContext) {
                val latestSession = existingSessions.firstOrNull()
                if (latestSession != null) {
                    val msgs = chatRepo.getMessages(latestSession.id).map {
                        ChatMessage(it.text, it.isUser)
                    }
                    setMessages(msgs)
                    setCurrentSessionId(latestSession.id)
                    setSuggestedQuestions(emptyList())
                }
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

            val toolKnowledge = toolKnowledgeService.getPromptContext(text, aiContext?.toolId)
            val chatHistory = buildChatHistory(messages)
            val prompt = AiPromptBuilder.buildUserPrompt(aiContext, text, toolKnowledge, chatHistory)
            val images = listOfNotNull(attachedImage ?: aiContext?.image)
            aiContext = null
            setAttachedImage(null)

            val responseBuilder = StringBuilder()
            aiSubsystem.sendMessage(
                input = prompt,
                images = images,
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
                        val final_ = messages + userMessage + ChatMessage(
                            responseBuilder.toString(),
                            isUser = false
                        )
                        setMessages(final_)
                        setIsGenerating(false)
                        scope.launch {
                            val sessionId = currentSessionId ?: chatRepo.createSession(
                                text.take(50)
                            ).also { setCurrentSessionId(it) }
                            chatRepo.addMessage(sessionId, text, isUser = true)
                            chatRepo.addMessage(sessionId, responseBuilder.toString(), isUser = false)
                            setSessions(chatRepo.getAllSessions())
                        }
                    }

                    override fun onError(throwable: Throwable) {
                        val errorMsg = if (throwable is java.util.concurrent.CancellationException) {
                            responseBuilder.toString()
                        } else {
                            getString(R.string.ai_inference_error)
                        }
                        val final_ = messages + userMessage + ChatMessage(errorMsg, isUser = false)
                        setMessages(final_)
                        setIsGenerating(false)
                        if (responseBuilder.isNotEmpty()) {
                            scope.launch {
                                val sessionId = currentSessionId ?: chatRepo.createSession(
                                    text.take(50)
                                ).also { setCurrentSessionId(it) }
                                chatRepo.addMessage(sessionId, text, isUser = true)
                                chatRepo.addMessage(sessionId, responseBuilder.toString(), isUser = false)
                                setSessions(chatRepo.getAllSessions())
                            }
                        } else {
                            scope.launch {
                                val sessionId = currentSessionId ?: chatRepo.createSession(
                                    text.take(50)
                                ).also { setCurrentSessionId(it) }
                                chatRepo.addMessage(sessionId, text, isUser = true)
                                chatRepo.addMessage(sessionId, errorMsg, isUser = false)
                                setSessions(chatRepo.getAllSessions())
                            }
                        }
                    }
                }
            )
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
                        val msgs = chatRepo.getMessages(session.id).map {
                            ChatMessage(it.text, it.isUser)
                        }
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
                            } else {
                                val msgs = chatRepo.getMessages(latestSession.id).map {
                                    ChatMessage(it.text, it.isUser)
                                }
                                setMessages(msgs)
                                setCurrentSessionId(latestSession.id)
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
                onRemoveImage = { setAttachedImage(null) },
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
                    setSuggestedQuestions(emptyList())
                    scope.launch {
                        aiSubsystem.createConversation(
                            systemInstruction = Contents.of(listOf(Content.Text(
                                AiPromptBuilder.buildSystemPrompt(
                                    resources.configuration.locales[0] ?: Locale.ENGLISH
                                )
                            )))
                        )
                    }
                },
                listState = listState,
                contextCard = aiContext?.summary
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
    attachedImage: Bitmap?,
    onInputChanged: (String) -> Unit,
    onSend: (String) -> Unit,
    onStop: () -> Unit,
    onCameraClick: () -> Unit,
    onRemoveImage: () -> Unit,
    onHistoryClick: () -> Unit,
    onNewChat: () -> Unit,
    listState: androidx.compose.foundation.lazy.LazyListState,
    contextCard: String?,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onHistoryClick, modifier = Modifier.testTag("history_button")) {
                Icon(painterResource(R.drawable.ic_tool_notes), contentDescription = stringResource(R.string.ai_chat_history))
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

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onCameraClick,
                enabled = !isGenerating && error == null,
                modifier = Modifier.testTag("camera_button")
            ) {
                Icon(painterResource(R.drawable.ic_camera), contentDescription = stringResource(R.string.ai_attach_photo))
            }
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
                    Icon(painterResource(R.drawable.ic_cancel), contentDescription = null)
                } else {
                    Icon(painterResource(R.drawable.ic_send), contentDescription = null)
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessage, modifier: Modifier = Modifier) {
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
