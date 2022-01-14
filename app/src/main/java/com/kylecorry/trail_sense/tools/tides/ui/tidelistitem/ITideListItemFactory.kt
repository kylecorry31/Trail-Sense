package com.kylecorry.trail_sense.tools.tides.ui.tidelistitem

import com.kylecorry.sol.science.oceanography.Tide

interface ITideListItemFactory {
    fun create(tide: Tide): TideListItem
}