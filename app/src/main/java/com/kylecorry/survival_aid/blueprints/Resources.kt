package com.kylecorry.survival_aid.blueprints

/**
 * A collection of resources
 */
object Resources {
    // Basic resources
    val charcoal = Resource("Charcoal", "Charcoal can be obtained from burnt/charred softwoods or in grills.")
    val sand = Resource("Sand", "Sand can be obtained on beaches or made from grinding up gravel.")
    val gravel = Resource("Gravel", "Gravel can be obtained from river beds or exposed earth.")
    val coffeeFilter = Resource("Coffee Filter", "A coffee filter, can be substituted with a fine cloth.")
    val plasticBottle = Resource("Plastic Bottle", "A plastic bottle used to contain water or soda.")
    val pot = Resource("Pot", "A cooking pot which can be exposed to open heat.")
    val water = Resource("Water", "Water from a natural source - may need to be filtered and disinfected in order to drink.")
    val carCharger = Resource("Car Charger", "A car cigarette lighter (12V) to 5V USB device which can be used to charge a cellphone. May require an additional USB cable to charge.")
    val aaBattery = Resource("AA Battery", "A AA battery which provides 1.5V of electricity. Typically used in small electronics such as TV remotes.")
    val aaaBattery = Resource("AAA Battery", "A AAA battery which provides 1.5V of electricity. Typically used in small electronics such as flashlights.")
    val nineVoltBattery = Resource("9V Battery", "A 9V battery which provides 9 volts of electricity. Typically used in smoke detectors.")
    val copperWire = Resource("Copper Wire", "Electrical wire made of copper, can be found in many large electronics or in the walls of buildings.")
    val tape = Resource("Tape", "Tape can be used to secure two items together, and is found in many different forms.")

    // Composite resources
    val waterFilter = Resource("Water Filter", "A water filter is used to remove particulate contaminates from water - this will not disinfect water, so it should still be boiled / treated.")
    val stove = Resource("Stove", "A stove which can be used for cooking.")
    val filteredWater = Resource("Filtered Water", "Water that has been filtered of particular impurities.")
    val potableWater = Resource("Potable Water", "Water which is drinkable and safe to use in cooking.")
    val phoneCharger = Resource("Phone Charger", "A device which can be used to charge a cellphone.")

    // The resources available
    val resources = listOf(
        charcoal,
        sand,
        gravel,
        coffeeFilter,
        plasticBottle,
        pot,
        water,
        aaBattery,
        aaaBattery,
        nineVoltBattery,
        copperWire,
        tape,
        waterFilter,
        stove,
        filteredWater,
        potableWater,
        phoneCharger
    )
}