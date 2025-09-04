package com.kylecorry.trail_sense.tools.whitenoise.infrastructure.streams.andromeda

import android.media.AudioTrack
import com.kylecorry.andromeda.core.time.CoroutineTimer
import com.kylecorry.andromeda.core.tryOrLog
import com.kylecorry.andromeda.sound.ISoundPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

open class StreamSoundPlayer(private val stream: AudioStream, sampleRate: Int = 44100) :
    ISoundPlayer {

    private val sound = Audio.createStream(sampleRate)
    private val scope = CoroutineScope(Dispatchers.Default)
    private var job: Job? = null

    private var volume = 0f

    private var releaseWhenOff = false

    private val fadeOffIntervalometer = CoroutineTimer {
        volume -= 0.1f
        sound.setVolume(volume.coerceIn(0f, 1f))
        if (volume <= 0f) {
            if (releaseWhenOff) release() else off()
        }
    }

    private val fadeOnTimer: CoroutineTimer = CoroutineTimer {
        volume += 0.1f
        sound.setVolume(volume.coerceIn(0f, 1f))
        if (volume >= 1f) {
            stopFadeOn()
        }
    }

    override fun on() {
        if (isOn()) {
            return
        }
        volume = 1f
        setVolume(volume)
        sound.play()
        job?.cancel()
        job = scope.launch {
            tryOrLog {
                stream.writeToTrack(sound)
            }
        }
        fadeOffIntervalometer.stop()
        fadeOnTimer.stop()
    }

    override fun fadeOn() {
        if (isOn()) {
            return
        }
        volume = 0f
        setVolume(0f)
        sound.play()
        job?.cancel()
        job = scope.launch {
            tryOrLog {
                stream.writeToTrack(sound) {
                    fadeOnTimer.interval(20)
                }
            }
        }
        fadeOffIntervalometer.stop()
    }

    override fun off() {
        if (!isOn()) {
            return
        }
        fadeOffIntervalometer.stop()
        fadeOnTimer.stop()
        sound.pause()
        job?.cancel()
    }

    override fun fadeOff(releaseWhenOff: Boolean) {
        if (!isOn()) {
            return
        }
        this.releaseWhenOff = releaseWhenOff
        fadeOnTimer.stop()
        fadeOffIntervalometer.interval(20)
    }

    override fun isOn(): Boolean {
        return sound.playState == AudioTrack.PLAYSTATE_PLAYING
    }

    override fun release() {
        off()
        sound.release()
    }

    override fun setVolume(volume: Float) {
        fadeOffIntervalometer.stop()
        fadeOnTimer.stop()
        sound.setVolume(volume.coerceIn(0f, 1f))
    }

    private fun stopFadeOn() {
        fadeOnTimer.stop()
    }
}