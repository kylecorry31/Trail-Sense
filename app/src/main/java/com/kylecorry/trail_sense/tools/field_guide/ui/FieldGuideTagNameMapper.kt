package com.kylecorry.trail_sense.tools.field_guide.ui

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.field_guide.domain.FieldGuidePageTag

class FieldGuideTagNameMapper(private val context: Context) {

    fun getName(tag: FieldGuidePageTag): String {
        return when (tag) {
            FieldGuidePageTag.Africa -> context.getString(R.string.africa)
            FieldGuidePageTag.Antarctica -> context.getString(R.string.antarctica)
            FieldGuidePageTag.Asia -> context.getString(R.string.asia)
            FieldGuidePageTag.Australia -> context.getString(R.string.australia)
            FieldGuidePageTag.Europe -> context.getString(R.string.europe)
            FieldGuidePageTag.NorthAmerica -> context.getString(R.string.north_america)
            FieldGuidePageTag.SouthAmerica -> context.getString(R.string.south_america)
            FieldGuidePageTag.Plant -> context.getString(R.string.plant)
            FieldGuidePageTag.Animal -> context.getString(R.string.animal)
            FieldGuidePageTag.Fungus -> context.getString(R.string.fungus)
            FieldGuidePageTag.Bird -> context.getString(R.string.bird)
            FieldGuidePageTag.Mammal -> context.getString(R.string.mammal)
            FieldGuidePageTag.Reptile -> context.getString(R.string.reptile)
            FieldGuidePageTag.Amphibian -> context.getString(R.string.amphibian)
            FieldGuidePageTag.Fish -> context.getString(R.string.fish)
            FieldGuidePageTag.Invertebrate -> context.getString(R.string.invertebrate)
            FieldGuidePageTag.Rock -> context.getString(R.string.rock)
            FieldGuidePageTag.Insect -> context.getString(R.string.insect)
            FieldGuidePageTag.Arachnid -> context.getString(R.string.arachnid)
            FieldGuidePageTag.Crustacean -> context.getString(R.string.crustacean)
            FieldGuidePageTag.Mollusk -> context.getString(R.string.mollusk)
            FieldGuidePageTag.Sponge -> context.getString(R.string.sponge)
            FieldGuidePageTag.Coral -> context.getString(R.string.coral)
            FieldGuidePageTag.Jellyfish -> context.getString(R.string.jellyfish)
            FieldGuidePageTag.Worm -> context.getString(R.string.worm)
            FieldGuidePageTag.Echinoderm -> context.getString(R.string.echinoderm)
            FieldGuidePageTag.Other -> context.getString(R.string.other)
            FieldGuidePageTag.Forest -> context.getString(R.string.forest)
            FieldGuidePageTag.Desert -> context.getString(R.string.desert)
            FieldGuidePageTag.Grassland -> context.getString(R.string.grassland)
            FieldGuidePageTag.Wetland -> context.getString(R.string.wetland)
            FieldGuidePageTag.Mountain -> context.getString(R.string.mountain)
            FieldGuidePageTag.Urban -> context.getString(R.string.urban)
            FieldGuidePageTag.Marine -> context.getString(R.string.marine)
            FieldGuidePageTag.Freshwater -> context.getString(R.string.freshwater)
            FieldGuidePageTag.Cave -> context.getString(R.string.cave)
            FieldGuidePageTag.Tundra -> context.getString(R.string.tundra)
            FieldGuidePageTag.Diurnal -> context.getString(R.string.diurnal)
            FieldGuidePageTag.Nocturnal -> context.getString(R.string.nocturnal)
            FieldGuidePageTag.Crepuscular -> context.getString(R.string.crepuscular)
            FieldGuidePageTag.Edible -> context.getString(R.string.edible)
            FieldGuidePageTag.Inedible -> context.getString(R.string.inedible)
            FieldGuidePageTag.Dangerous -> context.getString(R.string.dangerous)
            FieldGuidePageTag.Crafting -> context.getString(R.string.crafting)
            FieldGuidePageTag.Medicinal -> context.getString(R.string.medicinal)
            FieldGuidePageTag.Weather -> context.getString(R.string.weather)
        }
    }

}