package com.kylecorry.trail_sense.shared.plugins

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.kylecorry.luna.coroutines.onDefault
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout

abstract class PluginService<T>(
    protected val context: Context,
    private val packageId: String,
    private val action: String
) {
    private var isBound = false
    private var isConnecting = false
    protected var service: T? = null
        private set
    private var lock = Any()

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            synchronized(lock) {
                service = getServiceInterface(binder)
                isBound = true
                isConnecting = false
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            synchronized(lock) {
                service = null
                isBound = false
                isConnecting = false
            }
        }
    }

    fun connect(): Boolean {
        synchronized(lock) {
            if (isConnecting || isBound) {
                return true
            }
            isConnecting = true
        }
        val intent = Intent(action).apply {
            setPackage(packageId)
        }
        return context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    fun disconnect() {
        synchronized(lock) {
            if (!isBound) {
                return
            }
            context.unbindService(connection)
            service = null
            isBound = false
            isConnecting = false
        }
    }

    fun isConnected(): Boolean {
        synchronized(lock) {
            return isBound
        }
    }

    suspend fun waitUntilConnected(timeoutMillis: Long = 1000): Unit = onDefault {
        var isNowConnected = false
        synchronized(lock) {
            if (!isConnecting && !isBound) {
                throw IllegalStateException("Service is not connected")
            }
            isNowConnected = isBound
        }

        withTimeout(timeoutMillis) {
            while (!isNowConnected) {
                synchronized(lock) {
                    isNowConnected = isBound
                }
                delay(20)
            }
        }
    }

    abstract fun getServiceInterface(binder: IBinder?): T

}