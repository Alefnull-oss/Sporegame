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

            // Outer crusted rock polygon
            rockPath.reset()
            val points = 8
            val seed = (meteor.id % 100).toFloat()
            for (i in 0 until points) {
                val angle = (i.toFloat() / points) * 2f * PI.toFloat()
                val radiusWobble = drawRadius * (0.85f + 0.25f * sin(angle * 3f + seed))
                val px = center.x + cos(angle) * radiusWobble
                val py = center.y + sin(angle) * radiusWobble
                if (i == 0) rockPath.moveTo(px, py) else rockPath.lineTo(px, py)
            }
            rockPath.close()

            // Crusted mineral fill
            this.drawPath(
                path = rockPath,
                brush = Brush.radialGradient(
                    colors = listOf(
                        meteor.coreColor.copy(alpha = 0.85f),
                        meteor.crustedColor,
                        Color(0xFF2E1C14)
                    ),
                    center = center,
                    radius = drawRadius
                ),
                style = Fill
            )

            // Crust edge stroke
            this.drawPath(
                path = rockPath,
                color = Color(0xFF8D6E63),
                style = Stroke(width = 2.5f * zoom)
            )

            // Inner glowing genetic core / cracks
            val pulse = 0.8f + 0.2f * sin(timeSeconds * 4f + meteor.wobblePhase)
            this.drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White,
                        meteor.coreColor,
                        Color.Transparent
                    ),
                    center = center,
                    radius = drawRadius * 0.55f * pulse
                ),
                radius = drawRadius * 0.55f * pulse,
                center = center
            )

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
                    style = Stroke(width = 3f * zoom, cap = StrokeCap.Round)
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

        // Outer beacon aura
        drawScope.drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    capsule.iconColor.copy(alpha = 0.45f),
                    capsule.iconColor.copy(alpha = 0.15f),
                    Color.Transparent
                ),
                center = center,
                radius = drawRadius * 2.5f * pulse
            ),
            radius = drawRadius * 2.5f * pulse,
            center = center
        )

        // Rotating DNA spark ring
        drawScope.rotate(rot, center) {
            this.drawCircle(
                color = capsule.iconColor.copy(alpha = 0.8f),
                radius = drawRadius * 1.25f,
                center = center,
                style = Stroke(width = 2f * zoom, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(12f * zoom, 8f * zoom)))
            )
        }

        // Inner translucent vesicle body
        drawScope.drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White,
                    capsule.iconColor.copy(alpha = 0.85f),
                    capsule.iconColor.copy(alpha = 0.4f)
                ),
                center = center - Offset(drawRadius * 0.25f, drawRadius * 0.25f),
                radius = drawRadius
            ),
            radius = drawRadius * pulse,
            center = center
        )

        // Vesicle membrane rim
        drawScope.drawCircle(
            color = Color.White.copy(alpha = 0.85f),
            radius = drawRadius * pulse,
            center = center,
            style = Stroke(width = 2.5f * zoom)
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
            style = Stroke(width = 3.5f * zoom)
        )

        if (drawRadius > 25f * zoom) {
            drawScope.drawCircle(
                color = wave.color.copy(alpha = alpha * 0.35f),
                radius = drawRadius * 0.75f,
                center = center,
                style = Stroke(width = 2f * zoom)
            )
        }

        if (drawRadius > 50f * zoom) {
            drawScope.drawCircle(
                color = wave.color.copy(alpha = alpha * 0.18f),
                radius = drawRadius * 0.5f,
                center = center,
                style = Stroke(width = 1.5f * zoom)
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

        // Romantic Pheromone Halo
        val haloPulse = 1f + 0.15f * sin(timeSeconds * 6f)
        drawScope.drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFF4081).copy(alpha = 0.35f),
                    Color(0xFFFF80AB).copy(alpha = 0.15f),
                    Color.Transparent
                ),
                center = center,
                radius = radius * 2.2f * haloPulse
            ),
            radius = radius * 2.2f * haloPulse,
            center = center
        )

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
        drawScope.drawPath(heartPath, color = Color.White.copy(alpha = color.alpha * 0.8f), style = Stroke(width = 1.2f))
    }
}
