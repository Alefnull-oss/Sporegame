package com.example.spore.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.example.spore.data.model.CellEvolutionEntity
import com.example.spore.game.engine.DietType
import com.example.spore.game.engine.FoodKind
import com.example.spore.game.engine.FoodParticle
import com.example.spore.game.engine.TrophicTier
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

object CellVisualRenderer {

    fun drawCell(
        drawScope: DrawScope,
        center: Offset,
        radius: Float,
        angleRad: Float,
        primaryColor: Color,
        mouthType: DietType,
        flagellaCount: Int,
        ciliaCount: Int,
        spikesCount: Int,
        hasPoison: Boolean,
        hasElectric: Boolean,
        eyeType: String = "BASIC",
        timeSeconds: Float,
        isDashing: Boolean = false,
        damageFlash: Boolean = false
    ) {
        val angleDeg = Math.toDegrees(angleRad.toDouble()).toFloat()

        drawScope.rotate(degrees = angleDeg, pivot = center) {
            // 1. Draw Flagella (Tail) protruding from rear (angle 180° = -X in local space)
            if (flagellaCount > 0) {
                drawFlagella(this, center, radius, flagellaCount, timeSeconds, isDashing, primaryColor)
            }

            // 2. Draw Cilia (perimeter hairs)
            if (ciliaCount > 0) {
                drawCilia(this, center, radius, ciliaCount, timeSeconds, primaryColor)
            }

            // 3. Draw Spikes
            if (spikesCount > 0) {
                drawSpikes(this, center, radius, spikesCount)
            }

            // 4. Main Cell Body (Membrane & Cytoplasm)
            val membraneColor = if (damageFlash) Color(0xFFFF1744) else primaryColor
            val innerColor = if (damageFlash) Color(0xFFFF8A80) else primaryColor.copy(alpha = 0.45f)

            // Outer glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(innerColor, membraneColor.copy(alpha = 0.15f), Color.Transparent),
                    center = center,
                    radius = radius * 1.35f
                ),
                radius = radius * 1.35f,
                center = center
            )

            // Cytoplasm fill
            drawCircle(
                color = innerColor,
                radius = radius,
                center = center
            )

            // Dynamic membrane ring
            drawCircle(
                color = membraneColor,
                radius = radius,
                center = center,
                style = Stroke(width = (radius * 0.12f).coerceIn(2.5f, 7f))
            )

