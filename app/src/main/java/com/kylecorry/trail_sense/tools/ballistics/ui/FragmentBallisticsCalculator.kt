package com.kylecorry.trail_sense.tools.ballistics.ui

import androidx.core.widget.addTextChangedListener
import com.google.android.material.textfield.TextInputEditText
import com.kylecorry.andromeda.core.math.DecimalFormatter
import com.kylecorry.andromeda.core.ui.useService
import com.kylecorry.andromeda.fragments.useBackgroundEffect
import com.kylecorry.andromeda.views.list.AndromedaListView
import com.kylecorry.andromeda.views.list.ListItem
import com.kylecorry.luna.text.toFloatCompat
import com.kylecorry.sol.math.SolMath
import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.science.physics.NoDragModel
import com.kylecorry.sol.science.physics.PhysicsService
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.sol.units.Speed
import com.kylecorry.sol.units.TimeUnits
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.DistanceUtils
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.extensions.TrailSenseReactiveFragment
import com.kylecorry.trail_sense.shared.extensions.useCoroutineQueue
import com.kylecorry.trail_sense.shared.views.DistanceInputView
import com.kylecorry.trail_sense.tools.ballistics.domain.G1DragModel
import com.kylecorry.trail_sense.tools.ballistics.domain.LinearInterpolator
import java.time.Duration

