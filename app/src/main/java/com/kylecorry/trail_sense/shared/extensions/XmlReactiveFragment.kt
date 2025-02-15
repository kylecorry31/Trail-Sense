package com.kylecorry.trail_sense.shared.extensions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import com.kylecorry.andromeda.fragments.AndromedaFragment

// TODO: ANDROMEDA
abstract class XmlReactiveFragment(@LayoutRes private val layoutId: Int) : AndromedaFragment() {

    private var layoutEffects: MutableList<String> = mutableListOf()
    private var tempCurrentHookIndex = 0

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

    abstract override fun onUpdate()
}