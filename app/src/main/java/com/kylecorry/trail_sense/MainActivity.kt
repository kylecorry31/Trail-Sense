package com.kylecorry.trail_sense

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.kylecorry.trail_sense.astronomy.infrastructure.receivers.SunsetAlarmReceiver
import com.kylecorry.trail_sense.astronomy.ui.AstronomyFragment
import com.kylecorry.trail_sense.tools.ui.ToolsFragment
import com.kylecorry.trail_sense.navigation.infrastructure.GeoUriParser
import com.kylecorry.trail_sense.navigation.ui.NavigatorFragment
import com.kylecorry.trail_sense.shared.DisclaimerMessage
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.doTransaction
import com.kylecorry.trail_sense.weather.infrastructure.WeatherAlarmScheduler
import com.kylecorry.trail_sense.weather.ui.BarometerFragment


class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavigation: BottomNavigationView

    private var geoIntentLocation: GeoUriParser.NamedCoordinate? = null

    private lateinit var userPrefs: UserPreferences
    private lateinit var disclaimer: DisclaimerMessage

    private val permissions = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, "android.permission.FLASHLIGHT")

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        userPrefs = UserPreferences(this)
        disclaimer = DisclaimerMessage(this)
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)

        val mode = when (userPrefs.theme){
            UserPreferences.Theme.Light -> AppCompatDelegate.MODE_NIGHT_NO
            UserPreferences.Theme.Dark -> AppCompatDelegate.MODE_NIGHT_YES
            UserPreferences.Theme.Black -> AppCompatDelegate.MODE_NIGHT_YES
            UserPreferences.Theme.System -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(mode)
        setContentView(R.layout.activity_main)
        bottomNavigation = findViewById(R.id.bottom_navigation)

        if (userPrefs.theme == UserPreferences.Theme.Black) {
            window.decorView.rootView.setBackgroundColor(Color.BLACK)
            bottomNavigation.setBackgroundColor(Color.BLACK)
        }


        if (!prefs.getBoolean(getString(R.string.pref_onboarding_completed), false)){
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish()
            return
        }

        if (!hasPermissions()){
            getPermission()
        } else {
            startApp()
        }
    }

    private fun startApp(){

        if (disclaimer.shouldShow()){
            disclaimer.show()
        }

        if(userPrefs.weather.shouldMonitorWeather) {
            WeatherAlarmScheduler.start(this)
        } else {
            WeatherAlarmScheduler.stop(this)
            val item: MenuItem = bottomNavigation.menu.findItem(R.id.action_weather)
            item.isVisible = false
        }

        if (!userPrefs.experimentalEnabled){
            val item: MenuItem = bottomNavigation.menu.findItem(R.id.action_experimental_tools)
            item.isVisible = false
        }

        val sunsetIntent = SunsetAlarmReceiver.intent(applicationContext)
        sendBroadcast(sunsetIntent)

        val intentData = intent.data
        if (intent.scheme == "geo" && intentData != null) {
            val namedCoordinate = GeoUriParser().parse(intentData)
            geoIntentLocation = namedCoordinate
            bottomNavigation.selectedItemId = R.id.action_navigation
        }

        if (intent.hasExtra(getString(R.string.extra_action))){
            val desiredAction = intent.getIntExtra(getString(R.string.extra_action), R.id.action_navigation)
            bottomNavigation.selectedItemId = desiredAction
        }

        syncFragmentWithSelection(bottomNavigation.selectedItemId)

        bottomNavigation.setOnNavigationItemSelectedListener { item ->
            syncFragmentWithSelection(item.itemId)
            true
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent == null){
            return
        }

        setIntent(intent)

        if (intent.hasExtra(getString(R.string.extra_action))){
            val desiredAction = intent.getIntExtra(getString(R.string.extra_action), R.id.action_navigation)
            bottomNavigation.selectedItemId = desiredAction
        }

        syncFragmentWithSelection(bottomNavigation.selectedItemId)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        bottomNavigation.selectedItemId = savedInstanceState.getInt("page",
            R.id.action_navigation
        )
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("page", bottomNavigation.selectedItemId)
    }

    private fun syncFragmentWithSelection(selection: Int){
        when (selection) {
            R.id.action_navigation -> {
                val namedCoord = geoIntentLocation
                if (namedCoord != null) {
                    geoIntentLocation = null
                    switchFragment(NavigatorFragment(null, namedCoord))
                } else {
                    switchFragment(NavigatorFragment())
                }
            }
            R.id.action_weather -> {
                switchFragment(BarometerFragment())
            }
            R.id.action_astronomy -> {
                switchFragment(AstronomyFragment())
            }
            R.id.action_experimental_tools -> {
                switchFragment(ToolsFragment())
            }
            R.id.action_settings -> {
                switchFragment(SettingsFragment())
            }
        }
    }

    private fun switchFragment(fragment: Fragment){
        supportFragmentManager.doTransaction {
            this.replace(R.id.fragment_holder, fragment)
        }
    }

    override fun onBackPressed() {
        val count = supportFragmentManager.backStackEntryCount

        if (count == 0) {
            super.onBackPressed()
            //additional code
        } else {
            supportFragmentManager.popBackStackImmediate()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        val granted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
        if (!granted){
            Toast.makeText(this, getString(R.string.not_all_permissions_granted), Toast.LENGTH_LONG).show()
            startApp()
        } else {
            startApp()
        }
    }

    private fun hasPermissions(): Boolean {
        for (permission in permissions){
            if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED){
                return false
            }
        }

        return true
    }

    private fun getPermission(){
        ActivityCompat.requestPermissions(this,
            permissions.toTypedArray(),
            1
        )
    }

    companion object {

        fun weatherIntent(context: Context): Intent {
            val intent = Intent(context, MainActivity::class.java)
            intent.putExtra(context.getString(R.string.extra_action), R.id.action_weather)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            return intent
        }

        fun astronomyIntent(context: Context): Intent {
            val intent = Intent(context, MainActivity::class.java)
            intent.putExtra(context.getString(R.string.extra_action), R.id.action_astronomy)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            return intent
        }
    }
    
}
