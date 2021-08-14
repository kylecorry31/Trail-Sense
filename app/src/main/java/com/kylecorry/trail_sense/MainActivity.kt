package com.kylecorry.trail_sense

import android.Manifest
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.MenuItem
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.bundleOf
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.kylecorry.andromeda.core.system.PackageUtils
import com.kylecorry.andromeda.core.system.ScreenService
import com.kylecorry.andromeda.core.tryOrNothing
import com.kylecorry.andromeda.core.units.Coordinate
import com.kylecorry.andromeda.markdown.MarkdownService
import com.kylecorry.andromeda.permissions.PermissionService
import com.kylecorry.andromeda.permissions.requestPermissions
import com.kylecorry.andromeda.preferences.Preferences
import com.kylecorry.andromeda.sense.SensorChecker
import com.kylecorry.trail_sense.astronomy.domain.AstronomyService
import com.kylecorry.trail_sense.navigation.domain.MyNamedCoordinate
import com.kylecorry.trail_sense.onboarding.OnboardingActivity
import com.kylecorry.trail_sense.receivers.TrailSenseServiceUtils
import com.kylecorry.trail_sense.shared.DisclaimerMessage
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.views.ErrorBannerView
import com.kylecorry.trail_sense.volumeactions.FlashlightToggleVolumeAction
import com.kylecorry.trail_sense.volumeactions.VolumeAction
import com.kylecorry.trailsensecore.infrastructure.system.*
import java.time.Duration
import kotlin.system.exitProcess


