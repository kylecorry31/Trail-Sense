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
        val headers = listOf("Item Name", "Category", "desc", "qty", "weight", "unit")
        val items = pack.map {
            listOf(
                it.name,
                "${it.category.id} - ${it.category.name}",
                "",
                if (it.desiredAmount == 0.0) it.amount.toString() else it.desiredAmount.toString(),
                it.weight?.weight.toString(),
                it.weight?.units.toString() // TODO: Map units to match LighterPack
            )
        }
        return listOf(headers) + items
    }

    private fun fromCsv(data: List<List<String>>): List<PackItem>? {
        if (data.isEmpty()) {
            return null
        }

        return data.drop(1).map {
            val name = it.getOrNull(0) ?: ""
            val category = parseCategoryString(it.getOrNull(1) ?: "")
            val qty = it.getOrNull(3)?.toDoubleCompat() ?: 0.0
            val weight = it.getOrNull(4)?.toFloatOrNull()
            val unit = it.getOrNull(5)?.let { parseWeightUnit(it) } ?: WeightUnits.Grams
            PackItem(
                0,
                0,
                name,
                category,
                desiredAmount = qty,
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