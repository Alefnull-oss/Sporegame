package com.example.spore.game.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * Procedural Audio Synthesizer for in-game Spore cellular events:
 * - Mating Call acoustic sonar song & response echo
 * - New Part Discovery triumphant celestial fanfare
 * - Meteorite mineral impact cracking & shattering
 * - Courtship dance cellular zygote chimes
 * 100% offline, zero external audio assets required.
 *
 * Anti-stutter hardening: every sound effect previously BUILT A NEW
 * AudioTrack (AudioTrack.Builder + AudioAttributes.Builder + AudioFormat.Builder
 * + native AudioFlinger round-trip) and RELEASED it after playback. AudioTrack
 * construction is one of the most expensive Android media operations and was a
 * visible frame spike source every time a sound triggered.
 *
 * This version keeps a small SELF-HEALING AudioTrack pool in MODE_STREAM:
 * - Tracks are reused across sounds (config is identical for every effect).
 * - A track is recycled only after a clean stop()/flush() and a healthy
 *   STATE_INITIALIZED check; any exception or bad state releases the track and
 *   removes it from the pool (the next request simply builds a fresh one).
 * - The pool is bounded, so memory stays flat even during sound spam.
 */
object SporeAudioEngine {
    private val scope = CoroutineScope(Dispatchers.Default)
    private const val SAMPLE_RATE = 22050

    /** Maximum pooled tracks (there are 5 distinct effects; overlap is rare). */
    private const val MAX_POOLED_TRACKS = 4

    /** Minimum internal buffer for pooled stream tracks (avoids tiny buffers). */
    private const val MIN_BUFFER_BYTES = 8192

    var isSoundEnabled: Boolean = true

    private class PooledTrack(val track: AudioTrack, val capacityBytes: Int)

    private val pool = ArrayDeque<PooledTrack>()

    private fun createTrack(bufferBytes: Int): PooledTrack {
        val capacity = bufferBytes.coerceAtLeast(MIN_BUFFER_BYTES)
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
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(capacity)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        return PooledTrack(track, capacity)
    }

    /** Pops a healthy pooled track with enough capacity, or builds a new one. */
    private fun obtainTrack(minBytes: Int): PooledTrack {
        synchronized(pool) {
            val iter = pool.iterator()
            while (iter.hasNext()) {
                val p = iter.next()
                if (p.capacityBytes >= minBytes) {
                    iter.remove()
                    return p
                }
            }
        }
        return createTrack(minBytes)
    }

    /**
     * Returns a track to the pool after verifying it is still healthy.
     * Self-healing: any failure releases the track instead of pooling it.
     */
    private fun recycleTrack(pooled: PooledTrack) {
        val healthy = try {
            pooled.track.pause()
            pooled.track.flush()
            pooled.track.state == AudioTrack.STATE_INITIALIZED
        } catch (_: Throwable) {
            false
        }
        if (!healthy) {
            try { pooled.track.release() } catch (_: Throwable) {}
            return
        }
        val accepted = synchronized(pool) {
            if (pool.size < MAX_POOLED_TRACKS) {
                pool.addFirst(pooled)
                true
            } else {
                false
            }
        }
        if (!accepted) {
            try { pooled.track.release() } catch (_: Throwable) {}
        }
    }

    private fun playToneSequence(durationMs: Int, generator: (Float) -> Float) {
        if (!isSoundEnabled) return
        scope.launch {
            try {
                val totalSamples = (SAMPLE_RATE * (durationMs / 1000f)).toInt()
                val buffer = ShortArray(totalSamples)

                for (i in 0 until totalSamples) {
                    val t = i.toFloat() / SAMPLE_RATE.toFloat()
                    val sampleFloat = generator(t).coerceIn(-1.0f, 1.0f)
                    buffer[i] = (sampleFloat * 32767f).toInt().toShort()
                }

                val pooled = obtainTrack(buffer.size * 2)
                try {
                    // Stream mode: queue the whole clip into the internal buffer,
                    // then drain it. write() with a large-enough buffer is non-blocking.
                    pooled.track.write(buffer, 0, buffer.size)
                    pooled.track.play()

                    // Wait for playback to finish before recycling the track.
                    delay(durationMs.toLong() + 120L)

                    recycleTrack(pooled)
                } catch (_: Throwable) {
                    // Self-heal: the track is in an unexpected state - discard it.
                    try { pooled.track.release() } catch (_: Throwable) {}
                }
            } catch (_: Exception) {}
        }
    }

