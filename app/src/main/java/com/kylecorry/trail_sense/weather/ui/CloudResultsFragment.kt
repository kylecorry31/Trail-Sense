package com.kylecorry.trail_sense.weather.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.list.ListView
import com.kylecorry.sol.science.meteorology.clouds.CloudGenus
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentCloudResultsBinding
import com.kylecorry.trail_sense.databinding.ListItemCloudBinding
import com.kylecorry.trail_sense.weather.infrastructure.clouds.CloudObservation
import com.kylecorry.trail_sense.weather.infrastructure.clouds.CloudRepo

class CloudResultsFragment : BoundFragment<FragmentCloudResultsBinding>() {

    private val cloudRepo by lazy { CloudRepo(requireContext()) }
    private lateinit var listView: ListView<Pair<CloudGenus, Float>>

    private var observation: CloudObservation? = null


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
            CloudListItem(item.first, cloudRepo, item.second).display(itemBinding)
        }

        listView.addLineSeparator()

        observation?.let {
            setObservation(it)
        }
    }

    fun setObservation(observation: CloudObservation) {
        this.observation = observation
        if (!isBound) {
            return
        }

        listView.setData(observation.possibleClouds)
        listView.scrollToPosition(0, false)
    }
}