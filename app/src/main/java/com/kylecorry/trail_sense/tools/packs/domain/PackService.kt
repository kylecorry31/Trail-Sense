package com.kylecorry.trail_sense.tools.packs.domain

import com.kylecorry.andromeda.core.units.Weight
import com.kylecorry.andromeda.core.units.WeightUnits

class PackService {

    fun getPackWeight(items: List<PackItem>, units: WeightUnits): Weight? {
        val totalWeight = items.mapNotNull { it.packedWeight }.reduceOrNull { weight, acc -> acc + weight }
        return totalWeight?.convertTo(units)
    }

    fun getPercentPacked(items: List<PackItem>): Float {
        if (items.isEmpty()) {
            return 100f
        }

        val sum = items.sumOf { it.percentPacked.toDouble().coerceAtMost(100.0) }
        return (sum / items.size).toFloat()
    }

    fun isFullyPacked(items: List<PackItem>): Boolean {
        return items.all { it.isFullyPacked }
    }

}