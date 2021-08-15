package com.kylecorry.trail_sense.settings.ui

import android.content.Context
import android.util.AttributeSet
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.github.mikephil.charting.charts.LineChart
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.weather.ui.PressureChart
import com.kylecorry.trailsensecore.domain.units.PressureUnits

class PressureChartPreference(context: Context, attributeSet: AttributeSet) : Preference(context, attributeSet) {

    private var chart: PressureChart? = null
    private var data: List<Pair<Number, Number>> = listOf()
    private var units = PressureUnits.Hpa


    init {
        layoutResource = R.layout.preference_pressure_chart
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        holder.itemView.isClickable = false

        chart = PressureChart(holder.findViewById(R.id.chart) as LineChart, Resources.color(context, R.color.colorPrimary))
        chart?.setUnits(units)
        chart?.plot(data)
    }

    fun plot(data: List<Pair<Number, Number>>){
        this.data = data
        chart?.plot(data)
    }

    fun setUnits(units: PressureUnits){
        this.units = units
        chart?.setUnits(units)
    }

}