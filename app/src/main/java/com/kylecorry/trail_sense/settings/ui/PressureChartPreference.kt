package com.kylecorry.trail_sense.settings.ui

import android.content.Context
import android.util.AttributeSet
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.github.mikephil.charting.charts.LineChart
import com.kylecorry.sol.units.Pressure
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.weather.ui.PressureChart

class PressureChartPreference(context: Context, attributeSet: AttributeSet) : Preference(context, attributeSet) {

    private var chart: PressureChart? = null
    private var data: List<Reading<Pressure>> = listOf()


    init {
        layoutResource = R.layout.preference_pressure_chart
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        holder.itemView.isClickable = false

        chart = PressureChart(holder.findViewById(R.id.chart) as LineChart)
        chart?.plot(data)
    }

    fun plot(data: List<Reading<Pressure>>){
        this.data = data
        chart?.plot(data)
    }
}