    /**
     * Iconic Spore Mating Call: Deep, warm oceanic whale-like acoustic sweep with fluid vibrato.
     */
    fun playMatingCall() {
        playToneSequence(durationMs = 950) { t ->
            val env = when {
                t < 0.15f -> t / 0.15f
                t > 0.75f -> ((0.95f - t) / 0.20f).coerceAtLeast(0f)
                else -> 1.0f
            }
            // Frequency glides from 260Hz up to 520Hz with a gentle sub-harmonic
            val freq = 260f + 260f * (t / 0.95f)
            val vibrato = sin(2f * PI.toFloat() * 6.5f * t) * 14f
            val s1 = sin(2f * PI.toFloat() * (freq + vibrato) * t)
            val s2 = sin(2f * PI.toFloat() * ((freq * 0.5f) + vibrato) * t) * 0.45f
            val s3 = sin(2f * PI.toFloat() * (freq * 1.5f) * t) * 0.25f
            (s1 + s2 + s3) * env * 0.65f
        }
    }

    /**
     * Mating Response Echo: Distant harmonic response echoing back through the fluid.
     */
    fun playMateResponseEcho() {
        playToneSequence(durationMs = 850) { t ->
            val env = when {
                t < 0.1f -> t / 0.1f
                t > 0.65f -> ((0.85f - t) / 0.20f).coerceAtLeast(0f)
                else -> 0.85f
            }
            // Answering call chords (392Hz -> 587Hz -> 784Hz)
            val freq = 392f + 280f * sin(t * 3.14f * 0.7f)
            val vibrato = sin(2f * PI.toFloat() * 7.5f * t) * 12f
            val s1 = sin(2f * PI.toFloat() * (freq + vibrato) * t)
            val s2 = sin(2f * PI.toFloat() * (freq * 1.25f) * t) * 0.35f
            (s1 + s2) * env * 0.5f
        }
    }

    /**
     * Triumphant 4-note ascending celestial arpeggio when discovering a new Spore part!
     */
    fun playPartDiscovered() {
        playToneSequence(durationMs = 900) { t ->
            val noteIndex = (t / 0.22f).toInt().coerceIn(0, 3)
            val baseFreq = when (noteIndex) {
                0 -> 523.25f // C5
                1 -> 659.25f // E5
                2 -> 783.99f // G5
                else -> 1046.50f // C6
            }
            val noteT = (t % 0.22f)
            val noteEnv = (1f - (noteT / 0.22f)).coerceIn(0f, 1f)
            val s1 = sin(2f * PI.toFloat() * baseFreq * t)
            val s2 = sin(2f * PI.toFloat() * (baseFreq * 2f) * t) * 0.3f
            val s3 = sin(2f * PI.toFloat() * (baseFreq * 3f) * t) * 0.15f
            (s1 + s2 + s3) * noteEnv * 0.7f
        }
    }

    /**
     * Rocky crunch and mineral fracture impact when cracking a Meteorite Shard.
     */
    fun playMeteorHit(shattered: Boolean = false) {
        val dur = if (shattered) 450 else 180
        playToneSequence(durationMs = dur) { t ->
            val env = (1f - (t / (dur / 1000f))).coerceIn(0f, 1f)
            val noise = (Random.nextFloat() * 2f - 1f) * 0.5f
            val baseRumble = sin(2f * PI.toFloat() * (shattered.let { if (it) 90f else 140f }) * t)
            val crackle = if (shattered) sin(2f * PI.toFloat() * 1200f * t) * 0.3f else 0f
            (baseRumble * 0.6f + noise * 0.4f + crackle) * env * 0.75f
        }
    }

    /**
     * Courtship Dance Zygote Formation Chime: Tender bioluminescent chime with gentle bubbles.
     */
    fun playCourtshipZygote() {
        playToneSequence(durationMs = 1200) { t ->
            val env = when {
                t < 0.2f -> t / 0.2f
                t > 0.9f -> ((1.2f - t) / 0.3f).coerceAtLeast(0f)
                else -> 1.0f
            }
            val sweep = 440f + 440f * (t / 1.2f)
            val pulse = (1f + sin(2f * PI.toFloat() * 12f * t) * 0.4f)
            val s1 = sin(2f * PI.toFloat() * sweep * t)
            val s2 = sin(2f * PI.toFloat() * (sweep * 1.5f) * t) * 0.4f
            val chime = sin(2f * PI.toFloat() * (1320f + 200f * sin(t * 10f)) * t) * 0.25f
            (s1 + s2 + chime) * pulse * env * 0.6f
        }
    }
}
