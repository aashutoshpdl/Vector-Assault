package com.vectorassault.game

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import java.util.concurrent.Executors
import kotlin.math.sin

class SoundManager(private val context: Context) {
    private val executor = Executors.newFixedThreadPool(3)
    private var vibrator: Vibrator? = null

    init {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    fun playLaserSound() {
        executor.execute {
            synthesizeChirp(880f, 320f, 0.06f, 0.35f)
        }
    }

    fun playExplosionSound() {
        triggerVibration(70)
        executor.execute {
            synthesizeNoise(0.22f, 0.6f)
        }
    }

    fun playPlayerHitSound() {
        triggerVibration(120)
        executor.execute {
            synthesizeChirp(200f, 60f, 0.25f, 0.8f)
        }
    }

    fun playPickupSound() {
        triggerVibration(30)
        executor.execute {
            synthesizeChirp(523f, 1046f, 0.12f, 0.4f)
        }
    }

    fun triggerVibration(durationMs: Long) {
        try {
            vibrator?.let {
                if (it.hasVibrator()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        it.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        @Suppress("DEPRECATION")
                        it.vibrate(durationMs)
                    }
                }
            }
        } catch (_: Exception) {}
    }

    private fun synthesizeChirp(startFreq: Float, endFreq: Float, durationSec: Float, maxVolume: Float) {
        try {
            val sampleRate = 22050
            val numSamples = (sampleRate * durationSec).toInt()
            val generatedSnd = ShortArray(numSamples)
            var phase = 0.0
            val twoPi = 2.0 * Math.PI

            for (i in 0 until numSamples) {
                val progress = i.toFloat() / numSamples
                val currentFreq = startFreq + (endFreq - startFreq) * progress
                val amp = (1f - progress) * maxVolume
                phase += twoPi * currentFreq / sampleRate
                generatedSnd[i] = (sin(phase) * amp * Short.MAX_VALUE).toInt().toShort()
            }

            playPcm(generatedSnd, sampleRate)
        } catch (_: Exception) {}
    }

    private fun synthesizeNoise(durationSec: Float, maxVolume: Float) {
        try {
            val sampleRate = 22050
            val numSamples = (sampleRate * durationSec).toInt()
            val generatedSnd = ShortArray(numSamples)
            val rand = java.util.Random()

            for (i in 0 until numSamples) {
                val progress = i.toFloat() / numSamples
                val envelope = (1f - progress) * (1f - progress) * maxVolume
                val sampleVal = (rand.nextFloat() * 2f - 1f) * envelope
                generatedSnd[i] = (sampleVal * Short.MAX_VALUE).toInt().toShort()
            }

            playPcm(generatedSnd, sampleRate)
        } catch (_: Exception) {}
    }

    private fun playPcm(pcm: ShortArray, sampleRate: Int) {
        val track = AudioTrack.Builder()
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
            .setBufferSizeInBytes(pcm.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        track.write(pcm, 0, pcm.size)
        track.play()
        // Clean up when done
        val waitTime = (pcm.size * 1000L / sampleRate) + 50L
        Thread.sleep(waitTime)
        track.stop()
        track.release()
    }

    fun release() {
        executor.shutdownNow()
    }
}
