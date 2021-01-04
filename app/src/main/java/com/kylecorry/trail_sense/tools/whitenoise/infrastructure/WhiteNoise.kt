package com.kylecorry.trail_sense.tools.whitenoise.infrastructure

import android.media.AudioTrack

class WhiteNoise {

    private val tone = WhiteNoiseGenerator().getNoise(durationSeconds = 5)

    fun on() {
        if (isOn()){
            return
        }
        tone.play()
    }

    fun off() {
        if (!isOn()){
            return
        }
        tone.pause()
    }

    fun isOn(): Boolean {
        return tone.playState == AudioTrack.PLAYSTATE_PLAYING
    }

    fun release() {
        off()
        tone.release()
    }

}