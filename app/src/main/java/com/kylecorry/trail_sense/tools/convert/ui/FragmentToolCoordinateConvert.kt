package com.kylecorry.trail_sense.tools.convert.ui

import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import com.kylecorry.andromeda.core.ui.useService
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.domain.BuiltInCoordinateFormat
import com.kylecorry.trail_sense.shared.extensions.TrailSenseReactiveFragment
import com.kylecorry.trail_sense.shared.extensions.useCoordinateInputView

class FragmentToolCoordinateConvert :
    TrailSenseReactiveFragment(R.layout.fragment_tool_coordinate_convert) {

    private val formats = BuiltInCoordinateFormat.entries

    override fun update() {
        val context = useAndroidContext()

        // Views
        val coordinateView = useCoordinateInputView(R.id.coordinate_edit, lifecycleHookTrigger)
        val toUnitsView = useView<Spinner>(R.id.to_units)
        val resultView = useView<TextView>(R.id.result)

        // Services
        val formatter = useService<FormatService>()

        // State
        val (coordinate, setCoordinate) = useState<Coordinate?>(coordinateView.coordinate)
        val (formatIndex, setFormatIndex) = useState(0)

        useEffect(coordinateView) {
            setCoordinate(coordinateView.coordinate)
            coordinateView.setOnCoordinateChangeListener {
                setCoordinate(it)
            }
        }

        useEffect(toUnitsView, context, formatter) {
            toUnitsView.adapter = ArrayAdapter(
                context,
                R.layout.spinner_item_plain,
                R.id.item_name,
                formats.map { formatter.formatCoordinateType(it) }
            )
            toUnitsView.prompt = getString(R.string.distance_from)
            toUnitsView.setSelection(formatIndex)
            toUnitsView.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    setFormatIndex(position)
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    setFormatIndex(toUnitsView.selectedItemPosition)
                }
            }
        }

        useEffect(resultView, coordinate, formatIndex, formatter) {
            resultView.text = coordinate?.let {
                formatter.formatLocation(it, formats[formatIndex])
            } ?: ""
        }
    }
}
