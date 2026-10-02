package com.example.spore.game.noise

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * FastNoiseLite - Zero-dependency, ultra-fast procedural noise generator.
 * Translated & optimized for Kotlin/Android.
 * Original author: Jordan Peck (Auburn)
 */
class FastNoiseLite(var seed: Int = 1337) {

    enum class NoiseType {
        OpenSimplex2,
        OpenSimplex2S,
        Cellular,
        Perlin,
        ValueCubic,
        Value
    }

    enum class FractalType {
        None,
        FBm,
        Ridged,
        PingPong,
        DomainWarpProgressive,
        DomainWarpIndependent
    }

    enum class CellularDistanceFunction {
        Euclidean,
        EuclideanSq,
        Manhattan,
        Hybrid
    }

    enum class CellularReturnType {
        CellValue,
        Distance,
        Distance2,
        Distance2Add,
        Distance2Sub,
        Distance2Mul,
        Distance2Div
    }

    enum class DomainWarpType {
        OpenSimplex2,
        OpenSimplex2Reduced,
        BasicGrid
    }

    var noiseType: NoiseType = NoiseType.OpenSimplex2
    var frequency: Float = 0.01f
    var fractalType: FractalType = FractalType.None
    var octaves: Int = 3
    var lacunarity: Float = 2.0f
    var gain: Float = 0.5f
    var weightedStrength: Float = 0.0f
    var pingPongStrength: Float = 2.0f

    var cellularDistanceFunction: CellularDistanceFunction = CellularDistanceFunction.EuclideanSq
    var cellularReturnType: CellularReturnType = CellularReturnType.Distance
    var cellularJitter: Float = 1.0f

    var domainWarpType: DomainWarpType = DomainWarpType.OpenSimplex2
    var domainWarpAmp: Float = 1.0f

    // 2D Noise calculation
    fun getNoise(x: Float, y: Float): Float {
        val px = x * frequency
        val py = y * frequency

        return when (fractalType) {
            FractalType.FBm -> genFractalFBm(px, py)
            FractalType.Ridged -> genFractalRidged(px, py)
            FractalType.PingPong -> genFractalPingPong(px, py)
            else -> genBaseNoise(px, py)
        }
    }

    private fun genBaseNoise(x: Float, y: Float): Float {
        return when (noiseType) {
            NoiseType.OpenSimplex2 -> singleOpenSimplex2(seed, x, y)
            NoiseType.OpenSimplex2S -> singleOpenSimplex2S(seed, x, y)
            NoiseType.Cellular -> singleCellular(seed, x, y)
            NoiseType.Perlin -> singlePerlin(seed, x, y)
            NoiseType.ValueCubic -> singleValueCubic(seed, x, y)
            NoiseType.Value -> singleValue(seed, x, y)
        }
    }

    private fun genFractalFBm(x: Float, y: Float): Float {
        var sum = 0f
        var amp = 1f
        var maxAmp = 0f
        var px = x
        var py = y
        var curSeed = seed

        for (i in 0 until octaves) {
            val n = genBaseNoise(px, py)
            sum += n * amp
            maxAmp += amp
            amp *= gain
            px *= lacunarity
            py *= lacunarity
            curSeed++
        }
        return if (maxAmp > 0f) sum / maxAmp else sum
    }

    private fun genFractalRidged(x: Float, y: Float): Float {
        var sum = 0f
        var amp = 1f
        var maxAmp = 0f
        var px = x
        var py = y

        for (i in 0 until octaves) {
            val n = abs(genBaseNoise(px, py))
            sum += (1.0f - n) * amp
            maxAmp += amp
            amp *= gain
            px *= lacunarity
            py *= lacunarity
        }
        return if (maxAmp > 0f) sum / maxAmp else sum
    }

    private fun genFractalPingPong(x: Float, y: Float): Float {
        var sum = 0f
        var amp = 1f
        var maxAmp = 0f
        var px = x
        var py = y

        for (i in 0 until octaves) {
            var n = pingPong((genBaseNoise(px, py) + 1f) * 0.5f * pingPongStrength)
            sum += (n - 0.5f) * 2f * amp
            maxAmp += amp
            amp *= gain
            px *= lacunarity
            py *= lacunarity
        }
        return if (maxAmp > 0f) sum / maxAmp else sum
    }

    private fun pingPong(t: Float): Float {
        val t2 = t - floor(t * 0.5f) * 2f
        return if (t2 < 1f) t2 else 2f - t2
    }

