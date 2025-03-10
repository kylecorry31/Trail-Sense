package com.kylecorry.trail_sense.tools.field_guide.domain

import com.kylecorry.trail_sense.shared.data.Identifiable

enum class FieldGuidePageTagType {
    Location,
    Habitat,
    Classification,
    ActivityPattern,
    HumanInteraction
}

enum class FieldGuidePageTag(
    override val id: Long,
    val type: FieldGuidePageTagType,
    val parentId: Long? = null
) : Identifiable {
    // Location
    Africa(1, FieldGuidePageTagType.Location),
    Antarctica(2, FieldGuidePageTagType.Location),
    Asia(3, FieldGuidePageTagType.Location),
    Australia(4, FieldGuidePageTagType.Location),
    Europe(5, FieldGuidePageTagType.Location),
    NorthAmerica(6, FieldGuidePageTagType.Location),
    SouthAmerica(7, FieldGuidePageTagType.Location),

    // Classification
    Plant(100, FieldGuidePageTagType.Classification),
    Animal(101, FieldGuidePageTagType.Classification),
    Fungus(102, FieldGuidePageTagType.Classification),
    Bird(103, FieldGuidePageTagType.Classification, 101),
    Mammal(104, FieldGuidePageTagType.Classification, 101),
    Reptile(105, FieldGuidePageTagType.Classification, 101),
    Amphibian(106, FieldGuidePageTagType.Classification, 101),
    Fish(107, FieldGuidePageTagType.Classification, 101),
    Invertebrate(108, FieldGuidePageTagType.Classification, 101),
    Rock(109, FieldGuidePageTagType.Classification),
    Insect(110, FieldGuidePageTagType.Classification, 108),
    Arachnid(111, FieldGuidePageTagType.Classification, 108),
    Crustacean(112, FieldGuidePageTagType.Classification, 108),
    Mollusk(113, FieldGuidePageTagType.Classification, 108),
    Sponge(114, FieldGuidePageTagType.Classification, 108),
    Coral(115, FieldGuidePageTagType.Classification, 108),
    Jellyfish(116, FieldGuidePageTagType.Classification, 108),
    Worm(117, FieldGuidePageTagType.Classification, 108),
    Echinoderm(118, FieldGuidePageTagType.Classification, 108),
    Other(119, FieldGuidePageTagType.Classification),
    Weather(120, FieldGuidePageTagType.Classification),

    // Habitat
    Forest(200, FieldGuidePageTagType.Habitat),
    Desert(
        201,
        FieldGuidePageTagType.Habitat
    ),
    Grassland(
        202,
        FieldGuidePageTagType.Habitat
    ),
    Wetland(
        203,
        FieldGuidePageTagType.Habitat
    ),
    Mountain(
        204,
        FieldGuidePageTagType.Habitat
    ),
    Urban(
        205,
        FieldGuidePageTagType.Habitat
    ),
    Marine(
        206,
        FieldGuidePageTagType.Habitat
    ),
    Freshwater(
        207,
        FieldGuidePageTagType.Habitat
    ),
    Cave(
        208,
        FieldGuidePageTagType.Habitat
    ),
    Tundra(
        209,
        FieldGuidePageTagType.Habitat
    ),

    // Activity pattern
    Diurnal(
        300,
        FieldGuidePageTagType.ActivityPattern
    ),
    Nocturnal(
        301,
        FieldGuidePageTagType.ActivityPattern
    ),
    Crepuscular(
        302,
        FieldGuidePageTagType.ActivityPattern
    ),

    // Human interaction
    Edible(400, FieldGuidePageTagType.HumanInteraction),
    Inedible(401, FieldGuidePageTagType.HumanInteraction),
    Dangerous(402, FieldGuidePageTagType.HumanInteraction),
    Crafting(403, FieldGuidePageTagType.HumanInteraction),
    Medicinal(404, FieldGuidePageTagType.HumanInteraction),
}