package com.kylecorry.survival_aid.blueprints

/**
 * A collection of resources
 */
object Resources {
    // Basic resources
    val charcoal = Resource("Charcoal", "A black, somewhat brittle material which can be obtained from burnt/charred softwoods.")
    val sand = Resource("Sand", "Obtained on beaches and contains a high concentration of quartz.")
    val gravel = Resource("Gravel", "Small stones (smaller than a US Quarter) which can be obtained from river beds or beaches.")
    val coffeeFilter = Resource("Coffee Filter", "A filter designed for use in a coffee maker. Can be substituted with a fine cloth.")
    val plasticBottle = Resource("Plastic Bottle", "A plastic bottle used to contain water or soda. Typically made from PET plastic.")
    val pot = Resource("Cooking Pot", "A metal cooking pot which can be exposed to open heat.")
    val water = Resource("Water", "Water from a natural source - may need to be filtered and disinfected in order to drink.")
    val carCharger = Resource("Car Charger", "A car cigarette lighter (12V) to 5V USB device which can be used to charge a cellphone. May require an additional USB cable to charge.")
    val aaBattery = Resource("AA Battery", "A disposable battery which provides 1.5V of electricity. Typically used in small electronics such as TV remotes.")
    val aaaBattery = Resource("AAA Battery", "A disposable battery which provides 1.5V of electricity. Typically used in small electronics such as flashlights.")
    val nineVoltBattery = Resource("9V Battery", "A disposable battery which provides 9 volts of electricity. Typically used in smoke detectors.")
    val copperWire = Resource("Copper Wire", "Electrical grade wire made of copper, can be found in many large electronics or in the walls of buildings.")
    val tape = Resource("Tape", "An strip adhesive used to fasten items together, and is found in many different forms such as electrical, duct, shipping, or masking.")

    // Composite resources
    val waterFilter = Resource("Water Filter", "A filter is used to remove particles from water. Note: this will not disinfect water, so it should still be boiled / treated.")
    val stove = Resource("Stove", "A heating element which can be used for cooking.")
    val filteredWater = Resource("Filtered Water", "Water that has been filtered of particles.")
    val potableWater = Resource("Drinking Water", "Water which is drinkable and safe to use in cooking. Also called potable water.")
    val phoneCharger = Resource("Phone Charger", "A device which can be used to charge a cellphone.")
}