    // Cellular (Voronoi) 2D
    private fun singleCellular(seed: Int, x: Float, y: Float): Float {
        val xr = fastRound(x)
        val yr = fastRound(y)

        var distance0 = 1e10f
        var distance1 = 1e10f
        var closestHash = 0

        for (xi in xr - 1..xr + 1) {
            for (yi in yr - 1..yr + 1) {
                val hash = hash2D(seed, xi, yi)
                val vecX = xi - x + (hash and 0xFFFF) / 65535.0f * cellularJitter
                val vecY = yi - y + ((hash shr 16) and 0xFFFF) / 65535.0f * cellularJitter

                val newDistance = when (cellularDistanceFunction) {
                    CellularDistanceFunction.Euclidean -> sqrt(vecX * vecX + vecY * vecY)
                    CellularDistanceFunction.EuclideanSq -> vecX * vecX + vecY * vecY
                    CellularDistanceFunction.Manhattan -> abs(vecX) + abs(vecY)
                    CellularDistanceFunction.Hybrid -> (abs(vecX) + abs(vecY)) + (vecX * vecX + vecY * vecY) * 0.5f
                }

                if (newDistance < distance0) {
                    distance1 = distance0
                    distance0 = newDistance
                    closestHash = hash
                } else if (newDistance < distance1) {
                    distance1 = newDistance
                }
            }
        }

        return when (cellularReturnType) {
            CellularReturnType.CellValue -> (closestHash and 0xFFFF) / 32767.5f - 1.0f
            CellularReturnType.Distance -> distance0 - 1.0f
            CellularReturnType.Distance2 -> distance1 - 1.0f
            CellularReturnType.Distance2Add -> (distance1 + distance0) * 0.5f - 1.0f
            CellularReturnType.Distance2Sub -> distance1 - distance0 - 1.0f
            CellularReturnType.Distance2Mul -> distance1 * distance0 * 0.5f - 1.0f
            CellularReturnType.Distance2Div -> distance0 / max(distance1, 0.0001f) - 1.0f
        }
    }

    // OpenSimplex2 2D
    private fun singleOpenSimplex2(seed: Int, x: Float, y: Float): Float {
        val s = (x + y) * 0.366025403784439f
        val xs = x + s
        val ys = y + s

        var xsb = fastFloor(xs)
        var ysb = fastFloor(ys)

        val xi = xs - xsb
        val yi = ys - ysb

        val t = (xi + yi) * 0.211324865405187f
        val dx0 = xi - t
        val dy0 = yi - t

        var a0 = 2.0f / 3.0f - dx0 * dx0 - dy0 * dy0
        var value = 0.0f
        if (a0 > 0) {
            a0 *= a0
            value += a0 * a0 * gradCoord2D(seed, xsb, ysb, dx0, dy0)
        }

        val c = 2.0f * (1.0f - 2.0f * 0.211324865405187f) * (1.0f / 0.366025403784439f - 2.0f)
        val xsv_ext = xsb + 1
        val ysv_ext = ysb + 1
        val dx_ext = dx0 - 1.0f + 2.0f * 0.211324865405187f
        val dy_ext = dy0 - 1.0f + 2.0f * 0.211324865405187f
        var a_ext = 2.0f / 3.0f - dx_ext * dx_ext - dy_ext * dy_ext
        if (a_ext > 0) {
            a_ext *= a_ext
            value += a_ext * a_ext * gradCoord2D(seed, xsv_ext, ysv_ext, dx_ext, dy_ext)
        }

        if (xi > yi) {
            val dx1 = dx0 - 1.0f + 0.211324865405187f
            val dy1 = dy0 + 0.211324865405187f
            var a1 = 2.0f / 3.0f - dx1 * dx1 - dy1 * dy1
            if (a1 > 0) {
                a1 *= a1
                value += a1 * a1 * gradCoord2D(seed, xsb + 1, ysb, dx1, dy1)
            }
        } else {
            val dx1 = dx0 + 0.211324865405187f
            val dy1 = dy0 - 1.0f + 0.211324865405187f
            var a1 = 2.0f / 3.0f - dx1 * dx1 - dy1 * dy1
            if (a1 > 0) {
                a1 *= a1
                value += a1 * a1 * gradCoord2D(seed, xsb, ysb + 1, dx1, dy1)
            }
        }
        return value * 18.2419f
    }

    private fun singleOpenSimplex2S(seed: Int, x: Float, y: Float): Float {
        return singleOpenSimplex2(seed xor 0x55555555, x, y)
    }

