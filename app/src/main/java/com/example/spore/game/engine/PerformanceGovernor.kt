package com.example.spore.game.engine

/**
 * Adaptive performance governor with Dynamic Resolution Scaling (DRS).
 *
 * Extreme anti-stutter strategy for mid/low-end devices (4-6GB RAM, targeting a stable 60 FPS):
 *
 * 1. **Median frame-time window (32 frames)** — the median is immune to single-frame outliers
 *    (GC pauses, vsync hiccups), so the governor reacts to *sustained* overload instead of
 *    jittering up and down on individual spikes.
 *
 * 2. **Stutter EMA** — an exponential moving average of the "bad frame" rate inside the window
 *    gives a smoothed severity signal (0..1) used for diagnostics and smooth degradation.
 *
 * 3. **5-level DRS (100% -> 40%)** — visual density levels with asymmetric hysteresis:
 *    - Degrade fast: ~2 consecutive bad frames are enough to drop one level (user perception
 *      of a stutter must be answered immediately).
 *    - Recover slow: ~4 seconds of healthy frames are required to climb back one level,
 *      preventing oscillation ("DRS thrashing") between levels.
 *
 * 4. **Fixed 60 Hz timestep accumulator** (consumed by the render loop) — decouples simulation
 *    determinism from display refresh rate (60/90/120 Hz screens all simulate at exactly 60 Hz)
 *    while capping catch-up steps to avoid the death spiral on slow frames.
 *
 * The governor NEVER touches gameplay logic: it only scales *rendering density*
 * (particle stride, caustic spacing, ripple detail, radar indicators, god rays).
 */
class PerformanceGovernor {

    companion object {
        /** Logical simulation rate: fixed timestep in seconds (60 Hz). */
        const val FIXED_DT = 1f / 60f

        /** Frame budget at 60 FPS in seconds. */
        const val FRAME_BUDGET_60 = 1f / 60f

        /** A frame is "bad" when it exceeds the budget by this factor. */
        private const val BAD_FRAME_FACTOR = 1.25f

        /** Number of recent frame times feeding the median filter. */
        private const val WINDOW_SIZE = 32

        /** Consecutive bad frames required to degrade one DRS level. */
        private const val DEGRADE_CONSECUTIVE_BAD = 2

        /** Healthy milliseconds required before recovering one DRS level. */
        private const val RECOVERY_HEALTHY_MS = 4000f

        /** Visual density scale per DRS level (level 0 = 100% quality). */
        private val DETAIL_SCALES = floatArrayOf(1.0f, 0.85f, 0.7f, 0.55f, 0.4f)
    }

    /** Ring buffer of recent frame times (seconds). */
    private val frameTimes = FloatArray(WINDOW_SIZE)
    private var frameCount = 0
    private var writeIndex = 0

    /** Scratch array reused for median computation (zero allocation per frame). */
    private val sortScratch = FloatArray(WINDOW_SIZE)

    /** Consecutive bad-frame counter for fast degradation. */
    private var consecutiveBadFrames = 0

    /** Accumulated healthy time (ms) for slow hysteresis recovery. */
    private var healthyMs = 0f

    /** Smoothed stutter severity (0 = perfectly smooth, 1 = heavy stutter). */
    var stutterEma: Float = 0f
        private set

    /** Current DRS level: 0 = full quality, 4 = minimum quality (40%). */
    var detailLevel: Int = 0
        private set

    /** Current visual density scale in [0.4, 1.0] derived from [detailLevel]. */
    val detailScale: Float
        get() = DETAIL_SCALES[detailLevel]

    /** True while the governor has reduced visual density below 100%. */
    val isThrottling: Boolean
        get() = detailLevel > 0

    /** Median-based FPS estimate (immune to outliers). */
    var fpsEstimate: Float = 60f
        private set

    /**
     * Feeds one raw frame delta (seconds, un-clamped) into the governor.
     * Call exactly once per rendered frame, BEFORE the fixed-timestep accumulator.
     */
    fun onFrame(rawDeltaSeconds: Float) {
        val dt = if (rawDeltaSeconds.isFinite() && rawDeltaSeconds > 0f) rawDeltaSeconds else FRAME_BUDGET_60

        // --- Ring buffer bookkeeping ---
        frameTimes[writeIndex] = dt
        writeIndex = (writeIndex + 1) % WINDOW_SIZE
        if (frameCount < WINDOW_SIZE) frameCount++

        // --- Median frame time (only when window is full; cheap: 32 elements) ---
        val medianDt: Float = if (frameCount == WINDOW_SIZE) {
            System.arraycopy(frameTimes, 0, sortScratch, 0, WINDOW_SIZE)
            sortScratch.sortInPlace()
            (sortScratch[WINDOW_SIZE / 2 - 1] + sortScratch[WINDOW_SIZE / 2]) * 0.5f
        } else {
            dt
        }
        fpsEstimate = if (medianDt > 0f) 1f / medianDt else 60f

        // --- Stutter detection with asymmetric hysteresis ---
        val isBadFrame = dt > FRAME_BUDGET_60 * BAD_FRAME_FACTOR
        if (isBadFrame) {
            consecutiveBadFrames++
            healthyMs = 0f
            if (consecutiveBadFrames >= DEGRADE_CONSECUTIVE_BAD && detailLevel < DETAIL_SCALES.lastIndex) {
                detailLevel++
                consecutiveBadFrames = 0
            }
        } else {
            consecutiveBadFrames = 0
            if (detailLevel > 0) {
                healthyMs += dt * 1000f
                if (healthyMs >= RECOVERY_HEALTHY_MS) {
                    detailLevel--
                    healthyMs = 0f
                }
            }
        }

        // --- Smoothed severity EMA (alpha 0.1) ---
        val severity = if (isBadFrame) 1f else 0f
        stutterEma += (severity - stutterEma) * 0.1f
    }

    /** Resets all statistics (e.g. when returning to the game after a pause). */
    fun reset() {
        frameCount = 0
        writeIndex = 0
        consecutiveBadFrames = 0
        healthyMs = 0f
        stutterEma = 0f
        fpsEstimate = 60f
    }
}

/** Allocation-free in-place sort for the small fixed-size median scratch buffer. */
private fun FloatArray.sortInPlace() {
    // Insertion sort: optimal for the fixed 32-element window (no boxing, no recursion).
    for (i in 1 until size) {
        val key = this[i]
        var j = i - 1
        while (j >= 0 && this[j] > key) {
            this[j + 1] = this[j]
            j--
        }
        this[j + 1] = key
    }
}
