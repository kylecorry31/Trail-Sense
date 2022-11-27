package com.kylecorry.trail_sense.weather.ui.charts

import android.content.Context
import android.util.AttributeSet
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.kylecorry.ceres.chart.Chart
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.R

class TemperatureChartPreference(context: Context, attributeSet: AttributeSet) :
    Preference(context, attributeSet) {

    private var chart: TemperatureChart? = null
    private var data: List<Reading<Float>> = emptyList()
    private var raw: List<Reading<Float>>? = null


    init {
        layoutResource = R.layout.preference_chart
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        holder.itemView.isClickable = false

        chart = TemperatureChart(holder.findViewById(R.id.chart) as Chart)
        chart?.plot(data, raw)
    }

    fun plot(data: List<Reading<Float>>, raw: List<Reading<Float>>? = null) {
        this.data = data
        this.raw = raw
        chart?.plot(data, raw)
    }
}