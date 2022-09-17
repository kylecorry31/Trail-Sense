package com.kylecorry.trail_sense.weather.ui.clouds

import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.sol.science.meteorology.clouds.CloudGenus
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.databinding.FragmentCloudResultsBinding
import com.kylecorry.trail_sense.shared.ClassificationResult
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.debugging.DebugCloudCommand
import com.kylecorry.trail_sense.shared.extensions.inBackground
import com.kylecorry.trail_sense.shared.extensions.onDefault
import com.kylecorry.trail_sense.shared.extensions.onMain
import com.kylecorry.trail_sense.weather.domain.clouds.classification.ICloudClassifier
import com.kylecorry.trail_sense.weather.domain.clouds.classification.TextureCloudClassifier
import com.kylecorry.trail_sense.weather.infrastructure.persistence.CloudObservation
import com.kylecorry.trail_sense.weather.infrastructure.persistence.CloudRepo
import java.time.Instant

class CloudResultsFragment : BoundFragment<FragmentCloudResultsBinding>() {

    private var image: Bitmap? = null
    private var classifier: ICloudClassifier = TextureCloudClassifier(this::debugLogFeatures)
    private var selection: List<CloudSelection> = emptyList()
    private val repo by lazy { CloudRepo.getInstance(requireContext()) }
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

    // TODO: Read URI from arguments, if present analyze else show all clouds

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentCloudResultsBinding {
        return FragmentCloudResultsBinding.inflate(layoutInflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.cloudImage.clipToOutline = true
        binding.cloudTitle.rightButton.setOnClickListener {
            save()
        }
    }

    override fun onResume() {
        super.onResume()
        if (selection.isEmpty()) {
            analyze()
        }
    }

    private fun save() {
        inBackground {
            val now = Instant.now()
            val readings =
                selection.filter { it.isSelected }
                    .map { Reading(CloudObservation(0, it.genus), now) }
            readings.forEach {
                repo.add(it)
            }
            onMain {
                findNavController().navigateUp()
            }
        }
    }

    fun clearImage() {
        this.image = null
        if (isBound) {
            binding.cloudImage.setImageBitmap(null)
        }
    }

    fun setImage(image: Bitmap) {
        this.image = image
        selection = emptyList()
        if (isBound) {
            binding.cloudImage.setImageBitmap(image)
        }
    }

    private fun debugLogFeatures(features: List<Float>) {
        DebugCloudCommand(requireContext(), features).execute()
    }

    private fun analyze() {
        binding.cloudImage.setImageBitmap(image)
        binding.cloudImage.isVisible = true
        binding.loadingIndicator.isVisible = true
        binding.cloudList.setItems(emptyList())
        inBackground {
            val results = onDefault {
                image?.let { classifier.classify(it) }
            }

            onMain {
                results?.let { setResult(results) }
            }

        }
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
        selection = result.mapIndexed { index, value ->
            CloudSelection(value.value, value.confidence, index == 0)
        }
        CustomUiUtils.setButtonState(
            binding.cloudTitle.rightButton,
            selection.any { it.isSelected })
        updateItems()
        binding.cloudList.scrollToPosition(0, false)
    }
}