            // 5. Nucleus with chromatin
            val nucleusRadius = radius * 0.38f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.9f), membraneColor, membraneColor.copy(alpha = 0.3f)),
                    center = center,
                    radius = nucleusRadius
                ),
                radius = nucleusRadius,
                center = center
            )

            // 6. Organelles (Poison vesicles or electric discharge nodes)
            if (hasPoison) {
                val pCenter = center + Offset(-radius * 0.4f, radius * 0.3f)
                drawCircle(color = Color(0xFF76FF03), radius = radius * 0.22f, center = pCenter)
                drawCircle(color = Color(0xFFCCFF90), radius = radius * 0.10f, center = pCenter)
            }
            if (hasElectric) {
                val eCenter = center + Offset(-radius * 0.4f, -radius * 0.3f)
                drawCircle(color = Color(0xFF00E5FF), radius = radius * 0.22f, center = eCenter)
                drawCircle(color = Color.White, radius = radius * 0.10f, center = eCenter)
            }

            // 7. Sensory Eyes (Front: +X direction)
            if (eyeType != "NONE") {
                val eyeOffset = Offset(radius * 0.65f, -radius * 0.25f)
                val eyeOffset2 = Offset(radius * 0.65f, radius * 0.25f)
                val eyeR = radius * 0.18f

                // Eye 1
                drawCircle(color = Color.White, radius = eyeR, center = center + eyeOffset)
                drawCircle(
                    color = if (eyeType == "COMPOUND") Color(0xFFFF5252) else Color(0xFF0B192C),
                    radius = eyeR * 0.55f,
                    center = center + eyeOffset + Offset(eyeR * 0.3f, 0f)
                )

                // Eye 2
                drawCircle(color = Color.White, radius = eyeR, center = center + eyeOffset2)
                drawCircle(
                    color = if (eyeType == "COMPOUND") Color(0xFFFF5252) else Color(0xFF0B192C),
                    radius = eyeR * 0.55f,
                    center = center + eyeOffset2 + Offset(eyeR * 0.3f, 0f)
                )
            }

            // 8. Mouth (Front: +X direction)
            drawMouth(this, center, radius, mouthType, timeSeconds)
        }
    }

    private fun drawFlagella(
        drawScope: DrawScope,
        center: Offset,
        radius: Float,
        count: Int,
        time: Float,
        isDashing: Boolean,
        color: Color
    ) {
        val tailLength = radius * (if (isDashing) 2.2f else 1.6f)
        val waveFreq = if (isDashing) 18f else 10f
        val waveAmp = radius * 0.35f

        val spacing = when (count) {
            1 -> listOf(0f)
            2 -> listOf(-0.3f, 0.3f)
            3 -> listOf(-0.5f, 0f, 0.5f)
            else -> listOf(-0.6f, -0.2f, 0.2f, 0.6f)
        }

        for ((idx, offsetFactor) in spacing.withIndex()) {
            val startY = center.y + (radius * offsetFactor)
            val startX = center.x - (radius * 0.95f)

            val path = Path()
            path.moveTo(startX, startY)

            val segments = 8
            for (i in 1..segments) {
                val t = i.toFloat() / segments
                val segX = startX - (tailLength * t)
                val phase = time * waveFreq + (idx * 1.5f) + (t * 6f)
                val segY = startY + sin(phase) * (waveAmp * t)
                path.lineTo(segX, segY)
            }

            drawScope.drawPath(
                path = path,
                color = color.copy(alpha = 0.85f),
                style = Stroke(width = (radius * 0.1f).coerceIn(2f, 5f))
            )
        }
    }

    private fun drawCilia(
        drawScope: DrawScope,
        center: Offset,
        radius: Float,
        ciliaTier: Int,
        time: Float,
        color: Color
    ) {
        val hairsCount = 14 + (ciliaTier * 6)
        val hairLen = radius * 0.28f

        for (i in 0 until hairsCount) {
            val angle = (i.toFloat() / hairsCount) * (2f * PI.toFloat())
            // Skip front where mouth is
            if (cos(angle) > 0.65f) continue

            val flutter = sin(time * 12f + i * 0.8f) * 0.25f
            val base = center + Offset(cos(angle) * radius, sin(angle) * radius)
            val tip = center + Offset(cos(angle + flutter) * (radius + hairLen), sin(angle + flutter) * (radius + hairLen))

            drawScope.drawLine(
                color = color.copy(alpha = 0.75f),
                start = base,
                end = tip,
                strokeWidth = 2.5f
            )
        }
    }

    private fun drawSpikes(
        drawScope: DrawScope,
        center: Offset,
        radius: Float,
        spikesCount: Int
    ) {
        val spikeAngles = when (spikesCount) {
            1 -> listOf(0f)
            2 -> listOf(-0.35f, 0.35f)
            3 -> listOf(-0.55f, 0f, 0.55f)
            else -> listOf(-0.75f, -0.25f, 0.25f, 0.75f)
        }

        for (ang in spikeAngles) {
            val tipDistance = radius * 1.65f
            val base1 = center + Offset(cos(ang - 0.15f) * radius, sin(ang - 0.15f) * radius)
            val base2 = center + Offset(cos(ang + 0.15f) * radius, sin(ang - 0.15f) * radius)
            val tip = center + Offset(cos(ang) * tipDistance, sin(ang) * tipDistance)

            val path = Path().apply {
                moveTo(base1.x, base1.y)
                lineTo(tip.x, tip.y)
                lineTo(base2.x, base2.y)
                close()
            }
            drawScope.drawPath(path = path, color = Color(0xFFECEFF1))
            drawScope.drawPath(path = path, color = Color(0xFF37474F), style = Stroke(width = 2f))
        }
    }

    private fun drawMouth(
        drawScope: DrawScope,
        center: Offset,
        radius: Float,
        mouthType: DietType,
        time: Float
    ) {
        val mouthOrigin = center + Offset(radius * 0.95f, 0f)

        when (mouthType) {
            DietType.HERBIVORE -> {
                // Suction filter: green fringe that pulses gently
                val filterPulse = sin(time * 6f) * (radius * 0.08f)
                val filterR = radius * 0.35f + filterPulse
                drawScope.drawCircle(
                    color = Color(0xFF00E676),
                    radius = filterR,
                    center = mouthOrigin,
                    style = Stroke(width = 3.5f)
                )
                // Filter slit
                drawScope.drawLine(
                    color = Color(0xFF69F0AE),
                    start = mouthOrigin + Offset(0f, -filterR * 0.6f),
                    end = mouthOrigin + Offset(0f, filterR * 0.6f),
                    strokeWidth = 3f
                )
            }
            DietType.CARNIVORE -> {
                // Serrated tooth jaw: sharp triangular teeth
                val jawW = radius * 0.35f
                val jawH = radius * 0.45f
                val toothPath = Path().apply {
                    moveTo(mouthOrigin.x, mouthOrigin.y - jawH)
                    lineTo(mouthOrigin.x + jawW, mouthOrigin.y - jawH * 0.5f)
                    lineTo(mouthOrigin.x, mouthOrigin.y)
                    lineTo(mouthOrigin.x + jawW, mouthOrigin.y + jawH * 0.5f)
                    lineTo(mouthOrigin.x, mouthOrigin.y + jawH)
                }
                drawScope.drawPath(
                    path = toothPath,
                    color = Color(0xFFFF5252),
                    style = Stroke(width = 4f)
                )
                // Sharp fangs
                drawScope.drawCircle(color = Color.White, radius = 3.5f, center = mouthOrigin + Offset(jawW, -jawH * 0.5f))
                drawScope.drawCircle(color = Color.White, radius = 3.5f, center = mouthOrigin + Offset(jawW, jawH * 0.5f))
            }
            DietType.OMNIVORE -> {
                // Proboscis: flexible tubular snout
                val snoutLen = radius * 0.45f
                val snoutTip = mouthOrigin + Offset(snoutLen, sin(time * 4f) * (radius * 0.1f))
                drawScope.drawLine(
                    color = Color(0xFFFFD600),
                    start = mouthOrigin,
                    end = snoutTip,
                    strokeWidth = radius * 0.22f
                )
                drawScope.drawCircle(
                    color = Color(0xFFFFAB00),
                    radius = radius * 0.15f,
                    center = snoutTip
                )
            }
        }
    }
}

