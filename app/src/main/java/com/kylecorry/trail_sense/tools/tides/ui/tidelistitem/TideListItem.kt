package com.kylecorry.trail_sense.tools.tides.ui.tidelistitem

import com.kylecorry.sol.science.oceanography.Tide
import com.kylecorry.trail_sense.databinding.ListItemTideBinding

class TideListItem(
    private val tide: Tide,
    private val formatTideType: (Tide) -> String,
    private val formatTime: (Tide) -> String,
    private val formatHeight: (Tide) -> String,
    private val onClick: (Tide) -> Unit = {}) {

    fun display(binding: ListItemTideBinding) {
        binding.tideType.text = formatTideType(tide)
        binding.tideTime.text = formatTime(tide)
        binding.tideHeight.text = formatHeight(tide)
        binding.root.setOnClickListener { onClick(tide) }
    }

}