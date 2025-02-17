package com.kylecorry.trail_sense.shared.extensions

import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.activity.OnBackPressedCallback
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.core.ui.ReactiveComponent
import com.kylecorry.andromeda.core.ui.useService
import com.kylecorry.andromeda.fragments.AndromedaFragment
import com.kylecorry.andromeda.fragments.useTopic
import com.kylecorry.andromeda.sense.location.IGPS
import com.kylecorry.andromeda.signal.ICellSignalSensor
import com.kylecorry.luna.coroutines.CoroutineQueueRunner
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.views.SearchView
import java.time.Duration

fun ReactiveComponent.useCoroutineQueue(): CoroutineQueueRunner {
    return useMemo {
        CoroutineQueueRunner()
    }
}

// Sensors

fun ReactiveComponent.useGPSSensor(frequency: Duration = Duration.ofMillis(20)): IGPS {
    val sensors = useService<SensorService>()
    return useMemo(sensors, frequency.seconds, frequency.nano) { sensors.getGPS(frequency) }
}

fun ReactiveComponent.useCellSignalSensor(removeUnregisteredSignals: Boolean = true): ICellSignalSensor {
    val sensors = useService<SensorService>()
    return useMemo(sensors, removeUnregisteredSignals) {
        sensors.getCellSignal(
            removeUnregisteredSignals
        )
    }
}

// Common sensor readings
fun AndromedaFragment.useLocation(frequency: Duration = Duration.ofMillis(20)): Coordinate {
    val gps = useGPSSensor(frequency)
    return useTopic(gps, gps.location) {
        it.location
    }
}

fun AndromedaFragment.useNavController(): NavController {
    return useMemo(useRootView()) { findNavController() }
}

fun AndromedaFragment.useArguments(): Bundle {
    return requireArguments()
}

fun <T> AndromedaFragment.useArgument(key: String): T? {
    val arguments = useArguments()
    return useMemo(arguments, key) {
        @Suppress("DEPRECATION", "UNCHECKED_CAST")
        arguments.get(key) as? T?
    }
}

fun AndromedaFragment.useBackPressedCallback(
    vararg values: Any?,
    callback: OnBackPressedCallback.() -> Boolean
) {
    val navController = useNavController()
    useEffectWithCleanup(*values) {
        val listener = onBackPressed {
            val consumed = callback()
            if (!consumed) {
                remove()
                navController.popBackStack()
            }
        }

        return@useEffectWithCleanup {
            listener.remove()
        }
    }
}

fun AndromedaFragment.useSearch(view: SearchView, onSearch: (String) -> Unit) {
    useEffect(view) {
        view.setOnSearchListener(onSearch)
    }
}

fun AndromedaFragment.useShowDisclaimer(
    title: String,
    message: CharSequence,
    shownKey: String,
    okText: String = getString(android.R.string.ok),
    cancelText: String? = getString(android.R.string.cancel),
    considerShownIfCancelled: Boolean = true,
    shownValue: Boolean = true,
    onClose: (cancelled: Boolean, agreed: Boolean) -> Unit = { _, _ -> }
) {
    val context = useAndroidContext()
    useEffect {
        CustomUiUtils.disclaimer(
            context,
            title,
            message,
            shownKey,
            okText,
            cancelText,
            considerShownIfCancelled,
            shownValue,
            onClose
        )
    }
}

fun ReactiveComponent.useClickCallback(view: View, vararg values: Any?, callback: () -> Unit) {
    useEffect(view, *values) {
        view.setOnClickListener {
            callback()
        }
    }
}