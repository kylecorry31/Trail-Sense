package com.kylecorry.trail_sense.shared.lists

import com.kylecorry.andromeda.core.coroutines.ControlledRunner
import com.kylecorry.trail_sense.shared.extensions.onIO
import com.kylecorry.trail_sense.shared.grouping.Groupable
import com.kylecorry.trail_sense.shared.grouping.ISearchableGroupLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class GroupListManager<T : Groupable>(
    private val scope: CoroutineScope,
    private val loader: ISearchableGroupLoader<T>,
    initialRoot: T? = null,
    private val augment: suspend (List<T>) -> List<T> = { it }
) {

    val root: T?
        get() = _root

    var onChange: (root: T?, items: List<T>, rootChanged: Boolean) -> Unit = { _, _, _ -> }

    private var _root: T? = initialRoot
    private var query: String? = null
    private val runner = ControlledRunner<Unit>()

    fun refresh(resetScroll: Boolean = false) {
        scope.launch {
            runner.cancelPreviousThenRun {
                val items = onIO {
                    augment(loader.load(query, root?.id))
                }
                onChange(root, items, resetScroll)
            }
        }
    }

    fun search(query: CharSequence?) {
        this.query = query?.toString()
        refresh(true)
    }

    fun clear(resetRoot: Boolean = true) {
        if (resetRoot) {
            _root = null
        }
        onChange(root, emptyList(), true)
    }

    private fun loadGroup(id: Long) {
        scope.launch {
            _root = onIO { loader.getGroup(id) }
            refresh(true)
        }
    }

    fun open(groupId: Long?) {
        if (groupId == null) {
            _root = null
            refresh(true)
        } else {
            loadGroup(groupId)
        }
    }

    fun up(): Boolean {
        if (_root == null) {
            return false
        }
        val parent = _root?.parentId
        if (parent == null) {
            _root = null
            refresh(true)
        } else {
            loadGroup(parent)
        }
        return true
    }

}