class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController
    private lateinit var bottomNavigation: BottomNavigationView
    val errorBanner: ErrorBannerView by lazy { findViewById(R.id.error_banner) }

    private var geoIntentLocation: GeoUriParser.NamedCoordinate? = null

    private val sensorChecker by lazy { SensorChecker(this) }
    private val permissionService by lazy { PermissionService(this) }

    private lateinit var userPrefs: UserPreferences
    private lateinit var disclaimer: DisclaimerMessage
    private val cache by lazy { Preferences(this) }

    private val permissions = mutableListOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        "android.permission.FLASHLIGHT"
    )

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            permissions.add(Manifest.permission.FOREGROUND_SERVICE)
        }

        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        ExceptionUtils.onUncaughtException(Duration.ofMinutes(1)) {
            it.printStackTrace()
            UiUtils.alertWithCancel(
                this@MainActivity,
                getString(R.string.error_occurred),
                getString(R.string.error_occurred_message),
                getString(R.string.pref_email_title),
                getString(R.string.dialog_cancel)
            ) { cancelled ->
                if (cancelled) {
                    exitProcess(2)
                } else {
                    ExceptionUtils.report(
                        this@MainActivity,
                        it,
                        getString(R.string.email),
                        getString(R.string.app_name)
                    )
                }
            }
        }

        NotificationChannels.createChannels(applicationContext)

        userPrefs = UserPreferences(this)
        val mode = when (userPrefs.theme) {
            UserPreferences.Theme.Light -> AppCompatDelegate.MODE_NIGHT_NO
            UserPreferences.Theme.Dark -> AppCompatDelegate.MODE_NIGHT_YES
            UserPreferences.Theme.Black -> AppCompatDelegate.MODE_NIGHT_YES
            UserPreferences.Theme.System -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            UserPreferences.Theme.SunriseSunset -> sunriseSunsetTheme()
        }
        AppCompatDelegate.setDefaultNightMode(mode)
        super.onCreate(savedInstanceState)

        ScreenService(window).setAllowScreenshots(!userPrefs.privacy.isScreenshotProtectionOn)

        disclaimer = DisclaimerMessage(this)
        val cache = Preferences(this)

        setContentView(R.layout.activity_main)
        navController =
            (supportFragmentManager.findFragmentById(R.id.fragment_holder) as NavHostFragment).navController
        bottomNavigation = findViewById(R.id.bottom_navigation)
        bottomNavigation.setupWithNavController(navController)

        if (userPrefs.theme == UserPreferences.Theme.Black) {
            window.decorView.rootView.setBackgroundColor(Color.BLACK)
            bottomNavigation.setBackgroundColor(Color.BLACK)
        }

        PackageUtils.setComponentEnabled(
            this,
            "com.kylecorry.trail_sense.AliasMainActivity",
            userPrefs.navigation.areMapsEnabled
        )

        if (cache.getBoolean(getString(R.string.pref_onboarding_completed)) != true) {
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish()
            return
        }

        requestPermissions(permissions, RequestCodes.REQUEST_CODE_LOCATION_PERMISSION)
    }

    private fun startApp() {
        errorBanner.dismissAll()
        if (navController.currentDestination?.id == R.id.action_navigation) {
            navController.navigate(R.id.action_navigation)
        }

        if (disclaimer.shouldShow()) {
            disclaimer.show()
        }

        if (userPrefs.isLowPowerModeOn) {
            UiUtils.shortToast(this, getString(R.string.low_power_mode_on_message))
        }

        TrailSenseServiceUtils.restartServices(this)

        if (!sensorChecker.hasBarometer()) {
            val item: MenuItem = bottomNavigation.menu.findItem(R.id.action_weather)
            item.isVisible = false
        }

        handleIntentAction(intent)
    }

    private fun handleIntentAction(intent: Intent) {
        val intentData = intent.data
        if (intent.scheme == "geo" && intentData != null) {
            val namedCoordinate = GeoUriParser().parse(intentData)
            geoIntentLocation = namedCoordinate
            bottomNavigation.selectedItemId = R.id.action_navigation
            if (namedCoordinate != null) {
                val bundle = bundleOf("initial_location" to MyNamedCoordinate.from(namedCoordinate))
                navController.navigate(
                    R.id.beacon_list,
                    bundle
                )
            }
        } else if ((intent.type?.startsWith("image/") == true || intent.type?.startsWith("application/pdf") == true) && userPrefs.navigation.areMapsEnabled) {
            bottomNavigation.selectedItemId = R.id.action_experimental_tools
            val intentUri = intent.clipData?.getItemAt(0)?.uri
            val bundle = bundleOf("map_intent_uri" to intentUri)
            navController.navigate(R.id.action_tools_to_maps_list, bundle)
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent == null) {
            return
        }

        setIntent(intent)
        handleIntentAction(intent)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        bottomNavigation.selectedItemId = savedInstanceState.getInt(
            "page",
            R.id.action_navigation
        )
        if (savedInstanceState.containsKey("navigation")) {
            tryOrNothing {
                val bundle = savedInstanceState.getBundle("navigation_arguments")
                navController.navigate(savedInstanceState.getInt("navigation"), bundle)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("page", bottomNavigation.selectedItemId)
        navController.currentBackStackEntry?.arguments?.let {
            outState.putBundle("navigation_arguments", it)
        }
        navController.currentDestination?.id?.let {
            outState.putInt("navigation", it)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == RequestCodes.REQUEST_CODE_LOCATION_PERMISSION && shouldRequestBackgroundLocation()) {
            requestBackgroundLocation()
        } else if (requestCode == RequestCodes.REQUEST_CODE_LOCATION_PERMISSION || requestCode == RequestCodes.REQUEST_CODE_BACKGROUND_LOCATION_PERMISSION) {
            startApp()
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun hasBackgroundLocation(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || permissionService.hasPermission(
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        )
    }

    private fun shouldRequestBackgroundLocation(): Boolean {
        return permissionService.canGetFineLocation() &&
                !hasBackgroundLocation() &&
                cache.getBoolean(Manifest.permission.ACCESS_BACKGROUND_LOCATION) != true
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun requestBackgroundLocation() {
        cache.putBoolean(Manifest.permission.ACCESS_BACKGROUND_LOCATION, true)

        val markdown = MarkdownService(this)
        val contents = markdown.toMarkdown(getString(R.string.access_background_location_rationale))

        PermissionUtils.requestPermissionsWithRationale(
            this,
            listOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION), PermissionRationale(
                getString(R.string.access_background_location),
                contents
            ),
            RequestCodes.REQUEST_CODE_BACKGROUND_LOCATION_PERMISSION,
            getString(R.string.dialog_grant),
            getString(R.string.dialog_deny)
        )
    }

    private fun sunriseSunsetTheme(): Int {
        val astronomyService = AstronomyService()
        val sensorService by lazy { SensorService(applicationContext) }
        val gps by lazy { sensorService.getGPS() }
        if (gps.location == Coordinate.zero) {
            return AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        val isSunUp = astronomyService.isSunUp(gps.location)
        return if (isSunUp) {
            AppCompatDelegate.MODE_NIGHT_NO
        } else {
            AppCompatDelegate.MODE_NIGHT_YES
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            return onVolumePressed(isVolumeUp = false, isButtonPressed = true)
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            return onVolumePressed(isVolumeUp = true, isButtonPressed = true)
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            return onVolumePressed(isVolumeUp = false, isButtonPressed = false)
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            return onVolumePressed(isVolumeUp = true, isButtonPressed = false)
        }
        return super.onKeyUp(keyCode, event)
    }

    private fun onVolumePressed(isVolumeUp: Boolean, isButtonPressed: Boolean): Boolean {
        if (!shouldOverrideVolumePress()) {
            return false
        }

        val action =
            (if (isVolumeUp) getVolumeUpAction() else getVolumeDownAction()) ?: return false

        if (isButtonPressed) {
            action.onButtonDown()
        } else {
            action.onButtonUp()
        }

        return true
    }

    private fun shouldOverrideVolumePress(): Boolean {
        val excluded = listOf(R.id.toolWhistleFragment, R.id.fragmentToolWhiteNoise)
        if (excluded.contains(navController.currentDestination?.id)) {
            return false
        }
        return true
    }


    private fun getVolumeDownAction(): VolumeAction? {
        if (userPrefs.flashlight.toggleWithVolumeButtons) {
            return FlashlightToggleVolumeAction(this)
        }

        return null
    }

    private fun getVolumeUpAction(): VolumeAction? {
        if (userPrefs.flashlight.toggleWithVolumeButtons) {
            return FlashlightToggleVolumeAction(this)
        }

        return null
    }


}
