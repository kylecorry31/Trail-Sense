package com.kylecorry.trail_sense.tools.field_guide.infrastructure

import android.annotation.SuppressLint
import android.content.Context
import com.kylecorry.andromeda.json.JsonConvert
import com.kylecorry.luna.hooks.Hooks
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.ProguardIgnore
import com.kylecorry.trail_sense.shared.text.TextUtils
import com.kylecorry.trail_sense.tools.field_guide.domain.FieldGuidePage
import com.kylecorry.trail_sense.tools.field_guide.domain.FieldGuidePageTag

object BuiltInFieldGuide {

    private class Pages : ProguardIgnore {
        var pages: List<BuiltInFieldGuidePage> = emptyList()
    }

    private class BuiltInFieldGuidePage : ProguardIgnore {
        var content: String = ""
        var image: String = ""
        var tags: List<String> = emptyList()
    }

    private val hooks = Hooks()

    private fun getPages(context: Context): List<BuiltInFieldGuidePage> {
        return hooks.memo("pages") {
            val rawJson = TextUtils.loadTextFromResources(context, R.raw.field_guide_pages)
            JsonConvert.fromJson<Pages>(rawJson)?.pages ?: emptyList()
        }
    }

    fun getFieldGuidePage(context: Context, id: Long): FieldGuidePage? {
        return getPages(context).getOrNull(-id.toInt() - 1)
            ?.let { loadPage(context, it, -id.toInt()) }
    }

    fun getFieldGuide(context: Context): List<FieldGuidePage> {
        return getPages(context).mapIndexed { index, page ->
            loadPage(context, page, index)
        }
    }

    @SuppressLint("DiscouragedApi")
    private fun loadPage(
        context: Context,
        page: BuiltInFieldGuidePage,
        index: Int
    ): FieldGuidePage {
        val resourceId = context.resources.getIdentifier(page.content, "raw", context.packageName)
        val text = TextUtils.loadTextFromResources(context, resourceId)
        val lines = text.split("\n")
        val name = lines.first()
        val notes = lines.drop(1).joinToString("\n").trim()
        return FieldGuidePage(
            -(index.toLong() + 1),
            name,
            listOf("android-assets://${page.image}"),
            page.tags.map { FieldGuidePageTag.valueOf(it.split("_").last()) },
            notes,
            isReadOnly = true
        )
    }
}
