package com.kylecorry.trail_sense.tools.species.domain

import java.io.InputStream

data class SpeciesTaxon(
    val taxonId: Int,
    val name: String,
    val rankLevel: Float,
    val leafClassId: Int
)

data class SpeciesPrediction(
    val taxon: SpeciesTaxon,
    val confidence: Float
)

class SpeciesTaxonomy private constructor(
    private val taxaByClassId: List<SpeciesTaxon?>
) {

    val modelSize: Int
        get() = taxaByClassId.size

    fun getPredictions(scores: FloatArray, limit: Int = 3): List<SpeciesPrediction> {
        return scores.indices
            .asSequence()
            .filter { it < taxaByClassId.size && taxaByClassId[it] != null }
            .sortedByDescending { scores[it] }
            .take(limit)
            .map { SpeciesPrediction(taxaByClassId[it]!!, scores[it]) }
            .toList()
    }

    companion object {
        fun load(input: InputStream): SpeciesTaxonomy {
            val taxa = input.bufferedReader().useLines { lines ->
                lines.drop(1).mapNotNull { line ->
                    val columns = line.split(',', limit = COLUMN_COUNT)
                    val leafClassId = columns.getOrNull(LEAF_CLASS_ID_COLUMN)?.toIntOrNull()
                        ?: return@mapNotNull null
                    val taxonId = columns.getOrNull(TAXON_ID_COLUMN)?.toIntOrNull()
                        ?: return@mapNotNull null
                    val rankLevel = columns.getOrNull(RANK_LEVEL_COLUMN)?.toFloatOrNull()
                        ?: return@mapNotNull null
                    val name = columns.getOrNull(NAME_COLUMN)?.trim().orEmpty()
                    if (name.isEmpty()) {
                        return@mapNotNull null
                    }
                    SpeciesTaxon(taxonId, name, rankLevel, leafClassId)
                }.toList()
            }

            val modelSize = (taxa.maxOfOrNull { it.leafClassId } ?: -1) + 1
            val byClassId = MutableList<SpeciesTaxon?>(modelSize) { null }
            taxa.forEach { byClassId[it.leafClassId] = it }
            return SpeciesTaxonomy(byClassId)
        }

        private const val COLUMN_COUNT = 8
        private const val TAXON_ID_COLUMN = 1
        private const val RANK_LEVEL_COLUMN = 2
        private const val LEAF_CLASS_ID_COLUMN = 3
        private const val NAME_COLUMN = 7
    }
}
