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
            aiContext = null

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
                        val final_ = messages + userMessage + ChatMessage(
                            responseBuilder.toString(),
                            isUser = false
                        )
                        setMessages(final_)
                        setIsGenerating(false)
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
