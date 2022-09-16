package com.kylecorry.trail_sense.weather.ui.clouds

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.list.ListView
import com.kylecorry.sol.science.meteorology.clouds.CloudGenus
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentCloudResultsBinding
import com.kylecorry.trail_sense.databinding.ListItemCloudBinding
import com.kylecorry.trail_sense.shared.ClassificationResult
import com.kylecorry.trail_sense.shared.debugging.DebugCloudCommand
import com.kylecorry.trail_sense.shared.extensions.inBackground
import com.kylecorry.trail_sense.shared.extensions.onDefault
import com.kylecorry.trail_sense.shared.extensions.onMain
import com.kylecorry.trail_sense.weather.domain.clouds.classification.ICloudClassifier
import com.kylecorry.trail_sense.weather.domain.clouds.classification.TextureCloudClassifier
import com.kylecorry.trail_sense.weather.infrastructure.clouds.CloudRepo

class CloudResultsFragment : BoundFragment<FragmentCloudResultsBinding>() {

    private val cloudRepo by lazy { CloudRepo(requireContext()) }
    private lateinit var listView: ListView<ClassificationResult<CloudGenus>>

    private var result: List<ClassificationResult<CloudGenus>> = emptyList()
    private var image: Bitmap? = null
    private var classifier: ICloudClassifier = TextureCloudClassifier(this::debugLogFeatures)

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentCloudResultsBinding {
        return FragmentCloudResultsBinding.inflate(layoutInflater, container, false)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        listView = ListView(binding.cloudList, R.layout.list_item_cloud) { itemView, item ->
            val itemBinding = ListItemCloudBinding.bind(itemView)
            CloudListItem(item.value, cloudRepo, item.confidence).display(itemBinding)
        }

        listView.addLineSeparator()

        setResult(result)
    }

    override fun onResume() {
        super.onResume()
        analyze()
    }

    fun setImage(image: Bitmap) {
        this.image = image
    }

    private fun debugLogFeatures(features: List<Float>) {
        DebugCloudCommand(requireContext(), features).execute()
    }

    private fun analyze() {
        if (result.isEmpty()) {
            binding.emptyText.isVisible = false
            binding.loadingIndicator.isVisible = true
        }
        inBackground {
            val results = onDefault {
                image?.let { classifier.classify(it) }
            }

            onMain {
                results?.let { setResult(results) }
            }

        }
    }

    private fun setResult(result: List<ClassificationResult<CloudGenus>>) {
        this.result = result
        if (!isBound) {
            return
        }

        binding.loadingIndicator.isVisible = false
        binding.emptyText.isVisible = result.isEmpty()
        listView.setData(result)
        listView.scrollToPosition(0, false)
    }
}