package com.example.spore.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import com.example.spore.game.engine.DietType
import com.example.spore.game.engine.FoodKind
import com.example.spore.game.noise.FastNoiseLite
import com.example.spore.game.physics.ElasticAppendageChain
import com.example.spore.game.physics.SoftBodyMembrane
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * 2.5D Plasticine (Plastilina) & Caricaturesque visual renderer for Spore cells.
 * Recreates the beloved original Spore Cell Stage aesthetic:
 * - 2.5D liquid depth: elevation-dependent drop shadows, fluid banking/rolling tilt.
 * - Modeled plasticine volume: spherical hemispherical clay lighting, rich cartoon crease rims,
 *   organic modeling grooves, and high-gloss specular clay sheen.
 * - Googly expressive cartoon eyes: bulging 3D clay eyeballs on raised sockets, rhythmic cartoon
 *   blinking, dynamic pupils that look ahead with googly spring, and double star glints.
 * - Goofy animated mouths: chomping carnivore jaws with conical clay fangs, pulsing herbivore
 *   suction spouts, and bendy striped omnivore proboscis.
 * - Sculpted clay horns, caterpillar paddle cilia, segmented bead flagella, and chitinous armor plates.
 * - Zero-allocation architecture for constant 60fps without GC stutter.
 */
object CellVisualRenderer {

    // Pre-allocated reusable Paths to prevent GC pauses
    private val pathBody = Path()
    private val pathShadow = Path()
    private val pathJawUpper = Path()
    private val pathJawLower = Path()
    private val pathSpike = Path()
    private val pathGroove = Path()
    private val pathArmor = Path()

    // Static flagella & spike angular offsets
    private val FLAGELLA_OFFSETS_1 = floatArrayOf(0f)
    private val FLAGELLA_OFFSETS_2 = floatArrayOf(-0.35f, 0.35f)
    private val FLAGELLA_OFFSETS_3 = floatArrayOf(-0.55f, 0f, 0.55f)
    private val FLAGELLA_OFFSETS_4 = floatArrayOf(-0.75f, -0.25f, 0.25f, 0.75f)

    private val SPIKE_ANGLES_1 = floatArrayOf(0f)
    private val SPIKE_ANGLES_2 = floatArrayOf(-0.35f, 0.35f)
    private val SPIKE_ANGLES_3 = floatArrayOf(-0.55f, 0f, 0.55f)
    private val SPIKE_ANGLES_4 = floatArrayOf(-0.75f, -0.25f, 0.25f, 0.75f)

