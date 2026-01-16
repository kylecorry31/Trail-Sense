package com.kylecorry.trail_sense.main.errors

import android.widget.Button
import android.widget.TextView
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.clipboard.Clipboard
import com.kylecorry.andromeda.core.system.CurrentApp
import com.kylecorry.andromeda.core.system.Intents
import com.kylecorry.andromeda.fragments.useArgument
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.extensions.TrailSenseReactiveFragment
import com.kylecorry.trail_sense.shared.extensions.useNavController
import com.kylecorry.trail_sense.shared.openTool
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

class ErrorHandlerFragment : TrailSenseReactiveFragment(R.layout.fragment_tool_error_handler) {

    override fun update() {
        val toolId = useArgument<Long>("tool_id") ?: 0L
        val errorDetails = useArgument<String>("error") ?: ""

        val errorMessageTextView = useView<TextView>(R.id.error_message)
        val emailButton = useView<Button>(R.id.email_developer)
        val copyButton = useView<Button>(R.id.copy_error)
        val detailsButton = useView<Button>(R.id.view_error_details)
        val reopenButton = useView<Button>(R.id.reopen_tool)
        val restartButton = useView<Button>(R.id.restart_app)
        val settingsButton = useView<Button>(R.id.open_settings)

        val context = useAndroidContext()
        val navController = useNavController()

        val tool = useMemo(context, toolId) {
            Tools.getTools(context).firstOrNull { it.id == toolId }
        }
        val toolName = tool?.name ?: getString(R.string.app_name)

        useEffect(errorMessageTextView, toolName) {
            errorMessageTextView.text = toolName
        }

        useEffect(emailButton, toolName, errorDetails, context) {
            emailButton.setOnClickListener {
                val intent = Intents.email(
                    getString(R.string.email),
                    "Error in ${getString(R.string.app_name)}",
                    errorDetails
                )
                startActivity(intent)
            }
        }

        useEffect(copyButton, errorDetails, context) {
            copyButton.setOnClickListener {
                Clipboard.copy(
                    context,
                    errorDetails,
                    getString(R.string.copied_to_clipboard_toast)
                )
            }
        }

        useEffect(detailsButton, errorDetails, context) {
            detailsButton.setOnClickListener {
                Alerts.dialog(
                    context,
                    getString(R.string.error_occurred),
                    errorDetails,
                    cancelText = null
                )
            }
        }

        useEffect(reopenButton, toolId, navController) {
            reopenButton.setOnClickListener {
                navController.openTool(toolId)
            }
        }

        useEffect(restartButton, context) {
            restartButton.setOnClickListener {
                CurrentApp.restart(context)
            }
        }

        useEffect(settingsButton, navController) {
            settingsButton.setOnClickListener {
                navController.openTool(Tools.SETTINGS)
            }
        }
    }
}
