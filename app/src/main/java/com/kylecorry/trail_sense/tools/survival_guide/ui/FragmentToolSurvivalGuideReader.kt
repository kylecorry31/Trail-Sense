package com.kylecorry.trail_sense.tools.survival_guide.ui

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.net.toUri
import androidx.core.view.isVisible
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.coroutines.onIO
import com.kylecorry.andromeda.core.ui.Views
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.andromeda.markdown.MarkdownService
import com.kylecorry.andromeda.pdf.PdfConvert
import com.kylecorry.andromeda.print.ColorMode
import com.kylecorry.andromeda.print.Orientation
import com.kylecorry.andromeda.print.Printer
import com.kylecorry.andromeda.print.ScaleMode
import com.kylecorry.luna.coroutines.onMain
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentSurvivalGuideBinding
import com.kylecorry.trail_sense.shared.debugging.isDebug
import com.kylecorry.trail_sense.shared.io.DeleteTempFilesCommand
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.shared.text.TextUtils
import com.kylecorry.trail_sense.tools.survival_guide.domain.Chapters
import kotlinx.coroutines.delay

class FragmentToolSurvivalGuideReader : BoundFragment<FragmentSurvivalGuideBinding>() {

    private var chapterResourceId: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        chapterResourceId = requireArguments().getInt("chapter_resource_id")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val chapters = Chapters.getChapters(requireContext())
        val chapter = chapters.firstOrNull { it.resource == chapterResourceId } ?: return
        binding.guideName.title.text = chapter.title
        binding.guideName.subtitle.text = chapter.chapter
        inBackground {
            val res = chapterResourceId ?: return@inBackground
            val content = onIO {
                TextUtils.loadTextFromResources(requireContext(), res)
            }
            if (isBound) {
                binding.guideScroll.removeAllViews()
                binding.guideScroll.addView(
                    TextUtils.getMarkdownView(
                        requireContext(),
                        content,
                        shouldUppercaseSubheadings = true
                    )
                )
            }
        }

        binding.guideName.rightButton.isVisible = isDebug()
        binding.guideName.rightButton.setOnClickListener {
            inBackground {
                onIO {
                    val loading =
                        onMain { Alerts.loading(requireContext(), getString(R.string.loading)) }
                    try {
                        // TODO: Extract this to a shared markdown printer
                        val printer = Printer(requireContext())
                        printer.setColorMode(ColorMode.Color)
                        printer.setScaleMode(ScaleMode.Fit)
                        printer.setOrientation(Orientation.Portrait)
                        val content =
                            "# ${chapter.chapter}: ${chapter.title}\n" + TextUtils.loadTextFromResources(
                                requireContext(),
                                chapterResourceId ?: return@onIO
                            )
                        val files = FileSubsystem.getInstance(requireContext())
                        val file = files.createTemp("pdf")
                        // TODO: Indented bullet points do not work properly when on a separate page
                        // TODO: Bullet point continuation does not render properly on a separate page
                        // TODO: Images are overlapping text
                        val markdown = MarkdownService(requireContext())
                        val parsed = markdown.toMarkdown(content)
                        val textView = Views.text(requireContext(), null) as TextView
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

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentSurvivalGuideBinding {
        return FragmentSurvivalGuideBinding.inflate(layoutInflater, container, false)
    }
}