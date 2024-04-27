package com.kylecorry.trail_sense.shared.grouping.lists

import com.kylecorry.andromeda.core.coroutines.onIO
import com.kylecorry.andromeda.core.coroutines.onMain
import com.kylecorry.luna.coroutines.CoroutineQueueRunner
import com.kylecorry.trail_sense.shared.grouping.Groupable
import com.kylecorry.trail_sense.shared.grouping.persistence.ISearchableGroupLoader
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
    private val runner = CoroutineQueueRunner()

    fun refresh(resetScroll: Boolean = false) {
        scope.launch {
            runner.replace {
                val items = onIO {
                    augment(loader.load(query, root?.id))
                }
                onMain {
                    onChange(root, items, resetScroll)
                }
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