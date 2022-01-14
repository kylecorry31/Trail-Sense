package com.kylecorry.trail_sense.weather.ui.clouds

import android.annotation.SuppressLint
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
import com.kylecorry.trail_sense.weather.domain.clouds.ClassificationResult
import com.kylecorry.trail_sense.weather.infrastructure.clouds.CloudRepo

class CloudResultsFragment : BoundFragment<FragmentCloudResultsBinding>() {

    private val cloudRepo by lazy { CloudRepo(requireContext()) }
    private lateinit var listView: ListView<ClassificationResult<CloudGenus>>

    private var result: List<ClassificationResult<CloudGenus>> = emptyList()


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

    fun setResult(result: List<ClassificationResult<CloudGenus>>) {
        this.result = result
        if (!isBound) {
            return
        }

        binding.emptyText.isVisible = result.isEmpty()
        listView.setData(result)
        listView.scrollToPosition(0, false)
    }
}