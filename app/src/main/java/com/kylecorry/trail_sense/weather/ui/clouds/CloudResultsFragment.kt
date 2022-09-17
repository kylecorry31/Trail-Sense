package com.kylecorry.trail_sense.weather.ui.clouds

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.sol.science.meteorology.clouds.CloudGenus
import com.kylecorry.trail_sense.databinding.FragmentCloudResultsBinding
import com.kylecorry.trail_sense.shared.ClassificationResult
import com.kylecorry.trail_sense.shared.debugging.DebugCloudCommand
import com.kylecorry.trail_sense.shared.extensions.inBackground
import com.kylecorry.trail_sense.shared.extensions.onDefault
import com.kylecorry.trail_sense.shared.extensions.onMain
import com.kylecorry.trail_sense.weather.domain.clouds.classification.ICloudClassifier
import com.kylecorry.trail_sense.weather.domain.clouds.classification.TextureCloudClassifier

class CloudResultsFragment : BoundFragment<FragmentCloudResultsBinding>() {

    private var image: Bitmap? = null
    private var classifier: ICloudClassifier = TextureCloudClassifier(this::debugLogFeatures)
    private var selection: List<CloudSelection> = emptyList()
    private val mapper by lazy {
        CloudSelectionListItemMapper(requireContext()) { genus, selected ->
            selection = selection.map {
                if (genus == it.genus) {
                    it.copy(isSelected = selected)
                } else {
                    it
                }
            }
            updateItems()
        }
    }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentCloudResultsBinding {
        return FragmentCloudResultsBinding.inflate(layoutInflater, container, false)
    }

    override fun onResume() {
        super.onResume()
        if (selection.isEmpty()) {
            analyze()
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
        if (isBound) {
            binding.cloudImage.setImageBitmap(image)
        }
    }

    private fun debugLogFeatures(features: List<Float>) {
        DebugCloudCommand(requireContext(), features).execute()
    }

    private fun analyze() {
        binding.cloudImage.setImageBitmap(image)
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
        updateItems()
        binding.cloudList.scrollToPosition(0, false)
    }
}