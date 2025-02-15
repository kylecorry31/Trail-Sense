package com.kylecorry.trail_sense.shared.extensions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import com.kylecorry.andromeda.fragments.AndromedaFragment

// TODO: ANDROMEDA
abstract class XmlReactiveFragment(@LayoutRes private val layoutId: Int) : AndromedaFragment() {

    protected val resetOnResume
        get() = lifecycleHookTrigger.onResume()

    protected val resetOnStart
        get() = lifecycleHookTrigger.onStart()

    protected val resetOnCreate
        get() = lifecycleHookTrigger.onCreate()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(layoutId, container, false)
    }

    abstract override fun onUpdate()
}