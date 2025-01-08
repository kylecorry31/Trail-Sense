package com.kylecorry.trail_sense.tools.species_catalog

import com.kylecorry.trail_sense.shared.data.Identifiable

enum class FieldGuidePageTagType {
    Continent,
    Habitat,
    Classification,
    ActivityPattern
}

enum class FieldGuidePageTag(override val id: Long, type: FieldGuidePageTagType) : Identifiable {
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
    Forest(20, FieldGuidePageTagType.Habitat),
    Desert(21, FieldGuidePageTagType.Habitat),
    Grassland(22, FieldGuidePageTagType.Habitat),
    Wetland(23, FieldGuidePageTagType.Habitat),
    Mountain(24, FieldGuidePageTagType.Habitat),
    Urban(25, FieldGuidePageTagType.Habitat),
    Marine(26, FieldGuidePageTagType.Habitat),
    Freshwater(27, FieldGuidePageTagType.Habitat),
    Cave(28, FieldGuidePageTagType.Habitat),
    Tundra(29, FieldGuidePageTagType.Habitat),
    Rock(30, FieldGuidePageTagType.Classification),
    Diurnal(31, FieldGuidePageTagType.ActivityPattern),
    Nocturnal(32, FieldGuidePageTagType.ActivityPattern),
    Crepuscular(33, FieldGuidePageTagType.ActivityPattern)
}