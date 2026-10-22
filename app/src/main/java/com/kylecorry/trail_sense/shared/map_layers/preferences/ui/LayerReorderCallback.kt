package com.kylecorry.trail_sense.shared.map_layers.preferences.ui

import android.graphics.drawable.Drawable
import android.view.HapticFeedbackConstants
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.kylecorry.andromeda.core.system.Resources

/**
 * Reorders layer headers by long press and drag.
 *
 * The list must use a [ReorderablePreferenceAdapter]. The layer settings are hidden for the
 * duration of the drag, so the caller must rebuild the preferences in [onDragEnded].
 *
 * @param displayedLayerIds the layer ids in display order, which is updated as headers are moved
 * @param onDragStarted called before the layer settings are hidden
 * @param onDragEnded called after the drag finishes, whether or not the order changed
 */
class LayerReorderCallback(
    private val displayedLayerIds: MutableList<String>,
    private val onDragStarted: () -> Unit,
    private val onDragEnded: () -> Unit
) : ItemTouchHelper.SimpleCallback(DRAG_DIRECTIONS, 0) {

    private var originalBackground: Drawable? = null

    override fun getDragDirs(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ): Int {
        return if (isHeader(viewHolder)) DRAG_DIRECTIONS else 0
    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        val from = viewHolder.bindingAdapterPosition
        val to = target.bindingAdapterPosition
        val isValidMove = isHeader(target) &&
                from in displayedLayerIds.indices &&
                to in displayedLayerIds.indices
        if (!isValidMove) {
            return false
        }

        displayedLayerIds.add(to, displayedLayerIds.removeAt(from))
        (recyclerView.adapter as? ReorderablePreferenceAdapter)?.move(from, to)
        return true
    }

    override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
        super.onSelectedChanged(viewHolder, actionState)
        if (actionState != ItemTouchHelper.ACTION_STATE_DRAG || viewHolder == null) {
            return
        }

        val view = viewHolder.itemView
        originalBackground = view.background
        view.setBackgroundColor(Resources.androidBackgroundColorPrimary(view.context))
        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        onDragStarted()
        (viewHolder.bindingAdapter as? ReorderablePreferenceAdapter)?.hideLayerSettings()
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)
        viewHolder.itemView.background = originalBackground
        recyclerView.post { onDragEnded() }
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        // Swiping is disabled
    }

    private fun isHeader(viewHolder: RecyclerView.ViewHolder): Boolean {
        return viewHolder.itemView.tag is LayerHeaderPreference
    }

    private companion object {
        const val DRAG_DIRECTIONS = ItemTouchHelper.UP or ItemTouchHelper.DOWN
    }
}
