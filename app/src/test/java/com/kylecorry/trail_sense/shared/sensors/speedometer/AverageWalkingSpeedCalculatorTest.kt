package com.kylecorry.trail_sense.shared.sensors.speedometer

import android.graphics.Color
import com.kylecorry.andromeda.core.time.IZonedDateTimeProvider
import com.kylecorry.sol.math.Range
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.sol.units.Speed
import com.kylecorry.sol.units.TimeUnits
import com.kylecorry.trail_sense.tools.paths.domain.IPathService
import com.kylecorry.trail_sense.tools.paths.domain.LineStyle
import com.kylecorry.trail_sense.tools.paths.domain.Path
import com.kylecorry.trail_sense.tools.paths.domain.PathMetadata
import com.kylecorry.trail_sense.tools.paths.domain.PathPointColoringStyle
import com.kylecorry.trail_sense.tools.paths.domain.PathStyle
import com.kylecorry.trail_sense.tools.pedometer.domain.IStepTrackerService
import com.kylecorry.trail_sense.tools.pedometer.domain.StepCountBucket
import com.kylecorry.trail_sense.tools.pedometer.domain.StepLengthPaceCalculator
import com.kylecorry.trail_sense.tools.pedometer.domain.StepTrackingPeriod
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

internal class AverageWalkingSpeedCalculatorTest {

    private lateinit var stepTrackerService: IStepTrackerService
    private lateinit var pathService: IPathService
    private lateinit var timeProvider: IZonedDateTimeProvider
    private lateinit var calculator: AverageWalkingSpeedCalculator
    private val now = Instant.now()

    @BeforeEach
    fun setup() {
        stepTrackerService = mock()
        pathService = mock()
        timeProvider = mock()
        whenever(timeProvider.getTime()).thenReturn(now.atZone(ZoneId.of("UTC")))
        calculator = createCalculator(isPedometerEnabled = true)
    }

    @Test
    fun `uses backtrack when pedometer is disabled with a session still open`() = runBlocking {
        val calculator = createCalculator(isPedometerEnabled = false)
        whenever(stepTrackerService.getOpenStepTrackingPeriod()).thenReturn(
            period(1, now.minus(Duration.ofDays(3)), null, 3600, Duration.ofHours(1))
        )
        setBacktrack(Distance.meters(6480f), Duration.ofHours(1))

        assertSpeed(1.8f, calculator.getAverageWalkingSpeed())
    }

    @Test
    fun `uses open pedometer session when long enough`() = runBlocking {
        whenever(stepTrackerService.getOpenStepTrackingPeriod()).thenReturn(
            period(1, now.minus(Duration.ofHours(2)), null, 3600, Duration.ofHours(1))
        )
        setBacktrack(Distance.meters(6480f), Duration.ofHours(1))
        whenever(stepTrackerService.getAllStepTrackingPeriods()).thenReturn(
            listOf(closedPeriod(2, daysAgo = 1, steps = 4320, activeTime = Duration.ofHours(1)))
        )

        assertSpeed(1f, calculator.getAverageWalkingSpeed())
    }

    @Test
    fun `uses backtrack when open pedometer session is too short`() = runBlocking {
        whenever(stepTrackerService.getOpenStepTrackingPeriod()).thenReturn(
            period(1, now.minus(Duration.ofHours(1)), null, 900, Duration.ofMinutes(15))
        )
        setBacktrack(Distance.meters(6480f), Duration.ofHours(1))
        whenever(stepTrackerService.getAllStepTrackingPeriods()).thenReturn(
            listOf(closedPeriod(2, daysAgo = 1, steps = 4320, activeTime = Duration.ofHours(1)))
        )

        assertSpeed(1.8f, calculator.getAverageWalkingSpeed())
    }

