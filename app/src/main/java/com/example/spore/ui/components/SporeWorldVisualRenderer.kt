package com.example.spore.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import com.example.spore.game.engine.AcousticWave
import com.example.spore.game.engine.HeartBubble
import com.example.spore.game.engine.MeteorShard
import com.example.spore.game.engine.PartCapsule
import com.example.spore.game.engine.SymbioticMate
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

object SporeWorldVisualRenderer {

    private val rockPath = Path()
    private val heartPath = Path()

    // ---------------------------------------------------------------------
    // Cached UNIT radial gradients + canvas transforms: each gradient is built
    // once (center = origin, radius = 1) and positioned via withTransform,
    // eliminating per-frame Brush/List allocations while keeping identical
    // on-screen geometry.
    // ---------------------------------------------------------------------

    private var meteorCoreBrush: Brush? = null
    private var meteorCoreKey = Int.MIN_VALUE
    private fun meteorCoreBrush(coreColor: Color): Brush {
        val k = coreColor.hashCode()
        var b = meteorCoreBrush
        if (b == null || k != meteorCoreKey) {
            b = Brush.radialGradient(
                colors = listOf(Color.White, coreColor, Color.Transparent),
                center = Offset.Zero,
                radius = 1f
            )
            meteorCoreBrush = b
            meteorCoreKey = k
        }
        return b
    }

    private var crustBrush: Brush? = null
    private var crustKey = Long.MIN_VALUE
    private fun meteorCrustBrush(coreColor: Color, crustedColor: Color, radiusPx: Float): Brush {
        // Radius quantized to 4px steps (dynamic zoom changes it slowly with biomass).
        // The brush is centered at the ORIGIN in unit space; the caller translates
        // the canvas to the meteor center, so the gradient follows the shard.
        val k = (crustedColor.hashCode().toLong() shl 12) or ((radiusPx / 4f).toLong() and 0xFFF)
        var b = crustBrush
        if (b == null || k != crustKey) {
            b = Brush.radialGradient(
                colors = listOf(
                    coreColor.copy(alpha = 0.85f),
                    crustedColor,
                    Color(0xFF2E1C14)
                ),
                center = Offset.Zero,
                radius = radiusPx
            )
            crustBrush = b
            crustKey = k
        }
        return b
    }

    private var capsuleAuraBrush: Brush? = null
    private var capsuleAuraKey = Int.MIN_VALUE
    private fun capsuleAuraBrush(iconColor: Color): Brush {
        val k = iconColor.hashCode()
        var b = capsuleAuraBrush
        if (b == null || k != capsuleAuraKey) {
            b = Brush.radialGradient(
                colors = listOf(
                    iconColor.copy(alpha = 0.45f),
                    iconColor.copy(alpha = 0.15f),
                    Color.Transparent
                ),
                center = Offset.Zero,
                radius = 1f
            )
            capsuleAuraBrush = b
            capsuleAuraKey = k
        }
        return b
    }

    private var capsuleBodyBrush: Brush? = null
    private var capsuleBodyKey = Int.MIN_VALUE
    private fun capsuleBodyBrush(iconColor: Color): Brush {
        val k = iconColor.hashCode()
        var b = capsuleBodyBrush
        if (b == null || k != capsuleBodyKey) {
            b = Brush.radialGradient(
                colors = listOf(
                    Color.White,
                    iconColor.copy(alpha = 0.85f),
                    iconColor.copy(alpha = 0.4f)
                ),
                // Unit-space offset center (-0.25, -0.25), matching the previous
                // center - Offset(0.25 * radius, 0.25 * radius) in screen space.
                center = Offset(-0.25f, -0.25f),
                radius = 1f
            )
            capsuleBodyBrush = b
            capsuleBodyKey = k
        }
        return b
    }

    private val mateHaloBrush by lazy {
        Brush.radialGradient(
            colors = listOf(
                Color(0xFFFF4081).copy(alpha = 0.35f),
                Color(0xFFFF80AB).copy(alpha = 0.15f),
                Color.Transparent
            ),
            center = Offset.Zero,
            radius = 1f
        )
    }

