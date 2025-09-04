package com.kylecorry.trail_sense.tools.whitenoise.infrastructure.streams.andromeda

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack

object Audio {
    fun createStream(
        sampleRate: Int = 44100,
        bufferSizeInFrames: Int = 2048
    ): AudioTrack {
        val channelConfig = AudioFormat.CHANNEL_OUT_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT

        val minBufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            channelConfig,
            audioFormat
        )

        val bufferSize = maxOf(bufferSizeInFrames * 2, minBufferSize)

        return AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(audioFormat)
                    .setSampleRate(sampleRate)
                    .setChannelMask(channelConfig)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
    }


}