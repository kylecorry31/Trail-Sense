package com.kylecorry.trail_sense.shared.lists

import com.kylecorry.andromeda.core.coroutines.ControlledRunner
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
    initialBackstack: List<T> = emptyList(),
    private val sort: suspend (List<T>) -> List<T> = { it }
) {

    val root: T?
        get() = _backStack.lastOrNull()

    val backstack: List<T>
        get() = _backStack.toList()

    var onChange: (root: T?, items: List<T>, rootChanged: Boolean) -> Unit = { _, _, _ -> }

    private val _backStack = initialBackstack.toMutableList()
    private var query: String? = null
    private val runner = ControlledRunner<Unit>()

    fun refresh(resetScroll: Boolean = false) {
        scope.launch {
            runner.cancelPreviousThenRun {
                loadingIndicator.show()
                val items = onIO {
                    sort(loader.load(query, root?.id))
                }
                onChange(root, items, resetScroll)
                loadingIndicator.hide()
            }
        }
    }

    fun search(query: CharSequence?) {
        this.query = query?.toString()
        refresh(true)
    }

    fun clear(resetRoot: Boolean = true) {
        if (resetRoot) {
            _backStack.clear()
        }
        onChange(root, emptyList(), true)
    }

    fun open(group: T?) {
        if (group == null) {
            _backStack.clear()
        } else {
            _backStack.add(group)
        }
        refresh(true)
    }

    fun up(): Boolean {
        if (_backStack.isEmpty()) {
            return false
        }
        _backStack.removeLast()
        refresh(true)
        return true
    }

}