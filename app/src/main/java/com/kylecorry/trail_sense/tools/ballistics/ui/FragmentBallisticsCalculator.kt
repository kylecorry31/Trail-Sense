package com.kylecorry.trail_sense.tools.ballistics.ui

import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.kylecorry.andromeda.core.math.DecimalFormatter
import com.kylecorry.andromeda.core.ui.useService
import com.kylecorry.andromeda.fragments.useBackgroundEffect
import com.kylecorry.andromeda.list.GridView
import com.kylecorry.luna.text.toFloatCompat
import com.kylecorry.sol.math.SolMath
import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.math.interpolation.LinearInterpolator
import com.kylecorry.sol.science.physics.NoDragModel
import com.kylecorry.sol.science.physics.Physics
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

class FragmentBallisticsCalculator :
    TrailSenseReactiveFragment(R.layout.fragment_ballistics_calculator) {

    override fun update() {
        val ballisticsTableView = useView<RecyclerView>(R.id.ballistics_table)
        val ballisticsGrid = useMemo(ballisticsTableView) {
            GridView(ballisticsTableView, R.layout.list_item_grid_cell, 4) { view, value: String ->
                view.findViewById<TextView>(R.id.content).text = value
            }
        }
        val zeroDistanceView = useView<DistanceInputView>(R.id.zero_distance)
        val scopeHeightView = useView<DistanceInputView>(R.id.scope_height)
        val bulletSpeedView = useView<DistanceInputView>(R.id.bullet_speed)
        val loadingView = useView<ProgressBar>(R.id.loading)
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
        val (loading, setLoading) = useState(false)

        useEffect(
            zeroDistanceView,
            scopeHeightView,
            bulletSpeedView,
            ballisticCoefficientView,
            ballisticsGrid
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

            ballisticsGrid.addLineSeparator()
        }

        useBackgroundEffect(zeroDistance, scopeHeight, bulletSpeed, ballisticCoefficient) {
            queue.replace {
                setLoading(true)
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
                setLoading(false)
            }
        }

        useEffect(ballisticsTableView, trajectory, zeroDistance, bulletSpeed) {
            val smallUnits = if (prefs.distanceUnits == UserPreferences.DistanceUnits.Feet) {
                DistanceUnits.Inches
            } else {
                DistanceUnits.Centimeters
            }
            // TODO: Set units in header
            val listItems = listOf(
                getString(R.string.time_header_units, "s"),
                getString(
                    R.string.range_header_units,
                    formatter.getDistanceUnitName(zeroDistance.units, true)
                ),
                getString(R.string.velocity_header_units, "fps"),
                getString(
                    R.string.drop_header_units,
                    formatter.getDistanceUnitName(smallUnits, true)
                )
            ) + trajectory.flatMap { point ->
                val seconds = DecimalFormatter.format(
                    point.time,
                    2
                )
                val distance = DecimalFormatter.format(
                    point.distance.convertTo(zeroDistance.units).distance,
                    Units.getDecimalPlaces(zeroDistance.units)
                )
                val drop = DecimalFormatter.format(
                    point.drop.convertTo(smallUnits).distance,
                    1
                )

                val velocity = DecimalFormatter.format(
                    point.velocity.convertTo(
                        bulletSpeed.distanceUnits,
                        bulletSpeed.timeUnits
                    ).speed,
                    0
                )

                listOf(
                    seconds,
                    distance,
                    velocity,
                    drop
                )
            }

            ballisticsGrid.setData(listItems)
        }

        useEffect(loadingView, ballisticsTableView, loading) {
            ballisticsTableView.isVisible = !loading
            loadingView.isVisible = loading
        }
    }


    private fun calculateTrajectory(
        zeroDistance: Distance,
        scopeHeight: Distance,
        bulletSpeed: Speed,
        ballisticCoefficient: Float?
    ): List<TrajectoryPoint> {
        val dragModel = if (ballisticCoefficient == null || SolMath.isZero(ballisticCoefficient)) {
            NoDragModel()
        } else {
            G1DragModel(ballisticCoefficient)
        }

        val initialVelocity = Physics.getVelocityVectorForImpact(
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

        val trajectory = Physics.getTrajectory2D(
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