class FragmentBallisticsCalculator :
    TrailSenseReactiveFragment(R.layout.fragment_ballistics_calculator) {

    override fun update() {
        val ballisticsTableView = useView<AndromedaListView>(R.id.ballistics_table)
        val zeroDistanceView = useView<DistanceInputView>(R.id.zero_distance)
        val scopeHeightView = useView<DistanceInputView>(R.id.scope_height)
        val bulletSpeedView = useView<DistanceInputView>(R.id.bullet_speed)
        // TODO: Picker for different bullet types with option for Custom BC
        val ballisticCoefficientView = useView<TextInputEditText>(R.id.ballistic_coefficient)

        val formatter = useService<FormatService>()
        val prefs = useService<UserPreferences>()
        val queue = useCoroutineQueue()

        val referenceDistance = useMemo {
            Distance(
                100f, if (prefs.distanceUnits == UserPreferences.DistanceUnits.Feet) {
                    DistanceUnits.Yards
                } else {
                    DistanceUnits.Meters
                }
            )
        }

        // TODO: Determine normal value for metric
        val normalScopeHeight = useMemo {
            Distance(1.5f, DistanceUnits.Inches)
        }

        val (zeroDistance, setZeroDistance) = useState(referenceDistance)
        val (scopeHeight, setScopeHeight) = useState(normalScopeHeight)
        val (bulletSpeed, setBulletSpeed) = useState(
            Speed(
                0f,
                DistanceUnits.Feet,
                TimeUnits.Seconds
            )
        )
        val (ballisticCoefficient, setBallisticCoefficient) = useState<Float?>(null)
        val (trajectory, setTrajectory) = useState(emptyList<TrajectoryPoint>())

        useEffect(
            zeroDistanceView,
            scopeHeightView,
            bulletSpeedView,
            ballisticCoefficientView
        ) {
            zeroDistanceView.units =
                formatter.sortDistanceUnits(DistanceUtils.hikingDistanceUnits)
            zeroDistanceView.hint = getString(R.string.sight_in_range)
            zeroDistanceView.value = referenceDistance
            zeroDistanceView.setOnValueChangeListener {
                setZeroDistance(it ?: Distance(0f, DistanceUnits.Yards))
            }

            scopeHeightView.units = formatter.sortDistanceUnits(DistanceUtils.rulerDistanceUnits)
            scopeHeightView.hint = getString(R.string.scope_height)
            scopeHeightView.value = normalScopeHeight
            scopeHeightView.setOnValueChangeListener {
                setScopeHeight(it ?: Distance(0f, DistanceUnits.Inches))
            }

            bulletSpeedView.units = formatter.sortDistanceUnits(DistanceUtils.hikingDistanceUnits)
            bulletSpeedView.hint = getString(R.string.muzzle_velocity)
            bulletSpeedView.setOnValueChangeListener {
                setBulletSpeed(
                    Speed(
                        it?.distance ?: 0f,
                        it?.units ?: DistanceUnits.Feet,
                        TimeUnits.Seconds
                    )
                )
            }

            ballisticCoefficientView.addTextChangedListener {
                setBallisticCoefficient(it?.toString()?.toFloatCompat())
            }
        }

        useBackgroundEffect(zeroDistance, scopeHeight, bulletSpeed, ballisticCoefficient) {
            queue.replace {
                setTrajectory(
                    if (bulletSpeed.speed > 0) {
                        calculateTrajectory(
                            zeroDistance,
                            scopeHeight,
                            bulletSpeed,
                            ballisticCoefficient
                        )
                    } else {
                        emptyList()
                    }
                )
            }
        }

        useEffect(ballisticsTableView, trajectory, zeroDistance) {
            val smallUnits = if (prefs.distanceUnits == UserPreferences.DistanceUnits.Feet) {
                DistanceUnits.Inches
            } else {
                DistanceUnits.Centimeters
            }
            val listItems = trajectory.mapIndexed { index, point ->
                val seconds =
                    formatter.formatDuration(
                        Duration.ZERO,
                        short = true,
                        includeSeconds = true
                    ).replace("0", DecimalFormatter.format(point.time, 2))
                val distance = formatter.formatDistance(
                    point.distance.convertTo(zeroDistance.units),
                    Units.getDecimalPlaces(zeroDistance.units)
                )
                val drop = formatter.formatDistance(
                    point.drop.convertTo(smallUnits),
                    1
                )

                ListItem(
                    index.toLong(),
                    distance,
                    seconds,
                    trailingText = getString(R.string.drop_amount, drop)
                )
            }

            ballisticsTableView.setItems(listItems)
        }
    }


    private fun calculateTrajectory(
        zeroDistance: Distance,
        scopeHeight: Distance,
        bulletSpeed: Speed,
        ballisticCoefficient: Float?
    ): List<TrajectoryPoint> {
        val physics = PhysicsService()

        val dragModel = if (ballisticCoefficient == null || SolMath.isZero(ballisticCoefficient)) {
            NoDragModel()
        } else {
            G1DragModel(ballisticCoefficient)
        }

        val initialVelocity = physics.getVelocityVectorForImpact(
            Vector2(zeroDistance.meters().distance, 0f),
            bulletSpeed.convertTo(DistanceUnits.Meters, TimeUnits.Seconds).speed,
            Vector2(0f, -scopeHeight.meters().distance),
            timeStep = 0.01f,
            maxTime = 2f,
            minAngle = 0f,
            maxAngle = 1f,
            angleStep = 0.001f,
            dragModel = dragModel
        )

        val trajectory = physics.getTrajectory2D(
            initialPosition = Vector2(0f, -scopeHeight.meters().distance),
            initialVelocity = initialVelocity,
            dragModel = dragModel,
            timeStep = 0.01f,
            maxTime = 2f
        )

        val maxDistance = trajectory.maxOf { it.position.x }

        val interpolator = LinearInterpolator()

        // Recalculate using interpolation
        val xs = trajectory.map { it.position.x }
        val ys = trajectory.map { it.position.y }
        val times = trajectory.map { it.time }
        val velocities = trajectory.map { it.velocity.x }

        val newXs = (0..500 step 10).map {
            Distance(
                it.toFloat(), if (zeroDistance.units.isMetric) {
                    DistanceUnits.Meters
                } else {
                    DistanceUnits.Yards
                }
            ).meters().distance
        }.filter { it <= maxDistance }

        return newXs.filter { it <= maxDistance }.map {
            TrajectoryPoint(
                interpolator.interpolate(it, xs, times),
                Distance.meters(it),
                Speed(
                    interpolator.interpolate(it, xs, velocities),
                    DistanceUnits.Meters,
                    TimeUnits.Seconds
                ),
                Distance.meters(interpolator.interpolate(it, xs, ys))
            )
        }
    }

    data class TrajectoryPoint(
        val time: Float,
        val distance: Distance,
        val velocity: Speed,
        val drop: Distance
    )
}