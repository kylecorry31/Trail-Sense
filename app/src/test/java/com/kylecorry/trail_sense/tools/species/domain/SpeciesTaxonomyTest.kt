package com.kylecorry.trail_sense.tools.species.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SpeciesTaxonomyTest {

    @Test
    fun loadsLeafTaxaAndRanksPredictions() {
        val taxonomy = SpeciesTaxonomy.load(
            """
            parent_taxon_id,taxon_id,rank_level,leaf_class_id,iconic_class_id,spatial_class_id,spatial_threshold,name
            ,1,70,,,,,Animalia
            1,10,10,0,0,0,0.1,Species one
            1,20,10,1,0,1,0.1,Species two
            1,30,10,2,0,2,0.1,Species three
            """.trimIndent().byteInputStream()
        )

        val predictions = taxonomy.getPredictions(floatArrayOf(0.2f, 0.7f, 0.1f), 2)

        assertEquals(3, taxonomy.modelSize)
        assertEquals(listOf("Species two", "Species one"), predictions.map { it.taxon.name })
        assertEquals(listOf(0.7f, 0.2f), predictions.map { it.confidence })
    }

    @Test
    fun supportsGapsInLeafClassIds() {
        val taxonomy = SpeciesTaxonomy.load(
            """
            parent_taxon_id,taxon_id,rank_level,leaf_class_id,iconic_class_id,spatial_class_id,spatial_threshold,name
            1,10,10,0,0,0,0.1,Species one
            1,30,10,2,0,2,0.1,Species three
            """.trimIndent().byteInputStream()
        )

        val predictions = taxonomy.getPredictions(floatArrayOf(0.2f, 0.7f, 0.1f), 3)

        assertEquals(listOf("Species one", "Species three"), predictions.map { it.taxon.name })
    }
}
