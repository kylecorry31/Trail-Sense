package com.kylecorry.trail_sense.tools.whitenoise.infrastructure.streams.andromeda

import android.media.AudioTrack

interface AudioStream {
    suspend fun next(sampleRate: Int): Float
    suspend fun reset()
}

interface SeekableAudioStream : AudioStream {
    suspend fun peek(time: Double, sampleRate: Int): Float
}

suspend fun AudioStream.nextBuffer(sampleRate: Int, buffer: ShortArray) {
    for (i in buffer.indices) {
        val sample = next(sampleRate).coerceIn(-1f, 1f)
        buffer[i] = (sample * Short.MAX_VALUE).toInt().toShort()
    }
}

suspend fun AudioStream.writeToTrack(track: AudioTrack, onFirstBufferWrite: () -> Unit = {}) {
    reset()
    val buffer = ShortArray(track.bufferSizeInFrames)
    var hasBuffer = false
    while (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
        nextBuffer(track.sampleRate, buffer)
        track.write(buffer, 0, buffer.size)
        if (!hasBuffer) {
            hasBuffer = true
            onFirstBufferWrite()
        }
    }
}