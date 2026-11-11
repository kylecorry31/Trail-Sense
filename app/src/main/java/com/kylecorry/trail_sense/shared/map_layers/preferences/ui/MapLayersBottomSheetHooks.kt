package com.kylecorry.trail_sense.shared.map_layers.preferences.ui

import androidx.fragment.app.Fragment
import com.kylecorry.andromeda.core.tryOrNothing
import com.kylecorry.andromeda.core.ui.ReactiveComponent
import com.kylecorry.andromeda.core.ui.useCallback
import com.kylecorry.andromeda.fragments.show
import com.kylecorry.trail_sense.shared.extensions.usePauseEffect

fun <T> T.useMapLayersSheet(
    mapId: String,
    onShow: () -> Unit,
    onDismiss: () -> Unit
): () -> Unit where T : Fragment, T : ReactiveComponent {
    val sheet = useMemo(mapId) { MapLayersBottomSheet(mapId) }

    val onShowRef = useRef(onShow)
    val onDismissRef = useRef(onDismiss)
    onShowRef.current = onShow
    onDismissRef.current = onDismiss

    usePauseEffect(sheet) {
        tryOrNothing {
            sheet.setOnDismissListener(null)
            sheet.dismiss()
        }
    }

    return useCallback<Unit>(sheet) {
        onShowRef.current()
        sheet.setOnDismissListener { onDismissRef.current() }
        sheet.show(this)
    }
}
