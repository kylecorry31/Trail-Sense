package com.kylecorry.trail_sense.shared

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.kylecorry.trail_sense.shared.quickactions.QuickActionButtonView

abstract class QuickActionButton(
    protected val button: QuickActionButtonView,
    protected val fragment: Fragment
) {
    protected val context: Context by lazy { fragment.requireContext() }
    private var wasStateSet = false

    private val observer = LifecycleEventObserver { _, event ->
        when (event) {
            Lifecycle.Event.ON_CREATE -> onCreate()
            Lifecycle.Event.ON_RESUME -> onResume()
            Lifecycle.Event.ON_PAUSE -> onPause()
            Lifecycle.Event.ON_DESTROY -> onDestroy()
            else -> {
                // Do nothing
            }
        }
    }

    fun bind(lifecycleOwner: LifecycleOwner) {
        lifecycleOwner.lifecycle.addObserver(observer)
    }

    fun unbind(lifecycleOwner: LifecycleOwner) {
        lifecycleOwner.lifecycle.removeObserver(observer)
    }

    open fun onCreate() {
        button.isVisible = true
        button.setOnClickListener {
            onClick()
        }
        button.setOnLongClickListener {
            onLongClick()
        }
    }

    open fun onResume() {
        // Do nothing
    }

    open fun onPause() {
        // Do nothing
    }

    open fun onDestroy() {
        // Do nothing
    }

    open fun onClick() {
        // Do nothing
    }

    open fun onLongClick(): Boolean {
        return false
    }

    protected fun setIcon(@DrawableRes icon: Int) {
        button.setIcon(icon)
        if (!wasStateSet) {
            setState(false)
        }
    }

    protected fun setState(enabled: Boolean) {
        button.setState(enabled)
        wasStateSet = true
    }
}
