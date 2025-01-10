package com.kylecorry.trail_sense.tools.field_guide.domain

import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.data.Identifiable

enum class FieldGuidePageTagType {
    Continent,
    Habitat,
    Classification,
    ActivityPattern,
    HumanInteraction
}

enum class FieldGuidePageTag(
    override val id: Long,
    val type: FieldGuidePageTagType,
    @DrawableRes val icon: Int? = null,
    @ColorInt val color: Int = Color.WHITE
) : Identifiable {
    Africa(1, FieldGuidePageTagType.Continent),
    Antarctica(2, FieldGuidePageTagType.Continent),
    Asia(3, FieldGuidePageTagType.Continent),
    Australia(4, FieldGuidePageTagType.Continent),
    Europe(5, FieldGuidePageTagType.Continent),
    NorthAmerica(6, FieldGuidePageTagType.Continent),
    SouthAmerica(7, FieldGuidePageTagType.Continent),
    Plant(8, FieldGuidePageTagType.Classification),
    Animal(9, FieldGuidePageTagType.Classification),
    Fungus(10, FieldGuidePageTagType.Classification),
    Bird(11, FieldGuidePageTagType.Classification),
    Mammal(12, FieldGuidePageTagType.Classification),
    Reptile(13, FieldGuidePageTagType.Classification),
    Amphibian(14, FieldGuidePageTagType.Classification),
    Fish(15, FieldGuidePageTagType.Classification),
    Insect(16, FieldGuidePageTagType.Classification),
    Arachnid(17, FieldGuidePageTagType.Classification),
    Crustacean(18, FieldGuidePageTagType.Classification),
    Mollusk(19, FieldGuidePageTagType.Classification),
    Forest(20, FieldGuidePageTagType.Habitat, icon = R.drawable.tree, color = AppColor.Green.color),
    Desert(
        21,
        FieldGuidePageTagType.Habitat,
        icon = R.drawable.thermometer,
        color = AppColor.Yellow.color
    ),
    Grassland(
        22,
        FieldGuidePageTagType.Habitat,
        icon = R.drawable.ic_grass,
        color = AppColor.Green.color
    ),
    Wetland(
        23,
        FieldGuidePageTagType.Habitat,
        icon = R.drawable.ic_grass,
        color = AppColor.Brown.color
    ),
    Mountain(
        24,
        FieldGuidePageTagType.Habitat,
        icon = R.drawable.ic_altitude,
        color = AppColor.Gray.color
    ),
    Urban(
        25,
        FieldGuidePageTagType.Habitat,
        icon = R.drawable.ic_building,
        color = AppColor.Gray.color
    ),
    Marine(
        26,
        FieldGuidePageTagType.Habitat,
        icon = R.drawable.ic_tide_table,
        color = AppColor.Blue.color
    ),
    Freshwater(
        27,
        FieldGuidePageTagType.Habitat,
        icon = R.drawable.ic_category_water,
        color = AppColor.Blue.color
    ),
    Cave(
        28,
        FieldGuidePageTagType.Habitat,
        icon = R.drawable.ic_ruins,
        color = AppColor.Gray.color
    ),
    Tundra(
        29,
        FieldGuidePageTagType.Habitat,
        icon = R.drawable.ic_precipitation_snow,
        color = AppColor.Gray.color
    ),
    Rock(30, FieldGuidePageTagType.Classification),
    Diurnal(
        31,
        FieldGuidePageTagType.ActivityPattern,
        icon = R.drawable.ic_sun,
        color = AppColor.Yellow.color
    ),
    Nocturnal(
        32,
        FieldGuidePageTagType.ActivityPattern,
        icon = R.drawable.ic_moon,
        color = AppColor.Gray.color
    ),
    Crepuscular(
        33,
        FieldGuidePageTagType.ActivityPattern,
        icon = R.drawable.ic_sun,
        color = AppColor.Orange.color
    ),
    Edible(34, FieldGuidePageTagType.HumanInteraction),
    Inedible(35, FieldGuidePageTagType.HumanInteraction),
    Dangerous(36, FieldGuidePageTagType.HumanInteraction),
    Crafting(37, FieldGuidePageTagType.HumanInteraction),
    Medicinal(38, FieldGuidePageTagType.HumanInteraction),
}