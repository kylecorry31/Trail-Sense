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
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.kylecorry.trail_sense.astronomy.infrastructure.receivers.SunsetAlarmReceiver
import com.kylecorry.trail_sense.astronomy.ui.AstronomyFragment
import com.kylecorry.trail_sense.tools.ui.ToolsFragment
import com.kylecorry.trail_sense.navigation.ui.NavigatorFragment
import com.kylecorry.trail_sense.shared.*
import com.kylecorry.trail_sense.weather.infrastructure.WeatherUpdateScheduler
import com.kylecorry.trail_sense.weather.ui.BarometerFragment
import com.kylecorry.trailsensecore.infrastructure.persistence.Cache
import com.kylecorry.trailsensecore.infrastructure.system.GeoUriParser
import com.kylecorry.trailsensecore.infrastructure.system.UiUtils


class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavigation: BottomNavigationView

    private var geoIntentLocation: GeoUriParser.NamedCoordinate? = null

    private lateinit var userPrefs: UserPreferences
    private lateinit var disclaimer: DisclaimerMessage
    private val cache by lazy { Cache(this) }

    private val permissions = mutableListOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        "android.permission.FLASHLIGHT"
    )

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            permissions.add(Manifest.permission.FOREGROUND_SERVICE)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        userPrefs = UserPreferences(this)
        val mode = when (userPrefs.theme) {
            UserPreferences.Theme.Light -> AppCompatDelegate.MODE_NIGHT_NO
            UserPreferences.Theme.Dark -> AppCompatDelegate.MODE_NIGHT_YES
            UserPreferences.Theme.Black -> AppCompatDelegate.MODE_NIGHT_YES
            UserPreferences.Theme.System -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(mode)
        super.onCreate(savedInstanceState)

        disclaimer = DisclaimerMessage(this)
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)

        setContentView(R.layout.activity_main)
        bottomNavigation = findViewById(R.id.bottom_navigation)

        if (userPrefs.theme == UserPreferences.Theme.Black) {
            window.decorView.rootView.setBackgroundColor(Color.BLACK)
            bottomNavigation.setBackgroundColor(Color.BLACK)
        }


        if (!prefs.getBoolean(getString(R.string.pref_onboarding_completed), false)) {
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish()
            return
        }

        requestPermissions(permissions)
    }

    private fun startApp() {

        if (disclaimer.shouldShow()) {
            disclaimer.show()
        }

        if (userPrefs.weather.shouldMonitorWeather) {
            WeatherUpdateScheduler.start(this)
        } else {
            WeatherUpdateScheduler.stop(this)
            val item: MenuItem = bottomNavigation.menu.findItem(R.id.action_weather)
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

        if (intent.hasExtra(getString(R.string.extra_action))) {
            val desiredAction =
                intent.getIntExtra(getString(R.string.extra_action), R.id.action_navigation)
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
        if (intent == null) {
            return
        }

        setIntent(intent)

        if (intent.hasExtra(getString(R.string.extra_action))) {
            val desiredAction =
                intent.getIntExtra(getString(R.string.extra_action), R.id.action_navigation)
            bottomNavigation.selectedItemId = desiredAction
        }

        syncFragmentWithSelection(bottomNavigation.selectedItemId)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        bottomNavigation.selectedItemId = savedInstanceState.getInt(
            "page",
            R.id.action_navigation
        )
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("page", bottomNavigation.selectedItemId)
    }

    private fun syncFragmentWithSelection(selection: Int) {
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

    private fun switchFragment(fragment: Fragment) {
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
        if (requestCode == 1 && shouldRequestBackgroundLocation()) {
            requestBackgroundLocation()
        } else {
            startApp()
        }
    }

    private fun hasBackgroundLocation(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || PermissionUtils.hasPermission(
            this,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        )
    }

    private fun shouldRequestBackgroundLocation(): Boolean {
        return PermissionUtils.isLocationEnabled(this) &&
                !hasBackgroundLocation() &&
                cache.getBoolean(Manifest.permission.ACCESS_BACKGROUND_LOCATION) != true
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun requestBackgroundLocation() {
        cache.putBoolean(Manifest.permission.ACCESS_BACKGROUND_LOCATION, true)
        PermissionUtils.requestPermissionsWithRationale(
            this,
            listOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION), PermissionRationale(
                getString(R.string.access_background_location),
                getString(R.string.access_background_location_rationale)
            ),
            2
        )
    }

    private fun requestPermissions(permissions: List<String>, requestCode: Int = 1) {
        PermissionUtils.requestPermissions(this, permissions, requestCode)
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
