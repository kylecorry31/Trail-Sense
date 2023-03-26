package com.kylecorry.trail_sense.weather.ui.clouds

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toFile
import androidx.core.view.isVisible
import androidx.exifinterface.media.ExifInterface
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.core.bitmap.BitmapUtils
import com.kylecorry.andromeda.core.bitmap.BitmapUtils.resizeExact
import com.kylecorry.andromeda.core.bitmap.BitmapUtils.rotate
import com.kylecorry.andromeda.core.tryOrDefault
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.pickers.CoroutinePickers
import com.kylecorry.sol.science.meteorology.clouds.CloudGenus
import com.kylecorry.sol.time.Time.toZonedDateTime
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.databinding.FragmentCloudResultsBinding
import com.kylecorry.trail_sense.shared.ClassificationResult
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.debugging.DebugCloudCommand
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.trail_sense.shared.extensions.onDefault
import com.kylecorry.trail_sense.shared.extensions.onIO
import com.kylecorry.trail_sense.shared.extensions.onMain
import com.kylecorry.trail_sense.shared.io.DeleteTempFilesCommand
import com.kylecorry.trail_sense.weather.domain.clouds.classification.ICloudClassifier
import com.kylecorry.trail_sense.weather.domain.clouds.classification.SoftmaxCloudClassifier
import com.kylecorry.trail_sense.weather.infrastructure.persistence.CloudObservation
import com.kylecorry.trail_sense.weather.infrastructure.persistence.CloudRepo
import java.time.Instant
import kotlin.math.abs

class CloudResultsFragment : BoundFragment<FragmentCloudResultsBinding>() {

    private var image: Bitmap? = null
    private var classifier: ICloudClassifier = SoftmaxCloudClassifier(this::debugLogFeatures)
    private var selection: List<CloudSelection> = emptyList()
    private val repo by lazy { CloudRepo.getInstance(requireContext()) }
    private var time = Instant.now()
    private val formatter by lazy { FormatService.getInstance(requireContext()) }
    private val mapper by lazy {
        CloudSelectionListItemMapper(requireContext()) { genus, selected ->
            selection = selection.map {
                if (genus == it.genus) {
                    it.copy(isSelected = selected)
                } else {
                    it
                }
            }
            CustomUiUtils.setButtonState(
                binding.cloudTitle.rightButton,
                selection.any { it.isSelected })
            updateItems()
        }
    }

    private var uri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        uri = arguments?.getParcelable("image")
    }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentCloudResultsBinding {
        return FragmentCloudResultsBinding.inflate(layoutInflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        time = Instant.now()
        binding.cloudImage.clipToOutline = true
        binding.cloudTitle.subtitle.text =
            formatter.formatDateTime(
                time.toZonedDateTime(),
                relative = true,
                abbreviateMonth = true
            )
        binding.cloudTitle.subtitle.setOnClickListener {
            runInBackground {
                val current = time.toZonedDateTime().toLocalDateTime()
                val newTime = CoroutinePickers.datetime(
                    requireContext(),
                    UserPreferences(requireContext()).use24HourTime,
                    current
                )
                if (newTime != null) {
                    time = newTime.toZonedDateTime().toInstant()
                    onMain {
                        if (isBound) {
                            binding.cloudTitle.subtitle.text =
                                formatter.formatDateTime(
                                    time.toZonedDateTime(),
                                    relative = true,
                                    abbreviateMonth = true
                                )
                        }
                    }
                }
            }
        }
        binding.cloudTitle.rightButton.setOnClickListener {
            save()
        }
    }

    override fun onResume() {
        super.onResume()
        if (uri != null && selection.isEmpty()) {
            analyze()
        } else {
            selection = CloudGenus.values().map { CloudSelection(it, null, false) } + listOf(
                CloudSelection(
                    null,
                    null,
                    false
                )
            )
            updateItems()
        }
    }

    private fun save() {
        inBackground {
            val readings =
                selection.filter { it.isSelected }
                    .map { Reading(CloudObservation(0, it.genus), time) }
            readings.forEach {
                repo.add(it)
            }
            onMain {
                findNavController().navigateUp()
            }
        }
    }

    private fun debugLogFeatures(features: List<Float>) {
        DebugCloudCommand(requireContext(), features).execute()
    }

    private fun analyze() {
        val uri = uri ?: return
        binding.loadingIndicator.isVisible = true
        selection = emptyList()
        binding.cloudList.setItems(emptyList())
        inBackground {
            onIO {
                if (image == null) {
                    image = loadImage(uri)
                    DeleteTempFilesCommand(requireContext()).execute()
                }
            }
            val results = onDefault {
                image?.let { classifier.classify(it) }
            }
            onMain {
                binding.cloudImage.isVisible = true
                binding.cloudImage.setImageBitmap(image)
                results?.let { setResult(results) }
            }
        }
    }

    private suspend fun loadImage(uri: Uri): Bitmap? = onIO {
        val file = uri.toFile()
        val path = file.path
        val rotation = tryOrDefault(0) {
            val exif = ExifInterface(uri.toFile())
            exif.rotationDegrees
        }
        val full = BitmapUtils.decodeBitmapScaled(
            path,
            SoftmaxCloudClassifier.IMAGE_SIZE,
            SoftmaxCloudClassifier.IMAGE_SIZE
        ) ?: return@onIO null
        val bmp = full.resizeExact(
            SoftmaxCloudClassifier.IMAGE_SIZE,
            SoftmaxCloudClassifier.IMAGE_SIZE
        )
        full.recycle()
        val rotated = bmp.rotate(rotation.toFloat())
        bmp.recycle()
        rotated
    }

    override fun onDestroy() {
        super.onDestroy()
        image?.recycle()
    }

    private fun updateItems() {
        if (isBound) {
            binding.cloudList.setItems(selection, mapper)
        }
    }

    private fun setResult(result: List<ClassificationResult<CloudGenus?>>) {
        if (!isBound) {
            return
        }

        binding.loadingIndicator.isVisible = false
        val maxConfidence = result[0].confidence
        val threshold = if (result[0].value == null) 0.001f else 0.1f
        selection = result.map {
            CloudSelection(
                it.value,
                it.confidence,
                abs(it.confidence - maxConfidence) <= threshold
            )
        }
        CustomUiUtils.setButtonState(
            binding.cloudTitle.rightButton,
            selection.any { it.isSelected })
        updateItems()
        binding.cloudList.scrollToPosition(0, false)
    }
}