    fun drawMeteorShard(
        drawScope: DrawScope,
        meteor: MeteorShard,
        screenPos: Offset,
        zoom: Float,
        timeSeconds: Float
    ) {
        val drawRadius = meteor.radius * zoom
        val screenX = screenPos.x
        val screenY = screenPos.y

        if (screenX < -drawRadius * 2 || screenX > drawScope.size.width + drawRadius * 2 ||
            screenY < -drawRadius * 2 || screenY > drawScope.size.height + drawRadius * 2
        ) return

        val center = screenPos

        drawScope.rotate(meteor.rotation * 57.2958f, center) {
            // Drop shadow
            this.drawCircle(
                color = Color.Black.copy(alpha = 0.35f),
                radius = drawRadius * 1.15f,
                center = center + Offset(drawRadius * 0.25f, drawRadius * 0.35f)
            )

            // Outer crusted rock polygon (built CENTER-RELATIVE so the cached
            // origin-centered gradient brush follows the shard via translate)
            rockPath.reset()
            val points = 8
            val seed = (meteor.id % 100).toFloat()
            for (i in 0 until points) {
                val angle = (i.toFloat() / points) * 2f * PI.toFloat()
                val radiusWobble = drawRadius * (0.85f + 0.25f * sin(angle * 3f + seed))
                val px = cos(angle) * radiusWobble
                val py = sin(angle) * radiusWobble
                if (i == 0) rockPath.moveTo(px, py) else rockPath.lineTo(px, py)
            }
            rockPath.close()

            this.withTransform({ translate(center.x, center.y) }) {
                // Crusted mineral fill (cached brush, rebuilt only when zoom crosses a 4px step)
                drawPath(
                    path = rockPath,
                    brush = meteorCrustBrush(meteor.coreColor, meteor.crustedColor, drawRadius),
                    style = Fill
                )

                // Crust edge stroke (pure translate does not distort stroke width)
                drawPath(
                    path = rockPath,
                    color = Color(0xFF8D6E63),
                    style = StrokeCache.plain(2.5f * zoom)
                )
            }

            // Inner glowing genetic core / cracks (cached unit gradient + transform)
            val pulse = 0.8f + 0.2f * sin(timeSeconds * 4f + meteor.wobblePhase)
            val coreR = drawRadius * 0.55f * pulse
            this.withTransform({
                translate(center.x, center.y)
                scale(coreR, coreR)
            }) {
                drawCircle(
                    brush = meteorCoreBrush(meteor.coreColor),
                    radius = 1f,
                    center = Offset.Zero
                )
            }

            // Health indicator if damaged
            if (meteor.health < meteor.maxHealth) {
                val healthPct = (meteor.health / meteor.maxHealth).coerceIn(0f, 1f)
                this.drawArc(
                    color = Color(0xFFFF5252),
                    startAngle = -90f,
                    sweepAngle = 360f * healthPct,
                    useCenter = false,
                    topLeft = center - Offset(drawRadius * 1.3f, drawRadius * 1.3f),
                    size = Size(drawRadius * 2.6f, drawRadius * 2.6f),
                    style = StrokeCache.round(3f * zoom)
                )
            }
        }
    }

    fun drawPartCapsule(
        drawScope: DrawScope,
        capsule: PartCapsule,
        screenPos: Offset,
        zoom: Float,
        timeSeconds: Float
    ) {
        val drawRadius = capsule.radius * zoom
        val screenX = screenPos.x
        val screenY = screenPos.y

        if (screenX < -drawRadius * 2 || screenX > drawScope.size.width + drawRadius * 2 ||
            screenY < -drawRadius * 2 || screenY > drawScope.size.height + drawRadius * 2
        ) return

        val center = screenPos
        val pulse = 1f + 0.12f * sin(timeSeconds * 5f + capsule.wobblePhase)
        val rot = (timeSeconds * 45f) % 360f

        // Outer beacon aura (cached unit gradient + transform)
        val auraR = drawRadius * 2.5f * pulse
        drawScope.withTransform({
            translate(center.x, center.y)
            scale(auraR, auraR)
        }) {
            drawCircle(
                brush = capsuleAuraBrush(capsule.iconColor),
                radius = 1f,
                center = Offset.Zero
            )
        }

        // Rotating DNA spark ring (cached stroke + dash effect)
        drawScope.rotate(rot, center) {
            this.drawCircle(
                color = capsule.iconColor.copy(alpha = 0.8f),
                radius = drawRadius * 1.25f,
                center = center,
                style = StrokeCache.plainDashed(2f * zoom, 12f * zoom, 8f * zoom)
            )
        }

        // Inner translucent vesicle body (cached unit gradient + transform;
        // gradient radius = drawRadius, drawn circle radius = drawRadius * pulse)
        drawScope.withTransform({
            translate(center.x, center.y)
            scale(drawRadius, drawRadius)
        }) {
            drawCircle(
                brush = capsuleBodyBrush(capsule.iconColor),
                radius = pulse,
                center = Offset.Zero
            )
        }

        // Vesicle membrane rim
        drawScope.drawCircle(
            color = Color.White.copy(alpha = 0.85f),
            radius = drawRadius * pulse,
            center = center,
            style = StrokeCache.plain(2.5f * zoom)
        )

        // Center Organ Core / Fossil icon badge
        drawScope.drawCircle(
            color = Color.White,
            radius = drawRadius * 0.35f,
            center = center
        )
    }