    @Test
    fun `uses backtrack when open pedometer session is too fast`() = runBlocking {
        whenever(stepTrackerService.getOpenStepTrackingPeriod()).thenReturn(
            period(1, now.minus(Duration.ofHours(2)), null, 9000, Duration.ofHours(1))
        )
        setBacktrack(Distance.meters(6480f), Duration.ofHours(1))

        assertSpeed(1.8f, calculator.getAverageWalkingSpeed())
    }

    @Test
    fun `uses backtrack when open pedometer session is too slow`() = runBlocking {
        whenever(stepTrackerService.getOpenStepTrackingPeriod()).thenReturn(
            period(1, now.minus(Duration.ofHours(2)), null, 720, Duration.ofHours(1))
        )
        setBacktrack(Distance.meters(6480f), Duration.ofHours(1))

        assertSpeed(1.8f, calculator.getAverageWalkingSpeed())
    }

    @Test
    fun `uses backtrack when no pedometer session is open`() = runBlocking {
        setBacktrack(Distance.kilometers(3f), Duration.ofMinutes(50))

        assertSpeed(1f, calculator.getAverageWalkingSpeed())
    }

    @Test
    fun `uses past pedometer sessions when backtrack is too short`() = runBlocking {
        setBacktrack(Distance.meters(900f), Duration.ofMinutes(15))
        whenever(stepTrackerService.getAllStepTrackingPeriods()).thenReturn(
            listOf(closedPeriod(1, daysAgo = 1, steps = 5400, activeTime = Duration.ofHours(1)))
        )

        assertSpeed(1.5f, calculator.getAverageWalkingSpeed())
    }

    @Test
    fun `uses past pedometer sessions when backtrack is too fast`() = runBlocking {
        setBacktrack(Distance.kilometers(36f), Duration.ofHours(1))
        whenever(stepTrackerService.getAllStepTrackingPeriods()).thenReturn(
            listOf(closedPeriod(1, daysAgo = 1, steps = 5400, activeTime = Duration.ofHours(1)))
        )

        assertSpeed(1.5f, calculator.getAverageWalkingSpeed())
    }

    @Test
    fun `uses past pedometer sessions when backtrack is too slow`() = runBlocking {
        setBacktrack(Distance.meters(720f), Duration.ofHours(1))
        whenever(stepTrackerService.getAllStepTrackingPeriods()).thenReturn(
            listOf(closedPeriod(1, daysAgo = 1, steps = 5400, activeTime = Duration.ofHours(1)))
        )

        assertSpeed(1.5f, calculator.getAverageWalkingSpeed())
    }

    @Test
    fun `uses past pedometer sessions when backtrack has no times`() = runBlocking {
        setBacktrack(Distance.meters(6480f), null)
        whenever(stepTrackerService.getAllStepTrackingPeriods()).thenReturn(
            listOf(closedPeriod(1, daysAgo = 1, steps = 5400, activeTime = Duration.ofHours(1)))
        )

        assertSpeed(1.5f, calculator.getAverageWalkingSpeed())
    }

    @Test
    fun `uses past pedometer sessions when backtrack is not running`() = runBlocking {
        whenever(pathService.getBacktrackPathId()).thenReturn(null)
        whenever(stepTrackerService.getAllStepTrackingPeriods()).thenReturn(
            listOf(closedPeriod(1, daysAgo = 1, steps = 5400, activeTime = Duration.ofHours(1)))
        )

        assertSpeed(1.5f, calculator.getAverageWalkingSpeed())
    }

    @Test
    fun `averages only recent long past pedometer sessions`() = runBlocking {
        val shortOpenSession =
            period(5, now.minus(Duration.ofHours(1)), null, 600, Duration.ofMinutes(10))
        whenever(stepTrackerService.getOpenStepTrackingPeriod()).thenReturn(shortOpenSession)
        whenever(stepTrackerService.getAllStepTrackingPeriods()).thenReturn(
            listOf(
                closedPeriod(1, daysAgo = 1, steps = 3600, activeTime = Duration.ofHours(1)),
                closedPeriod(2, daysAgo = 29, steps = 7200, activeTime = Duration.ofHours(1)),
                closedPeriod(3, daysAgo = 31, steps = 6480, activeTime = Duration.ofHours(1)),
                closedPeriod(4, daysAgo = 2, steps = 1620, activeTime = Duration.ofMinutes(15)),
                shortOpenSession
            )
        )

        assertSpeed(1.5f, calculator.getAverageWalkingSpeed())
    }

