package com.kylecorry.trail_sense.tools.field_guide.ui

import android.content.Context
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.views.list.ListItem
import com.kylecorry.andromeda.views.list.ListItemMapper
import com.kylecorry.andromeda.views.list.ResourceListIcon
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.field_guide.domain.FieldGuidePageTag


class FieldGuidePageTagListItemMapper(
    private val context: Context,
    private val onOpen: (FieldGuidePageTag) -> Unit,
) : ListItemMapper<Pair<FieldGuidePageTag, Int>> {

    private val tagNameMapper = FieldGuideTagNameMapper(context)
    private val tagImageMap = mapOf(
        FieldGuidePageTag.Plant to R.drawable.ic_category_natural,
        FieldGuidePageTag.Fungus to R.drawable.mushroom,
        FieldGuidePageTag.Animal to R.drawable.paw,
        FieldGuidePageTag.Mammal to R.drawable.ic_deer,
        FieldGuidePageTag.Bird to R.drawable.bird,
        FieldGuidePageTag.Reptile to R.drawable.lizard,
        FieldGuidePageTag.Amphibian to R.drawable.frog,
        FieldGuidePageTag.Fish to R.drawable.fish,
        FieldGuidePageTag.Invertebrate to R.drawable.ant,
        FieldGuidePageTag.Rock to R.drawable.gem,
        FieldGuidePageTag.Weather to R.drawable.cloud,
        FieldGuidePageTag.Other to R.drawable.ic_help
    )
    private val iconTint = Resources.androidTextColorSecondary(context)

    override fun map(value: Pair<FieldGuidePageTag, Int>): ListItem {
        return ListItem(
            value.first.id,
            tagNameMapper.getName(value.first),
            context.resources.getQuantityString(
                R.plurals.page_group_summary,
                value.second,
                value.second
            ),
            icon = ResourceListIcon(
                tagImageMap[value.first] ?: R.drawable.ic_help,
                tint = iconTint
            )
        ) {
            onOpen(value.first)
        }
    }
}