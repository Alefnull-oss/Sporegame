package com.example.spore.game.engine

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class Vector2(val x: Float = 0f, val y: Float = 0f) {
    operator fun plus(other: Vector2) = Vector2(x + other.x, y + other.y)
    operator fun minus(other: Vector2) = Vector2(x - other.x, y - other.y)
    operator fun unaryMinus() = Vector2(-x, -y)
    operator fun times(scalar: Float) = Vector2(x * scalar, y * scalar)
    operator fun div(scalar: Float) = if (scalar != 0f) Vector2(x / scalar, y / scalar) else Vector2(0f, 0f)

    fun length(): Float = sqrt(x * x + y * y)
    fun lengthSquared(): Float = x * x + y * y

    fun distanceTo(other: Vector2): Float = (this - other).length()

    fun wrappedDistanceTo(other: Vector2, worldWidth: Float, worldHeight: Float): Float {
        var dx = kotlin.math.abs(x - other.x)
        var dy = kotlin.math.abs(y - other.y)
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

    fun normalized(): Vector2 {
        val len = length()
        return if (len > 0.0001f) Vector2(x / len, y / len) else Vector2(0f, 0f)
    }

    fun angle(): Float = atan2(y, x)

    companion object {
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
