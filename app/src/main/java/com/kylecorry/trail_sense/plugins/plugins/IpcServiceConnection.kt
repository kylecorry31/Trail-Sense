package com.kylecorry.trail_sense.plugins.plugins

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import com.kylecorry.luna.coroutines.onDefault
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import java.io.Closeable
import kotlin.coroutines.resume

class IpcServiceConnection(
    private val context: Context,
    private val intent: Intent,
) : Closeable {
    private var isBound = false
    private var isConnecting = false
    private var lock = Any()

    private var messenger: Messenger? = null

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            synchronized(lock) {
                messenger = Messenger(binder)
                isBound = true
                isConnecting = false
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            synchronized(lock) {
                messenger = null
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
        return context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
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

    suspend fun send(path: String, payload: ByteArray?): ByteArray? = suspendCancellableCoroutine {
        val message = Message.obtain()
        val bundle = message.data
        bundle.putString("route", path)
        bundle.putByteArray("payload", payload)

        val replyHandler = object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                it.resume(msg.data.getByteArray("payload"))
            }
        }

        val replyMessenger = Messenger(replyHandler)
        message.replyTo = replyMessenger
        messenger?.send(message) ?: it.resume(null)
    }

    override fun close() {
        synchronized(lock) {
            if (!isBound && !isConnecting) {
                return
            }
            context.unbindService(connection)
            messenger = null
            isBound = false
            isConnecting = false
        }
    }

}