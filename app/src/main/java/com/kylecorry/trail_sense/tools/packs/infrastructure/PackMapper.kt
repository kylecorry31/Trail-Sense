package com.kylecorry.trail_sense.tools.packs.infrastructure

import com.kylecorry.trailsensecore.domain.packs.Pack
import com.kylecorry.trailsensecore.domain.packs.PackItem
import com.kylecorry.trailsensecore.domain.units.Weight

class PackMapper {

    fun mapToPackItem(item: PackItemEntity): PackItem {
        val weight = if (item.weight != null && item.weightUnits != null) {
            Weight(item.weight, item.weightUnits)
        } else {
            null
        }
        return PackItem(
            item.id,
            item.packId,
            item.name,
            item.category,
            item.amount,
            item.desiredAmount,
            weight
        )
    }

    fun mapToItemEntity(item: PackItem): PackItemEntity {
        return PackItemEntity(
            item.name,
            item.packId,
            item.category,
            item.amount,
            item.desiredAmount,
            item.weight?.weight,
            item.weight?.units
        ).also {
            it.id = item.id
        }
    }

    fun mapToPack(pack: PackEntity): Pack {
        return Pack(pack.id, pack.name)
    }

    fun mapToPackEntity(pack: Pack): PackEntity {
        return PackEntity(pack.name).also {
            it.id = pack.id
        }
    }

}