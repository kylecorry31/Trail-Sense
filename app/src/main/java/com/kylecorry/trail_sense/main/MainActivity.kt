package com.kylecorry.trail_sense.main

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import androidx.activity.enableEdgeToEdge
import androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import com.google.android.material.navigation.NavigationBarView
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.system.Screen
import com.kylecorry.andromeda.core.tryOrNothing
import com.kylecorry.andromeda.fragments.AndromedaActivity
import com.kylecorry.andromeda.fragments.AndromedaFragment
import com.kylecorry.andromeda.fragments.ColorTheme
import com.kylecorry.andromeda.permissions.Permissions
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.ActivityMainBinding
import com.kylecorry.trail_sense.main.errors.ExceptionHandler
import com.kylecorry.trail_sense.onboarding.OnboardingActivity
import com.kylecorry.trail_sense.receivers.RestartServicesCommand
import com.kylecorry.trail_sense.settings.backup.BackupService
import com.kylecorry.trail_sense.settings.ui.SettingsMoveNotice
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.commands.ComposedCommand
import com.kylecorry.trail_sense.shared.extensions.findNavController
import com.kylecorry.trail_sense.shared.navigation.NavigationUtils.setupWithNavController
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.sensors.SensorSubsystem
import com.kylecorry.trail_sense.shared.views.ErrorBannerView
import com.kylecorry.trail_sense.shared.volume.VolumeAction
import com.kylecorry.trail_sense.tools.astronomy.domain.AstronomyService
import com.kylecorry.trail_sense.tools.battery.BatteryToolRegistration
import com.kylecorry.trail_sense.tools.battery.infrastructure.commands.PowerSavingModeAlertCommand
import com.kylecorry.trail_sense.tools.flashlight.infrastructure.FlashlightSubsystem
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.subsystem.PedometerSubsystem
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolVolumeActionPriority
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.quickactions.MainActivityQuickActionBinder
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AndromedaActivity() {

    private var _binding: ActivityMainBinding? = null
    private val binding: ActivityMainBinding
        get() = _binding!!

    private val navController: NavController
        get() = findNavController()

    val errorBanner: ErrorBannerView
        get() = binding.errorBanner

    private lateinit var userPrefs: UserPreferences
    private val cache by lazy { PreferencesSubsystem.getInstance(this).preferences }

    private val permissions = mutableListOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    private var bottomInsets = 0

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        ExceptionHandler.initialize(this)

        userPrefs = UserPreferences(applicationContext)
        val mode = when (userPrefs.theme) {
            UserPreferences.Theme.Light -> ColorTheme.Light
            UserPreferences.Theme.Dark, UserPreferences.Theme.Black, UserPreferences.Theme.Night -> ColorTheme.Dark
            UserPreferences.Theme.System -> ColorTheme.System
            UserPreferences.Theme.SunriseSunset -> sunriseSunsetTheme()
        }
        val isBlackTheme =
            userPrefs.theme == UserPreferences.Theme.Black || userPrefs.theme == UserPreferences.Theme.Night
        setColorTheme(mode, userPrefs.useDynamicColors)
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        Screen.setAllowScreenshots(window, !userPrefs.privacy.isScreenshotProtectionOn)

        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(_binding?.root)

        // Handle black theme
        if (isBlackTheme) {
            window.decorView.rootView.setBackgroundColor(Color.BLACK)
            binding.bottomNavigation.setBackgroundColor(Color.BLACK)
        }

        if (userPrefs.theme == UserPreferences.Theme.Night) {
            binding.colorFilter.setColorFilter(
                PorterDuffColorFilter(
                    Color.RED,
                    PorterDuff.Mode.MULTIPLY
                )
            )
        }

        updateBottomNavigation()

        navController.addOnDestinationChangedListener { _, _, _ ->
            // Reset the quick actions
            binding.quickActions.removeAllViews()
            binding.quickActionsSheet.isVisible = false

            updateBottomNavSelection()
        }

        binding.quickActionsToolbar.rightButton.setOnClickListener {
            binding.quickActionsSheet.isVisible = false
        }

        bindLayoutInsets()

        if (cache.getBoolean(getString(R.string.pref_onboarding_completed)) != true) {
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish()
            return
        }

        val previousPermissionStatus = permissions.map {
            Permissions.hasPermission(this, it)
        }
        requestPermissions(permissions) {
            val currentPermissionStatus = permissions.map {
                Permissions.hasPermission(this, it)
            }
            val permissionsChanged =
                previousPermissionStatus.zip(currentPermissionStatus).any { it.first != it.second }
            startApp(permissionsChanged)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    /**
     * Update the bottom nav selection based on the current navigation destination
     */
    private fun updateBottomNavSelection() {
        val currentNavId = navController.currentDestination?.id
        val tools = Tools.getTools(this)
        val selectedTool = tools.firstOrNull { it.isOpen(currentNavId ?: 0) }
        // If the tool appears in the bottom nav, select it. Otherwise, select the Tools tab
        val idToSelect = if (selectedTool != null) {
            val isToolPinnedToNav =
                binding.bottomNavigation.menu.findItem(selectedTool.navAction) != null
            if (isToolPinnedToNav) {
                selectedTool.navAction
            } else {
                R.id.action_experimental_tools
            }
        } else {
            R.id.action_experimental_tools
        }

        binding.bottomNavigation.menu.findItem(idToSelect)?.isChecked = true
    }

    fun changeBottomNavLabelsVisibility(useCompactMode: Boolean) {
        userPrefs.useCompactMode = useCompactMode
        setBottomNavLabelsVisibility()
    }

    private fun setBottomNavLabelsVisibility() {
        binding.bottomNavigation.apply {
            if (userPrefs.useCompactMode) {
                layoutParams.height = Resources.dp(context, 55f).toInt() + bottomInsets
                labelVisibilityMode = NavigationBarView.LABEL_VISIBILITY_UNLABELED
            } else {
                layoutParams.height = LayoutParams.WRAP_CONTENT
                labelVisibilityMode = NavigationBarView.LABEL_VISIBILITY_AUTO
            }
        }
    }

    private fun bindLayoutInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = insets.top
            }
            bottomInsets = insets.bottom
            setBottomNavLabelsVisibility()
            windowInsets
        }
    }

    fun reloadTheme() {
        cache.putBoolean("pref_theme_just_changed", true)
        recreate()
    }

    override fun onResume() {
        super.onResume()
        FlashlightSubsystem.getInstance(this).startSystemMonitor()
        PedometerSubsystem.getInstance(this).recalculateState()
        Tools.subscribe(
            BatteryToolRegistration.BROADCAST_POWER_SAVING_MODE_ENABLED,
            ::onPowerSavingModeChanged
        )
        Tools.subscribe(
            BatteryToolRegistration.BROADCAST_POWER_SAVING_MODE_DISABLED,
            ::onPowerSavingModeChanged
        )
    }

    override fun onPause() {
        super.onPause()
        FlashlightSubsystem.getInstance(this).stopSystemMonitor()
        Tools.unsubscribe(
            BatteryToolRegistration.BROADCAST_POWER_SAVING_MODE_ENABLED,
            ::onPowerSavingModeChanged
        )
        Tools.unsubscribe(
            BatteryToolRegistration.BROADCAST_POWER_SAVING_MODE_DISABLED,
            ::onPowerSavingModeChanged
        )
    }

    private fun startApp(shouldReloadNavigation: Boolean) {
        if (cache.getBoolean("pref_theme_just_changed") == true) {
            cache.putBoolean("pref_theme_just_changed", false)
            recreate()
        }

        errorBanner.dismissAll()

        if (cache.getBoolean(BackupService.RECENTLY_BACKED_UP_KEY) == true) {
            cache.remove(BackupService.RECENTLY_BACKED_UP_KEY)
            navController.navigate(R.id.action_settings)
        } else if (shouldReloadNavigation) {
            navController.navigate(
                binding.bottomNavigation.selectedItemId,
                null,
                NavOptions.Builder().setPopUpTo(
                    navController.currentDestination?.id ?: R.id.action_experimental_tools, true
                ).build()
            )
        }

        ComposedCommand(
            ShowDisclaimerCommand(this),
            PowerSavingModeAlertCommand(this),
            RestartServicesCommand(this, false),
            SettingsMoveNotice(this)
        ).execute()

        if (!Tools.isToolAvailable(this, Tools.WEATHER)) {
            val item = binding.bottomNavigation.menu.findItem(R.id.action_weather)
            item?.isVisible = false
        }

        handleIntentAction(intent)
    }

    private fun handleIntentAction(intent: Intent) {
        val tools = Tools.getTools(this)
        tools.forEach { tool ->
            tool.intentHandlers.forEach { handler ->
                if (handler.handle(this, intent)) {
                    return
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent ?: return
        setIntent(intent)
        handleIntentAction(intent)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        binding.bottomNavigation.selectedItemId = savedInstanceState.getInt(
            "page",
            binding.bottomNavigation.menu.getItem(0).itemId
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
        outState.putInt("page", binding.bottomNavigation.selectedItemId)
        navController.currentBackStackEntry?.arguments?.let {
            outState.putBundle("navigation_arguments", it)
        }
        navController.currentDestination?.id?.let {
            outState.putInt("navigation", it)
        }
    }

    private fun sunriseSunsetTheme(): ColorTheme {
        val astronomyService = AstronomyService()
        val location = SensorSubsystem.getInstance(this).lastKnownLocation
        if (location == Coordinate.zero) {
            return ColorTheme.System
        }
        val isSunUp = astronomyService.isSunUp(location)
        return if (isSunUp) {
            ColorTheme.Light
        } else {
            ColorTheme.Dark
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

    private fun getVolumeAction(): VolumeAction? {
        val navigationId = navController.currentDestination?.id
        val fragment = getFragment() as? AndromedaFragment ?: return null
        val tools = Tools.getTools(this)

        // Sort order = High priority active, open tool, normal priority active

        val highPriorityAction = tools.flatMap { tool ->
            tool.volumeActions.filter {
                it.priority == ToolVolumeActionPriority.High && it.isActive(
                    this,
                    tool.isOpen(navigationId ?: 0)
                )
            }
        }.firstOrNull()

        if (highPriorityAction != null) {
            return highPriorityAction.create(fragment)
        }

        val activeAction =
            tools.firstOrNull { it.isOpen(navigationId ?: 0) }?.volumeActions?.firstOrNull {
                it.isActive(this, true)
            }

        if (activeAction != null) {
            return activeAction.create(fragment)
        }

        val normalPriorityAction = tools.flatMap { tool ->
            tool.volumeActions.filter {
                it.priority == ToolVolumeActionPriority.Normal && it.isActive(
                    this,
                    tool.isOpen(navigationId ?: 0)
                )
            }
        }.firstOrNull()

        return normalPriorityAction?.create?.invoke(fragment)
    }

    private fun onVolumePressed(isVolumeUp: Boolean, isButtonPressed: Boolean): Boolean {
        val action = getVolumeAction()

        if (action != null) {
            return if (isButtonPressed) {
                action.onButtonPress()
            } else {
                action.onButtonRelease()
            }
        }

        return false
    }

    fun updateBottomNavigation() {
        setBottomNavLabelsVisibility()

        val bottomNavTools = userPrefs.bottomNavigationTools

        val tools = Tools.getTools(this)
        binding.bottomNavigation.menu.clear()
        bottomNavTools.take(4).forEachIndexed { index, toolId ->
            val toolItem = tools.firstOrNull { it.id == toolId } ?: return@forEachIndexed
            binding.bottomNavigation.menu.add(
                Menu.NONE,
                toolItem.navAction,
                index,
                toolItem.name
            ).setIcon(toolItem.icon)
        }

        binding.bottomNavigation.menu.add(
            Menu.NONE,
            R.id.action_experimental_tools,
            4,
            getString(R.string.tools)
        ).setIcon(R.drawable.apps)

        // Loop through each item of the bottom navigation and override the long press behavior
        for (i in 0 until binding.bottomNavigation.menu.size()) {
            val item = binding.bottomNavigation.menu.getItem(i)
            val view = binding.bottomNavigation.findViewById<View>(item.itemId)
            view.setOnLongClickListener {
                val fragment = getFragment()

                if (fragment == null) {
                    Alerts.toast(this, getString(R.string.quick_actions_are_unavailable))
                    return@setOnLongClickListener true
                }

                MainActivityQuickActionBinder(fragment, binding).bind()
                binding.quickActionsSheet.isVisible = true
                true
            }
        }

        // Open the left most item by default (and clear the back stack)
        val leftMostItem = binding.bottomNavigation.menu.getItem(0)

        // Only initialize the nav graph once
        effect("navGraph") {
            initializeNavGraph(leftMostItem.itemId)
        }
        // Bind to navigation
        binding.bottomNavigation.setupWithNavController(navController, false)

        updateBottomNavSelection()
    }

    private fun initializeNavGraph(startDestination: Int) {
        val navGraph = navController.navInflater.inflate(R.navigation.nav_graph)
        navGraph.setStartDestination(startDestination)
        navController.graph = navGraph
    }

    private fun onPowerSavingModeChanged(data: Bundle): Boolean {
        recreate()
        return true
    }

    companion object {
        fun intent(context: Context): Intent {
            return Intent(context, MainActivity::class.java)
        }

        fun pendingIntent(context: Context): PendingIntent {
            return PendingIntent.getActivity(
                context,
                27383254,
                intent(context),
                PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }

}
