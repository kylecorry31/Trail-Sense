package com.kylecorry.survival_aid

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import java.util.*
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity(), Observer {

    private lateinit var compass: Compass
    private lateinit var azimuthTxt: TextView
    private lateinit var directionTxt: TextView
    private lateinit var needleImg: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bearing)
        compass = Compass(this)
        azimuthTxt = findViewById(R.id.compass_azimuth)
        directionTxt = findViewById(R.id.compass_direction)
        needleImg = findViewById(R.id.needle)
    }

    override fun onResume() {
        super.onResume()
        compass.start()
        compass.addObserver(this)
    }

    override fun onPause() {
        super.onPause()
        compass.stop()
        compass.deleteObserver(this)
    }

    override fun update(o: Observable?, arg: Any?) {
        val azimuthValue = compass.azimuth
        val azimuth = (azimuthValue.roundToInt() % 360).toString().padStart(3, ' ')
        val direction = compass.direction.symbol.toUpperCase().padEnd(2, ' ')
        azimuthTxt.text = "${azimuth}°"
        directionTxt.text = direction
        needleImg.rotation = -azimuthValue
    }
}
