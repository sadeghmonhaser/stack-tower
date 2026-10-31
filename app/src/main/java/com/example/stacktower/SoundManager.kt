package com.example.stacktower

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import java.util.concurrent.Executors
import kotlin.math.sin

class SoundManager {
    var enabled: Boolean = true
    private val executor = Executors.newSingleThreadExecutor()

    fun playDrop(pitchStep: Int = 0) {
        if (!enabled) return
        executor.execute {
            val freq = 400.0 * Math.pow(1.05946, (pitchStep % 12).toDouble())
            playTone(freq, 0.08, 0.25f)
        }
    }

    fun playPerfect(combo: Int = 1) {
        if (!enabled) return
        executor.execute {
            val baseFreq = 523.25 * Math.pow(1.05946, (combo * 2 % 12).toDouble())
            playChord(doubleArrayOf(baseFreq, baseFreq * 1.25, baseFreq * 1.5), 0.12, 0.35f)
        }
    }

    fun playGameOver() {
        if (!enabled) return
        executor.execute {
            playTone(400.0, 0.1, 0.3f)
            try { Thread.sleep(110) } catch (_: Exception) {}
            playTone(350.0, 0.1, 0.3f)
            try { Thread.sleep(110) } catch (_: Exception) {}
            playTone(300.0, 0.2, 0.35f)
        }
    }

    private fun playTone(freq: Double, durationSec: Double, volume: Float) {
        try {
            val sampleRate = 22050
            val numSamples = (durationSec * sampleRate).toInt()
            val sample = ShortArray(numSamples)
            for (i in 0 until numSamples) {
                val t = i.toDouble() / sampleRate
                val env = 1.0 - (t / durationSec)
                val wave = sin(2.0 * Math.PI * freq * t)
                sample[i] = (wave * 32767 * volume * env).toInt().coerceIn(-32768, 32767).toShort()
            }
            val track = createAudioTrack(sampleRate, sample.size * 2)
            track.write(sample, 0, sample.size)
            track.play()
            try { Thread.sleep((durationSec * 1000).toLong() + 30) } catch (_: Exception) {}
            track.release()
        } catch (_: Exception) {}
    }

    private fun playChord(freqs: DoubleArray, durationSec: Double, volume: Float) {
        try {
            val sampleRate = 22050
            val numSamples = (durationSec * sampleRate).toInt()
            val sample = ShortArray(numSamples)
            for (i in 0 until numSamples) {
                val t = i.toDouble() / sampleRate
                val env = 1.0 - (t / durationSec)
                var waveSum = 0.0
                for (f in freqs) {
                    waveSum += sin(2.0 * Math.PI * f * t)
                }
                waveSum /= freqs.size
                sample[i] = (waveSum * 32767 * volume * env).toInt().coerceIn(-32768, 32767).toShort()
            }
            val track = createAudioTrack(sampleRate, sample.size * 2)
            track.write(sample, 0, sample.size)
            track.play()
            try { Thread.sleep((durationSec * 1000).toLong() + 30) } catch (_: Exception) {}
            track.release()
        } catch (_: Exception) {}
    }

    @Suppress("DEPRECATION")
    private fun createAudioTrack(sampleRate: Int, bufferSize: Int): AudioTrack {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
        } else {
            AudioTrack(
                AudioManager.STREAM_MUSIC,
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize,
                AudioTrack.MODE_STATIC
            )
        }
    }

    fun release() {
        executor.shutdownNow()
    }
}
