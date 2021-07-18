package com.kylecorry.trail_sense.tools.packs.domain.sort

import com.kylecorry.trail_sense.tools.packs.domain.PackItem

interface IPackItemSort {

    fun sort(items: List<PackItem>): List<PackItem>

}