package com.kylecorry.trail_sense.tools.whistle.infrastructure

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import com.kylecorry.andromeda.sound.SoundPlayer
import com.kylecorry.sol.math.SolMath
import kotlin.math.sin

class Whistle : SoundPlayer(WhistleGenerator().getTone(3800, 100))

private class WhistleGenerator {

    private val soundGenerator = SoundGenerator2()

    fun getTone(
        frequency: Int,
        warbleFrequency: Int,
        sampleRate: Int = 44100,
        baseFrequencyAmount: Float = 1f,
        doubleFrequencyAmount: Float = 0.3f
    ): AudioTrack {
        val totalAmount = baseFrequencyAmount + doubleFrequencyAmount
        val adjustedBaseAmount = baseFrequencyAmount / totalAmount
        val adjustedDoubleAmount = doubleFrequencyAmount / totalAmount

        // Calculate the shortest possible duration that is seamless
        val lcm = SolMath.leastCommonMultiple(1f / frequency, 1f / warbleFrequency)
        var duration = lcm.coerceIn(0f, 1f)
        if (duration == 0f) {
            duration = 1f
        }

        val inverseSample = 1.0 / sampleRate

        return soundGenerator.getSound(sampleRate, duration) {
            val t = it * inverseSample
            val factor = 2 * Math.PI * t
            val warble = sin(warbleFrequency * factor)
            val baseFrequency = sin(frequency * factor + warble)
            val doubleFrequency = sin(2 * frequency * factor + warble)
            (adjustedBaseAmount * baseFrequency + adjustedDoubleAmount * doubleFrequency)
        }
    }
}

// TODO: Replace with Andromeda SoundGenerator
private class SoundGenerator2 {

    fun getSound(
        sampleRate: Int = 64000,
        durationSeconds: Float = 1f,
        sampleGenerator: (i: Int) -> Double
    ): AudioTrack {
        // Adapted from https://stackoverflow.com/questions/2413426/playing-an-arbitrary-tone-with-android
        val size = (durationSeconds * sampleRate).toInt()
        val sound = ByteArray(2 * size)

        for (i in 0 until size) {
            val sample = sampleGenerator(i)
            val pcmSound = (sample * 32767).toInt()
            sound[i * 2] = (pcmSound and 0x00ff).toByte()
            sound[i * 2 + 1] = ((pcmSound and 0xff00) shr 8).toByte()
        }

        val track = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(sound.size)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
        } else {
            AudioTrack(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
                sound.size,
                AudioTrack.MODE_STATIC,
                AudioManager.AUDIO_SESSION_ID_GENERATE
            )
        }

        track.write(sound, 0, sound.size)
        track.setLoopPoints(0, size, Int.MAX_VALUE)
        return track
    }

}