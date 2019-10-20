package com.kylecorry.survival_aid.blueprints

/**
 * A collection of resources
 */
object Resources {
    // Basic resources
    val charcoal = Resource("Charcoal", "Charcoal can be obtained from burnt/charred softwoods or in grills. Measured in cups.")
    val sand = Resource("Sand", "Sand can be obtained on beaches or made from grinding up gravel. Measured in cups.")
    val gravel = Resource("Gravel", "Gravel can be obtained from river beds or exposed earth. Measured in cups.")
    val coffeeFilter = Resource("Coffee Filter", "A coffee filter, can be substituted with a fine cloth.")
    val plasticBottle = Resource("Plastic Bottle", "A plastic bottle used to contain water or soda (2-liter bottle).")

    // Composite resources
    val waterFilter = Resource("Water Filter", "A water filter is used to remove particulate contaminates from water - this will not disinfect water, so it should still be boiled / treated.")

    // The resources available
    val resources = listOf(charcoal, sand, gravel, coffeeFilter, plasticBottle, waterFilter)
}