    // Pre-allocated vertex buffers for membrane spline
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
        isBiting: Boolean = false,
        armorPlates: Int = 0,
        bankRoll: Float = 0f,
        elevationZ: Float = 0f,
        drawShadow: Boolean = true
    ) {
        val angleDeg = Math.toDegrees(angleRad.toDouble()).toFloat()

        // 2.5D Liquid Banking Scale: tilting compresses width across the bank axis
        val scaleBankY = (1.0f - abs(bankRoll) * 0.22f).coerceIn(0.75f, 1.0f)
        val bankShiftY = bankRoll * (radius * 0.16f)

        // 1. Draw 2.5D Fluid Drop Shadow beneath the cell (cast onto water column)
        if (drawShadow) {
            drawCellDropShadow(
                drawScope = drawScope,
                center = center,
                radius = radius,
                angleDeg = angleDeg,
                elevationZ = elevationZ,
                timeSeconds = timeSeconds,
                spikesCount = spikesCount,
                flagellaCount = flagellaCount
            )
        }

        drawScope.rotate(degrees = angleDeg, pivot = center) {
            scale(scaleX = 1.0f, scaleY = scaleBankY, pivot = center) {
                val shiftedCenter = center + Offset(0f, bankShiftY)

                // 2. Caterpillar-like Clay Cilia
                if (ciliaCount > 0) {
                    drawClayCilia(this, shiftedCenter, radius, ciliaCount, timeSeconds, primaryColor)
                }

                // 3. Segmented Plasticine Flagella
                if (flagellaCount > 0) {
                    drawClayFlagella(this, shiftedCenter, radius, flagellaCount, timeSeconds, isDashing, primaryColor, flagellaChains)
                }

                // 4. Chunky Sculpted Clay Spikes
                if (spikesCount > 0) {
                    drawClaySpikes(this, shiftedCenter, radius, spikesCount)
                }

                // 5. Main Chubby Clay Body (Hemispherical lighting + Modeling Grooves + Specular Sheen)
                drawClayBody(
                    drawScope = this,
                    center = shiftedCenter,
                    radius = radius,
                    primaryColor = primaryColor,
                    damageFlash = damageFlash,
                    timeSeconds = timeSeconds,
                    isDashing = isDashing,
                    softBody = softBody,
                    bankRoll = bankRoll
                )

                // 6. Layered Chitinous Clay Armor Plates
                if (armorPlates > 0) {
                    drawClayArmorPlates(this, shiftedCenter, radius, armorPlates, primaryColor)
                }

                // 7. Organelles (Toxic slime vesicle / Electric spark node)
                if (hasPoison) {
                    drawPoisonOrganelle(this, shiftedCenter, radius, timeSeconds)
                }
                if (hasElectric) {
                    drawElectricOrganelle(this, shiftedCenter, radius, timeSeconds)
                }

                // 8. Googly Expressive Cartoon Eyes (with 3D eyeballs & blinking)
                if (eyeType != "NONE") {
                    drawCartoonEyes(this, shiftedCenter, radius, eyeType, timeSeconds, isDashing, damageFlash, bankRoll)
                }

                // 9. Goofy Animated Cartoon Mouths
                drawCartoonMouth(this, shiftedCenter, radius, mouthType, timeSeconds, jawAperture, isBiting)
            }
        }
    }

    /**
     * Renders a soft 2.5D liquid drop shadow cast into the fluid depth beneath the cell.
     * Higher elevationZ (from swimming or dashing) lifts the cell, softening and expanding the shadow.
     */
    private fun drawCellDropShadow(
        drawScope: DrawScope,
        center: Offset,
        radius: Float,
        angleDeg: Float,
        elevationZ: Float,
        timeSeconds: Float,
        spikesCount: Int,
        flagellaCount: Int
    ) {
        val lightDirX = radius * (0.24f + elevationZ * 0.35f)
        val lightDirY = radius * (0.32f + elevationZ * 0.45f)
        val shadowCenter = center + Offset(lightDirX, lightDirY)
        val shadowScale = 1.0f + elevationZ * 0.20f
        val shadowAlpha = (0.32f / (1.0f + elevationZ * 0.75f)).coerceIn(0.08f, 0.36f)

        drawScope.rotate(degrees = angleDeg, pivot = shadowCenter) {
            val sRadius = radius * shadowScale

            // Body shadow
            drawScope.drawOval(
                color = Color.Black.copy(alpha = shadowAlpha),
                topLeft = Offset(shadowCenter.x - sRadius * 1.05f, shadowCenter.y - sRadius * 0.88f),
                size = Size(sRadius * 2.1f, sRadius * 1.76f)
            )

            // Flagella shadow blob
            if (flagellaCount > 0) {
                val tailLen = sRadius * 1.4f
                drawScope.drawOval(
                    color = Color.Black.copy(alpha = shadowAlpha * 0.7f),
                    topLeft = Offset(shadowCenter.x - sRadius * 0.9f - tailLen, shadowCenter.y - sRadius * 0.35f),
                    size = Size(tailLen, sRadius * 0.7f)
                )
            }

            // Spike shadow blobs
            if (spikesCount > 0) {
                drawScope.drawCircle(
                    color = Color.Black.copy(alpha = shadowAlpha * 0.5f),
                    radius = sRadius * 0.35f,
                    center = shadowCenter + Offset(sRadius * 0.8f, 0f)
                )
            }
        }
    }

    /**
     * Renders the hand-sculpted plasticine clay body.
     * Features:
     * - Organic soft-body noise contour.
     * - Rich dark cartoon ambient rim.
     * - Hemispherical spherical clay lighting (top-left warm highlight, bottom-right shaded clay).
     * - Modeled organic grooves (finger crease lines).
     * - High-gloss clay specular shine (curved sheen + dual white glints).
     * - Plump internal bouncy clay nucleus.
     */
    private fun drawClayBody(
        drawScope: DrawScope,
        center: Offset,
        radius: Float,
        primaryColor: Color,
        damageFlash: Boolean,
        timeSeconds: Float,
        isDashing: Boolean,
        softBody: SoftBodyMembrane? = null,
        bankRoll: Float = 0f
    ) {
        val baseColor = if (damageFlash) Color(0xFFFF2A4B) else primaryColor
        val darkRimColor = Color(
            (baseColor.red * 0.42f).coerceIn(0f, 1f),
            (baseColor.green * 0.42f).coerceIn(0f, 1f),
            (baseColor.blue * 0.42f).coerceIn(0f, 1f),
            1f
        )
        val warmClayColor = Color(
            (baseColor.red * 1.35f).coerceAtMost(1f),
            (baseColor.green * 1.35f).coerceAtMost(1f),
            (baseColor.blue * 1.35f).coerceAtMost(1f),
            1f
        )
        val deepClayShade = Color(
            (baseColor.red * 0.65f).coerceIn(0f, 1f),
            (baseColor.green * 0.65f).coerceIn(0f, 1f),
            (baseColor.blue * 0.65f).coerceIn(0f, 1f),
            1f
        )

        // Organic squish & breathing oscillation
        val breathe = sin(timeSeconds * 4.2f) * (radius * 0.045f)
        val dashStretch = if (isDashing) radius * 0.12f else 0f
        val rBase = radius + breathe

        val vertexCount = 16
        val step = (2f * PI.toFloat()) / vertexCount

        for (i in 0 until vertexCount) {
            val ang = i * step
            val noiseVal = organicMembraneNoise.getNoise(cos(ang) * 2.4f, sin(ang) * 2.4f + timeSeconds * 1.5f)
            val biologicalWobble = noiseVal * (radius * 0.065f)
            val physicalDeform = softBody?.deformations?.getOrNull(i) ?: 0f

            // Stretch slightly along movement axis (X) when dashing
            val stretchX = cos(ang) * dashStretch
            val r = (rBase + biologicalWobble + physicalDeform).coerceAtLeast(radius * 0.35f)
            val px = center.x + cos(ang) * r + stretchX
            val py = center.y + sin(ang) * r
            membranePoints[i] = Offset(px, py)
        }

        // Build smooth closed spline path
        pathBody.reset()
        val p0 = (membranePoints[0] + membranePoints[vertexCount - 1]) * 0.5f
        pathBody.moveTo(p0.x, p0.y)
        for (i in 0 until vertexCount) {
            val pCurrent = membranePoints[i]
            val pNext = membranePoints[(i + 1) % vertexCount]
            val mid = (pCurrent + pNext) * 0.5f
            pathBody.quadraticTo(pCurrent.x, pCurrent.y, mid.x, mid.y)
        }
        pathBody.close()

        // 1. Thick dark cartoon rim / clay crease (Contorno grueso de plastilina)
        drawScope.drawPath(
            path = pathBody,
            color = darkRimColor,
            style = Stroke(width = 7.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // 2. Base plasticine mass
        drawScope.drawPath(path = pathBody, color = baseColor, style = Fill)

        // 3. Spherical 3D Hemispherical Clay Lighting
        // Lit from upper-left sun direction
        val lightOffset = Offset(rBase * 0.26f, rBase * 0.28f + bankRoll * (rBase * 0.15f))
        val lightCenter = center - lightOffset
        drawScope.drawCircle(
            color = warmClayColor.copy(alpha = 0.72f),
            radius = rBase * 0.68f,
            center = lightCenter
        )

        // Bottom-right shaded clay undertone
        val shadeOffset = Offset(rBase * 0.22f, rBase * 0.24f)
        drawScope.drawCircle(
            color = deepClayShade.copy(alpha = 0.55f),
            radius = rBase * 0.62f,
            center = center + shadeOffset
        )

        // 4. Hand-Sculpted Clay Finger Grooves (Surcos orgánicos de modelado)
        pathGroove.reset()
        val gStart = center + Offset(-rBase * 0.45f, -rBase * 0.25f)
        val gControl = center + Offset(0f, -rBase * 0.48f)
        val gEnd = center + Offset(rBase * 0.45f, -rBase * 0.20f)
        pathGroove.moveTo(gStart.x, gStart.y)
        pathGroove.quadraticTo(gControl.x, gControl.y, gEnd.x, gEnd.y)
        drawScope.drawPath(
            path = pathGroove,
            color = darkRimColor.copy(alpha = 0.28f),
            style = Stroke(width = 3.2f, cap = StrokeCap.Round)
        )

        pathGroove.reset()
        val g2Start = center + Offset(-rBase * 0.38f, rBase * 0.22f)
        val g2Control = center + Offset(0f, rBase * 0.45f)
        val g2End = center + Offset(rBase * 0.42f, rBase * 0.25f)
        pathGroove.moveTo(g2Start.x, g2Start.y)
        pathGroove.quadraticTo(g2Control.x, g2Control.y, g2End.x, g2End.y)
        drawScope.drawPath(
            path = pathGroove,
            color = warmClayColor.copy(alpha = 0.45f),
            style = Stroke(width = 2.8f, cap = StrokeCap.Round)
        )

        // 5. Glossy Plasticine Specular Sheen (Brillo de Plastilina fresca)
        val specCenter = lightCenter - Offset(rBase * 0.08f, rBase * 0.08f)
        // Broad soft curved sheen
        drawScope.drawOval(
            color = Color.White.copy(alpha = 0.60f),
            topLeft = Offset(specCenter.x - rBase * 0.28f, specCenter.y - rBase * 0.16f),
            size = Size(rBase * 0.56f, rBase * 0.32f)
        )
        // Sharp primary specular glint
        drawScope.drawCircle(
            color = Color.White.copy(alpha = 0.95f),
            radius = rBase * 0.13f,
            center = specCenter - Offset(rBase * 0.06f, rBase * 0.04f)
        )
        // Secondary fill glint
        drawScope.drawCircle(
            color = Color.White.copy(alpha = 0.75f),
            radius = rBase * 0.06f,
            center = specCenter + Offset(rBase * 0.10f, rBase * 0.08f)
        )

        // 6. Plump Internal Bouncy Clay Nucleus
        val nucleusRadius = rBase * 0.30f
        val nucleusCenter = center + Offset(-rBase * 0.06f, rBase * 0.04f)
        drawScope.drawCircle(
            color = darkRimColor.copy(alpha = 0.65f),
            radius = nucleusRadius + 2f,
            center = nucleusCenter
        )
        drawScope.drawCircle(
            color = baseColor,
            radius = nucleusRadius,
            center = nucleusCenter
        )
        drawScope.drawCircle(
            color = warmClayColor.copy(alpha = 0.85f),
            radius = nucleusRadius * 0.65f,
            center = nucleusCenter - Offset(nucleusRadius * 0.22f, nucleusRadius * 0.22f)
        )
        drawScope.drawCircle(
            color = Color.White.copy(alpha = 0.90f),
            radius = nucleusRadius * 0.28f,
            center = nucleusCenter - Offset(nucleusRadius * 0.28f, nucleusRadius * 0.28f)
        )
    }

    /**
     * Renders layered chitinous clay armor plates along the cell's dorsal spine.
     * Gives cells that sculpted prehistoric armadillo/carapace look from Spore.
     */
    private fun drawClayArmorPlates(
        drawScope: DrawScope,
        center: Offset,
        radius: Float,
        armorPlates: Int,
        primaryColor: Color
    ) {
        val plateColor = Color(
            (primaryColor.red * 0.85f + 0.15f).coerceIn(0f, 1f),
            (primaryColor.green * 0.85f + 0.15f).coerceIn(0f, 1f),
            (primaryColor.blue * 0.85f + 0.15f).coerceIn(0f, 1f),
            1f
        )
        val rimColor = Color(0xFF263238)
        val plateCount = armorPlates.coerceIn(1, 3)

        for (i in 0 until plateCount) {
            val progress = i.toFloat() / plateCount
            val offsetX = -radius * 0.35f + (progress * radius * 0.55f)
            val plateWidth = radius * 0.48f
            val plateHeight = radius * (0.80f - progress * 0.15f)
            val plateCenter = center + Offset(offsetX, 0f)

            pathArmor.reset()
            val left = plateCenter.x - plateWidth * 0.5f
            val top = plateCenter.y - plateHeight * 0.5f
            pathArmor.addOval(Rect(left, top, left + plateWidth, top + plateHeight))

            // Dark cartoon rim
            drawScope.drawPath(pathArmor, color = rimColor, style = Stroke(width = 5.5f))
            // Clay carapace body
            drawScope.drawPath(pathArmor, color = plateColor, style = Fill)

            // Carapace ridge highlight
            drawScope.drawArc(
                color = Color.White.copy(alpha = 0.75f),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(left + 2f, top + 2f),
                size = Size(plateWidth - 4f, plateHeight - 4f),
                style = Stroke(width = 3f)
            )
        }
    }

    /**
     * Renders Googly Expressive Cartoon Eyes.
     * Features:
     * - Bulging white clay eyeballs set in plump clay sockets.
     * - Shaded 3D spherical eyeballs.
     * - Expressive iris ring.
     * - Animated pupils that look ahead with googly spring.
     * - Rhythmic cartoon blinking (eyelids close into a smiling crease).
     * - Wide comic startled look on dash/damage.
     * - Double star/glint cartoon sparkles inside each pupil.
     */
    private fun drawCartoonEyes(
        drawScope: DrawScope,
        center: Offset,
        radius: Float,
        eyeType: String,
        timeSeconds: Float,
        isDashing: Boolean,
        damageFlash: Boolean,
        bankRoll: Float
    ) {
        val eyeR = radius * 0.23f
        val eyeSpacing = radius * 0.30f
        val eyeForward = radius * 0.58f

        // Natural rhythmic cartoon blinking
        val blinkCycle = (timeSeconds * 0.85f + (center.x * 0.001f)) % 3.8f
        val isBlinking = blinkCycle < 0.15f

        val eye1Pos = center + Offset(eyeForward, -eyeSpacing + bankRoll * (radius * 0.10f))
        val eye2Pos = center + Offset(eyeForward, eyeSpacing + bankRoll * (radius * 0.10f))

        drawSingleCartoonEye(drawScope, eye1Pos, eyeR, eyeType, isDashing, damageFlash, isBlinking, timeSeconds)
        drawSingleCartoonEye(drawScope, eye2Pos, eyeR, eyeType, isDashing, damageFlash, isBlinking, timeSeconds)
    }

    private fun drawSingleCartoonEye(
        drawScope: DrawScope,
        eyePos: Offset,
        eyeR: Float,
        eyeType: String,
        isDashing: Boolean,
        damageFlash: Boolean,
        isBlinking: Boolean,
        timeSeconds: Float
    ) {
        // 1. Raised Clay Socket / Mound
        drawScope.drawCircle(
            color = Color(0xFF1A237E),
            radius = eyeR + 3.5f,
            center = eyePos
        )

        if (isBlinking && !damageFlash) {
            // Blinking: cute curved clay crease slit
            drawScope.drawCircle(
                color = Color(0xFFFAFAFA),
                radius = eyeR,
                center = eyePos
            )
            drawScope.drawLine(
                color = Color(0xFF1A237E),
                start = eyePos - Offset(eyeR * 0.85f, 0f),
                end = eyePos + Offset(eyeR * 0.85f, 0f),
                strokeWidth = 4.5f,
                cap = StrokeCap.Round
            )
            return
        }

        // 2. Glossy 3D Eyeball (Spherical shading)
        drawScope.drawCircle(
            color = Color(0xFFFDFDFD),
            radius = eyeR,
            center = eyePos
        )
        // Soft bottom eyeball shadow for spherical volume
        drawScope.drawCircle(
            color = Color(0xFFD1D5DB).copy(alpha = 0.55f),
            radius = eyeR * 0.85f,
            center = eyePos + Offset(eyeR * 0.15f, eyeR * 0.15f)
        )

        // 3. Eye Type Specialization
        if (eyeType == "COMPOUND") {
            // Iridescent multifaceted insect dome
            drawScope.drawCircle(
                color = Color(0xFFFF1744),
                radius = eyeR * 0.82f,
                center = eyePos
            )
            // Radial facet lines
            val facets = 6
            for (f in 0 until facets) {
                val fa = (f.toFloat() / facets) * (2f * PI.toFloat())
                drawScope.drawLine(
                    color = Color(0xFFFF8A80),
                    start = eyePos,
                    end = eyePos + Offset(cos(fa) * eyeR * 0.75f, sin(fa) * eyeR * 0.75f),
                    strokeWidth = 1.8f
                )
            }
            // Curved gloss arc
            drawScope.drawArc(
                color = Color.White.copy(alpha = 0.85f),
                startAngle = 190f,
                sweepAngle = 100f,
                useCenter = false,
                topLeft = eyePos - Offset(eyeR * 0.65f, eyeR * 0.65f),
                size = Size(eyeR * 1.3f, eyeR * 1.3f),
                style = Stroke(width = 3.5f)
            )
            return
        }

        // 4. Vibrant Iris Ring
        val irisRadius = eyeR * 0.65f
        val pupilOffset = if (isDashing) Offset(eyeR * 0.45f, 0f) else Offset(eyeR * 0.28f, 0f)
        val irisCenter = eyePos + pupilOffset
        drawScope.drawCircle(
            color = Color(0xFF00B0FF),
            radius = irisRadius,
            center = irisCenter
        )

        // 5. Deep Black Pupil with Cartoon Dilation
        val pupilRadius = if (damageFlash) eyeR * 0.55f else if (isDashing) eyeR * 0.42f else eyeR * 0.36f
        drawScope.drawCircle(
            color = Color(0xFF0A0E17),
            radius = pupilRadius,
            center = irisCenter
        )

        // 6. Double Cartoon Specular Sparkles (✨ Pixar / Spore twinkle)
        // Primary big white star sparkle
        drawScope.drawCircle(
            color = Color.White,
            radius = pupilRadius * 0.42f,
            center = irisCenter - Offset(pupilRadius * 0.35f, pupilRadius * 0.35f)
        )
        // Secondary little sparkle glint
        drawScope.drawCircle(
            color = Color.White.copy(alpha = 0.85f),
            radius = pupilRadius * 0.22f,
            center = irisCenter + Offset(pupilRadius * 0.35f, pupilRadius * 0.32f)
        )
    }

    /**
     * Renders Goofy Cartoon Animated Mouths.
     * Herbivore: pulsing accordion suction spout with round cartoon lips.
     * Carnivore: articulated snapping chomper jaws with cartoon fangs and gums.
     * Omnivore: flexible bendy party-blower proboscis snout.
     */
    private fun drawCartoonMouth(
        drawScope: DrawScope,
        center: Offset,
        radius: Float,
        mouthType: DietType,
        time: Float,
        jawAperture: Float = 1.0f,
        isBiting: Boolean = false
    ) {
        val mouthX = center.x + (radius * 0.94f)
        val mouthY = center.y

        when (mouthType) {
            DietType.HERBIVORE -> {
                // Chubby rubbery cartoon suction spout
                val suctionPulse = sin(time * 8f) * (radius * 0.08f)
                val lipR = (radius * 0.34f + suctionPulse).coerceAtLeast(7f)
                val snoutLen = radius * 0.22f
                val mouthPos = Offset(mouthX + snoutLen, mouthY)

                // Snout accordion tube
                drawScope.drawOval(
                    color = Color(0xFF1B5E20),
                    topLeft = Offset(mouthX - 4f, mouthY - lipR * 0.85f),
                    size = Size(snoutLen + 6f, lipR * 1.7f)
                )
                drawScope.drawOval(
                    color = Color(0xFF00E676),
                    topLeft = Offset(mouthX - 2f, mouthY - lipR * 0.75f),
                    size = Size(snoutLen + 4f, lipR * 1.5f)
                )

                // Thick dark cartoon lip contour
                drawScope.drawCircle(
                    color = Color(0xFF1B5E20),
                    radius = lipR + 3.5f,
                    center = mouthPos,
                    style = Stroke(width = 5.5f)
                )
                // Bright green pliable clay lips
                drawScope.drawCircle(
                    color = Color(0xFF00E676),
                    radius = lipR,
                    center = mouthPos,
                    style = Stroke(width = 6.5f)
                )
                // Dark inside suction void
                drawScope.drawCircle(
                    color = Color(0xFF003300),
                    radius = lipR - 3.5f,
                    center = mouthPos,
                    style = Fill
                )
                // Comical lip gloss glint
                drawScope.drawCircle(
                    color = Color.White.copy(alpha = 0.9f),
                    radius = lipR * 0.28f,
                    center = mouthPos - Offset(lipR * 0.38f, lipR * 0.38f)
                )
            }
            DietType.CARNIVORE -> {
                // Articulated physical carnivore jaws snapping shut or stalking open
                val effectiveAperture = if (isBiting) 0.12f else jawAperture.coerceIn(0.25f, 1.35f)
                val jawW = radius * 0.52f
                val jawH = radius * (0.18f + 0.36f * effectiveAperture)

                // Upper jaw
                pathJawUpper.reset()
                pathJawUpper.moveTo(mouthX, mouthY)
                pathJawUpper.lineTo(mouthX + jawW, mouthY - jawH * 0.55f)
                pathJawUpper.lineTo(mouthX + jawW * 0.75f, mouthY - jawH)
                pathJawUpper.lineTo(mouthX, mouthY - jawH * 0.65f)
                pathJawUpper.close()

                // Lower jaw
                pathJawLower.reset()
                pathJawLower.moveTo(mouthX, mouthY)
                pathJawLower.lineTo(mouthX + jawW, mouthY + jawH * 0.55f)
                pathJawLower.lineTo(mouthX + jawW * 0.75f, mouthY + jawH)
                pathJawLower.lineTo(mouthX, mouthY + jawH * 0.65f)
                pathJawLower.close()

                // Dark throat void
                drawScope.drawCircle(
                    color = Color(0xFF3E000C),
                    radius = jawH * 0.85f,
                    center = Offset(mouthX, mouthY)
                )

                // Dark jaw contours
                drawScope.drawPath(pathJawUpper, color = Color(0xFF880E4F), style = Stroke(width = 6.5f, join = StrokeJoin.Round))
                drawScope.drawPath(pathJawLower, color = Color(0xFF880E4F), style = Stroke(width = 6.5f, join = StrokeJoin.Round))

                // Bright cartoon gums
                drawScope.drawPath(pathJawUpper, color = Color(0xFFFF1744), style = Fill)
                drawScope.drawPath(pathJawLower, color = Color(0xFFFF1744), style = Fill)

                // Chunky hand-sculpted conical clay teeth
                val toothSize = radius * (if (isBiting) 0.16f else 0.13f)
                val tUpper1 = Offset(mouthX + jawW * 0.45f, mouthY - jawH * 0.35f)
                val tUpper2 = Offset(mouthX + jawW * 0.82f, mouthY - jawH * 0.45f)
                val tLower1 = Offset(mouthX + jawW * 0.55f, mouthY + jawH * 0.35f)
                val tLower2 = Offset(mouthX + jawW * 0.88f, mouthY + jawH * 0.45f)

                drawClayTooth(drawScope, tUpper1, toothSize, pointingDown = true)
                drawClayTooth(drawScope, tUpper2, toothSize * 1.1f, pointingDown = true)
                drawClayTooth(drawScope, tLower1, toothSize, pointingDown = false)
                drawClayTooth(drawScope, tLower2, toothSize * 1.1f, pointingDown = false)
            }
            DietType.OMNIVORE -> {
                // Flexible striped bendy-straw party-blower proboscis snout
                val proboscisLen = radius * 0.68f
                val wiggle = sin(time * 6f) * (radius * 0.14f)
                val tip = Offset(mouthX + proboscisLen, mouthY + wiggle)
                val base = Offset(mouthX, mouthY)

                // Outline
                drawScope.drawLine(
                    color = Color(0xFFE65100),
                    start = base,
                    end = tip,
                    strokeWidth = radius * 0.32f,
                    cap = StrokeCap.Round
                )
                // Gold body
                drawScope.drawLine(
                    color = Color(0xFFFFD600),
                    start = base,
                    end = tip,
                    strokeWidth = radius * 0.22f,
                    cap = StrokeCap.Round
                )
                // Comical orange spiral stripes
                for (s in 1..3) {
                    val st = s.toFloat() / 4f
                    val sPos = base + (tip - base) * st
                    drawScope.drawCircle(
                        color = Color(0xFFFF6D00),
                        radius = radius * 0.12f,
                        center = sPos
                    )
                }
                // Bulbous suction nozzle
                drawScope.drawCircle(
                    color = Color(0xFFFF6D00),
                    radius = radius * 0.20f,
                    center = tip
                )
                drawScope.drawCircle(
                    color = Color(0xFF4E342E),
                    radius = radius * 0.10f,
                    center = tip
                )
                drawScope.drawCircle(
                    color = Color.White.copy(alpha = 0.9f),
                    radius = radius * 0.08f,
                    center = tip - Offset(3f, 3f)
                )
            }
        }
    }

    private fun drawClayTooth(drawScope: DrawScope, pos: Offset, size: Float, pointingDown: Boolean) {
        val ySign = if (pointingDown) 1f else -1f
        pathSpike.reset()
        pathSpike.moveTo(pos.x - size * 0.6f, pos.y)
        pathSpike.lineTo(pos.x, pos.y + size * 1.4f * ySign)
        pathSpike.lineTo(pos.x + size * 0.6f, pos.y)
        pathSpike.close()

        drawScope.drawPath(pathSpike, color = Color(0xFF263238), style = Stroke(width = 4.5f, join = StrokeJoin.Round))
        drawScope.drawPath(pathSpike, color = Color(0xFFFFFDE7), style = Fill)
        drawScope.drawCircle(color = Color.White, radius = size * 0.35f, center = pos)
    }

    /**
     * Renders segmented gummy clay bead flagella trailing with water inertia.
     */
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
        val beadColor = color
        val beadOutline = Color(
            (color.red * 0.45f).coerceIn(0f, 1f),
            (color.green * 0.45f).coerceIn(0f, 1f),
            (color.blue * 0.45f).coerceIn(0f, 1f),
            1f
        )

        if (chains != null && chains.isNotEmpty()) {
            for (chain in chains) {
                val segs = chain.segments
                for (i in 0 until segs.size) {
                    val seg = segs[i]
                    val beadCenter = Offset(seg.position.x, seg.position.y)
                    val r = seg.radius

                    drawScope.drawCircle(color = beadOutline, radius = r + 2f, center = beadCenter)
                    drawScope.drawCircle(color = beadColor, radius = r, center = beadCenter)
                    drawScope.drawCircle(
                        color = Color.White.copy(alpha = 0.85f),
                        radius = r * 0.38f,
                        center = beadCenter - Offset(r * 0.32f, r * 0.32f)
                    )
                }
            }
            return
        }

        val waveFreq = if (isDashing) 18f else 9f
        val waveAmp = radius * (if (isDashing) 0.5f else 0.32f)
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
            val tailLen = radius * (if (isDashing) 2.5f else 1.8f)

            for (i in 1..segments) {
                val t = i.toFloat() / segments
                val beadX = startX - (tailLen * t)
                val phase = time * waveFreq + (tailIdx * 1.5f) + (t * 5.5f)
                val beadY = startY + sin(phase) * (waveAmp * t)
                val beadRadius = (radius * (0.18f * (1.15f - t * 0.55f))).coerceAtLeast(3.8f)

                drawScope.drawCircle(
                    color = beadOutline,
                    radius = beadRadius + 2f,
                    center = Offset(beadX, beadY)
                )
                drawScope.drawCircle(
                    color = beadColor,
                    radius = beadRadius,
                    center = Offset(beadX, beadY)
                )
                drawScope.drawCircle(
                    color = Color.White.copy(alpha = 0.85f),
                    radius = beadRadius * 0.38f,
                    center = Offset(beadX - beadRadius * 0.32f, beadY - beadRadius * 0.32f)
                )
            }
        }
    }

    /**
     * Renders chubby caterpillar paddle clay cilia waddling around the cell flanks.
     */
    private fun drawClayCilia(
        drawScope: DrawScope,
        center: Offset,
        radius: Float,
        ciliaTier: Int,
        time: Float,
        color: Color
    ) {
        val hairCount = 8 + (ciliaTier * 3)
        val hairLength = radius * 0.28f

        val darkOutline = Color(
            (color.red * 0.45f).coerceIn(0f, 1f),
            (color.green * 0.45f).coerceIn(0f, 1f),
            (color.blue * 0.45f).coerceIn(0f, 1f),
            1f
        )

        for (i in 0 until hairCount) {
            val baseAngle = (i.toFloat() / hairCount) * (2f * PI.toFloat())
            if (cos(baseAngle) > 0.60f) continue // Skip front mouth zone

            val paddleWobble = sin(time * 11f + i * 0.95f) * 0.32f
            val angle = baseAngle + paddleWobble

            val base = center + Offset(cos(angle) * (radius * 0.95f), sin(angle) * (radius * 0.95f))
            val tip = center + Offset(cos(angle) * (radius + hairLength), sin(angle) * (radius + hairLength))

            drawScope.drawLine(
                color = darkOutline,
                start = base,
                end = tip,
                strokeWidth = 7.5f,
                cap = StrokeCap.Round
            )
            drawScope.drawLine(
                color = color,
                start = base,
                end = tip,
                strokeWidth = 4.8f,
                cap = StrokeCap.Round
            )
            drawScope.drawCircle(
                color = Color.White.copy(alpha = 0.9f),
                radius = 2.8f,
                center = tip
            )
        }
    }

    /**
     * Renders chunky sculpted horns/spikes with carved ridges and ivory highlights.
     */
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
            val spikeLength = radius * 1.6f
            val baseSpread = 0.24f

            val b1 = center + Offset(cos(ang - baseSpread) * (radius * 0.92f), sin(ang - baseSpread) * (radius * 0.92f))
            val b2 = center + Offset(cos(ang + baseSpread) * (radius * 0.92f), sin(ang + baseSpread) * (radius * 0.92f))
            val tip = center + Offset(cos(ang) * spikeLength, sin(ang) * spikeLength)

            pathSpike.reset()
            pathSpike.moveTo(b1.x, b1.y)
            pathSpike.lineTo(tip.x, tip.y)
            pathSpike.lineTo(b2.x, b2.y)
            pathSpike.close()

            // Dark outline
            drawScope.drawPath(pathSpike, color = Color(0xFF263238), style = Stroke(width = 6.5f, join = StrokeJoin.Round))
            // Ivory clay horn mass
            drawScope.drawPath(pathSpike, color = Color(0xFFFFF9C4), style = Fill)

            // Carved horn ridges
            val midTip1 = (b1 + tip) * 0.5f
            val midTip2 = (b2 + tip) * 0.5f
            drawScope.drawLine(
                color = Color(0xFFFFD54F),
                start = midTip1,
                end = midTip2,
                strokeWidth = 3f,
                cap = StrokeCap.Round
            )

            // Ivory specular sheen edge
            drawScope.drawLine(
                color = Color.White,
                start = b1,
                end = tip,
                strokeWidth = 3.2f,
                cap = StrokeCap.Round
            )
        }
    }

    private fun drawPoisonOrganelle(drawScope: DrawScope, center: Offset, radius: Float, time: Float) {
        val pos = center + Offset(-radius * 0.38f, radius * 0.32f)
        val pulse = sin(time * 6f) * (radius * 0.05f)
        val orgR = radius * 0.24f + pulse

        // Bubbly slime green vesicle
        drawScope.drawCircle(color = Color(0xFF1B5E20), radius = orgR + 2.5f, center = pos)
        drawScope.drawCircle(color = Color(0xFF76FF03), radius = orgR, center = pos)
        drawScope.drawCircle(color = Color(0xFFCCFF90), radius = orgR * 0.45f, center = pos - Offset(orgR * 0.25f, orgR * 0.25f))
        // Dripping bubble
        drawScope.drawCircle(color = Color.White.copy(alpha = 0.85f), radius = orgR * 0.22f, center = pos - Offset(orgR * 0.35f, orgR * 0.35f))
    }

    private fun drawElectricOrganelle(drawScope: DrawScope, center: Offset, radius: Float, time: Float) {
        val pos = center + Offset(-radius * 0.38f, -radius * 0.32f)
        val orgR = radius * 0.24f
        val pulse = sin(time * 12f) * 0.15f + 0.85f

        drawScope.drawCircle(color = Color(0xFF006064), radius = orgR + 2.5f, center = pos)
        drawScope.drawCircle(color = Color(0xFF00E5FF).copy(alpha = pulse), radius = orgR, center = pos)
        drawScope.drawCircle(color = Color.White, radius = orgR * 0.52f, center = pos)
    }
}

