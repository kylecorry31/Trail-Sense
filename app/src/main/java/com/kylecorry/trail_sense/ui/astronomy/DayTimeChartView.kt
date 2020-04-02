package com.kylecorry.trail_sense.ui.astronomy

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import com.github.mikephil.charting.charts.BarChart
import com.kylecorry.trail_sense.R
import java.time.Duration
import java.time.LocalTime

class DayTimeChartView(context: Context, attrs: AttributeSet): ConstraintLayout(context, attrs) {

    private val chartView: BarChart
    private val chart: MpStackedBarChart
    private val image: ImageView
    private val cursor: View

    private var lastPct: Float = 0f
    private var colors: List<Int> = listOf(Color.BLACK, Color.WHITE)
    private var imageTint = Color.BLACK

    init {
        View.inflate(context, R.layout.view_day_time_chart, this)

        image = findViewById(R.id.day_chart_img)
        chartView = findViewById(R.id.chart)
        chart =
            MpStackedBarChart(chartView)
        cursor = findViewById(R.id.day_chart_cursor)

        val attributes = context.obtainStyledAttributes(attrs, R.styleable.DayTimeChartView)
        setCursorImageDrawable(attributes.getDrawable(R.styleable.DayTimeChartView_cursorImage))
        setCursorImageColor(attributes.getColor(R.styleable.DayTimeChartView_cursorImageColor, Color.BLACK))

        setCursorLineColor(attributes.getColor(R.styleable.DayTimeChartView_cursorLineColor, Color.BLACK))
        setCursorLineWidth(attributes.getDimension(R.styleable.DayTimeChartView_cursorLineWidth, 0f).toInt())

        attributes.recycle()
    }

    fun setCursorImageResource(imageId: Int){
        image.setImageResource(imageId)
        setCursorImageColor(imageTint)
    }

    fun setCursorImageDrawable(drawable: Drawable?){
        image.setImageDrawable(drawable)
        setCursorImageColor(imageTint)
    }

    fun setCursorImageColor(color: Int){
        image.drawable.setTint(color)
        imageTint = color
    }

    fun setCursorLineColor(color: Int){
        cursor.background.setTint(color)
    }

    fun setCursorLineWidth(width: Int){
        cursor.layoutParams.width = width
        moveCursors(lastPct)
    }

    fun setColors(colors: List<Int>){
        this.colors = colors
    }

    fun display(times: List<LocalTime>, current: LocalTime = LocalTime.now()){
        val sortedTimes = times.sorted().toMutableList()
        sortedTimes.add(LocalTime.MAX)

        val timesUntil = sortedTimes
            .map { Duration.between(LocalTime.MIN, it) }
            .map { if (it.isNegative) 0 else it.seconds }

        val durations = mutableListOf<Long>()

        var cumulativeTime = 0L

        for (time in timesUntil) {
            val dt = time - cumulativeTime
            durations.add(dt)
            cumulativeTime += dt
        }

        val pct = getPercentOfDay(current)

        moveCursors(pct)

        chart.plot(durations, colors)
    }

    private fun moveCursors(pct: Float) {
        cursor.x = chartView.width * pct + chartView.x - cursor.width / 2f
        image.x = chartView.width * pct + chartView.x - image.width / 2f
        lastPct = pct
    }


    private fun getPercentOfDay(current: LocalTime): Float {
        val duration = Duration.between(LocalTime.MIN, current).seconds
        return duration / Duration.ofDays(1).seconds.toFloat()
    }

}