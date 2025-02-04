package com.kylecorry.trail_sense.tools.field_guide.ui

import android.content.Context
import android.graphics.Bitmap
import android.util.Size
import android.widget.ImageView
import androidx.lifecycle.LifecycleOwner
import com.kylecorry.andromeda.core.coroutines.onIO
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.views.list.AsyncListIcon
import com.kylecorry.andromeda.views.list.ListItem
import com.kylecorry.andromeda.views.list.ListItemMapper
import com.kylecorry.andromeda.views.list.ListMenuItem
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.tools.field_guide.domain.FieldGuidePage

enum class FieldGuidePageListItemActionType {
    View,
    Edit,
    Delete
}

class FieldGuidePageListItemMapper(
    private val context: Context,
    private val viewLifecycleOwner: LifecycleOwner,
    private val action: (FieldGuidePageListItemActionType, FieldGuidePage) -> Unit
) : ListItemMapper<FieldGuidePage> {

    private val files by lazy { FileSubsystem.getInstance(context) }

    override fun map(value: FieldGuidePage): ListItem {
        val firstSentence =
            value.notes?.substringBefore(".")?.plus(if (value.notes.contains(".")) "." else "")
                ?: ""
        return ListItem(
            value.id,
            value.name,
            firstSentence.take(200),
            icon = AsyncListIcon(
                viewLifecycleOwner,
                { loadThumbnail(value) },
                size = 48f,
                scaleType = ImageView.ScaleType.CENTER_CROP,
                clearOnPause = true
            ),
            menu = listOfNotNull(
                if (!value.isReadOnly) ListMenuItem(context.getString(R.string.edit)) {
                    action(
                        FieldGuidePageListItemActionType.Edit,
                        value
                    )
                } else null,
                if (!value.isReadOnly) ListMenuItem(context.getString(R.string.delete)) {
                    action(
                        FieldGuidePageListItemActionType.Delete,
                        value
                    )
                } else null
            )
        ) {
            action(FieldGuidePageListItemActionType.View, value)
        }
    }

    private suspend fun loadThumbnail(species: FieldGuidePage): Bitmap = onIO {
        val size = Resources.dp(context, 48f).toInt()
        try {
            files.bitmap(species.images.first(), Size(size, size)) ?: getDefaultThumbnail()
        } catch (e: Exception) {
            getDefaultThumbnail()
        }
    }

    private fun getDefaultThumbnail(): Bitmap {
        val size = Resources.dp(context, 48f).toInt()
        return Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    }
}