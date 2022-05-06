package com.kylecorry.trail_sense.shared.lists

import com.kylecorry.trail_sense.shared.alerts.ILoadingIndicator
import com.kylecorry.trail_sense.shared.extensions.onIO
import com.kylecorry.trail_sense.shared.grouping.Groupable
import com.kylecorry.trail_sense.shared.grouping.ISearchableGroupLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class GroupListManager<T : Groupable>(
    private val scope: CoroutineScope,
    private val loadingIndicator: ILoadingIndicator,
    private val loader: ISearchableGroupLoader<T>,
    private val sort: suspend (List<T>) -> List<T> = { it },
) {

    val root: T?
        get() = backStack.lastOrNull()

    var onChange: (root: T?, items: List<T>, rootChanged: Boolean) -> Unit = { _, _, _ -> }

    private val backStack = mutableListOf<T>()
    private var query: String? = null

    fun refresh(resetScroll: Boolean = false) {
        scope.launch {
            loadingIndicator.show()
            val items = onIO {
                sort(loader.load(query, root?.id))
            }
            onChange(root, items, resetScroll)
            loadingIndicator.hide()
        }
    }

    fun search(query: CharSequence?) {
        this.query = query?.toString()
        refresh(true)
    }

    fun clear() {
        backStack.clear()
        onChange(null, emptyList(), true)
    }

    fun open(group: T?) {
        if (group == null) {
            backStack.clear()
        } else {
            backStack.add(group)
        }
        refresh(true)
    }

    fun up(): Boolean {
        if (backStack.isEmpty()) {
            return false
        }
        backStack.removeLast()
        refresh(true)
        return true
    }

}