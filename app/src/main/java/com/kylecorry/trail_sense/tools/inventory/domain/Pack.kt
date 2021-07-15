package com.kylecorry.trail_sense.tools.inventory.domain

import com.kylecorry.trailsensecore.domain.units.Weight
import com.kylecorry.trailsensecore.domain.units.WeightUnits

data class Pack(val id: Long, val name: String) {
    fun getPackWeight(items: List<PackItem>, weightUnits: WeightUnits): Weight {
        val total = items.filter { it.packId == id }.mapNotNull {
            if (it.weight == null) {
                null
            } else {
                Weight(it.weight.weight * it.amount.toFloat(), it.weight.units).convertTo(
                    weightUnits
                ).weight
            }
        }.sum()
        return Weight(total, weightUnits)
    }
}