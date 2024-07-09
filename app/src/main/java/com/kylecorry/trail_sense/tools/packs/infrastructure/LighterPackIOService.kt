package com.kylecorry.trail_sense.tools.packs.infrastructure

import com.kylecorry.andromeda.fragments.AndromedaFragment
import com.kylecorry.luna.text.toDoubleCompat
import com.kylecorry.sol.units.Weight
import com.kylecorry.sol.units.WeightUnits
import com.kylecorry.trail_sense.shared.io.CsvIOService
import com.kylecorry.trail_sense.shared.io.ExternalUriService
import com.kylecorry.trail_sense.shared.io.FragmentUriPicker
import com.kylecorry.trail_sense.shared.io.IOService
import com.kylecorry.trail_sense.shared.io.UriPicker
import com.kylecorry.trail_sense.shared.io.UriService
import com.kylecorry.trail_sense.tools.packs.domain.ItemCategory
import com.kylecorry.trail_sense.tools.packs.domain.PackItem
import kotlin.math.max

/**
 * Imports/exports pack items to/from the LighterPack CSV format
 */
class LighterPackIOService(uriPicker: UriPicker, uriService: UriService) :
    IOService<List<PackItem>> {

    private val csvService = CsvIOService(uriPicker, uriService)

    override suspend fun export(data: List<PackItem>, filename: String): Boolean {
        val csv = toCsv(data)
        return csvService.export(csv, filename)
    }

    override suspend fun import(): List<PackItem>? {
        val data = csvService.import() ?: return null
        return fromCsv(data)
    }

    private fun toCsv(pack: List<PackItem>): List<List<String>> {
        val headers = listOf(
            "item name",
            "category",
            "desc",
            "qty",
            "weight",
            "unit",
            "packed qty",
            "desired qty"
        )
        val items = pack.map {
            listOf(
                it.name,
                "${it.category.id} - ${it.category.name}",
                "", // No description, required for LighterPack
                max(it.amount, it.desiredAmount).toString(),
                (it.weight?.weight ?: 0f).toString(),
                formatWeightUnit(it.weight?.units ?: WeightUnits.Grams),
                it.amount.toString(),
                it.desiredAmount.toString()
            )
        }
        return listOf(headers) + items
    }

    private fun fromCsv(data: List<List<String>>): List<PackItem>? {
        if (data.isEmpty()) {
            return null
        }

        return data.drop(1).map { it ->
            val name = it.getOrNull(0) ?: ""
            val category = parseCategoryString(it.getOrNull(1) ?: "")
            val weight = it.getOrNull(4)?.toFloatOrNull()
            val unit = it.getOrNull(5)?.let { parseWeightUnit(it) } ?: WeightUnits.Grams
            val packedQty = it.getOrNull(6)?.toDoubleCompat() ?: 0.0
            val desiredQty = it.getOrNull(7)?.toDoubleCompat() ?: 0.0
            PackItem(
                0,
                0,
                name,
                category,
                amount = packedQty,
                desiredAmount = desiredQty,
                weight = weight?.let { Weight(it, unit) })
        }
    }

    private fun parseCategoryString(category: String): ItemCategory {
        // Format: id - name OR name
        val parts = category.split(" - ")

        // Try to use the ID if it exists
        if (parts.size == 2 && parts[0].toIntOrNull() != null) {
            val match = ItemCategory.entries.find { it.id == parts[0].toInt() }
            if (match != null) {
                return match
            }
        }

        // Otherwise, use the name
        return ItemCategory.entries.find { it.name.lowercase() == category.lowercase() }
            ?: ItemCategory.Other
    }

    private fun formatWeightUnit(unit: WeightUnits): String {
        return unit.name.lowercase()
    }

    private fun parseWeightUnit(unit: String): WeightUnits {
        return WeightUnits.entries.find { it.name.lowercase() == unit.lowercase() }
            ?: WeightUnits.Grams
    }

    companion object {
        fun create(fragment: AndromedaFragment): LighterPackIOService {
            return LighterPackIOService(
                FragmentUriPicker(fragment),
                ExternalUriService(fragment.requireContext())
            )
        }
    }


}