package com.example.spore.game.engine

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 2D vector with BOTH immutable-style operators (cold paths) and a zero-allocation
 * in-place API (hot per-frame paths).
 *
 * The in-place methods mutate the receiver and return `this` for chaining. They must
 * ONLY be used on vectors owned by a mutable entity (position/velocity fields), never
 * on shared singletons such as [ZERO] (which must remain immutable by convention).
 * All formulas are bit-identical to their immutable counterparts.
 */
data class Vector2(var x: Float = 0f, var y: Float = 0f) {
    // ---------------------------------------------------------------------
    // Immutable-style operators (allocate; use only on cold / one-shot paths)
    // ---------------------------------------------------------------------
    operator fun plus(other: Vector2) = Vector2(x + other.x, y + other.y)
    operator fun minus(other: Vector2) = Vector2(x - other.x, y - other.y)
    operator fun unaryMinus() = Vector2(-x, -y)
    operator fun times(scalar: Float) = Vector2(x * scalar, y * scalar)
    operator fun div(scalar: Float) = if (scalar != 0f) Vector2(x / scalar, y / scalar) else Vector2(0f, 0f)

    fun length(): Float = sqrt(x * x + y * y)
    fun lengthSquared(): Float = x * x + y * y

    fun distanceTo(other: Vector2): Float = (this - other).length()

    fun wrappedDistanceTo(other: Vector2, worldWidth: Float, worldHeight: Float): Float {
        return wrappedDistanceTo(other.x, other.y, worldWidth, worldHeight)
    }

    /** Zero-allocation distance variant taking the other point as raw coordinates. */
    fun wrappedDistanceTo(ox: Float, oy: Float, worldWidth: Float, worldHeight: Float): Float {
        var dx = kotlin.math.abs(x - ox)
        var dy = kotlin.math.abs(y - oy)
        if (dx > worldWidth * 0.5f) dx = worldWidth - dx
        if (dy > worldHeight * 0.5f) dy = worldHeight - dy
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }

    fun wrappedDeltaTo(other: Vector2, worldWidth: Float, worldHeight: Float): Vector2 {
        var dx = other.x - x
        var dy = other.y - y
        if (dx > worldWidth * 0.5f) dx -= worldWidth
        if (dx < -worldWidth * 0.5f) dx += worldWidth
        if (dy > worldHeight * 0.5f) dy -= worldHeight
        if (dy < -worldHeight * 0.5f) dy += worldHeight
        return Vector2(dx, dy)
    }

    /**
     * Zero-allocation variant: writes the wrapped delta (this -> other) into [out]
     * and returns it, so hot loops can reuse a scratch vector.
     */
    fun wrappedDeltaInto(other: Vector2, worldWidth: Float, worldHeight: Float, out: Vector2): Vector2 {
        var dx = other.x - x
        var dy = other.y - y
        if (dx > worldWidth * 0.5f) dx -= worldWidth
        if (dx < -worldWidth * 0.5f) dx += worldWidth
        if (dy > worldHeight * 0.5f) dy -= worldHeight
        if (dy < -worldHeight * 0.5f) dy += worldHeight
        out.x = dx
        out.y = dy
        return out
    }

    fun normalized(): Vector2 {
        val len = length()
        return if (len > 0.0001f) Vector2(x / len, y / len) else Vector2(0f, 0f)
    }

    fun angle(): Float = atan2(y, x)

    // ---------------------------------------------------------------------
    // In-place mutation API (zero-allocation; hot per-frame paths ONLY)
    // ---------------------------------------------------------------------

    /** Sets both components. Returns this for chaining. */
    fun set(nx: Float, ny: Float): Vector2 {
        x = nx
        y = ny
        return this
    }

    /** Copies components from [o]. Returns this for chaining. */
    fun setFrom(o: Vector2): Vector2 {
        x = o.x
        y = o.y
        return this
    }

    /** Polar setter: (cos * magnitude, sin * magnitude). */
    fun setFromAngle(angleRad: Float, magnitude: Float): Vector2 {
        x = cos(angleRad) * magnitude
        y = sin(angleRad) * magnitude
        return this
    }

    /** this += o */
    fun addInPlace(o: Vector2): Vector2 {
        x += o.x
        y += o.y
        return this
    }

    /** this += o * scalar (identical to `this + (o * scalar)` without allocating). */
    fun addScaledInPlace(o: Vector2, scalar: Float): Vector2 {
        x += o.x * scalar
        y += o.y * scalar
        return this
    }

    /** this -= o */
    fun subtractInPlace(o: Vector2): Vector2 {
        x -= o.x
        y -= o.y
        return this
    }

    /** this *= scalar */
    fun scaleInPlace(scalar: Float): Vector2 {
        x *= scalar
        y *= scalar
        return this
    }

    /** Normalizes in place; a zero-length vector stays zero. */
    fun normalizeInPlace(): Vector2 {
        val len = length()
        if (len > 0.0001f) {
            x /= len
            y /= len
        } else {
            x = 0f
            y = 0f
        }
        return this
    }

    /** Toroidal wrap on both axes (same math as GameSimulation.wrapCoord). */
    fun wrapInPlace(worldWidth: Float, worldHeight: Float): Vector2 {
        var rx = x % worldWidth
        if (rx < 0f) rx += worldWidth
        var ry = y % worldHeight
        if (ry < 0f) ry += worldHeight
        x = rx
        y = ry
        return this
    }

    companion object {
        /** Shared immutable-by-convention zero vector. NEVER mutate it in place. */
        val ZERO = Vector2(0f, 0f)

        fun fromAngle(angleRad: Float, magnitude: Float = 1f): Vector2 {
            return Vector2(cos(angleRad) * magnitude, sin(angleRad) * magnitude)
        }

        fun lerp(start: Vector2, end: Vector2, fraction: Float): Vector2 {
            val f = fraction.coerceIn(0f, 1f)
            return Vector2(
                start.x + (end.x - start.x) * f,
                start.y + (end.y - start.y) * f
            )
        }
    }
}
