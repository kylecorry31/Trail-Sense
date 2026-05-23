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