    // Perlin 2D
    private fun singlePerlin(seed: Int, x: Float, y: Float): Float {
        val x0 = fastFloor(x)
        val y0 = fastFloor(y)
        val xd0 = x - x0
        val yd0 = y - y0
        val xd1 = xd0 - 1f
        val yd1 = yd0 - 1f

        val xs = quinticHermite(xd0)
        val ys = quinticHermite(yd0)

        val xf0 = lerp(gradCoord2D(seed, x0, y0, xd0, yd0), gradCoord2D(seed, x0 + 1, y0, xd1, yd0), xs)
        val xf1 = lerp(gradCoord2D(seed, x0, y0 + 1, xd0, yd1), gradCoord2D(seed, x0 + 1, y0 + 1, xd1, yd1), xs)

        return lerp(xf0, xf1, ys) * 1.42476911f
    }

    // Value 2D
    private fun singleValue(seed: Int, x: Float, y: Float): Float {
        val x0 = fastFloor(x)
        val y0 = fastFloor(y)
        val xs = quinticHermite(x - x0)
        val ys = quinticHermite(y - y0)

        val yf0 = lerp(valCoord2D(seed, x0, y0), valCoord2D(seed, x0 + 1, y0), xs)
        val yf1 = lerp(valCoord2D(seed, x0, y0 + 1), valCoord2D(seed, x0 + 1, y0 + 1), xs)

        return lerp(yf0, yf1, ys)
    }

    private fun singleValueCubic(seed: Int, x: Float, y: Float): Float {
        val x1 = fastFloor(x)
        val y1 = fastFloor(y)
        val xs = x - x1
        val ys = y - y1

        return cubicLerp(
            cubicLerp(valCoord2D(seed, x1 - 1, y1 - 1), valCoord2D(seed, x1, y1 - 1), valCoord2D(seed, x1 + 1, y1 - 1), valCoord2D(seed, x1 + 2, y1 - 1), xs),
            cubicLerp(valCoord2D(seed, x1 - 1, y1), valCoord2D(seed, x1, y1), valCoord2D(seed, x1 + 1, y1), valCoord2D(seed, x1 + 2, y1), xs),
            cubicLerp(valCoord2D(seed, x1 - 1, y1 + 1), valCoord2D(seed, x1, y1 + 1), valCoord2D(seed, x1 + 1, y1 + 1), valCoord2D(seed, x1 + 2, y1 + 1), xs),
            cubicLerp(valCoord2D(seed, x1 - 1, y1 + 2), valCoord2D(seed, x1, y1 + 2), valCoord2D(seed, x1 + 1, y1 + 2), valCoord2D(seed, x1 + 2, y1 + 2), xs),
            ys
        )
    }

    // Helper math utilities
    private fun fastFloor(f: Float): Int = if (f >= 0) f.toInt() else (f - 1).toInt()
    private fun fastRound(f: Float): Int = if (f >= 0) (f + 0.5f).toInt() else (f - 0.5f).toInt()

    private fun quinticHermite(t: Float): Float = t * t * t * (t * (t * 6f - 15f) + 10f)
    private fun lerp(a: Float, b: Float, t: Float): Float = a + t * (b - a)

    private fun cubicLerp(a: Float, b: Float, c: Float, d: Float, t: Float): Float {
        val p = (d - c) - (a - b)
        return t * (t * (t * p + ((a - b) - p)) + (c - a)) + b
    }

    private fun hash2D(seed: Int, x: Int, y: Int): Int {
        var hash = seed xor (x * 374761393)
        hash = (hash xor (y * 668265263)) * 1274126177
        return hash xor (hash shr 16)
    }

    private fun valCoord2D(seed: Int, x: Int, y: Int): Float {
        val n = hash2D(seed, x, y)
        return (n and 0xFFFF) / 32767.5f - 1.0f
    }

    private val GRAD_X_2D = floatArrayOf(0f, 0f, 1f, -1f, 0.7071f, -0.7071f, 0.7071f, -0.7071f)
    private val GRAD_Y_2D = floatArrayOf(1f, -1f, 0f, 0f, 0.7071f, 0.7071f, -0.7071f, -0.7071f)

    private fun gradCoord2D(seed: Int, primX: Int, primY: Int, xd: Float, yd: Float): Float {
        val hash = hash2D(seed, primX, primY) and 7
        return xd * GRAD_X_2D[hash] + yd * GRAD_Y_2D[hash]
    }
}
