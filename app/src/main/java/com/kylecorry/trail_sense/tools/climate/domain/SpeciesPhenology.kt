package com.kylecorry.trail_sense.tools.climate.domain

import com.kylecorry.sol.science.ecology.GrowingDegreeDaysCalculationType
import com.kylecorry.sol.science.ecology.LifecycleEvent
import com.kylecorry.sol.science.ecology.triggers.CumulativeGrowingDegreeDaysTrigger
import com.kylecorry.sol.science.ecology.triggers.DurationTrigger
import com.kylecorry.sol.science.ecology.triggers.MultiTrigger
import com.kylecorry.sol.science.ecology.triggers.TemperatureTrigger
import com.kylecorry.sol.science.ecology.triggers.TemperatureTriggerType
import com.kylecorry.sol.units.Temperature
import com.kylecorry.sol.units.TemperatureUnits
import java.time.Duration

data class SpeciesPhenology(
    val id: String,
    val type: BiologicalActivityType,
    val events: List<LifecycleEvent>,
    val excludedClimates: List<String>
) {
    companion object {

        val ENTRIES = listOf(
            SpeciesPhenology(
                "mosquitoes",
                BiologicalActivityType.FliesAndMosquitoes,
                listOf(
                    LifecycleEvent(
                        PhenologyService.EVENT_ACTIVE_START,
                        CumulativeGrowingDegreeDaysTrigger(
                            230f,
                            TemperatureUnits.Fahrenheit,
                            Temperature.celsius(10f),
                            calculationType = GrowingDegreeDaysCalculationType.BaseMax
                        )
                    ),
                    LifecycleEvent(
                        PhenologyService.EVENT_ACTIVE_END,
                        TemperatureTrigger(
                            Temperature.celsius(5f),
                            above = false,
                            TemperatureTriggerType.Low
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
            SpeciesPhenology(
                "ticks",
                BiologicalActivityType.Ticks,
                // Ticks have lifecycles of 2 years, adults aren't driven by GDD - they are active whenever the temperature is ideal for them
                listOf(
                    LifecycleEvent(
                        PhenologyService.EVENT_ACTIVE_START,
                        TemperatureTrigger(Temperature.celsius(7.2f))
                    ),
                    LifecycleEvent(
                        PhenologyService.EVENT_ACTIVE_END,
                        TemperatureTrigger(
                            Temperature.celsius(0f),
                            above = false,
                            TemperatureTriggerType.Low
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
            SpeciesPhenology(
                "black-flies",
                BiologicalActivityType.FliesAndMosquitoes,
                listOf(
                    LifecycleEvent(
                        PhenologyService.EVENT_ACTIVE_START,
                        CumulativeGrowingDegreeDaysTrigger(
                            220f,
                            TemperatureUnits.Celsius,
                            Temperature.zero
                        )
                    ),
                    LifecycleEvent(
                        PhenologyService.EVENT_ACTIVE_END,
                        DurationTrigger(
                            CumulativeGrowingDegreeDaysTrigger(
                                220f,
                                TemperatureUnits.Celsius,
                                Temperature.zero
                            ),
                            Duration.ofDays(60)
                        ),
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
            SpeciesPhenology(
                "deer-horse-flies",
                BiologicalActivityType.FliesAndMosquitoes,
                listOf(
                    LifecycleEvent(
                        PhenologyService.EVENT_ACTIVE_START,
                        MultiTrigger(
                            TemperatureTrigger(Temperature.celsius(18f)),
                            CumulativeGrowingDegreeDaysTrigger(
                                225f,
                                TemperatureUnits.Celsius,
                                Temperature.celsius(10f)
                            )
                        )
                    ),
                    LifecycleEvent(
                        PhenologyService.EVENT_ACTIVE_END,
                        TemperatureTrigger(
                            Temperature.celsius(18f),
                            above = false,
                            TemperatureTriggerType.High
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
            SpeciesPhenology(
                "stable-flies",
                BiologicalActivityType.FliesAndMosquitoes,
                listOf(
                    LifecycleEvent(
                        PhenologyService.EVENT_ACTIVE_START,
                        CumulativeGrowingDegreeDaysTrigger(
                            225f,
                            TemperatureUnits.Celsius,
                            Temperature.celsius(10f)
                        )
                    ),
                    LifecycleEvent(
                        PhenologyService.EVENT_ACTIVE_END,
                        TemperatureTrigger(Temperature.celsius(10f), above = false)
                    )
                ),
                listOf(
                    "BWh",
                    "BWk",
                    "ET",
                    "EF"
                )
            ),
            SpeciesPhenology(
                "biting-midges",
                BiologicalActivityType.FliesAndMosquitoes,
                listOf(
                    LifecycleEvent(
                        PhenologyService.EVENT_ACTIVE_START,
                        CumulativeGrowingDegreeDaysTrigger(
                            200f,
                            TemperatureUnits.Celsius,
                            Temperature.celsius(10f)
                        )
                    ),
                    LifecycleEvent(
                        PhenologyService.EVENT_ACTIVE_END,
                        TemperatureTrigger(Temperature.celsius(10f), above = false)
                    )
                ),
                listOf(
                    "ET",
                    "EF"
                )
            )
        )
    }
}