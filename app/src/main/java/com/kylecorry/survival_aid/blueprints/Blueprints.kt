package com.kylecorry.survival_aid.blueprints

/**
 * A collection of resources
 */
object Blueprints {

    private val waterFilter = Blueprint(
        Resources.waterFilter,
        BlueprintCategory.WATER,
        listOf(
            Pair(Resources.charcoal, Quantity(1, "Cup")),
            Pair(Resources.sand, Quantity(1, "Cup")),
            Pair(Resources.gravel, Quantity(1, "Cup")),
            Pair(Resources.coffeeFilter, Quantity(1)),
            Pair(Resources.plasticBottle, Quantity(1, "2-Liter Bottle"))
        ),
        listOf(
            "Cut off the bottom of the bottle and hold upside down",
            "Unscrew the cap and cut a hole in it that extends nearly the whole diameter of the cap",
            "Finely grind the charcoal",
            "Place the cloth / coffee filter over the mouth of the bottle (outside)",
            "Screw the cap over the cloth - if the cap does not fit, you can secure the cloth with cordage or a rubber band",
            "Pour the charcoal into the bottle",
            "Pour the sand into the bottle, creating a layer over the charcoal",
            "Pour the gravel into the bottle, creating a layer over the sand"
        )
    )

    private val filteredWater = Blueprint(
        Resources.filteredWater,
        BlueprintCategory.WATER,
        listOf(
            Pair(Resources.water, Quantity(1)),
            Pair(Resources.waterFilter, Quantity(1))
        ),
        listOf(
            "Slowly pour water into the open end of the filter",
            "Collect water that drips from the cap of the filter in some sort of container (do not drink water from the container)"
        )
    )

    private val boiledWater = Blueprint(
        Resources.potableWater,
        BlueprintCategory.WATER,
        listOf(
            Pair(Resources.filteredWater, Quantity(1)),
            Pair(Resources.stove, Quantity(1)),
            Pair(Resources.pot, Quantity(1))
        ),
        listOf(
            "Pour water into the pot and place on the stove",
            "Bring the water to a rolling boil for 15 - 20 minutes",
            "Remove water from heat and cool before storage"
        )
    )

    private val phoneCharger = Blueprint(
        Resources.phoneCharger,
        BlueprintCategory.ELECTRICITY,
        listOf(
            Pair(Resources.carCharger, Quantity(1)),
            Pair(Resources.copperWire, Quantity(1)),
            Pair(Resources.tape, Quantity(1)),
            Pair(Resources.nineVoltBattery, Quantity(1))
        ),
        listOf(
            "(Optional) Remove the casing of the car charger",
            "Cut the copper wire into two equal size pieces (minimum 4 inches each)",
            "Strip both sides of each copper wire (about 1 inch)",
            "Tape one end of one piece of copper wire to the center prong of the car charger (Referred to as wire A)",
            "Tape one end of the other piece of copper wire to one of the outside prongs of the car charger (Referred to as wire B)",
            "Tape the other end of wire A to the positive terminal of the battery",
            "Tape the other end of wire B to the negative terminal of the battery",
            "(Optional) Replace the taped connection to the battery with a 9-Volt battery clip",
            "Charge your phone through the attached USB cable"
        )
    )

    private val oilCandle = Blueprint(
        Resources.candle,
        BlueprintCategory.LIGHTING,
        listOf(
            Pair(Resources.sodaCan, Quantity(1)),
            Pair(Resources.cookingOil, Quantity(1, "Cup")),
            Pair(Resources.paperTowel, Quantity(1, "Sheet"))
        ),
        listOf(
            "Empty the soda can",
            "Fill the can with oil to about halfway",
            "Roll up a piece of paper towel to form a wick",
            "Place the paper towel into the opening of the can so it is in the oil and still sticks out the top",
            "Light the exposed paper towel on fire"
        )
    )

    private val siphon = null
    private val lockPicks = null
    private val perimeterAlarm= null // With cans
    private val longBow = null
    private val splint = null
    private val flashlight = null
    private val solarOven = null
    private val compass = null
    private val fishingHook = null

    val blueprints = listOf(waterFilter, filteredWater, boiledWater, phoneCharger, oilCandle)

    /**
     * Get all resources which produce the given resource
     * @param resource the resource to produce
     * @return the list of resources which can produce the resource
     */
    fun getBlueprintsForResource(resource: Resource): List<Blueprint> {
        return blueprints.filter { it.resource == resource }
    }
}