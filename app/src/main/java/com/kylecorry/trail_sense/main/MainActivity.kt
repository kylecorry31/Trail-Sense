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
import android.view.inputmethod.InputMethodManager
import androidx.activity.enableEdgeToEdge
import androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import com.google.android.material.navigation.NavigationBarView
import com.google.android.material.textfield.TextInputEditText
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
import com.kylecorry.trail_sense.main.errors.SafeMode
import com.kylecorry.trail_sense.onboarding.OnboardingActivity
import com.kylecorry.trail_sense.receivers.RestartServicesCommand
import com.kylecorry.trail_sense.shared.CustomUiUtils.isDarkThemeOn
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
        updateTheme()

        val isBlackTheme =
            userPrefs.theme == UserPreferences.Theme.Black
                    || userPrefs.theme == UserPreferences.Theme.Night
                    || (isDarkThemeOn() && userPrefs.theme == UserPreferences.Theme.SystemBlack)

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
            binding.quickActionsSheet.close()
            updateBottomNavSelection()
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

        lastKnownFragment = getFragment()?.javaClass?.simpleName
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
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = insets.top
                leftMargin = insets.left
                rightMargin = insets.right
            }
            bottomInsets = insets.bottom
            setBottomNavLabelsVisibility()
            windowInsets
        }
    }

    private fun updateTheme(){
        val mode = when (userPrefs.theme) {
            UserPreferences.Theme.Light -> ColorTheme.Light
            UserPreferences.Theme.Dark, UserPreferences.Theme.Black, UserPreferences.Theme.Night -> ColorTheme.Dark
            UserPreferences.Theme.System, UserPreferences.Theme.SystemBlack -> ColorTheme.System
            UserPreferences.Theme.SunriseSunset -> sunriseSunsetTheme()
        }
        setColorTheme(mode, userPrefs.useDynamicColors)
    }

    fun reloadTheme() {
        updateTheme()
        cache.putBoolean("pref_theme_just_changed", true)
        recreate()
    }

    override fun onResume() {
        super.onResume()
        updateAllWidgets()
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

        if (shouldReloadNavigation) {
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
            NotifyForkPoliciesCommand(this)
        ).execute()

        if (!Tools.isToolAvailable(this, Tools.WEATHER)) {
            val item = binding.bottomNavigation.menu.findItem(R.id.action_weather)
            item?.isVisible = false
        }

        handleIntentAction(intent)

        if (SafeMode.isEnabled()) {
            Alerts.toast(
                this,
                getString(R.string.safe_mode_toast),
                false
            )
        }
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
                    tool.isOpen(navigationId ?: 0),
                    fragment
                )
            }
        }.firstOrNull()

        if (highPriorityAction != null) {
            return highPriorityAction.create(fragment)
        }

        val activeAction =
            tools.firstOrNull { it.isOpen(navigationId ?: 0) }?.volumeActions?.firstOrNull {
                it.isActive(this, true, fragment)
            }

        if (activeAction != null) {
            return activeAction.create(fragment)
        }

        val normalPriorityAction = tools.flatMap { tool ->
            tool.volumeActions.filter {
                it.priority == ToolVolumeActionPriority.Normal && it.isActive(
                    this,
                    tool.isOpen(navigationId ?: 0),
                    fragment
                )
            }
        }.firstOrNull()

        return normalPriorityAction?.create?.invoke(fragment)
    }

    private fun onVolumePressed(isVolumeUp: Boolean, isButtonPressed: Boolean): Boolean {
        val action = getVolumeAction()

        if (action != null) {
            return if (isButtonPressed) {
                action.onButtonPress(isVolumeUp)
            } else {
                action.onButtonRelease(isVolumeUp)
            }
        }

        return false
    }

    fun updateBottomNavigation() {
        setBottomNavLabelsVisibility()

        val bottomNavTools = userPrefs.bottomNavigationTools

        val tools = Tools.getTools(this)
        binding.bottomNavigation.menu.clear()
        bottomNavTools.forEachIndexed { index, toolId ->
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
            bottomNavTools.size,
            getString(R.string.tools)
        ).setIcon(R.drawable.apps)
            .setOnMenuItemClickListener {
                if (navController.currentDestination?.id == R.id.action_experimental_tools && !binding.quickActionsSheet.isOpen()) {
                    val searchinput = findViewById<TextInputEditText>(R.id.search_view_edit_text)
                    if (searchinput?.requestFocus() == true) {
                        val imm = getSystemService(InputMethodManager::class.java)
                        imm.showSoftInput(searchinput, InputMethodManager.SHOW_IMPLICIT)
                        return@setOnMenuItemClickListener true
                    }
                }
                false
            }

        // Loop through each item of the bottom navigation and override the long press behavior
        for (i in 0 until binding.bottomNavigation.menu.size()) {
            val item = binding.bottomNavigation.menu.getItem(i)
            val view = binding.bottomNavigation.findViewById<View>(item.itemId)
            view.setOnLongClickListener {
                binding.quickActionsSheet.show(this)
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

    private fun updateAllWidgets() {
        Tools.getTools(this).flatMap { it.widgets }.forEach {
            Tools.triggerWidgetUpdate(this, it.id)
        }
    }

    fun openWidgets() {
        binding.quickActionsSheet.show(this, 1)
    }

    fun setBottomNavigationEnabled(isEnabled: Boolean) {
        if (isEnabled) {
            binding.bottomNavigation.enable()
        } else {
            binding.bottomNavigation.disable()
        }
    }

    companion object {

        var lastKnownFragment: String? = null

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
