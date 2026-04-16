package com.kylecorry.trail_sense.tools.augmented_reality.ui.guidance

import android.view.View
import android.widget.TextView
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.coroutines.onDefault
import com.kylecorry.andromeda.core.ui.Colors
import com.kylecorry.andromeda.views.list.AndromedaListView
import com.kylecorry.andromeda.views.list.ListItem
import com.kylecorry.andromeda.views.list.ResourceListIcon
import com.kylecorry.sol.science.astronomy.Astronomy
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.readableName
import com.kylecorry.trail_sense.shared.text.TextUtils
import com.kylecorry.trail_sense.shared.views.SearchView
import com.kylecorry.trail_sense.tools.astronomy.domain.AstronomyService
import com.kylecorry.trail_sense.tools.astronomy.ui.MoonPhaseImageMapper
import com.kylecorry.trail_sense.tools.astronomy.ui.format.PlanetMapper
import com.kylecorry.trail_sense.tools.augmented_reality.ui.AugmentedRealityView
import java.time.ZonedDateTime
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class AstronomyGuidanceTargetPicker(
    private val astronomyService: AstronomyService,
    private val drawBelowHorizon: Boolean,
    private val drawStars: Boolean,
    private val drawLowBrightnessObjects: Boolean,
    private val getDisplayedTime: () -> ZonedDateTime
) {

    suspend fun pick(view: AugmentedRealityView): ARGuidanceTarget? {
        val options = getGuidanceOptions(view)
        return pick(view, options)
    }

    private suspend fun getGuidanceOptions(view: AugmentedRealityView): List<AstronomyGuidanceOption> =
        onDefault {
            val location = view.location
            val time = getDisplayedTime()
            val planetMapper = PlanetMapper(view.context)
            val moonPhaseMapper = MoonPhaseImageMapper()
            val notVisible = view.context.getString(R.string.not_visible)

            val options = mutableListOf<AstronomyGuidanceOption>()

            val sunAltitude = astronomyService.getSunAltitude(location, time)
            if (drawBelowHorizon || sunAltitude > 0f) {
                options.add(
                    AstronomyGuidanceOption(
                        AstronomyGuidanceTarget(astronomyService, AstronomySelection.Sun, getDisplayedTime),
                        view.context.getString(R.string.sun),
                        if (sunAltitude > 0f) null else notVisible,
                        R.drawable.ic_sun,
                        sortOrder = if (sunAltitude > 0f) 0 else 2
                    )
                )
            }

            val moonAltitude = astronomyService.getMoonAltitude(location, time)
            if (drawBelowHorizon || moonAltitude > 0f) {
                val phase = Astronomy.getMoonPhase(time)
                options.add(
                    AstronomyGuidanceOption(
                        AstronomyGuidanceTarget(astronomyService, AstronomySelection.Moon, getDisplayedTime),
                        view.context.getString(R.string.moon),
                        if (moonAltitude > 0f) null else notVisible,
                        moonPhaseMapper.getPhaseImage(phase.phase),
                        iconRotation = astronomyService.getMoonTilt(location, time),
                        sortOrder = if (moonAltitude > 0f) 1 else 3
                    )
                )
            }

            astronomyService.getVisibleMeteorShowers(
                location,
                time,
                if (drawBelowHorizon) null else -10f
            ).forEach {
                options.add(
                    AstronomyGuidanceOption(
                        AstronomyGuidanceTarget(
                            astronomyService,
                            AstronomySelection.MeteorShowerTarget(it.first),
                            getDisplayedTime
                        ),
                        it.first.readableName(),
                        if (it.second.altitude > 0f) null else notVisible,
                        R.drawable.ic_meteor,
                        sortOrder = if (it.second.altitude > 0f) 4 else 5
                    )
                )
            }

            if (drawStars) {
                astronomyService.getVisiblePlanets(
                    location,
                    time,
                    if (drawBelowHorizon) null else 0f,
                    includeDimPlanets = drawLowBrightnessObjects
                ).forEach {
                    options.add(
                        AstronomyGuidanceOption(
                            AstronomyGuidanceTarget(
                                astronomyService,
                                AstronomySelection.PlanetTarget(it.first),
                                getDisplayedTime
                            ),
                            planetMapper.getName(it.first),
                            if (it.second.altitude > 0f) null else notVisible,
                            planetMapper.getImage(it.first),
                            sortOrder = if (it.second.altitude > 0f) 6 else 7
                        )
                    )
                }

                astronomyService.getVisibleStars(
                    location,
                    time,
                    if (drawBelowHorizon) null else 0f,
                    maxMagnitude = if (drawLowBrightnessObjects) null else 4.0f
                ).forEach {
                    options.add(
                        AstronomyGuidanceOption(
                            AstronomyGuidanceTarget(
                                astronomyService,
                                AstronomySelection.StarTarget(it.first),
                                getDisplayedTime
                            ),
                            "${view.context.getString(R.string.star)}: ${it.first.name}",
                            if (it.second.second > 0f) null else notVisible,
                            R.drawable.ic_star,
                            iconTint = Colors.fromColorTemperature(
                                Astronomy.getColorTemperature(it.first)
                            ),
                            sortOrder = if (it.second.second > 0f) 8 else 9
                        )
                    )
                }
            }

            options.sortedWith(compareBy<AstronomyGuidanceOption> { it.sortOrder }.thenBy { it.name })
        }

    private suspend fun pickGuidanceOption(
        view: AugmentedRealityView,
        options: List<AstronomyGuidanceOption>
    ): AstronomyGuidanceOption? = suspendCoroutine { cont ->
        val dialogView = View.inflate(view.context, R.layout.view_search_list_dialog, null)
        val search = dialogView.findViewById<SearchView>(R.id.search)
        val list = dialogView.findViewById<AndromedaListView>(R.id.list)
        val empty = dialogView.findViewById<TextView>(R.id.empty_text)
        var selected: AstronomyGuidanceOption? = null
        var resumed = false

        list.emptyView = empty
        empty.text = view.context.getString(R.string.no_results)

        val alert = Alerts.dialog(
            view.context,
            view.context.getString(R.string.locate),
            contentView = dialogView,
            okText = null
        ) {
            if (!resumed) {
                resumed = true
                cont.resume(selected)
            }
        }

        fun renderOptions(filtered: List<AstronomyGuidanceOption>) {
            list.setItems(filtered.mapIndexed { index, option ->
                ListItem(
                    index.toLong(),
                    option.name,
                    option.subtitle,
                    icon = ResourceListIcon(
                        option.icon,
                        tint = option.iconTint,
                        rotation = option.iconRotation
                    ),
                    action = {
                        selected = option
                        if (!resumed) {
                            resumed = true
                            cont.resume(option)
                        }
                        alert.dismiss()
                    }
                )
            })
        }

        search.setOnSearchListener { query ->
            val filtered = if (query.isBlank()) {
                options
            } else {
                TextUtils.search(query, options) {
                    listOfNotNull(it.name, it.subtitle)
                }
            }
            renderOptions(filtered)
        }

        renderOptions(options)
    }

    private data class AstronomyGuidanceOption(
        val target: ARGuidanceTarget,
        val name: String,
        val subtitle: String? = null,
        val icon: Int,
        val iconTint: Int? = null,
        val iconRotation: Float = 0f,
        val sortOrder: Int
    )

    private suspend fun pick(view: AugmentedRealityView, options: List<AstronomyGuidanceOption>): ARGuidanceTarget? {
        return pickGuidanceOption(view, options)?.target
    }
}
