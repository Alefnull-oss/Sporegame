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
import com.example.spore.game.noise.FastNoiseLite
import com.example.spore.game.physics.ElasticAppendageChain
import com.example.spore.game.physics.SoftBodyMembrane
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

    // Pre-allocated static arrays to eliminate per-frame List allocations
    private val FLAGELLA_OFFSETS_1 = floatArrayOf(0f)
    private val FLAGELLA_OFFSETS_2 = floatArrayOf(-0.35f, 0.35f)
    private val FLAGELLA_OFFSETS_3 = floatArrayOf(-0.55f, 0f, 0.55f)
    private val FLAGELLA_OFFSETS_4 = floatArrayOf(-0.7f, -0.25f, 0.25f, 0.7f)

    private val SPIKE_ANGLES_1 = floatArrayOf(0f)
    private val SPIKE_ANGLES_2 = floatArrayOf(-0.35f, 0.35f)
    private val SPIKE_ANGLES_3 = floatArrayOf(-0.55f, 0f, 0.55f)
    private val SPIKE_ANGLES_4 = floatArrayOf(-0.75f, -0.25f, 0.25f, 0.75f)

    // Pre-allocated vertex buffers for soft-body organic membrane contour (zero allocation)
    private val membranePoints = Array(16) { Offset.Zero }
    private val shadowPoints = Array(16) { Offset.Zero }

    private val organicMembraneNoise = FastNoiseLite(1337).apply {
        noiseType = FastNoiseLite.NoiseType.OpenSimplex2
        frequency = 0.08f
    }

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
        damageFlash: Boolean = false,
        softBody: SoftBodyMembrane? = null,
        flagellaChains: List<ElasticAppendageChain>? = null,
        jawAperture: Float = 1.0f,
        isBiting: Boolean = false
    ) {
        val angleDeg = Math.toDegrees(angleRad.toDouble()).toFloat()

        drawScope.rotate(degrees = angleDeg, pivot = center) {
            // 1. Chunky Clay Cilia (caterpillar-like plasticine fringe)
            if (ciliaCount > 0) {
                drawClayCilia(this, center, radius, ciliaCount, timeSeconds, primaryColor)
            }

            // 2. Segmented Clay Flagella with dynamic water inertia and spring kinematics
            if (flagellaCount > 0) {
                drawClayFlagella(this, center, radius, flagellaCount, timeSeconds, isDashing, primaryColor, flagellaChains)
            }

            // 3. Chunky Comic Spikes
            if (spikesCount > 0) {
                drawClaySpikes(this, center, radius, spikesCount)
            }

            // 4. Main Clay Body - Organic soft-body membrane with FastNoiseLite and physical collision deformations
            drawClayBody(this, center, radius, primaryColor, damageFlash, timeSeconds, isDashing, softBody)

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

            // 7. Goofy Animated Cartoon Mouth with articulated physical jaws
            drawCartoonMouth(this, center, radius, mouthType, timeSeconds, jawAperture, isBiting)
        }
    }

    private fun drawClayBody(
        drawScope: DrawScope,
        center: Offset,
        radius: Float,
        primaryColor: Color,
        damageFlash: Boolean,
        timeSeconds: Float,
        isDashing: Boolean,
        softBody: SoftBodyMembrane? = null
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
        val rBase = radius + breathe

        // Construct soft-body organic membrane contour with FastNoiseLite & softBody impulse deformations
        val vertexCount = 16
        val step = (2f * PI.toFloat()) / vertexCount
        val shadowOffset = Offset(rBase * 0.08f, rBase * 0.12f)

        for (i in 0 until vertexCount) {
            val ang = i * step
            // Organic biological oscillation from FastNoiseLite
            val noiseVal = organicMembraneNoise.getNoise(cos(ang) * 2.5f, sin(ang) * 2.5f + timeSeconds * 1.6f)
            val biologicalWobble = noiseVal * (radius * 0.065f)
            val physicalDeform = softBody?.deformations?.getOrNull(i) ?: 0f

            val r = (rBase + biologicalWobble + physicalDeform).coerceAtLeast(radius * 0.35f)
            val px = center.x + cos(ang) * r
            val py = center.y + sin(ang) * r
            membranePoints[i] = Offset(px, py)
            shadowPoints[i] = Offset(px + shadowOffset.x, py + shadowOffset.y)
        }

        // Build smooth closed spline path using midpoint quadratic curves
        reusablePath1.reset()
        val p0 = (membranePoints[0] + membranePoints[vertexCount - 1]) * 0.5f
        reusablePath1.moveTo(p0.x, p0.y)
        for (i in 0 until vertexCount) {
            val pCurrent = membranePoints[i]
            val pNext = membranePoints[(i + 1) % vertexCount]
            val mid = (pCurrent + pNext) * 0.5f
            reusablePath1.quadraticTo(pCurrent.x, pCurrent.y, mid.x, mid.y)
        }
        reusablePath1.close()

        // 1. Soft clay drop shadow (for tangible 3D plasticine feel)
        reusablePath3.reset()
        val sp0 = (shadowPoints[0] + shadowPoints[vertexCount - 1]) * 0.5f
        reusablePath3.moveTo(sp0.x, sp0.y)
        for (i in 0 until vertexCount) {
            val pCurrent = shadowPoints[i]
            val pNext = shadowPoints[(i + 1) % vertexCount]
            val mid = (pCurrent + pNext) * 0.5f
            reusablePath3.quadraticTo(pCurrent.x, pCurrent.y, mid.x, mid.y)
        }
        reusablePath3.close()
        drawScope.drawPath(reusablePath3, color = Color.Black.copy(alpha = 0.32f), style = Fill)

        // 2. Thick cartoon border (contour)
        drawScope.drawPath(reusablePath1, color = darkRimColor, style = Stroke(width = 6f))

        // 3. Main plasticine mass with curved light gradient
        drawScope.drawPath(reusablePath1, color = baseColor, style = Fill)
        drawScope.drawCircle(
            color = lightClayColor.copy(alpha = 0.65f),
            radius = (rBase - 2.5f) * 0.72f,
            center = center - Offset(rBase * 0.18f, rBase * 0.20f)
        )

        // 4. Glossy Specular Clay Sheen
        val highlightCenter = center - Offset(rBase * 0.32f, rBase * 0.35f)
        drawScope.drawCircle(
            color = Color.White.copy(alpha = 0.65f),
            radius = rBase * 0.28f,
            center = highlightCenter
        )
        drawScope.drawCircle(
            color = Color.White.copy(alpha = 0.90f),
            radius = rBase * 0.14f,
            center = highlightCenter - Offset(rBase * 0.05f, rBase * 0.05f)
        )

        // 5. Nucleus: Cute bouncy core inside the gummy cell (zero shader allocation)
        val nucleusRadius = rBase * 0.32f
        val nucleusCenter = center + Offset(-rBase * 0.08f, rBase * 0.05f)
        drawScope.drawCircle(
            color = darkRimColor.copy(alpha = 0.7f),
            radius = nucleusRadius + 1.5f,
            center = nucleusCenter
        )
        drawScope.drawCircle(
            color = baseColor,
            radius = nucleusRadius,
            center = nucleusCenter
        )
        drawScope.drawCircle(
            color = Color.White.copy(alpha = 0.75f),
            radius = nucleusRadius * 0.45f,
            center = nucleusCenter - Offset(nucleusRadius * 0.25f, nucleusRadius * 0.25f)
        )
    }

    private fun drawClayFlagella(
        drawScope: DrawScope,
        center: Offset,
        radius: Float,
        count: Int,
        time: Float,
        isDashing: Boolean,
        color: Color,
        chains: List<ElasticAppendageChain>? = null
    ) {
        val clayBeadColor = color
        val beadOutline = Color(
            (color.red * 0.5f).coerceIn(0f, 1f),
            (color.green * 0.5f).coerceIn(0f, 1f),
            (color.blue * 0.5f).coerceIn(0f, 1f),
            1f
        )

        // If physical kinematic chains are provided, render elastic chain segments with water inertia
        if (chains != null && chains.isNotEmpty()) {
            for (chain in chains) {
                val segs = chain.segments
                for (i in 0 until segs.size) {
                    val seg = segs[i]
                    val beadCenter = Offset(seg.position.x, seg.position.y)
                    val r = seg.radius

                    drawScope.drawCircle(color = beadOutline, radius = r + 1.5f, center = beadCenter)
                    drawScope.drawCircle(color = clayBeadColor, radius = r, center = beadCenter)
                    drawScope.drawCircle(
                        color = Color.White.copy(alpha = 0.7f),
                        radius = r * 0.35f,
                        center = beadCenter - Offset(r * 0.3f, r * 0.3f)
                    )
                }
            }
            return
        }

        val waveFreq = if (isDashing) 16f else 8.5f
        val waveAmp = radius * (if (isDashing) 0.45f else 0.3f)
        val segments = 5

        val offsets = when (count) {
            1 -> FLAGELLA_OFFSETS_1
            2 -> FLAGELLA_OFFSETS_2
            3 -> FLAGELLA_OFFSETS_3
            else -> FLAGELLA_OFFSETS_4
        }

        for (tailIdx in 0 until offsets.size) {
            val yFactor = offsets[tailIdx]
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
        val hairCount = 8 + (ciliaTier * 3)
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
            1 -> SPIKE_ANGLES_1
            2 -> SPIKE_ANGLES_2
            3 -> SPIKE_ANGLES_3
            else -> SPIKE_ANGLES_4
        }

        for (i in 0 until spikeAngles.size) {
            val ang = spikeAngles[i]
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

        drawSingleCartoonEye(drawScope, eye1Center, eyeR, eyeType, isDashing, damageFlash)
        drawSingleCartoonEye(drawScope, eye2Center, eyeR, eyeType, isDashing, damageFlash)
    }

    private fun drawSingleCartoonEye(
        drawScope: DrawScope,
        eyePos: Offset,
        eyeR: Float,
        eyeType: String,
        isDashing: Boolean,
        damageFlash: Boolean
    ) {
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

    private fun drawCartoonMouth(
        drawScope: DrawScope,
        center: Offset,
        radius: Float,
        mouthType: DietType,
        time: Float,
        jawAperture: Float = 1.0f,
        isBiting: Boolean = false
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
                // Articulated physical carnivore jaws snapping shut or stalking open
                val effectiveAperture = if (isBiting) 0.15f else jawAperture.coerceIn(0.2f, 1.3f)
                val jawW = radius * 0.48f
                val jawH = radius * (0.15f + 0.38f * effectiveAperture)

                reusablePath2.reset()
                reusablePath2.moveTo(mouthX, mouthY - jawH)
                reusablePath2.lineTo(mouthX + jawW, mouthY - jawH * 0.35f)
                reusablePath2.lineTo(mouthX + (jawW * 0.35f), mouthY)
                reusablePath2.lineTo(mouthX + jawW, mouthY + jawH * 0.35f)
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

                // Interlocking conical teeth
                val toothOffset = jawH * 0.4f
                val t1 = Offset(mouthX + jawW * 0.85f, mouthY - toothOffset)
                val t2 = Offset(mouthX + jawW * 0.85f, mouthY + toothOffset)
                val toothR = radius * (if (isBiting) 0.14f else 0.11f)
                drawScope.drawCircle(color = Color.White, radius = toothR, center = t1)
                drawScope.drawCircle(color = Color.White, radius = toothR, center = t2)
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

    fun drawFood(
        drawScope: DrawScope,
        screenX: Float,
        screenY: Float,
        radius: Float,
        kind: FoodKind,
        foodId: Long,
        timeSeconds: Float
    ) {
        val center = Offset(screenX, screenY)
        val r = radius

        when (kind) {
            FoodKind.ALGAE -> {
                // Cartoon Green Pea with Glossy Clay Shine (zero shader allocation)
                val wobble = sin(timeSeconds * 4f + foodId) * 0.8f
                val effectiveR = r + wobble

                // Dark outline
                drawScope.drawCircle(
                    color = Color(0xFF1B5E20),
                    radius = effectiveR + 1.8f,
                    center = center
                )
                // Bright lime-emerald clay body
                drawScope.drawCircle(
                    color = Color(0xFF00E676),
                    radius = effectiveR,
                    center = center
                )
                // Inner light core
                drawScope.drawCircle(
                    color = Color(0xFF69F0AE),
                    radius = effectiveR * 0.65f,
                    center = center - Offset(effectiveR * 0.2f, effectiveR * 0.2f)
                )
                // White specular clay highlight
                drawScope.drawCircle(
                    color = Color.White.copy(alpha = 0.85f),
                    radius = effectiveR * 0.35f,
                    center = center - Offset(effectiveR * 0.32f, effectiveR * 0.32f)
                )
            }
            FoodKind.MEAT_CHUNK -> {
                // Cartoon Comic Meat / Ham with White Marbling (zero shader allocation)
                // Dark contour
                drawScope.drawCircle(
                    color = Color(0xFF880E4F),
                    radius = r + 2f,
                    center = center
                )
                // Coral-red clay body
                drawScope.drawCircle(
                    color = Color(0xFFFF5252),
                    radius = r,
                    center = center
                )
                drawScope.drawCircle(
                    color = Color(0xFFFF8A80),
                    radius = r * 0.60f,
                    center = center - Offset(r * 0.15f, r * 0.15f)
                )
                // White comic fat marbling dot
                drawScope.drawCircle(
                    color = Color.White.copy(alpha = 0.9f),
                    radius = r * 0.35f,
                    center = center + Offset(r * 0.15f, -r * 0.1f)
                )
            }
            FoodKind.DNA_STRAND -> {
                // Golden Gummy DNA Helix (zero shader allocation)
                val pulse = sin(timeSeconds * 6f + foodId) * 1.5f
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
                    color = Color(0xFFFFD600),
                    radius = effectiveR,
                    center = center
                )
                drawScope.drawCircle(
                    color = Color(0xFFFFF59D),
                    radius = effectiveR * 0.65f,
                    center = center - Offset(effectiveR * 0.18f, effectiveR * 0.18f)
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
