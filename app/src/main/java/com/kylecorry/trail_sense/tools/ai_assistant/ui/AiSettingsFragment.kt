package com.kylecorry.trail_sense.tools.ai_assistant.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.extensions.TrailSenseComposeFragment
import com.kylecorry.trail_sense.shared.extensions.compose.useState
import com.kylecorry.trail_sense.tools.ai_assistant.infrastructure.AiModel
import com.kylecorry.trail_sense.tools.ai_assistant.infrastructure.ModelManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class AiSettingsFragment : TrailSenseComposeFragment() {

    @Composable
    override fun FragmentContent() {
        val modelManager = remember { ModelManager(requireContext()) }
        val models = remember { modelManager.models }
        val (selectedModelId, setSelectedModelId) = useState(modelManager.selectedModel.id)
        val (downloadedModelIds, setDownloadedModelIds) = useState(
            getDownloadedModelIds(modelManager)
        )
        val (downloadingModelId, setDownloadingModelId) = useState<String?>(null)
        val (progressByModel, setProgressByModel) = useState(getDownloadProgress(modelManager))
        val (errorsByModel, setErrorsByModel) = useState<Map<String, String>>(emptyMap())
        val scope = rememberCoroutineScope()

        AiSettingsContent(
            models = models,
            selectedModelId = selectedModelId,
            downloadedModelIds = downloadedModelIds,
            downloadingModelId = downloadingModelId,
            progressByModel = progressByModel,
            errorsByModel = errorsByModel,
            onSelect = { model ->
                modelManager.selectedModel = model
                setSelectedModelId(model.id)
            },
            onDownload = { model ->
                scope.launch {
                    setDownloadingModelId(model.id)
                    setErrorsByModel(errorsByModel - model.id)
                    try {
                        withContext(Dispatchers.IO) {
                            modelManager.downloadModel(model) { progress ->
                                scope.launch {
                                    setProgressByModel(progressByModel + (model.id to progress))
                                }
                            }
                        }
                        setDownloadedModelIds(getDownloadedModelIds(modelManager))
                        setProgressByModel(getDownloadProgress(modelManager))
                    } catch (e: Exception) {
                        setErrorsByModel(
                            errorsByModel + (model.id to (e.message
                                ?: getString(R.string.ai_inference_error)))
                        )
                    }
                    setDownloadingModelId(null)
                }
            },
            onDelete = { model ->
                modelManager.deleteModel(model)
                val downloadedIds = getDownloadedModelIds(modelManager)
                setDownloadedModelIds(downloadedIds)
                setProgressByModel(getDownloadProgress(modelManager))
                if (selectedModelId == model.id) {
                    val nextModel = models.firstOrNull { it.id in downloadedIds }
                        ?: ModelManager.DEFAULT_MODEL
                    modelManager.selectedModel = nextModel
                    setSelectedModelId(nextModel.id)
                }
            }
        )
    }

    private fun getDownloadedModelIds(modelManager: ModelManager): Set<String> {
        return modelManager.models.filter { modelManager.isModelDownloaded(it) }
            .map { it.id }
            .toSet()
    }

    private fun getDownloadProgress(modelManager: ModelManager): Map<String, Float> {
        return modelManager.models.associate { it.id to modelManager.getDownloadProgress(it) }
    }
}

@Composable
private fun AiSettingsContent(
    models: List<AiModel>,
    selectedModelId: String,
    downloadedModelIds: Set<String>,
    downloadingModelId: String?,
    progressByModel: Map<String, Float>,
    errorsByModel: Map<String, String>,
    onSelect: (AiModel) -> Unit,
    onDownload: (AiModel) -> Unit,
    onDelete: (AiModel) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = stringResource(R.string.ai_settings_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.testTag("ai_settings_title")
            )
        }

        item {
            Text(
                text = stringResource(R.string.ai_recommended_models),
                style = MaterialTheme.typography.titleMedium
            )
        }

        items(models) { model ->
            val isDownloaded = model.id in downloadedModelIds
            val isSelected = model.id == selectedModelId
            val isDownloading = model.id == downloadingModelId
            val progress = progressByModel[model.id] ?: 0f
            val hasPartialDownload = !isDownloaded && !isDownloading && progress > 0f

            AiModelCard(
                model = model,
                isSelected = isSelected,
                isDownloaded = isDownloaded,
                isDownloading = isDownloading,
                progress = progress,
                hasPartialDownload = hasPartialDownload,
                downloadEnabled = downloadingModelId == null || isDownloading,
                error = errorsByModel[model.id],
                onSelect = { onSelect(model) },
                onDownload = { onDownload(model) },
                onDelete = { onDelete(model) }
            )
        }

        item {
            Text(
                text = stringResource(R.string.ai_about),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            Text(
                text = stringResource(R.string.ai_disclaimer),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AiModelCard(
    model: AiModel,
    isSelected: Boolean,
    isDownloaded: Boolean,
    isDownloading: Boolean,
    progress: Float,
    hasPartialDownload: Boolean,
    downloadEnabled: Boolean,
    error: String?,
    onSelect: () -> Unit,
    onDownload: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                RadioButton(
                    selected = isSelected,
                    onClick = if (isDownloaded) onSelect else null,
                    enabled = isDownloaded,
                    modifier = Modifier.testTag("model_selector")
                )
                Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = model.displayName,
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (isSelected) {
                            Text(
                                text = stringResource(R.string.ai_selected_model),
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                    Text(
                        text = formatModelSize(model.sizeBytes),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

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
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!isSelected) {
                        Button(
                            onClick = onSelect,
                            modifier = Modifier.testTag("select_button")
                        ) {
                            Text(stringResource(R.string.ai_select_model))
                        }
                    }
                    OutlinedButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.testTag("delete_button")
                    ) {
                        Text(stringResource(R.string.ai_delete_model))
                    }
                }
            } else if (hasPartialDownload) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onDownload,
                    enabled = downloadEnabled,
                    modifier = Modifier.fillMaxWidth().testTag("download_button")
                ) {
                    Text(stringResource(R.string.ai_resume_download))
                }
            } else {
                Button(
                    onClick = onDownload,
                    enabled = downloadEnabled,
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
}

private fun formatModelSize(sizeBytes: Long): String {
    val gb = sizeBytes.toDouble() / 1_000_000_000.0
    return String.format(Locale.getDefault(), "%.1f GB", gb)
}