    @Test
    fun `averages only past pedometer sessions within walking speed range`() = runBlocking {
        whenever(stepTrackerService.getAllStepTrackingPeriods()).thenReturn(
            listOf(
                closedPeriod(1, daysAgo = 1, steps = 1080, activeTime = Duration.ofHours(1)),
                closedPeriod(2, daysAgo = 2, steps = 7200, activeTime = Duration.ofHours(1)),
                closedPeriod(3, daysAgo = 3, steps = 720, activeTime = Duration.ofHours(1)),
                closedPeriod(4, daysAgo = 4, steps = 9000, activeTime = Duration.ofHours(1))
            )
        )

        assertSpeed(1.15f, calculator.getAverageWalkingSpeed())
    }

    @Test
    fun `returns null when nothing qualifies`() = runBlocking {
        whenever(stepTrackerService.getOpenStepTrackingPeriod()).thenReturn(
            period(1, now.minus(Duration.ofMinutes(10)), null, 600, Duration.ofMinutes(10))
        )
        setBacktrack(Distance.kilometers(36f), Duration.ofHours(1))
        whenever(stepTrackerService.getAllStepTrackingPeriods()).thenReturn(
            listOf(
                closedPeriod(2, daysAgo = 40, steps = 3600, activeTime = Duration.ofHours(1)),
                closedPeriod(3, daysAgo = 1, steps = 600, activeTime = Duration.ofMinutes(10)),
                closedPeriod(4, daysAgo = 1, steps = 720, activeTime = Duration.ofHours(1))
            )
        )

        assertNull(calculator.getAverageWalkingSpeed())
    }

    @Test
    fun `returns null with no data`() = runBlocking {
        whenever(stepTrackerService.getAllStepTrackingPeriods()).thenReturn(emptyList())

        assertNull(calculator.getAverageWalkingSpeed())
    }

    private fun createCalculator(isPedometerEnabled: Boolean): AverageWalkingSpeedCalculator {
        return AverageWalkingSpeedCalculator(
            StepLengthPaceCalculator(Distance.meters(1f)),
            stepTrackerService,
            pathService,
            timeProvider,
            isPedometerEnabled
        )
    }

    private fun assertSpeed(expectedMetersPerSecond: Float, actual: Speed?) {
        val metersPerSecond = actual?.convertTo(DistanceUnits.Meters, TimeUnits.Seconds)?.value
        assertEquals(expectedMetersPerSecond, metersPerSecond!!, 0.001f)
    }

    private suspend fun setBacktrack(distance: Distance, duration: Duration?) {
        val times = duration?.let { Range(now.minus(it), now) }
        val path = Path(
            10,
            null,
            PathStyle(LineStyle.Solid, PathPointColoringStyle.None, Color.BLACK, true),
            PathMetadata(distance, 100, times, CoordinateBounds.empty)
        )
        whenever(pathService.getBacktrackPathId()).thenReturn(path.id)
        whenever(pathService.getPath(path.id)).thenReturn(path)
    }

    private fun closedPeriod(
        id: Long,
        daysAgo: Long,
        steps: Long,
        activeTime: Duration
    ): StepTrackingPeriod {
        val end = now.minus(Duration.ofDays(daysAgo))
        return period(id, end.minus(Duration.ofHours(4)), end, steps, activeTime)
    }

    private fun period(
        id: Long,
        start: Instant,
        end: Instant?,
        steps: Long,
        activeTime: Duration
    ): StepTrackingPeriod {
        val bucket = StepCountBucket(id, id, start, end ?: now, steps, activeTime)
        return StepTrackingPeriod(id, start, end, listOf(bucket))
    }
}
