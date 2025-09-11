package com.kylecorry.trail_sense.tools.whitenoise.infrastructure.streams

import com.kylecorry.andromeda.sound.stream.AudioStream
import com.kylecorry.sol.math.ComplexNumber
import com.kylecorry.sol.math.analysis.FrequencyAnalysis

class FFTAudioStream(
    private val stream: AudioStream,
    private val lowFreq: Double,
    private val highFreq: Double,
    private val fftSize: Int = 4096,
) : AudioStream {

    private val buffer = FloatArray(fftSize)
    private val twiddle = FrequencyAnalysis.getTwiddleFactorsFFT(fftSize)
    private var lastBlock = FloatArray(fftSize)
    private var blockIndex = 0
    private var mask: BooleanArray = BooleanArray(fftSize)
    private var isMaskPopulated = false

    override suspend fun next(sampleRate: Int): Float {
        if (!isMaskPopulated) {
            for (i in mask.indices) {
                val f = FrequencyAnalysis.getFrequencyFFT(i, fftSize, sampleRate.toFloat())
                mask[i] = f >= lowFreq && f <= highFreq
            }
            isMaskPopulated = true
        }

        if (blockIndex >= fftSize) {
            // Fill buffer with fresh samples
            for (i in 0 until fftSize) {
                buffer[i] = stream.next(sampleRate)
            }

            val fft = FrequencyAnalysis.fft(buffer.toList(), twiddle).toMutableList()

            // Masking
            for (i in fft.indices) {
                if (!mask[i]) {
                    fft[i] = ComplexNumber(0f, 0f)
                }
            }

            val ifft = FrequencyAnalysis.ifft(fft, twiddle)
            for (i in ifft.indices) {
                lastBlock[i] = ifft[i]
            }

            blockIndex = 0
        }

        return lastBlock[blockIndex++]
    }

    override suspend fun reset() {
        blockIndex = 0
        lastBlock.fill(0f)
        buffer.fill(0f)
        stream.reset()
    }
}