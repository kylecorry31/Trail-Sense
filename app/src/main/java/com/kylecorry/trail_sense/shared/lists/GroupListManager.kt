package com.kylecorry.trail_sense.shared.lists

import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.kylecorry.trail_sense.shared.alerts.ILoadingIndicator
import com.kylecorry.trail_sense.shared.extensions.onIO
import com.kylecorry.trail_sense.shared.extensions.setOnQueryTextListener
import com.kylecorry.trail_sense.shared.grouping.ISearchableGroupLoader
import com.kylecorry.trail_sense.shared.grouping.NamedGroupable
import kotlinx.coroutines.launch

class GroupListManager<T : NamedGroupable>(
    private val owner: LifecycleOwner,
    private val list: TSListView,
    private val search: SearchView,
    private val title: TextView,
    private val loadingIndicator: ILoadingIndicator,
    private val defaultTitle: String,
    private val loader: ISearchableGroupLoader<T>,
    private val mapper: ListItemMapper<T>,
    refreshOnLoad: Boolean = false,
    private val sort: suspend (List<T>) -> List<T> = { it }
) {

    init {
        title.text = defaultTitle
        if (refreshOnLoad) {
            refresh()
        }
        search.setOnQueryTextListener { _, _ ->
            refresh(true)
            true
        }
    }

    val root: T?
        get() = backStack.lastOrNull()

    private val backStack = mutableListOf<T>()

    fun refresh(resetScroll: Boolean = false) {
        owner.lifecycleScope.launch {
            loadingIndicator.show()
            title.text = root?.name ?: defaultTitle

            val items = onIO {
                sort(loader.load(search.query?.toString(), root?.id))
            }
            list.setItems(items, mapper)
            if (resetScroll) {
                list.scrollToPosition(0, false)
            }
            loadingIndicator.hide()
        }
    }

    fun clear() {
        backStack.clear()
        title.text = defaultTitle
        list.setItems(emptyList())
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