/**
 * Cartoon / Plasticine food sprite renderer.
 * Algae: shiny sculpted pea with leaf dent and glossy clay sheen.
 * Meat: marbled comic ham chunk with white bone.
 * DNA: chunky glowing golden helical ribbon with sparkles.
 */
object FoodVisualRenderer {

    fun drawFood(
        drawScope: DrawScope,
        screenX: Float,
        screenY: Float,
        radius: Float,
        kind: FoodKind,
        foodId: Long,
        timeSeconds: Float,
        elevationZ: Float = 0.2f
    ) {
        val center = Offset(screenX, screenY)
        val r = radius

        // 2.5D Drop shadow beneath the food particle
        val shadowOffset = Offset(r * 0.32f, r * 0.38f)
        drawScope.drawCircle(
            color = Color.Black.copy(alpha = 0.28f),
            radius = r * 1.05f,
            center = center + shadowOffset
        )

        when (kind) {
            FoodKind.ALGAE -> {
                // Cartoon Green Pea with Glossy Clay Shine
                val wobble = sin(timeSeconds * 4.5f + foodId) * 0.8f
                val effectiveR = r + wobble

                // Dark outline
                drawScope.drawCircle(
                    color = Color(0xFF1B5E20),
                    radius = effectiveR + 2f,
                    center = center
                )
                // Bright lime-emerald clay mass
                drawScope.drawCircle(
                    color = Color(0xFF00E676),
                    radius = effectiveR,
                    center = center
                )
                // Inner warm light core
                drawScope.drawCircle(
                    color = Color(0xFF69F0AE),
                    radius = effectiveR * 0.65f,
                    center = center - Offset(effectiveR * 0.22f, effectiveR * 0.22f)
                )
                // Curved leaf dent / groove
                drawScope.drawArc(
                    color = Color(0xFF1B5E20).copy(alpha = 0.6f),
                    startAngle = 45f,
                    sweepAngle = 120f,
                    useCenter = false,
                    topLeft = center - Offset(effectiveR * 0.45f, effectiveR * 0.45f),
                    size = Size(effectiveR * 0.9f, effectiveR * 0.9f),
                    style = Stroke(width = 2.2f)
                )
                // White specular clay highlight
                drawScope.drawCircle(
                    color = Color.White.copy(alpha = 0.92f),
                    radius = effectiveR * 0.32f,
                    center = center - Offset(effectiveR * 0.32f, effectiveR * 0.32f)
                )
            }
            FoodKind.MEAT_CHUNK -> {
                // Cartoon Comic Meat Cutlet with Bone
                // Dark contour
                drawScope.drawCircle(
                    color = Color(0xFF880E4F),
                    radius = r + 2.2f,
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
                    radius = r * 0.62f,
                    center = center - Offset(r * 0.16f, r * 0.16f)
                )
                // Fat marbling arc
                drawScope.drawCircle(
                    color = Color.White.copy(alpha = 0.90f),
                    radius = r * 0.36f,
                    center = center + Offset(r * 0.18f, -r * 0.12f)
                )
                // Little white clay bone sticking out
                drawScope.drawCircle(
                    color = Color(0xFF880E4F),
                    radius = r * 0.32f + 1.5f,
                    center = center - Offset(r * 0.72f, 0f)
                )
                drawScope.drawCircle(
                    color = Color.White,
                    radius = r * 0.32f,
                    center = center - Offset(r * 0.72f, 0f)
                )
            }
            FoodKind.DNA_STRAND -> {
                // Golden Gummy DNA Helix
                val pulse = sin(timeSeconds * 6f + foodId) * 1.5f
                val effectiveR = r + pulse

                // Golden glow aura
                drawScope.drawCircle(
                    color = Color(0xFFFFD600).copy(alpha = 0.38f),
                    radius = effectiveR * 1.55f,
                    center = center
                )
                // Dark gold rim
                drawScope.drawCircle(
                    color = Color(0xFFFF6F00),
                    radius = effectiveR + 2.5f,
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
                    center = center - Offset(effectiveR * 0.20f, effectiveR * 0.20f)
                )
                // Twinkling sparkle cross
                drawScope.drawLine(
                    color = Color.White,
                    start = center - Offset(effectiveR * 0.55f, 0f),
                    end = center + Offset(effectiveR * 0.55f, 0f),
                    strokeWidth = 3f,
                    cap = StrokeCap.Round
                )
                drawScope.drawLine(
                    color = Color.White,
                    start = center - Offset(0f, effectiveR * 0.55f),
                    end = center + Offset(0f, effectiveR * 0.55f),
                    strokeWidth = 3f,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

/**
 * Pre-allocated static text paint cache to eliminate allocations in Canvas.
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
