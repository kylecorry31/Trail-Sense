package com.kylecorry.trail_sense.tools.packs.domain

import com.kylecorry.sol.units.Weight
import com.kylecorry.sol.units.WeightUnits

class PackService {

    fun getPackWeight(items: List<PackItem>, units: WeightUnits): Weight? {
        val totalWeight =
            items.mapNotNull { it.packedWeight }.reduceOrNull { weight, acc -> acc + weight }
        return totalWeight?.convertTo(units)
    }

    fun getPercentPacked(items: List<PackItem>): Float {
        val requiredItems = items.filter { !it.isOptional }
        if (requiredItems.isEmpty()) {
            return 100f
        }

        val sum = requiredItems.sumOf { it.percentPacked.toDouble().coerceAtMost(100.0) }
        return (sum / requiredItems.size).toFloat()
    }

    fun isFullyPacked(items: List<PackItem>): Boolean {
        return items.filter { !it.isOptional }.all { it.isFullyPacked }
    }

}