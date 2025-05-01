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
                        // Execute the test
                        base.evaluate()
                        return
                    } catch (t: Throwable) {
                        // Store the exception
                        lastThrowable = t
                        currentTry++
                        
                        // Log the failure
                        if (currentTry < maxRetryCount) {
                            Log.w(tag, "Test ${description.displayName} failed (attempt $currentTry of $maxRetryCount). Retrying...", t)
                        } else {
                            Log.e(tag, "Test ${description.displayName} failed after $maxRetryCount attempts.", t)
                        }
                    }
                }
                
                // If we've exhausted all retries and still failed, throw the last exception
                if (lastThrowable != null) {
                    throw lastThrowable
                }
            }
        }
    }
}