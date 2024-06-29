package com.kylecorry.trail_sense.shared.automations

import android.content.Context
import android.os.Bundle

data class Automation(
    val broadcast: String,
    val receivers: List<AutomationReceiver>
)

data class AutomationReceiver(
    val receiverId: String,
    val parameterTransformers: List<ParameterTransformer> = listOf(
        PassThroughParameterTransformer()
    ),
    val enabled: Boolean = true,
)

interface ParameterTransformer {
    fun transform(input: Bundle, output: Bundle)
}

class BooleanParameterTransformer(
    private val inputKey: String,
    private val outputKey: String = inputKey,
    private val defaultValue: Boolean = false,
    private val invert: Boolean = false
) : ParameterTransformer {
    override fun transform(input: Bundle, output: Bundle) {
        val value = input.getBoolean(inputKey, defaultValue)
        output.putBoolean(outputKey, if (invert) !value else value)
    }
}

class PassThroughParameterTransformer : ParameterTransformer {
    override fun transform(input: Bundle, output: Bundle) {
        output.putAll(input)
    }
}