package com.kylecorry.trail_sense.tools.packs.infrastructure

import com.kylecorry.andromeda.fragments.AndromedaFragment
import com.kylecorry.luna.text.toDoubleCompat
import com.kylecorry.sol.units.Weight
import com.kylecorry.sol.units.WeightUnits
import com.kylecorry.trail_sense.shared.io.CsvIOService
import com.kylecorry.trail_sense.shared.io.ExternalUriService
import com.kylecorry.trail_sense.shared.io.IOService
import com.kylecorry.trail_sense.shared.io.IntentUriPicker
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
            HEADER_ITEM_NAME,
            HEADER_CATEGORY,
            HEADER_DESCRIPTION,
            HEADER_QUANTITY,
            HEADER_WEIGHT_AMOUNT,
            HEADER_WEIGHT_UNIT,
            HEADER_PACKED_QUANTITY,
            HEADER_DESIRED_QUANTITY
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

        val headerRow = data.first().map { it.lowercase() }
        val nameIdx = headerRow.indexOf(HEADER_ITEM_NAME.lowercase())
        val categoryIdx = headerRow.indexOf(HEADER_CATEGORY.lowercase())
        val weightAmountIdx = headerRow.indexOf(HEADER_WEIGHT_AMOUNT.lowercase())
        val weightUnitIdx = headerRow.indexOf(HEADER_WEIGHT_UNIT.lowercase())
        val qtyIdx = headerRow.indexOf(HEADER_QUANTITY.lowercase())
        val packedQtyIdx = headerRow.indexOf(HEADER_PACKED_QUANTITY.lowercase())
        val desiredQtyIdx = headerRow.indexOf(HEADER_DESIRED_QUANTITY.lowercase())

        return data.drop(1).map { it ->
            val name = it.getOrNull(nameIdx) ?: ""
            val category = parseCategoryString(it.getOrNull(categoryIdx) ?: "")
            val weight = it.getOrNull(weightAmountIdx)?.toFloatOrNull()
            val unit = it.getOrNull(weightUnitIdx)?.let { parseWeightUnit(it) } ?: WeightUnits.Grams
            val packedQty = it.getOrNull(packedQtyIdx)?.toDoubleCompat() ?: 0.0
            val desiredQty = it.getOrNull(desiredQtyIdx)?.toDoubleCompat() ?: it.getOrNull(qtyIdx)
                ?.toDoubleCompat() ?: 0.0
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
        return weightUnitMap[unit] ?: "gram"
    }

    private fun parseWeightUnit(unit: String): WeightUnits {
        return weightUnitMap.entries.find { it.value == unit }?.key ?: WeightUnits.Grams
    }

    companion object {

        private const val HEADER_ITEM_NAME = "item name"
        private const val HEADER_CATEGORY = "category"
        private const val HEADER_DESCRIPTION = "desc"
        private const val HEADER_QUANTITY = "qty"
        private const val HEADER_WEIGHT_AMOUNT = "weight"
        private const val HEADER_WEIGHT_UNIT = "unit"
        private const val HEADER_PACKED_QUANTITY = "packed qty"
        private const val HEADER_DESIRED_QUANTITY = "desired qty"

        private val weightUnitMap = mapOf(
            WeightUnits.Grams to "gram",
            WeightUnits.Kilograms to "kilogram",
            WeightUnits.Ounces to "ounce",
            WeightUnits.Pounds to "pound",
        )

        fun create(fragment: AndromedaFragment): LighterPackIOService {
            return LighterPackIOService(
                IntentUriPicker(fragment, fragment.requireContext()),
                ExternalUriService(fragment.requireContext())
            )
        }
    }


}