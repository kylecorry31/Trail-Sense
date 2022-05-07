package com.kylecorry.trail_sense.navigation.paths.ui

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import androidx.core.view.isVisible
import com.kylecorry.andromeda.core.coroutines.ControlledRunner
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.ViewPathGroupSelectBinding
import com.kylecorry.trail_sense.navigation.paths.domain.PathGroup
import com.kylecorry.trail_sense.navigation.paths.domain.pathsort.NamePathSortStrategy
import com.kylecorry.trail_sense.navigation.paths.infrastructure.persistence.PathService
import com.kylecorry.trail_sense.shared.extensions.onIO
import com.kylecorry.trail_sense.shared.extensions.onMain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

// TODO: Make this into a generic group picker
class PathGroupSelectView(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {

    private var binding: ViewPathGroupSelectBinding

    // TODO: Cancel the job on destroy
    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    private val runner = ControlledRunner<Unit>()

    var group: PathGroup? = null

    var groupFilter: List<Long?> = emptyList()
        set(value) {
            field = value
            updatePathList()
        }

    private val pathService by lazy { PathService.getInstance(this.context) }
    private val mapper by lazy {
        PathGroupListItemMapper(this.context) { group, action ->
            if (action == PathGroupAction.Open) {
                this.group = group
                updatePathList()
            }
        }
    }

    init {
        val view = inflate(context, R.layout.view_path_group_select, this)
        binding = ViewPathGroupSelectBinding.bind(view)
        binding.pathGroupTitle.leftQuickAction.setOnClickListener {
            loadGroup(group?.parentId)
        }
        binding.pathList.emptyView = binding.pathEmptyText
        updatePathList()
    }

    fun loadGroup(groupId: Long?) {
        scope.launch {
            group = onIO {
                if (groupId != null) {
                    pathService.getGroup(groupId)
                } else {
                    null
                }
            }
            updatePathList()
        }
    }

    private fun updatePathList() {
        scope.launch {
            runner.cancelPreviousThenRun {
                val groups = getGroups()
                onMain {
                    binding.pathGroupTitle.title.text =
                        group?.name ?: context.getString(R.string.no_group)
                    binding.pathGroupTitle.leftQuickAction.isVisible = group != null
                    val items = groups.map { mapper.map(it).copy(menu = emptyList()) }
                    binding.pathList.setItems(items)
                }
            }
        }
    }

    private suspend fun getGroups(): List<PathGroup> = onIO {
        val sort = NamePathSortStrategy()
        val group = group?.id
        // TODO: Instead of hiding the groups, just disable them so you can't click / appear grayed out
        val groups = pathService.getGroups(group)
            .filterNot { groupFilter.contains(it.id) || groupFilter.contains(it.parentId) }
        sort.sort(groups).map { it as PathGroup }
    }

}