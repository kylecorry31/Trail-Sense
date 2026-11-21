package com.kylecorry.trail_sense.test_utils

import android.util.Log
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

class RetryTestRule(private val maxRetryCount: Int = 3) : TestRule {
    
    private val tag = "RetryTestRule"
    
    override fun apply(base: Statement, description: Description): Statement {
        return statement(base, description)
    }
    
    private fun statement(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                var currentTry = 0
                var lastThrowable: Throwable? = null
                
                while (currentTry < maxRetryCount) {
                    try {
                        base.evaluate()
                        return
                    } catch (t: Throwable) {
                        lastThrowable = t
                        currentTry++
                        
                        if (currentTry < maxRetryCount) {
                            Log.w(tag, "Test ${description.displayName} failed (attempt $currentTry of $maxRetryCount). Retrying...", t)
                        } else {
                            Log.e(tag, "Test ${description.displayName} failed after $maxRetryCount attempts.", t)
                        }
                    }
                }
                
                if (lastThrowable != null) {
                    throw lastThrowable
                }
            }
        }
    }
}
