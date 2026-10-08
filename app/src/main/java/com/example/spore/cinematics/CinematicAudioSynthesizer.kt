package com.example.spore.cinematics

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * Procedural Audio Engine for Planet Cinematics.
 * Synthesizes deep planetary sub-drones, atmospheric entry wind, hydrothermal bubbling,
 * lightning crackles, cryogenic chimes, and cellular heartbeats directly using AudioTrack.
 * 100% offline, zero external files required.
 */
class CinematicAudioSynthesizer {

    private var audioTrack: AudioTrack? = null
    private var synthJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    private val sampleRate = 22050
    private val bufferSize = AudioTrack.getMinBufferSize(
        sampleRate,
        AudioFormat.CHANNEL_OUT_MONO,
        AudioFormat.ENCODING_PCM_16BIT
    ).coerceAtLeast(4096)

    @Volatile
    var isMuted: Boolean = false

    @Volatile
    var masterVolume: Float = 0.55f

    // Current sound parameters driven by cinematic stage and planet
    @Volatile
    private var baseDroneFreq: Float = 55f // A1 sub-bass

    @Volatile
    private var noiseAmount: Float = 0.2f

    @Volatile
    private var resonanceFreq: Float = 220f

    @Volatile
    private var bubbleRate: Float = 0f

    @Volatile
    private var crackleIntensity: Float = 0f

    @Volatile
    private var pulseRate: Float = 0f // Hz (e.g. 1.2 for heartbeat)

    fun start() {
        if (synthJob != null) return

        try {
            audioTrack = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
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
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()
            } else {
                @Suppress("DEPRECATION")
                AudioTrack(
                    android.media.AudioManager.STREAM_MUSIC,
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize,
                    AudioTrack.MODE_STREAM
                )
            }

            audioTrack?.play()
        } catch (_: Exception) {
            return
        }

        synthJob = scope.launch {
            val audioBuffer = ShortArray(bufferSize / 2)
            var phase1 = 0.0
            var phase2 = 0.0
            var phaseRes = 0.0
            var pulsePhase = 0.0
            var timeSec = 0.0
            val dt = 1.0 / sampleRate

            while (isActive) {
                if (isMuted || masterVolume <= 0.01f) {
                    audioBuffer.fill(0)
                } else {
                    val droneF = baseDroneFreq
                    val resF = resonanceFreq
                    val noiseLvl = noiseAmount
                    val bubR = bubbleRate
                    val crackle = crackleIntensity
                    val pulseR = pulseRate
                    val vol = masterVolume

                    for (i in audioBuffer.indices) {
                        timeSec += dt

                        // 1. Sub-bass Drone oscillator
                        phase1 += 2.0 * PI * droneF * dt
                        if (phase1 > 2.0 * PI) phase1 -= 2.0 * PI
                        val droneWave = sin(phase1) + 0.35 * sin(phase1 * 0.5)

                        // 2. Harmonic resonance overtone
                        phaseRes += 2.0 * PI * resF * dt
                        if (phaseRes > 2.0 * PI) phaseRes -= 2.0 * PI
                        val resWave = sin(phaseRes) * 0.25

                        // 3. Pinkish filtered atmospheric noise
                        val whiteNoise = (Random.nextFloat() * 2f - 1f) * noiseLvl

                        // 4. Random hydrothermal bubble pop or spark crackle
                        var transientSample = 0.0
                        if (bubR > 0.1f && Random.nextFloat() < (bubR * 0.003f)) {
                            phase2 = 0.0
                        }
                        if (phase2 < PI * 4) {
                            phase2 += 2.0 * PI * 440.0 * dt
                            transientSample += sin(phase2) * 0.4
                        }

                        // 5. Electric lightning crackle
                        if (crackle > 0.05f && Random.nextFloat() < (crackle * 0.008f)) {
                            transientSample += (Random.nextFloat() * 2f - 1f) * crackle * 0.8
                        }

                        // 6. Cellular heartbeat/pulse modulation
                        var ampMod = 1.0
                        if (pulseR > 0.1f) {
                            pulsePhase += 2.0 * PI * pulseR * dt
                            if (pulsePhase > 2.0 * PI) pulsePhase -= 2.0 * PI
                            val pulseWave = (sin(pulsePhase)).coerceAtLeast(0.0)
                            ampMod = 0.7 + 0.5 * (pulseWave * pulseWave)
                        }

                        val mixed = (droneWave * 0.45 + resWave + whiteNoise * 0.3 + transientSample) * ampMod * vol
                        val clamped = mixed.coerceIn(-1.0, 1.0)
                        audioBuffer[i] = (clamped * 32767.0).toInt().toShort()
                    }
                }

                try {
                    audioTrack?.write(audioBuffer, 0, audioBuffer.size)
                } catch (_: Exception) {
                    break
                }
            }
        }
    }

    /**
     * Tune the audio parameters according to the planet and current camera stage.
     */
    fun updateParametersForStage(planetId: String, stage: CinematicCameraStage) {
        when (stage) {
            CinematicCameraStage.ATMOSPHERE_ENTRY -> {
                baseDroneFreq = 48f
                noiseAmount = 0.65f // Roaring wind / ionization
                resonanceFreq = 180f
                bubbleRate = 0f
                crackleIntensity = if (planetId == "planet_rubrum") 0.4f else 0.05f
                pulseRate = 0f
            }
            CinematicCameraStage.GEOLOGICAL_DESCENT -> {
                baseDroneFreq = when (planetId) {
                    "planet_aqualis" -> 55f
                    "planet_rubrum" -> 42f
                    "planet_ametistia" -> 38f
                    "planet_solaria" -> 70f
                    else -> 50f
                }
                noiseAmount = 0.25f
                resonanceFreq = when (planetId) {
                    "planet_solaria" -> 520f // Solar hum
                    "planet_ametistia" -> 330f // Cryo chime
                    else -> 220f
                }
                bubbleRate = if (planetId == "planet_aqualis" || planetId == "planet_toxis") 0.6f else 0f
                crackleIntensity = if (planetId == "planet_rubrum") 0.7f else 0.1f
                pulseRate = 0f
            }
            CinematicCameraStage.CHEMICAL_REACTION -> {
                baseDroneFreq = 62f
                noiseAmount = 0.15f
                resonanceFreq = 380f
                bubbleRate = if (planetId == "planet_toxis") 0.8f else 0.4f
                crackleIntensity = if (planetId == "planet_rubrum") 0.85f else 0.2f
                pulseRate = 0.6f // Beginning of thermodynamic cycle
            }
            CinematicCameraStage.MOLECULAR_ASSEMBLY -> {
                baseDroneFreq = 72f
                noiseAmount = 0.08f
                resonanceFreq = 440f // A4 harmonic clarity
                bubbleRate = 0.3f
                crackleIntensity = 0.05f
                pulseRate = 1.0f // Rhythmic self-assembly pulse
            }
            CinematicCameraStage.CELLULAR_AWAKENING -> {
                baseDroneFreq = 82f
                noiseAmount = 0.05f
                resonanceFreq = 523.25f // C5 harmonic awakening
                bubbleRate = 0.2f
                crackleIntensity = 0f
                pulseRate = 1.4f // Lively heartbeat of first life
            }
        }
    }

    fun stop() {
        synthJob?.cancel()
        synthJob = null
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (_: Exception) {}
        audioTrack = null
    }
}
