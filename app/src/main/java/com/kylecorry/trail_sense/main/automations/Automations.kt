package com.kylecorry.trail_sense.main.automations

import android.content.Context
import com.kylecorry.luna.concurrency.CoroutineQueueRunner
import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.trail_sense.shared.automations.Automation
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import kotlinx.coroutines.runBlocking

object Automations {

    fun setup(context: Context) {
        val tools = Tools.getTools(context, false)
        val broadcasts = tools.flatMap { it.broadcasts.map { it.id } }

        broadcasts.forEach { broadcastId ->
            val queue = CoroutineQueueRunner(10)
            Tools.subscribe(broadcastId) { data ->
                queue.enqueue {
                    val automationsToRun = getAutomations(context, broadcastId)
                    val actionsToRun = automationsToRun.flatMap { it.actions }

                    val availableActions = tools
                        .flatMap { it.actions }
                        .filter { actionsToRun.contains(it.id) && it.isEnabled(context) }

                    automationsToRun.forEach {
                        for (action in it.actions) {
                            val toolAction =
                                availableActions.firstOrNull { r -> r.id == action } ?: continue

                            toolAction.action.onReceive(context, data)
                        }
                    }
                }
            }
        }
    }

    private suspend fun getAutomations(context: Context, broadcast: String): List<Automation> =
        onIO {
            val automations = listOf(
                PowerSavingModeAutomation.onEnabled(context),
                PowerSavingModeAutomation.onDisabled()
            )
            automations.filter { it.broadcast == broadcast }
        }
}
