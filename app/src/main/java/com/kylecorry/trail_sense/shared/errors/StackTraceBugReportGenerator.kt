package com.kylecorry.trail_sense.shared.errors

class StackTraceBugReportGenerator(private val throwable: Throwable) : IBugReportGenerator {
    override fun generate(): String {
        val message = throwable.message ?: ""
        val stackTrace = throwable.stackTraceToString()
        return "\nMessage: ${message}\n\n$stackTrace"
    }
}