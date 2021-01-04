package com.kylecorry.trail_sense.tools.whitenoise.infrastructure

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack

class WhiteNoiseGenerator {

    fun getNoise(sampleRate: Int = 64000, durationSeconds: Int = 1): AudioTrack {
        // Adapted from https://stackoverflow.com/questions/2413426/playing-an-arbitrary-tone-with-android
        val size = durationSeconds * sampleRate
        val sound = ByteArray(2 * size)

        for (i in 0 until size) {
            val sample = Math.random() * 2 - 1
            val pcmSound = (sample * 32767).toInt()
            sound[i * 2] = (pcmSound and 0x00ff).toByte()
            sound[i * 2 + 1] = ((pcmSound and 0xff00) shr 8).toByte()
        }

        val audioTrack = AudioTrack(
            AudioManager.STREAM_MUSIC,
            sampleRate, AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT, sound.size,
            AudioTrack.MODE_STATIC
        )
        audioTrack.write(sound, 0, sound.size)
        audioTrack.setLoopPoints(0, size, Int.MAX_VALUE)
        return audioTrack
    }

}