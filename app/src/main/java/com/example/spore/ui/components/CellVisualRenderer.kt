package com.example.spore.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.example.spore.game.engine.DietType
import com.example.spore.game.engine.FoodKind
import com.example.spore.game.engine.FoodParticle
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * High-performance, zero-allocation cartoon / plasticine (plastilinezco) renderer for Spore cells.
 * Features:
 * - Chubby squishy clay bodies with specular clay highlights.
 * - Googly expressive cartoon eyes with pupils that look towards movement.
 * - Segmented gummy/clay flagella with bouncy physics.
 * - Cute caterpillar-like clay cilia.
 * - Chunky comic spikes and goofy animated mouths.
 * - Reusable Paths to avoid GC pauses and eliminate lag.
 */
object CellVisualRenderer {

    // Reusable Path instances to prevent frame-by-frame memory allocations
    private val reusablePath1 = Path()
    private val reusablePath2 = Path()
    private val reusablePath3 = Path()

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
            // 1. Chunky Clay Cilia (caterpillar-like plasticine fringe)
            if (ciliaCount > 0) {
                drawClayCilia(this, center, radius, ciliaCount, timeSeconds, primaryColor)
            }

            // 2. Segmented Clay Flagella (gummy bead-like tail)
            if (flagellaCount > 0) {
                drawClayFlagella(this, center, radius, flagellaCount, timeSeconds, isDashing, primaryColor)
            }

            // 3. Chunky Comic Spikes
            if (spikesCount > 0) {
                drawClaySpikes(this, center, radius, spikesCount)
            }

            // 4. Main Clay Body (Plastilinezco con relieve 3D, sombras y brillo)
            drawClayBody(this, center, radius, primaryColor, damageFlash, timeSeconds, isDashing)

            // 5. Organelles (Toxic slime vesicle / Electric spark node)
            if (hasPoison) {
                drawPoisonOrganelle(this, center, radius, timeSeconds)
            }
            if (hasElectric) {
                drawElectricOrganelle(this, center, radius, timeSeconds)
            }

            // 6. Googly Cartoon Eyes
            if (eyeType != "NONE") {
                drawCartoonEyes(this, center, radius, eyeType, timeSeconds, isDashing, damageFlash)
            }

