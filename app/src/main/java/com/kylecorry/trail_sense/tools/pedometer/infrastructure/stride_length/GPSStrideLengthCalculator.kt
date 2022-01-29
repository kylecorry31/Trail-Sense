package com.kylecorry.trail_sense.tools.pedometer.infrastructure.stride_length

import com.kylecorry.andromeda.location.IGPS
import com.kylecorry.andromeda.sense.pedometer.IPedometer
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import kotlinx.coroutines.delay

class GPSStrideLengthCalculator(private val gps: IGPS, private val pedometer: IPedometer) :
    IStrideLengthCalculator {

    private var state = State.Stopped

    override suspend fun calculate(): Distance? {
        if (state != State.Stopped) return null
        val (startSteps, startLocation) = start()

        // TODO: Add other stop conditions
        while (state == State.Started) {
            delay(20)
        }

        pedometer.stop(null)

        if (state == State.Stopping) {
            pedometer.read()
            gps.read()
            val steps = pedometer.steps - startSteps
            val distance = gps.location.distanceTo(startLocation)

            if (steps == 0 || distance == 0f) {
                return null
            }

            return Distance.meters(distance / steps)
        }

        return null
    }

    override fun stop(force: Boolean) {
        if (state == State.Stopped) return
        state = if (force) {
            // TODO: Cancel job
            gps.stop(null)
            pedometer.stop(null)
            State.Stopped
        } else {
            State.Stopping
        }
    }

    private suspend fun start(): Pair<Int, Coordinate> {
        state = State.Starting

        pedometer.read()
        gps.read()

        // Start the pedometer to keep it listening to steps
        pedometer.start(this::onPedometer)

        state = State.Started
        return pedometer.steps to gps.location
    }

    private fun onPedometer(): Boolean {
        return state != State.Stopped
    }


    private enum class State {
        Stopped,
        Starting,
        Started,
        Stopping
    }

}