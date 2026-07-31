package com.kylecorry.trail_sense.tools.species.ui

import android.os.SystemClock
import android.util.Size
import android.view.View
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.kylecorry.andromeda.core.ui.useService
import com.kylecorry.andromeda.fragments.useBackgroundMemo
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.extensions.TrailSenseReactiveFragment
import com.kylecorry.trail_sense.shared.extensions.usePauseEffect
import com.kylecorry.trail_sense.shared.extensions.useResumeEffect
import com.kylecorry.trail_sense.shared.permissions.alertNoCameraPermission
import com.kylecorry.trail_sense.shared.permissions.requestCamera
import com.kylecorry.trail_sense.shared.views.CameraView
import com.kylecorry.trail_sense.tools.species.domain.SpeciesClassifier
import com.kylecorry.trail_sense.tools.species.domain.SpeciesPrediction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

class SpeciesIdentificationFragment : TrailSenseReactiveFragment(
    R.layout.fragment_species_identification
) {

    private var activeClassifier: SpeciesClassifier? = null

    override fun update() {
        val context = useAndroidContext()
        val cameraView = useView<CameraView>(R.id.camera)
        val loadingView = useView<View>(R.id.species_loading)
        val statusView = useView<TextView>(R.id.species_status)
        val nameView = useView<TextView>(R.id.species_name)
        val confidenceView = useView<TextView>(R.id.species_confidence)
        val alternativesView = useView<TextView>(R.id.species_alternatives)
        val formatter = useService<FormatService>()

        val (isCameraEnabled, setIsCameraEnabled) = useState(false)
        val (predictions, setPredictions) = useState(emptyList<SpeciesPrediction>())
        val isClassifying = useMemo { AtomicBoolean(false) }
        val lastClassification = useMemo { AtomicLong(0) }
        val classifierResult = useBackgroundMemo(context.applicationContext) {
            runCatching { SpeciesClassifier(context.applicationContext) }
        }
        val classifier = classifierResult?.getOrNull()
        activeClassifier = classifier

        useResumeEffect {
            requestCamera {
                setIsCameraEnabled(it)
                if (!it) {
                    alertNoCameraPermission()
                }
            }
        }

        useEffect(isCameraEnabled, resetOnResume, classifier) {
            if (isCameraEnabled && classifier != null) {
                cameraView.setShowTorch(true)
                cameraView.start(
                    resolution = Size(SpeciesClassifier.INPUT_SIZE, SpeciesClassifier.INPUT_SIZE),
                    readFrames = true,
                    preferBackCamera = true,
                    shouldStabilizePreview = false,
                    minimumFrameInterval = Duration.ofMillis(CLASSIFICATION_INTERVAL_MS)
                ) { bitmap ->
                    classifyFrame(
                        classifier,
                        bitmap,
                        isClassifying,
                        lastClassification,
                        setPredictions
                    )
                }
            } else {
                cameraView.stop()
            }
        }

        usePauseEffect(cameraView) {
            cameraView.stop()
        }

        useEffect(
            classifierResult,
            predictions,
            loadingView,
            statusView,
            nameView,
            confidenceView,
            alternativesView
        ) {
            val isLoading = classifierResult == null
            val error = classifierResult?.exceptionOrNull()
            loadingView.isVisible = isLoading
            statusView.isVisible = predictions.isEmpty()
            statusView.text = when {
                isLoading -> getString(R.string.species_model_loading)
                error != null -> getString(R.string.species_model_error)
                else -> getString(R.string.species_camera_hint)
            }

            val best = predictions.firstOrNull()
            nameView.isVisible = best != null
            confidenceView.isVisible = best != null
            alternativesView.isVisible = predictions.size > 1
            nameView.text = best?.taxon?.name.orEmpty()
            confidenceView.text = best?.let {
                getString(
                    R.string.species_best_match_confidence,
                    formatter.formatPercentage(it.confidence * 100)
                )
            }.orEmpty()
            alternativesView.text = predictions.drop(1).joinToString(separator = "\n") {
                getString(
                    R.string.species_alternative_match,
                    it.taxon.name,
                    formatter.formatPercentage(it.confidence * 100)
                )
            }
        }
    }

    override fun onDestroy() {
        activeClassifier?.close()
        activeClassifier = null
        super.onDestroy()
    }

    private fun classifyFrame(
        classifier: SpeciesClassifier,
        bitmap: android.graphics.Bitmap,
        isClassifying: AtomicBoolean,
        lastClassification: AtomicLong,
        setPredictions: (List<SpeciesPrediction>) -> Unit
    ) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastClassification.get() < CLASSIFICATION_INTERVAL_MS ||
            !isClassifying.compareAndSet(false, true)
        ) {
            return
        }
        lastClassification.set(now)
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Default) {
            try {
                val predictions = runCatching { classifier.classify(bitmap) }.getOrNull()
                if (predictions != null) {
                    withContext(Dispatchers.Main) {
                        setPredictions(predictions)
                    }
                }
            } finally {
                isClassifying.set(false)
            }
        }
    }

    companion object {
        private const val CLASSIFICATION_INTERVAL_MS = 750L
    }
}
