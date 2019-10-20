package com.kylecorry.survival_aid.blueprints

/**
 * A collection of blueprints
 */
object Blueprints {

    private val waterFilter = Blueprint(
        Resources.waterFilter,
        listOf(
            Pair(Resources.charcoal, 1f),
            Pair(Resources.sand, 1f),
            Pair(Resources.gravel, 1f),
            Pair(Resources.coffeeFilter, 1f),
            Pair(Resources.plasticBottle, 1f)
        ),
        listOf(
            "Cut off the bottom of the bottle and hold upside down",
            "Unscrew the cap and cut a hole in it that extends nearly the whole diameter of the cap",
            "Finely grind the charcoal",
            "Place the cloth / coffee filter over the mouth of the bottle (outside)",
            "Screw the cap over the cloth - if the cap does not fit, you can secure the cloth with cordage or a rubber band",
            "Pour the charcoal into the bottle",
            "Pour the sand into the bottle, creating a layer over the charcoal",
            "Pour the gravel into the bottle, creating a layer over the sand",
            "Pour water into the open end of the bottle and collect what drips out the cap - this is the filtered water"
        )
    )

    private val DISINFECTED_WATER = null

    private val PHONE_CHARGER = null

    val blueprints = listOf(waterFilter)
}