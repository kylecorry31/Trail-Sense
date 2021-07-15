package com.kylecorry.trail_sense.tools.inventory.domain

import com.kylecorry.trailsensecore.domain.units.Weight

data class PackItem(
    val id: Long,
    val packId: Long,
    val name: String,
    val category: ItemCategory,
    val amount: Double = 0.0,
    val desiredAmount: Double = 0.0,
    val weight: Weight? = null
) {
    val packedWeight: Weight?
        get() {
            weight ?: return null
            return Weight(weight.weight * amount.toFloat(), weight.units)
        }

    val desiredWeight: Weight?
        get() {
            weight ?: return null
            return Weight(weight.weight * desiredAmount.toFloat(), weight.units)
        }

    val percentPacked: Float
        get() {
            return when {
                amount == 0.0 -> 0f
                desiredAmount == 0.0 || amount == desiredAmount -> 100f
                else -> 100 * (amount / desiredAmount).toFloat()
            }
        }

    val isFullyPacked: Boolean
        get() = percentPacked >= 100f
}
