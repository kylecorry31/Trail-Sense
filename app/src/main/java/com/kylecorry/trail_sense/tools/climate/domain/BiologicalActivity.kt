package com.kylecorry.trail_sense.tools.climate.domain

import com.kylecorry.sol.science.ecology.GrowingDegreeDaysCalculationType
import com.kylecorry.sol.science.ecology.LifecycleEvent
import com.kylecorry.sol.science.ecology.SpeciesPhenology
import com.kylecorry.sol.science.ecology.triggers.AboveTemperatureTrigger
import com.kylecorry.sol.science.ecology.triggers.MinimumGrowingDegreeDaysTrigger
import com.kylecorry.sol.science.ecology.triggers.MultiTrigger
import com.kylecorry.sol.units.Temperature
import com.kylecorry.sol.units.TemperatureUnits
import com.kylecorry.trail_sense.shared.andromeda_temp.BelowTemperatureTrigger
import com.kylecorry.trail_sense.shared.andromeda_temp.TemperatureTriggerType
import java.time.Duration

// TODO: Instead of excluded climates, use a regex string for if the climate is valid
enum class BiologicalActivity(
    val type: BiologicalActivityType,
    val phenology: SpeciesPhenology,
    val excludedClimates: List<String>
) {
    Mosquito(
        BiologicalActivityType.Insect, // https://www.nrcc.cornell.edu/industry/mosquito/degreedays.html
        SpeciesPhenology(
            Temperature.Companion.celsius(10f),
            listOf(
                LifecycleEvent(
                    PhenologyService.Companion.EVENT_ACTIVE_START,
                    MinimumGrowingDegreeDaysTrigger(230f, TemperatureUnits.Fahrenheit)
                ),
                LifecycleEvent(
                    PhenologyService.Companion.EVENT_ACTIVE_END,
                    BelowTemperatureTrigger(
                        Temperature.Companion.celsius(5f),
                        TemperatureTriggerType.Low
                    )
                )
            ),
            growingDegreeDaysCalculationType = GrowingDegreeDaysCalculationType.BaseMax
        ),
        listOf(
            "BWh",
            "BWk",
            "ET",
            "EF"
        )
    ),
    Tick(
        BiologicalActivityType.Insect, SpeciesPhenology(
            // Ticks have lifecycles of 2 years, adults aren't driven by GDD - they are active whenever the temperature is ideal for them
            Temperature.Companion.celsius(7.2f),
            listOf(
                LifecycleEvent(
                    PhenologyService.Companion.EVENT_ACTIVE_START,
                    AboveTemperatureTrigger(Temperature.Companion.celsius(7.2f))
                ),
                LifecycleEvent(
                    PhenologyService.Companion.EVENT_ACTIVE_END,
                    BelowTemperatureTrigger(
                        Temperature.Companion.celsius(0f),
                        TemperatureTriggerType.Low
                    )
                )
            )
        ),
        listOf(
            "BWh",
            "BWk",
            "ET",
            "EF"
        )
    ),
    BlackFly(
        BiologicalActivityType.Insect, SpeciesPhenology(
            Temperature.Companion.celsius(0f),
            listOf(
                LifecycleEvent(
                    PhenologyService.Companion.EVENT_ACTIVE_START,
                    MinimumGrowingDegreeDaysTrigger(220f, TemperatureUnits.Celsius)
                ),
                LifecycleEvent(
                    PhenologyService.Companion.EVENT_ACTIVE_END,
                    MinimumGrowingDegreeDaysTrigger(
                        220f,
                        TemperatureUnits.Celsius
                    ),
                    offset = Duration.ofDays(60)
                )
            )
        ),
        listOf(
            "BWh",
            "BWk",
            "ET",
            "EF"
        )
    ),

    // Deer/horse flies
    Tabanidae(
        BiologicalActivityType.Insect, SpeciesPhenology(
            Temperature.Companion.celsius(10f),
            listOf(
                LifecycleEvent(
                    PhenologyService.Companion.EVENT_ACTIVE_START,
                    MultiTrigger(
                        AboveTemperatureTrigger(Temperature.Companion.celsius(18f)),
                        MinimumGrowingDegreeDaysTrigger(225f, TemperatureUnits.Celsius)
                    )
                ),
                LifecycleEvent(
                    PhenologyService.Companion.EVENT_ACTIVE_END,
                    BelowTemperatureTrigger(
                        Temperature.Companion.celsius(18f),
                        TemperatureTriggerType.High
                    )
                )
            )
        ),
        listOf(
            "BWh",
            "BWk",
            "ET",
            "EF"
        )
    ),
    StableFlies(
        BiologicalActivityType.Insect, SpeciesPhenology(
            Temperature.Companion.celsius(10f),
            listOf(
                LifecycleEvent(
                    PhenologyService.Companion.EVENT_ACTIVE_START,
                    MinimumGrowingDegreeDaysTrigger(225f, TemperatureUnits.Celsius)
                ),
                LifecycleEvent(
                    PhenologyService.Companion.EVENT_ACTIVE_END,
                    BelowTemperatureTrigger(Temperature.Companion.celsius(10f))
                )
            )
        ),
        listOf(
            "BWh",
            "BWk",
            "ET",
            "EF"
        )
    ),
    BitingMidges(
        BiologicalActivityType.Insect, SpeciesPhenology(
            Temperature.Companion.celsius(10f),
            listOf(
                LifecycleEvent(
                    PhenologyService.Companion.EVENT_ACTIVE_START,
                    MinimumGrowingDegreeDaysTrigger(200f, TemperatureUnits.Celsius)
                ),
                LifecycleEvent(
                    PhenologyService.Companion.EVENT_ACTIVE_END,
                    BelowTemperatureTrigger(Temperature.Companion.celsius(10f))
                )
            )
        ),
        listOf(
            "ET",
            "EF"
        )
    )
}