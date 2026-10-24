package com.kylecorry.trail_sense.tools.offline_maps.ui.photo_maps

import android.content.Context
import android.util.AttributeSet
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.view.isVisible
import com.kylecorry.trail_sense.shared.views.Toolbar
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.tools.paths.domain.PathEditState


class MapDistanceSheet(context: Context, attrs: AttributeSet?) : FrameLayout(context, attrs) {

    private val formatter by lazy { FormatService.getInstance(context) }

    var cancelListener: (() -> Unit)? = null
    var undoListener: (() -> Unit)? = null
    var redoListener: (() -> Unit)? = null
    var createPathListener: (() -> Unit)? = null

    private val toolbar: Toolbar
    private val undoButton: Button
    private val redoButton: Button
    private val hint: TextView

    init {
        inflate(context, R.layout.view_map_distance_sheet, this)
        toolbar = findViewById(R.id.map_distance_title)
        toolbar.rightButton.setOnClickListener {
            cancelListener?.invoke()
        }

        undoButton = findViewById(R.id.undo_btn)
        undoButton.setOnClickListener {
            undoListener?.invoke()
        }

        redoButton = findViewById(R.id.redo_btn)
        redoButton.setOnClickListener {
            redoListener?.invoke()
        }

        hint = findViewById(R.id.map_distance_hint)

        findViewById<Button>(R.id.create_path_btn).setOnClickListener {
            createPathListener?.invoke()
        }

        setEditState(PathEditState(canUndo = false, canRedo = false, hasSelection = false))
    }

    fun setEditState(state: PathEditState) {
        undoButton.isEnabled = state.canUndo
        redoButton.isEnabled = state.canRedo
        hint.setText(if (state.hasSelection) R.string.path_edit_hint_selected else R.string.path_edit_hint)
    }

    fun setDistance(distance: Distance) {
        toolbar.subtitle.text =
            formatter.formatDistance(distance, Units.getDecimalPlaces(distance.units), false)
    }

    fun show() {
        isVisible = true
    }

    fun hide() {
        isVisible = false
    }
}
