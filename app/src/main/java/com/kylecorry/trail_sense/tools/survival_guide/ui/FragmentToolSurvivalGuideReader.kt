package com.kylecorry.trail_sense.tools.survival_guide.ui

import android.graphics.Color
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.net.toUri
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.core.widget.NestedScrollView
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.coroutines.onIO
import com.kylecorry.andromeda.core.ui.ExpansionLayout
import com.kylecorry.andromeda.core.ui.Views
import com.kylecorry.andromeda.core.ui.useCallback
import com.kylecorry.andromeda.core.ui.useService
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.andromeda.fragments.useArgument
import com.kylecorry.andromeda.fragments.useBackgroundEffect
import com.kylecorry.andromeda.markdown.MarkdownService
import com.kylecorry.andromeda.pdf.PdfConvert
import com.kylecorry.andromeda.print.ColorMode
import com.kylecorry.andromeda.print.Orientation
import com.kylecorry.andromeda.print.Printer
import com.kylecorry.andromeda.print.ScaleMode
import com.kylecorry.andromeda.views.toolbar.Toolbar
import com.kylecorry.luna.coroutines.onMain
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.debugging.isDebug
import com.kylecorry.trail_sense.shared.extensions.TrailSenseReactiveFragment
import com.kylecorry.trail_sense.shared.io.DeleteTempFilesCommand
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.shared.text.TextUtils
import com.kylecorry.trail_sense.tools.survival_guide.domain.Chapter
import com.kylecorry.trail_sense.tools.survival_guide.domain.Chapters
import kotlinx.coroutines.delay

class FragmentToolSurvivalGuideReader :
    TrailSenseReactiveFragment(R.layout.fragment_survival_guide) {

    override fun update() {
        val context = useAndroidContext()

        // Views
        val titleView = useView<Toolbar>(R.id.guide_name)
        val scrollView = useView<NestedScrollView>(R.id.guide_scroll)

        // Arguments
        val chapter = useChapter()
        val content = useChapterContent()
        val headerIndex = useArgument<Int>("header_index") ?: 0

        // State
        val printChapter = usePrintChapter()

        // Render the title bar
        useEffect(titleView, chapter) {
            titleView.title.text = chapter.title
        }

        // Print button
        useEffect(printChapter, titleView) {
            titleView.rightButton.isVisible = isDebug()
            titleView.rightButton.setOnClickListener {
                printChapter(chapter)
            }
        }

        // Render the content
        useEffect(scrollView, content, headerIndex) {
            if (content == null) {
                return@useEffect
            }
            scrollView.removeAllViews()
            scrollView.addView(
                TextUtils.getMarkdownView(
                    context,
                    content,
                    shouldUppercaseSubheadings = true
                )
            )

            if (headerIndex > 0) {
                // Open the ExpansionLayout at the headerIndex - 1 and scroll to it

                val layout = scrollView.getChildAt(0) as? ViewGroup

                val header = layout?.children
                    ?.filterIsInstance<ExpansionLayout>()
                    ?.elementAtOrNull(headerIndex - 1)

                header?.expand()

                scrollView.post {
                    scrollView.scrollTo(0, header?.top ?: 0)
                }
            }
        }
    }

    private fun useChapter(): Chapter {
        val chapterResourceId = useArgument<Int>("chapter_resource_id")!!
        val context = useAndroidContext()
        return useMemo(context, chapterResourceId) {
            Chapters.getChapters(context).first { it.resource == chapterResourceId }
        }
    }

    private fun useChapterContent(): String? {
        val (content, setContent) = useState<String?>(null)
        val chapterResourceId = useArgument<Int>("chapter_resource_id")!!
        val context = useAndroidContext()
        useBackgroundEffect(context, chapterResourceId) {
            setContent(TextUtils.loadTextFromResources(context, chapterResourceId))
        }
        return content
    }

    private fun usePrintChapter(): (Chapter) -> Unit {
        val context = useAndroidContext()
        val files = useService<FileSubsystem>()
        val markdown = useService<MarkdownService>()
        return useCallback(context, files, markdown) { chapter: Chapter ->
            inBackground {
                onIO {
                    val loading =
                        onMain { Alerts.loading(context, getString(R.string.loading)) }
                    try {
                        // TODO: Extract this to a shared markdown printer
                        val printer = Printer(context)
                        printer.setColorMode(ColorMode.Color)
                        printer.setScaleMode(ScaleMode.Fit)
                        printer.setOrientation(Orientation.Portrait)
                        val content = TextUtils.removeMarkdownComments(
                            "# ${chapter.title}\n" + TextUtils.loadTextFromResources(
                                context,
                                chapter.resource
                            )
                        )
                        val file = files.createTemp("pdf")
                        // TODO: Indented bullet points do not work properly when on a separate page
                        // TODO: Bullet point continuation does not render properly on a separate page
                        // TODO: Images are overlapping text
                        val parsed = markdown.toMarkdown(content)
                        val textView = Views.text(context, null) as TextView
                        textView.textSize = 6f
                        textView.setTextColor(Color.BLACK)
                        markdown.setParsedMarkdown(textView, parsed)
                        // Wait for images to load
                        // TODO: Find a better way to do this
                        delay(2000)
                        val uri = file.toUri()
                        files.output(uri)?.use {
                            PdfConvert.toPdf(textView, it)
                        }
                        onMain { loading.dismiss() }
                        printer.print(uri)
                    } finally {
                        DeleteTempFilesCommand(requireContext()).execute()
                        onMain { loading.dismiss() }
                    }
                }
            }
        }
    }
}