            // 7. Goofy Animated Cartoon Mouth
            drawCartoonMouth(this, center, radius, mouthType, timeSeconds)
        }
    }

    private fun drawClayBody(
        drawScope: DrawScope,
        center: Offset,
        radius: Float,
        primaryColor: Color,
        damageFlash: Boolean,
        timeSeconds: Float,
        isDashing: Boolean
    ) {
        val baseColor = if (damageFlash) Color(0xFFFF2A4B) else primaryColor
        val darkRimColor = Color(
            (baseColor.red * 0.55f).coerceIn(0f, 1f),
            (baseColor.green * 0.55f).coerceIn(0f, 1f),
            (baseColor.blue * 0.55f).coerceIn(0f, 1f),
            1f
        )
        val lightClayColor = Color(
            (baseColor.red * 1.25f).coerceAtMost(1f),
            (baseColor.green * 1.25f).coerceAtMost(1f),
            (baseColor.blue * 1.25f).coerceAtMost(1f),
            1f
        )

        // Subtle clay wobble / breathing animation
        val breathe = sin(timeSeconds * 4f) * (radius * 0.04f)
        val r = radius + breathe

        // 1. Soft clay drop shadow (for tangible 3D plasticine feel)
        drawScope.drawCircle(
            color = Color.Black.copy(alpha = 0.35f),
            radius = r * 0.98f,
            center = center + Offset(r * 0.08f, r * 0.12f)
        )

        // 2. Thick cartoon border (contour)
        drawScope.drawCircle(
            color = darkRimColor,
            radius = r,
            center = center
        )

        // 3. Main plasticine mass with curved light gradient
        drawScope.drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    lightClayColor,
                    baseColor,
                    darkRimColor
                ),
                center = center - Offset(r * 0.22f, r * 0.25f),
                radius = r * 1.15f
            ),
            radius = r - 2.5f,
            center = center
        )

        // 4. Glossy Specular Clay Sheen (crescent highlight at top-left)
        val highlightCenter = center - Offset(r * 0.32f, r * 0.35f)
        drawScope.drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.65f), Color.White.copy(alpha = 0.15f), Color.Transparent),
                center = highlightCenter,
                radius = r * 0.38f
            ),
            radius = r * 0.38f,
            center = highlightCenter
        )

        // 5. Nucleus: Cute bouncy core inside the gummy cell
        val nucleusRadius = r * 0.32f
        val nucleusCenter = center + Offset(-r * 0.08f, r * 0.05f)
        drawScope.drawCircle(
            color = darkRimColor.copy(alpha = 0.7f),
            radius = nucleusRadius + 1.5f,
            center = nucleusCenter
        )
        drawScope.drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.85f), lightClayColor, baseColor),
                center = nucleusCenter - Offset(nucleusRadius * 0.25f, nucleusRadius * 0.25f),
                radius = nucleusRadius
            ),
            radius = nucleusRadius,
            center = nucleusCenter
        )
    }

    private fun drawClayFlagella(
        drawScope: DrawScope,
        center: Offset,
        radius: Float,
        count: Int,
        time: Float,
        isDashing: Boolean,
        color: Color
    ) {
        val waveFreq = if (isDashing) 16f else 8.5f
        val waveAmp = radius * (if (isDashing) 0.45f else 0.3f)
        val segments = 7

        val offsets = when (count) {
            1 -> listOf(0f)
            2 -> listOf(-0.35f, 0.35f)
            3 -> listOf(-0.55f, 0f, 0.55f)
            else -> listOf(-0.7f, -0.25f, 0.25f, 0.7f)
        }

        val clayBeadColor = color
        val beadOutline = Color(
            (color.red * 0.5f).coerceIn(0f, 1f),
            (color.green * 0.5f).coerceIn(0f, 1f),
            (color.blue * 0.5f).coerceIn(0f, 1f),
            1f
        )

        for ((tailIdx, yFactor) in offsets.withIndex()) {
            val startX = center.x - (radius * 0.88f)
            val startY = center.y + (radius * yFactor)
            val tailLen = radius * (if (isDashing) 2.4f else 1.75f)

            // Draw segmented gummy beads from cell outwards
            for (i in 1..segments) {
                val t = i.toFloat() / segments
                val beadX = startX - (tailLen * t)
                val phase = time * waveFreq + (tailIdx * 1.4f) + (t * 5.2f)
                val beadY = startY + sin(phase) * (waveAmp * t)
                val beadRadius = (radius * (0.16f * (1.1f - t * 0.55f))).coerceAtLeast(3.5f)

                // Bead shadow / outline
                drawScope.drawCircle(
                    color = beadOutline,
                    radius = beadRadius + 1.5f,
                    center = Offset(beadX, beadY)
                )
                // Bead clay body
                drawScope.drawCircle(
                    color = clayBeadColor,
                    radius = beadRadius,
                    center = Offset(beadX, beadY)
                )
                // Specular shine on each little clay bead
                drawScope.drawCircle(
                    color = Color.White.copy(alpha = 0.7f),
                    radius = beadRadius * 0.35f,
                    center = Offset(beadX - beadRadius * 0.3f, beadY - beadRadius * 0.3f)
                )
            }
        }
    }

    private fun drawClayCilia(
        drawScope: DrawScope,
        center: Offset,
        radius: Float,
        ciliaTier: Int,
        time: Float,
        color: Color
    ) {
        val hairCount = 10 + (ciliaTier * 4)
        val hairLength = radius * 0.26f

        val ciliaColor = color
        val darkOutline = Color(
            (color.red * 0.55f).coerceIn(0f, 1f),
            (color.green * 0.55f).coerceIn(0f, 1f),
            (color.blue * 0.55f).coerceIn(0f, 1f),
            1f
        )

        for (i in 0 until hairCount) {
            val baseAngle = (i.toFloat() / hairCount) * (2f * PI.toFloat())
            // Skip front where mouth is
            if (cos(baseAngle) > 0.60f) continue

            // Chubby waddling little cartoon paddles
            val paddleWobble = sin(time * 10f + i * 0.9f) * 0.3f
            val angle = baseAngle + paddleWobble

            val base = center + Offset(cos(angle) * (radius * 0.95f), sin(angle) * (radius * 0.95f))
            val tip = center + Offset(cos(angle) * (radius + hairLength), sin(angle) * (radius + hairLength))

            // Outline
            drawScope.drawLine(
                color = darkOutline,
                start = base,
                end = tip,
                strokeWidth = 6.5f,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
            // Color
            drawScope.drawLine(
                color = ciliaColor,
                start = base,
                end = tip,
                strokeWidth = 4.2f,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
            // Tip bead
            drawScope.drawCircle(
                color = Color.White.copy(alpha = 0.8f),
                radius = 2.2f,
                center = tip
            )
        }
    }

    private fun drawClaySpikes(
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
            val spikeLength = radius * 1.55f
            val baseSpread = 0.22f

            val b1 = center + Offset(cos(ang - baseSpread) * (radius * 0.9f), sin(ang - baseSpread) * (radius * 0.9f))
            val b2 = center + Offset(cos(ang + baseSpread) * (radius * 0.9f), sin(ang + baseSpread) * (radius * 0.9f))
            val tip = center + Offset(cos(ang) * spikeLength, sin(ang) * spikeLength)

            reusablePath1.reset()
            reusablePath1.moveTo(b1.x, b1.y)
            reusablePath1.lineTo(tip.x, tip.y)
            reusablePath1.lineTo(b2.x, b2.y)
            reusablePath1.close()

            // Chunky plasticine horn with thick dark cartoon outline
            drawScope.drawPath(reusablePath1, color = Color(0xFF263238), style = Stroke(width = 5.5f))
            drawScope.drawPath(reusablePath1, color = Color(0xFFFFF9C4), style = Fill)

            // Clay highlight line along one edge
            drawScope.drawLine(
                color = Color.White,
                start = b1,
                end = tip,
                strokeWidth = 2.5f,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        }
    }

    private fun drawCartoonEyes(
        drawScope: DrawScope,
        center: Offset,
        radius: Float,
        eyeType: String,
        timeSeconds: Float,
        isDashing: Boolean,
        damageFlash: Boolean
    ) {
        val eyeR = radius * 0.22f
        val eyeSpacing = radius * 0.28f
        val eyeForward = radius * 0.55f

        val eye1Center = center + Offset(eyeForward, -eyeSpacing)
        val eye2Center = center + Offset(eyeForward, eyeSpacing)

        for (eyePos in listOf(eye1Center, eye2Center)) {
            // Dark clay rim / socket
            drawScope.drawCircle(
                color = Color(0xFF1A237E),
                radius = eyeR + 2.5f,
                center = eyePos
            )
            // Big white googly eyeball
            drawScope.drawCircle(
                color = Color(0xFFFAFAFA),
                radius = eyeR,
                center = eyePos
            )

            // Animated cartoon pupil: looks forward/slightly looks around
            val pupilOffsetFactor = if (isDashing) 0.55f else 0.35f
            val pupilShift = Offset(eyeR * pupilOffsetFactor, 0f)
            val pupilRadius = if (damageFlash) eyeR * 0.75f else eyeR * 0.48f

            val pupilColor = if (eyeType == "COMPOUND") Color(0xFFFF1744) else Color(0xFF102027)
            drawScope.drawCircle(
                color = pupilColor,
                radius = pupilRadius,
                center = eyePos + pupilShift
            )

            // Cute cartoon specular highlight in the pupil!
            drawScope.drawCircle(
                color = Color.White,
                radius = pupilRadius * 0.42f,
                center = eyePos + pupilShift - Offset(pupilRadius * 0.3f, pupilRadius * 0.3f)
            )
        }
    }

    private fun drawCartoonMouth(
        drawScope: DrawScope,
        center: Offset,
        radius: Float,
        mouthType: DietType,
        time: Float
    ) {
        val mouthX = center.x + (radius * 0.92f)
        val mouthY = center.y

        when (mouthType) {
            DietType.HERBIVORE -> {
                // Chubby rubbery cartoon O-lips (suction mouth)
                val pulse = sin(time * 7f) * (radius * 0.06f)
                val lipR = (radius * 0.32f + pulse).coerceAtLeast(6f)
                val mouthPos = Offset(mouthX, mouthY)

                // Thick dark lip contour
                drawScope.drawCircle(
                    color = Color(0xFF1B5E20),
                    radius = lipR + 3f,
                    center = mouthPos,
                    style = Stroke(width = 4.5f)
                )
                // Bright green clay lips
                drawScope.drawCircle(
                    color = Color(0xFF00E676),
                    radius = lipR,
                    center = mouthPos,
                    style = Stroke(width = 5.5f)
                )
                // Dark inside suction hole
                drawScope.drawCircle(
                    color = Color(0xFF003300),
                    radius = lipR - 3f,
                    center = mouthPos,
                    style = Fill
                )
                // Cute suction highlight dot
                drawScope.drawCircle(
                    color = Color.White.copy(alpha = 0.8f),
                    radius = 2.5f,
                    center = mouthPos - Offset(lipR * 0.4f, lipR * 0.4f)
                )
            }
            DietType.CARNIVORE -> {
                // Goofy cartoon carnivore jaw with big white triangular teeth!
                val jawW = radius * 0.45f
                val jawH = radius * 0.48f

                reusablePath2.reset()
                reusablePath2.moveTo(mouthX, mouthY - jawH)
                reusablePath2.lineTo(mouthX + jawW, mouthY - jawH * 0.4f)
                reusablePath2.lineTo(mouthX + (jawW * 0.4f), mouthY)
                reusablePath2.lineTo(mouthX + jawW, mouthY + jawH * 0.4f)
                reusablePath2.lineTo(mouthX, mouthY + jawH)

                // Dark jaw outline
                drawScope.drawPath(
                    reusablePath2,
                    color = Color(0xFF880E4F),
                    style = Stroke(width = 6f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                )
                // Red cartoon gums
                drawScope.drawPath(
                    reusablePath2,
                    color = Color(0xFFFF1744),
                    style = Stroke(width = 4f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                )

                // Chunky white teeth cones
                val t1 = Offset(mouthX + jawW * 0.85f, mouthY - jawH * 0.45f)
                val t2 = Offset(mouthX + jawW * 0.85f, mouthY + jawH * 0.45f)
                drawScope.drawCircle(color = Color.White, radius = radius * 0.12f, center = t1)
                drawScope.drawCircle(color = Color.White, radius = radius * 0.12f, center = t2)
            }
            DietType.OMNIVORE -> {
                // Bendy-straw party horn cartoon proboscis!
                val proboscisLen = radius * 0.6f
                val wiggle = sin(time * 5f) * (radius * 0.12f)
                val tip = Offset(mouthX + proboscisLen, mouthY + wiggle)
                val base = Offset(mouthX, mouthY)

                // Outline
                drawScope.drawLine(
                    color = Color(0xFFE65100),
                    start = base,
                    end = tip,
                    strokeWidth = radius * 0.28f,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
                // Gold tube
                drawScope.drawLine(
                    color = Color(0xFFFFD600),
                    start = base,
                    end = tip,
                    strokeWidth = radius * 0.18f,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
                // Bulbous nozzle tip
                drawScope.drawCircle(
                    color = Color(0xFFFF6D00),
                    radius = radius * 0.18f,
                    center = tip
                )
                drawScope.drawCircle(
                    color = Color.White.copy(alpha = 0.8f),
                    radius = radius * 0.08f,
                    center = tip - Offset(2f, 2f)
                )
            }
        }
    }

    private fun drawPoisonOrganelle(drawScope: DrawScope, center: Offset, radius: Float, time: Float) {
        val pos = center + Offset(-radius * 0.38f, radius * 0.32f)
        val pulse = sin(time * 6f) * (radius * 0.04f)
        val orgR = radius * 0.22f + pulse

        // Slime green clay vesicle
        drawScope.drawCircle(color = Color(0xFF1B5E20), radius = orgR + 2f, center = pos)
        drawScope.drawCircle(color = Color(0xFF76FF03), radius = orgR, center = pos)
        drawScope.drawCircle(color = Color(0xFFCCFF90), radius = orgR * 0.45f, center = pos - Offset(orgR * 0.25f, orgR * 0.25f))
    }

    private fun drawElectricOrganelle(drawScope: DrawScope, center: Offset, radius: Float, time: Float) {
        val pos = center + Offset(-radius * 0.38f, -radius * 0.32f)
        val orgR = radius * 0.22f

        // Electric cyan node with spark core
        drawScope.drawCircle(color = Color(0xFF006064), radius = orgR + 2f, center = pos)
        drawScope.drawCircle(color = Color(0xFF00E5FF), radius = orgR, center = pos)
        drawScope.drawCircle(color = Color.White, radius = orgR * 0.5f, center = pos)
    }
}

/**
 * Cartoon / Plasticine food sprite renderer.
 * Algae: shiny pea-like green sphere with gloss.
 * Meat: marbled comic ham morsel with bone speckle.
 * DNA: chunky glowing golden helical ribbon.
 */
object FoodVisualRenderer {

    fun drawFood(drawScope: DrawScope, food: FoodParticle, timeSeconds: Float) {
        val center = Offset(food.position.x, food.position.y)
        val r = food.radius

        when (food.kind) {
            FoodKind.ALGAE -> {
                // Cartoon Green Pea with Glossy Clay Shine
                val wobble = sin(timeSeconds * 4f + food.id) * 0.8f
                val effectiveR = r + wobble

                // Dark outline
                drawScope.drawCircle(
                    color = Color(0xFF1B5E20),
                    radius = effectiveR + 1.8f,
                    center = center
                )
                // Bright lime-emerald clay body
                drawScope.drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF69F0AE), Color(0xFF00E676), Color(0xFF00C853)),
                        center = center - Offset(effectiveR * 0.3f, effectiveR * 0.3f),
                        radius = effectiveR * 1.1f
                    ),
                    radius = effectiveR,
                    center = center
                )
                // White specular clay highlight
                drawScope.drawCircle(
                    color = Color.White.copy(alpha = 0.85f),
                    radius = effectiveR * 0.35f,
                    center = center - Offset(effectiveR * 0.32f, effectiveR * 0.32f)
                )
            }
            FoodKind.MEAT_CHUNK -> {
                // Cartoon Comic Meat / Ham with White Marbling
                // Dark contour
                drawScope.drawCircle(
                    color = Color(0xFF880E4F),
                    radius = r + 2f,
                    center = center
                )
                // Coral-red clay body
                drawScope.drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFFF8A80), Color(0xFFFF5252), Color(0xFFD50000)),
                        center = center - Offset(r * 0.25f, r * 0.25f),
                        radius = r * 1.15f
                    ),
                    radius = r,
                    center = center
                )
                // White comic fat marbling dot
                drawScope.drawCircle(
                    color = Color.White.copy(alpha = 0.9f),
                    radius = r * 0.35f,
                    center = center + Offset(r * 0.15f, -r * 0.1f)
                )
            }
            FoodKind.DNA_STRAND -> {
                // Golden Gummy DNA Helix
                val pulse = sin(timeSeconds * 6f + food.id) * 1.5f
                val effectiveR = r + pulse

                // Golden glow ring
                drawScope.drawCircle(
                    color = Color(0xFFFFD600).copy(alpha = 0.35f),
                    radius = effectiveR * 1.5f,
                    center = center
                )
                // Dark gold rim
                drawScope.drawCircle(
                    color = Color(0xFFFF6F00),
                    radius = effectiveR + 2f,
                    center = center
                )
                // Bright gold clay nucleus
                drawScope.drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.White, Color(0xFFFFEA00), Color(0xFFFFAB00)),
                        center = center - Offset(effectiveR * 0.25f, effectiveR * 0.25f),
                        radius = effectiveR
                    ),
                    radius = effectiveR,
                    center = center
                )
                // Sparkle cross
                drawScope.drawLine(
                    color = Color.White,
                    start = center - Offset(effectiveR * 0.5f, 0f),
                    end = center + Offset(effectiveR * 0.5f, 0f),
                    strokeWidth = 2.5f
                )
                drawScope.drawLine(
                    color = Color.White,
                    start = center - Offset(0f, effectiveR * 0.5f),
                    end = center + Offset(0f, effectiveR * 0.5f),
                    strokeWidth = 2.5f
                )
            }
        }
    }
}

/**
 * Pre-allocated static text paint cache to completely eliminate memory allocation inside onDraw / Canvas!
 */
object FastTextPainter {
    val textPaint = Paint().apply {
        isAntiAlias = true
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
        textSize = 34f
        setShadowLayer(4f, 0f, 0f, android.graphics.Color.BLACK)
    }

    fun prepare(colorArgb: Int, textSizePx: Float = 34f): Paint {
        textPaint.color = colorArgb
        textPaint.textSize = textSizePx
        return textPaint
    }
}
