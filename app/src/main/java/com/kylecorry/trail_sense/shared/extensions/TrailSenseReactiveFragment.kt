package com.kylecorry.trail_sense.shared.extensions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import com.kylecorry.andromeda.fragments.XmlReactiveFragment
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.shared.hooks.HookTriggers
import java.time.Duration

abstract class TrailSenseReactiveFragment(
    @LayoutRes private val layoutId: Int,
    private val forcedInterval: Long? = null
) :
    XmlReactiveFragment(layoutId) {

    private var currentTriggerIndex = 0
    private val triggers = HookTriggers()

    protected var runEveryCycle = 0

    private var wasViewCreated = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)
        wasViewCreated = true
        if (forcedInterval != null) {
            scheduleUpdates(forcedInterval)
        }
        return view
    }

    override fun onDestroyView() {
        wasViewCreated = false
        super.onDestroyView()
    }

    fun distance(
        location: Coordinate,
        threshold: Distance,
        highAccuracy: Boolean = true
    ): Boolean {
        val key = "trigger-$currentTriggerIndex"
        currentTriggerIndex++
        return triggers.distance(key, location, threshold, highAccuracy)
    }

    fun frequency(threshold: Duration): Boolean {
        val key = "trigger-$currentTriggerIndex"
        currentTriggerIndex++
        return triggers.frequency(key, threshold)
    }

    override fun onUpdate() {
        if (!wasViewCreated) {
            return
        }
        runEveryCycle++
        currentTriggerIndex = 0
        update()
    }

    /**
     * Called whenever the fragment should update its content. Mostly due to changes in state.
     */
    abstract fun update()

    override fun onPause() {
        super.onPause()
        cleanupEffects()
    }
}