    fun drawAcousticWave(
        drawScope: DrawScope,
        wave: AcousticWave,
        screenPos: Offset,
        zoom: Float
    ) {
        val drawRadius = wave.currentRadius * zoom
        val center = screenPos

        val alpha = wave.alpha.coerceIn(0f, 1f)
        if (alpha <= 0.02f) return

        // Multi-ring acoustic sonar pulse
        drawScope.drawCircle(
            color = wave.color.copy(alpha = alpha * 0.6f),
            radius = drawRadius,
            center = center,
            style = StrokeCache.plain(3.5f * zoom)
        )

        if (drawRadius > 25f * zoom) {
            drawScope.drawCircle(
                color = wave.color.copy(alpha = alpha * 0.35f),
                radius = drawRadius * 0.75f,
                center = center,
                style = StrokeCache.plain(2f * zoom)
            )
        }

        if (drawRadius > 50f * zoom) {
            drawScope.drawCircle(
                color = wave.color.copy(alpha = alpha * 0.18f),
                radius = drawRadius * 0.5f,
                center = center,
                style = StrokeCache.plain(1.5f * zoom)
            )
        }
    }

    fun drawSymbioticMate(
        drawScope: DrawScope,
        mate: SymbioticMate,
        screenPos: Offset,
        zoom: Float,
        timeSeconds: Float
    ) {
        val center = screenPos
        val radius = mate.radius * zoom

        // Romantic Pheromone Halo (cached unit gradient + transform)
        val haloPulse = 1f + 0.15f * sin(timeSeconds * 6f)
        val haloR = radius * 2.2f * haloPulse
        drawScope.withTransform({
            translate(center.x, center.y)
            scale(haloR, haloR)
        }) {
            drawCircle(
                brush = mateHaloBrush,
                radius = 1f,
                center = Offset.Zero
            )
        }

        // Draw Cell Body with cute blushing color
        CellVisualRenderer.drawCell(
            drawScope = drawScope,
            center = center,
            radius = radius,
            angleRad = mate.angle,
            primaryColor = Color(0xFFFF80AB),
            mouthType = mate.mouthType,
            flagellaCount = 2,
            ciliaCount = 2,
            spikesCount = 0,
            hasPoison = false,
            hasElectric = false,
            eyeType = "BASIC",
            timeSeconds = timeSeconds,
            softBody = mate.softBody,
            flagellaChains = mate.flagellaChains,
            jawAperture = mate.jawAperture,
            isBiting = false,
            drawShadow = true
        )

        // Heart badge floating above mate
        val heartCenter = center + Offset(0f, -radius * 1.5f + sin(timeSeconds * 5f) * 4f)
        drawHeart(drawScope, heartCenter, size = 16f * zoom, color = Color(0xFFFF1744))
    }

    fun drawHeartBubble(
        drawScope: DrawScope,
        heart: HeartBubble,
        screenPos: Offset,
        zoom: Float
    ) {
        val center = screenPos
        val size = 18f * heart.scale * zoom
        val alpha = heart.alpha.coerceIn(0f, 1f)

        drawHeart(drawScope, center, size, Color(0xFFFF4081).copy(alpha = alpha))
    }

    private fun drawHeart(drawScope: DrawScope, center: Offset, size: Float, color: Color) {
        heartPath.reset()
        val s = size
        val topY = center.y - s * 0.4f
        val botY = center.y + s * 0.5f

        heartPath.moveTo(center.x, topY + s * 0.2f)
        heartPath.cubicTo(
            center.x - s * 0.55f, topY - s * 0.4f,
            center.x - s * 0.65f, topY + s * 0.25f,
            center.x, botY
        )
        heartPath.cubicTo(
            center.x + s * 0.65f, topY + s * 0.25f,
            center.x + s * 0.55f, topY - s * 0.4f,
            center.x, topY + s * 0.2f
        )
        heartPath.close()

        drawScope.drawPath(heartPath, color = color, style = Fill)
        drawScope.drawPath(heartPath, color = Color.White.copy(alpha = color.alpha * 0.8f), style = StrokeCache.plain(1.2f))
    }
}
