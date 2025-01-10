package com.kylecorry.trail_sense.tools.field_guide.infrastructure

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.text.TextUtils
import com.kylecorry.trail_sense.tools.field_guide.domain.FieldGuidePage
import com.kylecorry.trail_sense.tools.field_guide.domain.FieldGuidePageTag

object BuiltInFieldGuide {

    private data class BuiltInFieldGuidePage(
        val resourceId: Int,
        val imagePath: String,
        val tags: List<FieldGuidePageTag>
    )

    private val pages = listOf(
        BuiltInFieldGuidePage(
            R.raw.field_guide_squirrel,
            "field_guide/squirrel.webp",
            listOf(
                FieldGuidePageTag.Africa,
                FieldGuidePageTag.Asia,
                FieldGuidePageTag.Australia,
                FieldGuidePageTag.Europe,
                FieldGuidePageTag.NorthAmerica,
                FieldGuidePageTag.SouthAmerica,
                FieldGuidePageTag.Animal,
                FieldGuidePageTag.Mammal,
                FieldGuidePageTag.Forest,
                FieldGuidePageTag.Desert,
                FieldGuidePageTag.Grassland,
                FieldGuidePageTag.Mountain,
                FieldGuidePageTag.Urban,
                FieldGuidePageTag.Tundra,
                FieldGuidePageTag.Diurnal
            )
        ),
        BuiltInFieldGuidePage(
            R.raw.field_guide_sunfish,
            "field_guide/sunfish.webp",
            listOf(
                FieldGuidePageTag.Africa,
                FieldGuidePageTag.Asia,
                FieldGuidePageTag.Australia,
                FieldGuidePageTag.Europe,
                FieldGuidePageTag.NorthAmerica,
                FieldGuidePageTag.SouthAmerica,
                FieldGuidePageTag.Animal,
                FieldGuidePageTag.Fish,
                FieldGuidePageTag.Freshwater,
                FieldGuidePageTag.Diurnal
            )
        ),
        BuiltInFieldGuidePage(
            R.raw.field_guide_black_bass,
            "field_guide/black_bass.webp",
            listOf(
                FieldGuidePageTag.Africa,
                FieldGuidePageTag.Asia,
                FieldGuidePageTag.Australia,
                FieldGuidePageTag.Europe,
                FieldGuidePageTag.NorthAmerica,
                FieldGuidePageTag.SouthAmerica,
                FieldGuidePageTag.Animal,
                FieldGuidePageTag.Fish,
                FieldGuidePageTag.Freshwater,
                FieldGuidePageTag.Diurnal
            )
        ),
        BuiltInFieldGuidePage(
            R.raw.field_guide_carp,
            "field_guide/carp.webp",
            listOf(
                FieldGuidePageTag.Africa,
                FieldGuidePageTag.Asia,
                FieldGuidePageTag.Australia,
                FieldGuidePageTag.Europe,
                FieldGuidePageTag.NorthAmerica,
                FieldGuidePageTag.SouthAmerica,
                FieldGuidePageTag.Animal,
                FieldGuidePageTag.Fish,
                FieldGuidePageTag.Freshwater,
                FieldGuidePageTag.Nocturnal,
                FieldGuidePageTag.Crepuscular
            )
        ),
        BuiltInFieldGuidePage(
            R.raw.field_guide_crayfish,
            "field_guide/crayfish.webp",
            listOf(
                FieldGuidePageTag.Africa,
                FieldGuidePageTag.Asia,
                FieldGuidePageTag.Australia,
                FieldGuidePageTag.Europe,
                FieldGuidePageTag.NorthAmerica,
                FieldGuidePageTag.SouthAmerica,
                FieldGuidePageTag.Animal,
                FieldGuidePageTag.Crustacean,
                FieldGuidePageTag.Wetland,
                FieldGuidePageTag.Freshwater,
                FieldGuidePageTag.Nocturnal
            )
        ),
        BuiltInFieldGuidePage(
            R.raw.field_guide_crab,
            "field_guide/crab.webp",
            listOf(
                FieldGuidePageTag.Africa,
                FieldGuidePageTag.Asia,
                FieldGuidePageTag.Australia,
                FieldGuidePageTag.Europe,
                FieldGuidePageTag.NorthAmerica,
                FieldGuidePageTag.SouthAmerica,
                FieldGuidePageTag.Animal,
                FieldGuidePageTag.Crustacean,
                FieldGuidePageTag.Marine,
                FieldGuidePageTag.Freshwater,
                FieldGuidePageTag.Crepuscular,
                FieldGuidePageTag.Nocturnal
            )
        ),
        BuiltInFieldGuidePage(
            R.raw.field_guide_clam,
            "field_guide/clam.webp",
            listOf(
                FieldGuidePageTag.Africa,
                FieldGuidePageTag.Asia,
                FieldGuidePageTag.Australia,
                FieldGuidePageTag.Europe,
                FieldGuidePageTag.NorthAmerica,
                FieldGuidePageTag.SouthAmerica,
                FieldGuidePageTag.Animal,
                FieldGuidePageTag.Mollusk,
                FieldGuidePageTag.Marine,
                FieldGuidePageTag.Freshwater
            )
        ),
        BuiltInFieldGuidePage(
            R.raw.field_guide_mussel,
            "field_guide/mussel.webp",
            listOf(
                FieldGuidePageTag.Africa,
                FieldGuidePageTag.Asia,
                FieldGuidePageTag.Australia,
                FieldGuidePageTag.Europe,
                FieldGuidePageTag.NorthAmerica,
                FieldGuidePageTag.SouthAmerica,
                FieldGuidePageTag.Animal,
                FieldGuidePageTag.Mollusk,
                FieldGuidePageTag.Marine,
                FieldGuidePageTag.Freshwater
            )
        ),
        BuiltInFieldGuidePage(
            R.raw.field_guide_periwinkle,
            "field_guide/periwinkle.webp",
            listOf(
                FieldGuidePageTag.Africa,
                FieldGuidePageTag.Asia,
                FieldGuidePageTag.Australia,
                FieldGuidePageTag.Europe,
                FieldGuidePageTag.NorthAmerica,
                FieldGuidePageTag.SouthAmerica,
                FieldGuidePageTag.Animal,
                FieldGuidePageTag.Marine
            )
        ),
        BuiltInFieldGuidePage(
            R.raw.field_guide_rabbit,
            "field_guide/rabbit.webp",
            listOf(
                FieldGuidePageTag.Africa,
                FieldGuidePageTag.Asia,
                FieldGuidePageTag.Australia,
                FieldGuidePageTag.Europe,
                FieldGuidePageTag.NorthAmerica,
                FieldGuidePageTag.SouthAmerica,
                FieldGuidePageTag.Animal,
                FieldGuidePageTag.Mammal,
                FieldGuidePageTag.Forest,
                FieldGuidePageTag.Desert,
                FieldGuidePageTag.Grassland,
                FieldGuidePageTag.Wetland,
                FieldGuidePageTag.Mountain,
                FieldGuidePageTag.Tundra,
                FieldGuidePageTag.Crepuscular
            )
        ),
        BuiltInFieldGuidePage(
            R.raw.field_guide_mouse,
            "field_guide/mouse.webp",
            listOf(
                FieldGuidePageTag.Africa,
                FieldGuidePageTag.Asia,
                FieldGuidePageTag.Australia,
                FieldGuidePageTag.Europe,
                FieldGuidePageTag.NorthAmerica,
                FieldGuidePageTag.SouthAmerica,
                FieldGuidePageTag.Animal,
                FieldGuidePageTag.Mammal,
                FieldGuidePageTag.Forest,
                FieldGuidePageTag.Desert,
                FieldGuidePageTag.Grassland,
                FieldGuidePageTag.Mountain,
                FieldGuidePageTag.Wetland,
                FieldGuidePageTag.Tundra,
                FieldGuidePageTag.Urban,
                FieldGuidePageTag.Cave,
                FieldGuidePageTag.Nocturnal
            )
        ),
        BuiltInFieldGuidePage(
            R.raw.field_guide_grouse,
            "field_guide/grouse.webp",
            listOf(
                FieldGuidePageTag.Asia,
                FieldGuidePageTag.Europe,
                FieldGuidePageTag.NorthAmerica,
                FieldGuidePageTag.Animal,
                FieldGuidePageTag.Bird,
                FieldGuidePageTag.Forest,
                FieldGuidePageTag.Grassland,
                FieldGuidePageTag.Tundra,
                FieldGuidePageTag.Diurnal
            )
        ),
        BuiltInFieldGuidePage(
            R.raw.field_guide_termite,
            "field_guide/termite.webp",
            listOf(
                FieldGuidePageTag.Africa,
                FieldGuidePageTag.Asia,
                FieldGuidePageTag.Australia,
                FieldGuidePageTag.Europe,
                FieldGuidePageTag.NorthAmerica,
                FieldGuidePageTag.SouthAmerica,
                FieldGuidePageTag.Animal,
                FieldGuidePageTag.Insect,
                FieldGuidePageTag.Forest,
                FieldGuidePageTag.Grassland,
                FieldGuidePageTag.Wetland,
                FieldGuidePageTag.Urban,
                FieldGuidePageTag.Nocturnal
            )
        ),
        BuiltInFieldGuidePage(
            R.raw.field_guide_grasshopper,
            "field_guide/grasshopper.webp",
            listOf(
                FieldGuidePageTag.Africa,
                FieldGuidePageTag.Asia,
                FieldGuidePageTag.Australia,
                FieldGuidePageTag.Europe,
                FieldGuidePageTag.NorthAmerica,
                FieldGuidePageTag.SouthAmerica,
                FieldGuidePageTag.Animal,
                FieldGuidePageTag.Insect,
                FieldGuidePageTag.Forest,
                FieldGuidePageTag.Desert,
                FieldGuidePageTag.Grassland,
                FieldGuidePageTag.Mountain,
                FieldGuidePageTag.Urban,
                FieldGuidePageTag.Diurnal
            )
        ),
        BuiltInFieldGuidePage(
            R.raw.field_guide_cricket,
            "field_guide/cricket.webp",
            listOf(
                FieldGuidePageTag.Africa,
                FieldGuidePageTag.Asia,
                FieldGuidePageTag.Australia,
                FieldGuidePageTag.Europe,
                FieldGuidePageTag.NorthAmerica,
                FieldGuidePageTag.SouthAmerica,
                FieldGuidePageTag.Animal,
                FieldGuidePageTag.Insect,
                FieldGuidePageTag.Forest,
                FieldGuidePageTag.Desert,
                FieldGuidePageTag.Grassland,
                FieldGuidePageTag.Mountain,
                FieldGuidePageTag.Urban,
                FieldGuidePageTag.Cave,
                FieldGuidePageTag.Nocturnal
            )
        ),
        BuiltInFieldGuidePage(
            R.raw.field_guide_ant,
            "field_guide/ant.webp",
            listOf(
                FieldGuidePageTag.Africa,
                FieldGuidePageTag.Asia,
                FieldGuidePageTag.Australia,
                FieldGuidePageTag.Europe,
                FieldGuidePageTag.NorthAmerica,
                FieldGuidePageTag.SouthAmerica,
                FieldGuidePageTag.Animal,
                FieldGuidePageTag.Insect,
                FieldGuidePageTag.Forest,
                FieldGuidePageTag.Desert,
                FieldGuidePageTag.Grassland,
                FieldGuidePageTag.Mountain,
                FieldGuidePageTag.Urban,
                FieldGuidePageTag.Wetland,
                FieldGuidePageTag.Diurnal
            )
        ),
        BuiltInFieldGuidePage(
            R.raw.field_guide_earthworm,
            "field_guide/earthworm.webp",
            listOf(
                FieldGuidePageTag.Africa,
                FieldGuidePageTag.Asia,
                FieldGuidePageTag.Australia,
                FieldGuidePageTag.Europe,
                FieldGuidePageTag.NorthAmerica,
                FieldGuidePageTag.SouthAmerica,
                FieldGuidePageTag.Animal,
                FieldGuidePageTag.Insect,
                FieldGuidePageTag.Forest,
                FieldGuidePageTag.Grassland,
                FieldGuidePageTag.Urban,
                FieldGuidePageTag.Wetland,
                FieldGuidePageTag.Nocturnal
            )
        ),
        BuiltInFieldGuidePage(
            R.raw.field_guide_grub,
            "field_guide/grub.webp",
            listOf(
                FieldGuidePageTag.Africa,
                FieldGuidePageTag.Asia,
                FieldGuidePageTag.Australia,
                FieldGuidePageTag.Europe,
                FieldGuidePageTag.NorthAmerica,
                FieldGuidePageTag.SouthAmerica,
                FieldGuidePageTag.Animal,
                FieldGuidePageTag.Insect,
                FieldGuidePageTag.Forest,
                FieldGuidePageTag.Grassland,
                FieldGuidePageTag.Urban,
                FieldGuidePageTag.Wetland,
                FieldGuidePageTag.Mountain,
                FieldGuidePageTag.Nocturnal
            )
        ),
        // TODO: The below have not been updated
        BuiltInFieldGuidePage(
            R.raw.toxicodendron_radicans,
            "survival_guide/poison_ivy.webp",
            listOf(
                FieldGuidePageTag.Asia,
                FieldGuidePageTag.NorthAmerica,
                FieldGuidePageTag.SouthAmerica,
                FieldGuidePageTag.Plant,
                FieldGuidePageTag.Forest,
                FieldGuidePageTag.Grassland,
                FieldGuidePageTag.Wetland
            )
        ),
        BuiltInFieldGuidePage(
            R.raw.toxicodendron_diversilobum,
            "field_guide/Toxicodendron diversilobum.webp",
            listOf(
                FieldGuidePageTag.NorthAmerica,
                FieldGuidePageTag.Plant,
                FieldGuidePageTag.Forest,
                FieldGuidePageTag.Grassland,
                FieldGuidePageTag.Freshwater
            )
        ),
        BuiltInFieldGuidePage(
            R.raw.toxicodendron_vernix,
            "field_guide/Toxicodendron vernix.webp",
            listOf(FieldGuidePageTag.Plant, FieldGuidePageTag.Wetland)
        ),
        BuiltInFieldGuidePage(
            R.raw.urtica_dioica,
            "survival_guide/stinging_nettle.webp",
            listOf(
                FieldGuidePageTag.Africa,
                FieldGuidePageTag.Asia,
                FieldGuidePageTag.Australia,
                FieldGuidePageTag.Europe,
                FieldGuidePageTag.NorthAmerica,
                FieldGuidePageTag.SouthAmerica,
                FieldGuidePageTag.Plant,
                FieldGuidePageTag.Grassland,
                FieldGuidePageTag.Wetland
            )
        ),
        BuiltInFieldGuidePage(
            R.raw.taraxacum,
            "field_guide/Taraxacum.webp",
            listOf(
                FieldGuidePageTag.Antarctica,
                FieldGuidePageTag.Asia,
                FieldGuidePageTag.Europe,
                FieldGuidePageTag.NorthAmerica,
                FieldGuidePageTag.Plant,
                FieldGuidePageTag.Urban
            )
        ),
        BuiltInFieldGuidePage(
            R.raw.laminariales,
            "field_guide/Laminariales.webp",
            listOf(
                FieldGuidePageTag.Africa,
                FieldGuidePageTag.Antarctica,
                FieldGuidePageTag.Asia,
                FieldGuidePageTag.Australia,
                FieldGuidePageTag.NorthAmerica,
                FieldGuidePageTag.SouthAmerica,
                FieldGuidePageTag.Forest,
                FieldGuidePageTag.Marine
            )
        ),
        BuiltInFieldGuidePage(
            R.raw.rumex_acetosa,
            "field_guide/Rumex acetosa.webp",
            listOf(
                FieldGuidePageTag.Asia,
                FieldGuidePageTag.Australia,
                FieldGuidePageTag.Europe,
                FieldGuidePageTag.NorthAmerica,
                FieldGuidePageTag.Plant,
                FieldGuidePageTag.Grassland
            )
        ),
        BuiltInFieldGuidePage(
            R.raw.trifolium,
            "field_guide/Trifolium.webp",
            listOf(
                FieldGuidePageTag.Africa,
                FieldGuidePageTag.Asia,
                FieldGuidePageTag.Europe,
                FieldGuidePageTag.SouthAmerica,
                FieldGuidePageTag.Plant,
                FieldGuidePageTag.Forest,
                FieldGuidePageTag.Grassland,
                FieldGuidePageTag.Mountain,
                FieldGuidePageTag.Urban,
                FieldGuidePageTag.Marine
            )
        ),
        BuiltInFieldGuidePage(
            R.raw.typha,
            "field_guide/Typha.webp",
            listOf(
                FieldGuidePageTag.Africa,
                FieldGuidePageTag.Asia,
                FieldGuidePageTag.Australia,
                FieldGuidePageTag.Europe,
                FieldGuidePageTag.NorthAmerica,
                FieldGuidePageTag.Plant,
                FieldGuidePageTag.Grassland,
                FieldGuidePageTag.Wetland,
                FieldGuidePageTag.Freshwater
            )
        ),
        BuiltInFieldGuidePage(
            R.raw.bambusoideae,
            "field_guide/Bambusoideae.webp",
            listOf(
                FieldGuidePageTag.Africa,
                FieldGuidePageTag.Asia,
                FieldGuidePageTag.Australia,
                FieldGuidePageTag.Europe,
                FieldGuidePageTag.NorthAmerica,
                FieldGuidePageTag.SouthAmerica,
                FieldGuidePageTag.Plant,
                FieldGuidePageTag.Forest,
                FieldGuidePageTag.Grassland,
                FieldGuidePageTag.Mountain,
                FieldGuidePageTag.Marine,
                FieldGuidePageTag.Freshwater
            )
        ),
        BuiltInFieldGuidePage(
            R.raw.plantago_major,
            "field_guide/Plantago major.webp",
            listOf(
                FieldGuidePageTag.Asia,
                FieldGuidePageTag.Europe,
                FieldGuidePageTag.NorthAmerica,
                FieldGuidePageTag.Plant,
                FieldGuidePageTag.Grassland,
                FieldGuidePageTag.Urban
            )
        ),
        BuiltInFieldGuidePage(
            R.raw.rubus,
            "field_guide/Rubus.webp",
            listOf(
                FieldGuidePageTag.Asia,
                FieldGuidePageTag.Europe,
                FieldGuidePageTag.NorthAmerica,
                FieldGuidePageTag.Plant,
                FieldGuidePageTag.Grassland,
                FieldGuidePageTag.Urban
            )
        ),
        BuiltInFieldGuidePage(
            R.raw.laetiporus,
            "field_guide/Laetiporus.webp",
            listOf(
                FieldGuidePageTag.Africa,
                FieldGuidePageTag.Asia,
                FieldGuidePageTag.Europe,
                FieldGuidePageTag.NorthAmerica,
                FieldGuidePageTag.SouthAmerica,
                FieldGuidePageTag.Fungus,
                FieldGuidePageTag.Forest
            )
        ),
        BuiltInFieldGuidePage(
            R.raw.boletales,
            "field_guide/Boletales.webp",
            listOf(
                FieldGuidePageTag.Asia,
                FieldGuidePageTag.Europe,
                FieldGuidePageTag.NorthAmerica,
                FieldGuidePageTag.Fungus,
                FieldGuidePageTag.Forest
            )
        ),
        BuiltInFieldGuidePage(
            R.raw.morchella,
            "field_guide/Morchella.webp",
            listOf(
                FieldGuidePageTag.Asia,
                FieldGuidePageTag.Australia,
                FieldGuidePageTag.Europe,
                FieldGuidePageTag.NorthAmerica,
                FieldGuidePageTag.Fungus,
                FieldGuidePageTag.Forest,
                FieldGuidePageTag.Mountain,
                FieldGuidePageTag.Urban,
                FieldGuidePageTag.Cave
            )
        ),
        BuiltInFieldGuidePage(
            R.raw.pleurotus,
            "field_guide/Pleurotus.webp",
            listOf(
                FieldGuidePageTag.Africa,
                FieldGuidePageTag.Asia,
                FieldGuidePageTag.Australia,
                FieldGuidePageTag.Europe,
                FieldGuidePageTag.NorthAmerica,
                FieldGuidePageTag.SouthAmerica,
                FieldGuidePageTag.Fungus
            )
        ),
        BuiltInFieldGuidePage(
            R.raw.calvatia_gigantea,
            "field_guide/Calvatia gigantea.webp",
            listOf(
                FieldGuidePageTag.Fungus,
                FieldGuidePageTag.Forest,
                FieldGuidePageTag.Grassland,
                FieldGuidePageTag.Freshwater
            )
        ),
        BuiltInFieldGuidePage(
            R.raw.hericium_erinaceus,
            "field_guide/Hericium erinaceus.webp",
            listOf(
                FieldGuidePageTag.Asia,
                FieldGuidePageTag.Europe,
                FieldGuidePageTag.NorthAmerica,
                FieldGuidePageTag.Fungus,
                FieldGuidePageTag.Mountain
            )
        ),
        BuiltInFieldGuidePage(
            R.raw.cladonia_rangiferina,
            "field_guide/Cladonia rangiferina.webp",
            listOf(
                FieldGuidePageTag.Fungus,
                FieldGuidePageTag.Forest,
                FieldGuidePageTag.Grassland,
                FieldGuidePageTag.Mountain,
                FieldGuidePageTag.Tundra
            )
        ),
        BuiltInFieldGuidePage(
            R.raw.chert,
            "field_guide/Chert.webp",
            listOf(
                FieldGuidePageTag.Africa,
                FieldGuidePageTag.Australia,
                FieldGuidePageTag.NorthAmerica,
                FieldGuidePageTag.Mountain,
                FieldGuidePageTag.Marine,
                FieldGuidePageTag.Freshwater
            )
        ),
        BuiltInFieldGuidePage(
            R.raw.basalt,
            "field_guide/Basalt.webp",
            listOf(
                FieldGuidePageTag.Africa,
                FieldGuidePageTag.Asia,
                FieldGuidePageTag.Grassland,
                FieldGuidePageTag.Marine,
                FieldGuidePageTag.Freshwater
            )
        ),
        BuiltInFieldGuidePage(
            R.raw.granite,
            "field_guide/Granite.webp",
            listOf(
                FieldGuidePageTag.Antarctica,
                FieldGuidePageTag.Asia,
                FieldGuidePageTag.Australia,
                FieldGuidePageTag.Grassland,
                FieldGuidePageTag.Mountain,
                FieldGuidePageTag.Freshwater
            )
        ),
        BuiltInFieldGuidePage(
            R.raw.obsidian,
            "field_guide/Obsidian.webp",
            listOf(
                FieldGuidePageTag.Asia,
                FieldGuidePageTag.Australia,
                FieldGuidePageTag.Europe,
                FieldGuidePageTag.NorthAmerica,
                FieldGuidePageTag.Urban,
                FieldGuidePageTag.Marine,
                FieldGuidePageTag.Freshwater
            )
        ),
    )

    fun getFieldGuide(context: Context): List<FieldGuidePage> {
        return pages.mapIndexed { index, page ->
            val text = TextUtils.loadTextFromResources(context, page.resourceId)
            val lines = text.split("\n")
            val name = lines.first()
            val notes = lines.drop(1).joinToString("\n").trim()
            FieldGuidePage(
                -index.toLong(),
                name,
                listOf("android-assets://${page.imagePath}"),
                page.tags,
                notes
            )
        }
    }
}