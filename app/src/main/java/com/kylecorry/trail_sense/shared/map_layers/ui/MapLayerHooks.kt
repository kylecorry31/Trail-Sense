package com.kylecorry.trail_sense.shared.map_layers.ui

import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.kylecorry.andromeda.core.ui.ReactiveComponent
import com.kylecorry.andromeda.core.ui.useCallback
import com.kylecorry.andromeda.fragments.useBackgroundMemo
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IMapView
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.getAttribution

fun <T> T.useMapLayers(
    manager: IMapLayerManager,
    view: IMapView,
    vararg values: Any?
): MapLayerControls where T : Fragment, T : ReactiveComponent {
    val (key, setKey) = useState(0)
    val keyRef = useRef(0)

    val resume = useCallback<Unit>(manager, view) {
        manager.resume(view, this) {
            keyRef.current++
            setKey(keyRef.current)
        }
    }
    val pause = useCallback<Unit>(manager, view) {
        manager.pause(view)
    }

    useEffectWithCleanup(resume, pause, *values) {
        resume()
        return@useEffectWithCleanup pause
    }

    return useMemo(key, pause, resume) { MapLayerControls(key, pause, resume) }
}

fun ReactiveComponent.useMapAttribution(
    view: IMapView,
    attributionView: TextView,
    layersKey: Int,
    isMapVisible: Boolean = true
) {
    val context = useAndroidContext()

    useEffect(attributionView) {
        attributionView.setupMapAttribution()
    }

    val attribution = useBackgroundMemo(view, layersKey, isMapVisible, cancelWhenRerun = true) {
        if (isMapVisible) view.getAttribution(context) else null
    }

    useEffect(attributionView, attribution, isMapVisible) {
        attributionView.text = attribution
        attributionView.isVisible = isMapVisible && attribution != null
    }
}
