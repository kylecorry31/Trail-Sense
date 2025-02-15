package com.kylecorry.trail_sense.shared.extensions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import com.kylecorry.andromeda.fragments.AndromedaFragment
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.shared.hooks.HookTriggers
import java.time.Duration

// TODO: ANDROMEDA
abstract class TrailSenseReactiveFragment(@LayoutRes private val layoutId: Int) :
    AndromedaFragment() {

    private var currentTriggerIndex = 0
    private val triggers = HookTriggers()

    protected val resetOnResume
        get() = lifecycleHookTrigger.onResume()

    protected val resetOnStart
        get() = lifecycleHookTrigger.onStart()

    protected val resetOnCreate
        get() = lifecycleHookTrigger.onCreate()

    // TODO: Reset on create view support (replace resetHooksOnResume)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(layoutId, container, false)
    }

    fun useRootView(): View {
        return requireView()
    }

    // TODO: Replace useView in AndromedaFragment
    fun <T> useView2(@IdRes id: Int): T {
        return useMemo(useRootView(), id) {
            requireView().findViewById(id)!!
        }
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
        currentTriggerIndex = 0
        update()
    }

    abstract fun update()
}