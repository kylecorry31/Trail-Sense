package com.kylecorry.trail_sense.tools.maps.ui

import android.content.Context
import android.util.AttributeSet
import android.widget.Button
import android.widget.FrameLayout
import androidx.core.view.isVisible
import com.kylecorry.ceres.toolbar.CeresToolbar
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units


class MapDistanceSheet(context: Context, attrs: AttributeSet?) : FrameLayout(context, attrs) {

    private val formatter by lazy { FormatService.getInstance(context) }

    var cancelListener: (() -> Unit)? = null
    var undoListener: (() -> Unit)? = null
    var createPathListener: (() -> Unit)? = null

    private val toolbar: CeresToolbar

    init {
        inflate(context, R.layout.view_map_distance_sheet, this)
        toolbar = findViewById(R.id.map_distance_title)
        toolbar.rightButton.setOnClickListener {
            cancelListener?.invoke()
        }
        toolbar.leftButton.setOnClickListener {
            undoListener?.invoke()
        }

        findViewById<Button>(R.id.create_path_btn).setOnClickListener {
            createPathListener?.invoke()
        }

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