object FoodVisualRenderer {
    fun drawFood(drawScope: DrawScope, food: FoodParticle, timeSeconds: Float) {
        when (food.kind) {
            FoodKind.ALGAE -> {
                // Algae: green chloroplast with silica cell wall
                drawScope.drawCircle(
                    color = Color(0xFF00E676).copy(alpha = 0.35f),
                    radius = food.radius * 1.5f,
                    center = Offset(food.position.x, food.position.y)
                )
                drawScope.drawCircle(
                    color = Color(0xFF00E676),
                    radius = food.radius,
                    center = Offset(food.position.x, food.position.y)
                )
                drawScope.drawCircle(
                    color = Color(0xFFB9F6CA),
                    radius = food.radius * 0.45f,
                    center = Offset(food.position.x - food.radius * 0.25f, food.position.y - food.radius * 0.25f)
                )
            }
            FoodKind.MEAT_CHUNK -> {
                // Meat: textured organic morsel
                drawScope.drawCircle(
                    color = Color(0xFFFF5252).copy(alpha = 0.3f),
                    radius = food.radius * 1.4f,
                    center = Offset(food.position.x, food.position.y)
                )
                drawScope.drawCircle(
                    color = Color(0xFFFF5252),
                    radius = food.radius,
                    center = Offset(food.position.x, food.position.y)
                )
                drawScope.drawCircle(
                    color = Color(0xFFFF8A80),
                    radius = food.radius * 0.45f,
                    center = Offset(food.position.x, food.position.y)
                )
            }
            FoodKind.DNA_STRAND -> {
                // Glowing golden double-helix fragment
                val center = Offset(food.position.x, food.position.y)
                val pulse = sin(timeSeconds * 5f + food.id) * 3f
                drawScope.drawCircle(
                    color = Color(0xFFFFD600).copy(alpha = 0.4f),
                    radius = food.radius * 1.6f + pulse,
                    center = center
                )
                drawScope.drawCircle(
                    color = Color(0xFFFFD600),
                    radius = food.radius,
                    center = center
                )
                drawScope.drawCircle(
                    color = Color.White,
                    radius = food.radius * 0.45f,
                    center = center
                )
            }
        }
    }
}
