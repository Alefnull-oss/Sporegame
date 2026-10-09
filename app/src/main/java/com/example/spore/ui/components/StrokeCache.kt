package com.example.spore.ui.components

import android.util.SparseArray
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * Allocation-free [Stroke] cache.
 *
 * Every `drawPath` / `drawCircle` call with `style = Stroke(...)` allocates a new Stroke
 * data class. With ~23 cells x ~12 stroked elements + ripples + waves + UI, that summed to
 * 50+ short-lived objects per rendered frame - pure GC churn for zero visual benefit.
 *
 * This cache quantizes the stroke width to 0.05px steps (imperceptible: less than a third
 * of a physical pixel at typical densities) and returns a shared immutable instance.
 * The variants mirror the exact cap/join combinations used by the renderers; note that
 * StrokeCap/StrokeJoin are annotation classes (not enums), so there is no `.ordinal` -
 * each style combination gets its own explicit factory and bucket.
 *
 * Single-threaded render thread => plain SparseArray without synchronization.
 */
object StrokeCache {

    /** Width quantization step in pixels. */
    private const val QUANT = 0.05f

    // One bucket per cap/join combination actually used in the codebase.
    private val roundRound = SparseArray<Stroke>()   // cap=Round, join=Round (organic clay style)
    private val roundDefault = SparseArray<Stroke>() // cap=Round, default join (Miter)
    private val defaultDefault = SparseArray<Stroke>() // Butt cap, Miter join (plain)
    private val defaultRoundJoin = SparseArray<Stroke>() // Butt cap, join=Round (chunky outlines)

    // Dash path effects keyed by (dash, gap) pair quantized to 1px.
    private val dashEffects = SparseArray<PathEffect>()

    // Dashed strokes keyed by (quantized width, dash, gap).
    private val dashedStrokes = SparseArray<Stroke>()

    private fun key(width: Float): Int = (width.coerceAtLeast(QUANT) / QUANT + 0.5f).toInt()

    /** Stroke with Round cap + Round join (dominant hand-crafted clay look). */
    fun roundRound(width: Float): Stroke {
        val k = key(width)
        var s = roundRound.get(k)
        if (s == null) {
            s = Stroke(width.coerceAtLeast(0.05f), cap = StrokeCap.Round, join = StrokeJoin.Round)
            roundRound.put(k, s)
        }
        return s
    }

    /** Stroke with Round cap and default (Miter) join. */
    fun round(width: Float): Stroke {
        val k = key(width)
        var s = roundDefault.get(k)
        if (s == null) {
            s = Stroke(width.coerceAtLeast(0.05f), cap = StrokeCap.Round)
            roundDefault.put(k, s)
        }
        return s
    }

    /** Plain stroke (Butt cap, Miter join). */
    fun plain(width: Float): Stroke {
        val k = key(width)
        var s = defaultDefault.get(k)
        if (s == null) {
            s = Stroke(width.coerceAtLeast(0.05f))
            defaultDefault.put(k, s)
        }
        return s
    }

    /** Stroke with default (Butt) cap + Round join (chunky cartoon outlines). */
    fun joinRound(width: Float): Stroke {
        val k = key(width)
        var s = defaultRoundJoin.get(k)
        if (s == null) {
            s = Stroke(width.coerceAtLeast(0.05f), join = StrokeJoin.Round)
            defaultRoundJoin.put(k, s)
        }
        return s
    }

    /**
     * Cached plain stroke with a dash path effect (used by the rotating
     * part-capsule DNA ring).
     */
    fun plainDashed(width: Float, dash: Float, gap: Float): Stroke {
        val k = ((key(width) and 0x3FF) shl 20) or
            ((dash.coerceAtLeast(1f).toInt() and 0x1FF) shl 11) or
            (gap.coerceAtLeast(1f).toInt() and 0x7FF)
        var s = dashedStrokes.get(k)
        if (s == null) {
            s = Stroke(width.coerceAtLeast(0.05f), pathEffect = dashEffect(dash, gap))
            dashedStrokes.put(k, s)
        }
        return s
    }

    /**
     * Cached dash path effect (used by the rotating part-capsule DNA ring).
     * Dash/gap lengths are quantized to 1px steps; zoom is stable within a session,
     * so in practice one or two instances are ever created.
     */
    fun dashEffect(dash: Float, gap: Float): PathEffect {
        val k = ((dash.coerceAtLeast(1f).toInt()) shl 12) or (gap.coerceAtLeast(1f).toInt() and 0xFFF)
        var e = dashEffects.get(k)
        if (e == null) {
            e = PathEffect.dashPathEffect(floatArrayOf(dash, gap))
            dashEffects.put(k, e)
        }
        return e
    }
}
