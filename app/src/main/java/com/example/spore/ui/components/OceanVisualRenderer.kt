package com.example.spore.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.spore.game.terrain.CoralStructure
import com.example.spore.game.terrain.HydrothermalVent
import com.example.spore.game.terrain.KelpPlant
import com.example.spore.game.terrain.OceanBiomeType
import com.example.spore.game.terrain.OceanTerrainSystem
import com.example.spore.game.water.WaterRippleSystem
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * High-performance, zero-allocation visual renderer for oceanic terrain,
 * seabed bathymetry, water ripples, swimming wakes, and sun caustics.
 * Fully adapted to each planet's color identity (especially Ametistia's amethyst/violet palette).
 */
object OceanVisualRenderer {

    private val kelpPath = Path()

    /**
     * Renders the living ocean floor: sand/silt ridges, swaying kelp fronds,
     * hydrothermal vents, and bioluminescent corals.
     */
    fun drawSeabedTerrain(
        drawScope: DrawScope,
        terrain: OceanTerrainSystem,
        camX: Float,
        camY: Float,
        zoom: Float,
        viewLeft: Float,
        viewRight: Float,
        viewTop: Float,
        viewBottom: Float,
        worldWidth: Float,
        worldHeight: Float,
        timeSeconds: Float
    ) {
        // 1. Draw Submerged Silt/Sand Ripples in shallows (themed per planet)
        drawSandDuneRipples(
            drawScope = drawScope,
            camX = camX,
            camY = camY,
            zoom = zoom,
            viewTop = viewTop,
            viewBottom = viewBottom,
            timeSeconds = timeSeconds,
            duneColor = terrain.theme.duneColor
        )

        // 2. Draw Bioluminescent Corals (Culled to viewport)
        for (coral in terrain.corals) {
            val cx = coral.position.x
            val cy = coral.position.y
            if (isInView(cx, cy, viewLeft, viewRight, viewTop, viewBottom)) {
                val screenPos = Offset(cx * zoom + camX, cy * zoom + camY)
                drawCoral(drawScope, screenPos, coral, zoom, timeSeconds)
            }
        }

        // 3. Draw Hydrothermal Vents & Bubble Plumes (Culled)
        for (vent in terrain.vents) {
            val vx = vent.position.x
            val vy = vent.position.y
            if (isInView(vx, vy, viewLeft, viewRight, viewTop, viewBottom)) {
                val screenPos = Offset(vx * zoom + camX, vy * zoom + camY)
                drawHydrothermalVent(drawScope, screenPos, vent, zoom, timeSeconds)
            }
        }

        // 4. Draw Vent Bubbles
        for (bubble in terrain.ventBubbles) {
            val bx = bubble.position.x
            val by = bubble.position.y
            if (isInView(bx, by, viewLeft, viewRight, viewTop, viewBottom)) {
                val screenPos = Offset(bx * zoom + camX, by * zoom + camY)
                drawScope.drawCircle(
                    color = bubble.color.copy(alpha = bubble.alpha),
                    radius = bubble.radius * zoom,
                    center = screenPos
                )
            }
        }

        // 5. Draw Swaying Kelp Forests (Culled)
        for (kelp in terrain.kelpPlants) {
            val kx = kelp.anchor.x
            val ky = kelp.anchor.y
            if (isInView(kx, ky, viewLeft, viewRight, viewTop, viewBottom)) {
                val screenAnchor = Offset(kx * zoom + camX, ky * zoom + camY)
                drawSwayingKelp(drawScope, screenAnchor, kelp, zoom, timeSeconds, terrain.theme.duneColor)
            }
        }
    }

    /**
     * Renders expanding water ripple rings, swimming wakes, and cavitation bubbles.
     */
    fun drawWaterRipplesAndWakes(
        drawScope: DrawScope,
        rippleSystem: WaterRippleSystem,
        camX: Float,
        camY: Float,
        zoom: Float,
        viewLeft: Float,
        viewRight: Float,
        viewTop: Float,
        viewBottom: Float,
        troughColor: Color = Color(0xFF010A14)
    ) {
        // Draw active ripples
        for (i in 0 until WaterRippleSystem.MAX_RIPPLES) {
            val ripple = rippleSystem.ripplePool[i]
            if (!ripple.isActive) continue

            val rx = ripple.position.x
            val ry = ripple.position.y
            if (!isInView(rx, ry, viewLeft, viewRight, viewTop, viewBottom)) continue

            val screenPos = Offset(rx * zoom + camX, ry * zoom + camY)
            val scaledRadius = ripple.currentRadius * zoom
            val scaledWidth = (ripple.wavelength * 0.75f * zoom).coerceAtLeast(2.5f)
            val crestAlpha = (ripple.amplitude * 0.55f).coerceIn(0f, 1f)
            val troughAlpha = (ripple.amplitude * 0.30f).coerceIn(0f, 1f)

            // Inner dark fluid trough (planet-adapted relief)
            drawScope.drawCircle(
                color = troughColor.copy(alpha = troughAlpha),
                radius = (scaledRadius - scaledWidth * 0.5f).coerceAtLeast(1f),
                center = screenPos,
                style = Stroke(width = scaledWidth * 0.8f)
            )

            // Outer bright wave crest highlight (cresta iluminada)
            drawScope.drawCircle(
                color = ripple.color.copy(alpha = crestAlpha),
                radius = scaledRadius,
                center = screenPos,
                style = Stroke(width = scaledWidth)
            )

            // Sharp specular white reflection line on the wave crest
            if (ripple.amplitude > 0.35f) {
                drawScope.drawCircle(
                    color = Color.White.copy(alpha = (ripple.amplitude * 0.45f).coerceIn(0f, 0.7f)),
                    radius = scaledRadius,
                    center = screenPos,
                    style = Stroke(width = (scaledWidth * 0.35f).coerceAtLeast(1.2f))
                )
            }

            // Directional V-wake arms if generated by swimming movement
            if (ripple.isWake) {
                drawWakeVLine(drawScope, screenPos, ripple.wakeAngle, scaledRadius, ripple.amplitude, ripple.color)
            }
        }

        // Draw cavitation bubbles from swimming propulsion
        for (i in 0 until WaterRippleSystem.MAX_BUBBLES) {
            val bubble = rippleSystem.bubblePool[i]
            if (!bubble.isActive) continue

            val bx = bubble.position.x
            val by = bubble.position.y
            if (!isInView(bx, by, viewLeft, viewRight, viewTop, viewBottom)) continue

            val screenPos = Offset(bx * zoom + camX, by * zoom + camY)
            val r = (bubble.radius * zoom).coerceAtLeast(1f)

            drawScope.drawCircle(
                color = bubble.color.copy(alpha = bubble.alpha),
                radius = r,
                center = screenPos
            )
            // Bubble specular gloss
            drawScope.drawCircle(
                color = Color.White.copy(alpha = bubble.alpha * 0.8f),
                radius = (r * 0.4f).coerceAtLeast(0.6f),
                center = screenPos - Offset(r * 0.35f, r * 0.35f)
            )
        }
    }

    private fun drawWakeVLine(
        drawScope: DrawScope,
        center: Offset,
        angle: Float,
        radius: Float,
        amplitude: Float,
        color: Color
    ) {
        val wakeSpan = 0.65f // V opening angle
        val leftAngle = angle + PI.toFloat() - wakeSpan
        val rightAngle = angle + PI.toFloat() + wakeSpan

        val leftEnd = center + Offset(cos(leftAngle) * radius, sin(leftAngle) * radius)
        val rightEnd = center + Offset(cos(rightAngle) * radius, sin(rightAngle) * radius)

        val alpha = (amplitude * 0.35f).coerceIn(0f, 0.6f)
        drawScope.drawLine(
            color = color.copy(alpha = alpha),
            start = center,
            end = leftEnd,
            strokeWidth = 3f,
            cap = StrokeCap.Round
        )
        drawScope.drawLine(
            color = color.copy(alpha = alpha),
            start = center,
            end = rightEnd,
            strokeWidth = 3f,
            cap = StrokeCap.Round
        )
    }

    /**
     * Renders sunlight caustics and underwater atmospheric god rays matching the planet's palette.
     */
    fun drawWaterCausticsAndSunbeams(
        drawScope: DrawScope,
        screenWidth: Float,
        screenHeight: Float,
        camX: Float,
        camY: Float,
        zoom: Float,
        timeSeconds: Float,
        biome: OceanBiomeType,
        causticColor: Color,
        sunbeamColor: Color
    ) {
        if (biome.causticsIntensity <= 0.05f) return

        // 1. Moving Caustics Mesh Lattice (colored per planet theme)
        val causticSpacing = 160f * zoom
        val startX = (camX % causticSpacing) - causticSpacing
        val startY = (camY % causticSpacing) - causticSpacing
        val alphaBase = 0.08f * biome.causticsIntensity

        var x = startX
        while (x < screenWidth + causticSpacing) {
            val waveOsc = sin(x * 0.015f + timeSeconds * 1.2f) * 14f * zoom
            drawScope.drawLine(
                color = causticColor.copy(alpha = alphaBase),
                start = Offset(x + waveOsc, 0f),
                end = Offset(x - waveOsc, screenHeight),
                strokeWidth = 12f * zoom
            )
            x += causticSpacing
        }

        var y = startY
        while (y < screenHeight + causticSpacing) {
            val waveOsc = cos(y * 0.015f + timeSeconds * 1.5f) * 14f * zoom
            drawScope.drawLine(
                color = causticColor.copy(alpha = alphaBase * 0.85f),
                start = Offset(0f, y + waveOsc),
                end = Offset(screenWidth, y - waveOsc),
                strokeWidth = 10f * zoom
            )
            y += causticSpacing
        }

        // 2. Underwater Sunbeams (God Rays)
        if (biome == OceanBiomeType.SUNLIT_SHALLOWS || biome == OceanBiomeType.CORAL_REEF) {
            val rayCount = 4
            for (i in 0 until rayCount) {
                val rayPhase = timeSeconds * 0.35f + (i * 1.6f)
                val rayX = screenWidth * (0.15f + i * 0.25f) + sin(rayPhase) * 60f
                val rayAlpha = (0.045f + sin(rayPhase * 1.5f) * 0.02f) * biome.causticsIntensity

                drawScope.drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            sunbeamColor.copy(alpha = rayAlpha),
                            sunbeamColor.copy(alpha = rayAlpha * 0.35f),
                            Color.Transparent
                        ),
                        start = Offset(rayX, 0f),
                        end = Offset(rayX + 180f, screenHeight)
                    ),
                    topLeft = Offset(rayX - 60f, 0f),
                    size = Size(180f, screenHeight)
                )
            }
        }
    }

    private fun drawSandDuneRipples(
        drawScope: DrawScope,
        camX: Float,
        camY: Float,
        zoom: Float,
        viewTop: Float,
        viewBottom: Float,
        timeSeconds: Float,
        duneColor: Color
    ) {
        val duneSpacing = 180f
        val startDuneY = (viewTop / duneSpacing).toInt() * duneSpacing
        var dy = startDuneY
        while (dy <= viewBottom + duneSpacing) {
            val screenY = dy * zoom + camY
            val waveShift = sin(dy * 0.02f + timeSeconds * 0.4f) * 8f * zoom
            drawScope.drawLine(
                color = duneColor.copy(alpha = 0.16f),
                start = Offset(0f, screenY + waveShift),
                end = Offset(drawScope.size.width, screenY + waveShift + 6f * zoom),
                strokeWidth = 5f * zoom
            )
            dy += duneSpacing
        }
    }

    private fun drawSwayingKelp(
        drawScope: DrawScope,
        screenAnchor: Offset,
        kelp: KelpPlant,
        zoom: Float,
        timeSeconds: Float,
        holdfastColor: Color
    ) {
        kelpPath.reset()
        kelpPath.moveTo(screenAnchor.x, screenAnchor.y)

        val totalHeight = kelp.height * zoom
        val segmentLength = totalHeight / kelp.segmentCount

        var currentX = screenAnchor.x
        var currentY = screenAnchor.y

        for (seg in 1..kelp.segmentCount) {
            val progress = seg.toFloat() / kelp.segmentCount
            val swayAngle = sin(timeSeconds * kelp.swayFrequency + kelp.swayPhase + progress * 2.5f)
            val swayDisplacement = swayAngle * (progress * 38f * zoom)

            val nextX = screenAnchor.x + swayDisplacement
            val nextY = screenAnchor.y - (seg * segmentLength)

            // Quadratic bezier for fluid curve
            kelpPath.quadraticTo(
                (currentX + nextX) / 2f + swayAngle * 10f * zoom,
                (currentY + nextY) / 2f,
                nextX,
                nextY
            )

            // Draw kelp blade / leaf node
            if (seg % 2 == 0) {
                val leafDir = if (seg % 4 == 0) 1f else -1f
                val leafWidth = (22f * zoom).coerceAtLeast(4f)
                val leafHeight = (12f * zoom).coerceAtLeast(3f)
                drawScope.drawOval(
                    color = kelp.frondColor,
                    topLeft = Offset(nextX + (leafDir * leafWidth * 0.2f), nextY - leafHeight * 0.5f),
                    size = Size(leafWidth, leafHeight)
                )
            }

            currentX = nextX
            currentY = nextY
        }

        // Draw main flexible kelp stalk
        drawScope.drawPath(
            path = kelpPath,
            color = kelp.frondColor.copy(alpha = 0.85f),
            style = Stroke(width = (kelp.stalkWidth * zoom).coerceAtLeast(2f), cap = StrokeCap.Round)
        )

        // Holdfast anchor rock
        drawScope.drawCircle(
            color = holdfastColor,
            radius = 7f * zoom,
            center = screenAnchor
        )
    }

    private fun drawHydrothermalVent(
        drawScope: DrawScope,
        screenPos: Offset,
        vent: HydrothermalVent,
        zoom: Float,
        timeSeconds: Float
    ) {
        val w = vent.chimneyWidth * zoom
        val h = vent.chimneyHeight * zoom

        // Vent chimney body (basalt rock)
        val chimneyLeft = screenPos.x - w / 2f
        val chimneyTop = screenPos.y - h
        drawScope.drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF1A1A1A), Color(0xFF0D0D0D)),
                startY = chimneyTop,
                endY = screenPos.y
            ),
            topLeft = Offset(chimneyLeft, chimneyTop),
            size = Size(w, h)
        )

        // Mineral crystal rims
        drawScope.drawRect(
            color = vent.glowColor.copy(alpha = 0.75f),
            topLeft = Offset(chimneyLeft - 2f * zoom, chimneyTop),
            size = Size(w + 4f * zoom, 6f * zoom)
        )

        // Glowing thermal mouth
        val pulse = (sin(timeSeconds * 4f) * 0.15f + 0.85f)
        drawScope.drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    vent.glowColor.copy(alpha = 0.85f * pulse),
                    vent.glowColor.copy(alpha = 0.35f * pulse),
                    Color.Transparent
                ),
                center = Offset(screenPos.x, chimneyTop),
                radius = (w * 1.2f).coerceAtLeast(10f)
            ),
            radius = w * 1.2f,
            center = Offset(screenPos.x, chimneyTop)
        )
    }

    private fun drawCoral(
        drawScope: DrawScope,
        screenPos: Offset,
        coral: CoralStructure,
        zoom: Float,
        timeSeconds: Float
    ) {
        val r = coral.radius * zoom
        // Base coral mound
        drawScope.drawCircle(
            color = coral.secondaryColor.copy(alpha = 0.7f),
            radius = r,
            center = screenPos
        )

        // Radiating bioluminescent coral polyps
        for (i in 0 until coral.branchCount) {
            val angle = (i.toFloat() / coral.branchCount) * 2f * PI.toFloat()
            val branchDist = r * 0.75f
            val branchPos = screenPos + Offset(cos(angle) * branchDist, sin(angle) * branchDist)
            val polypPulse = (sin(timeSeconds * 2.5f + i) * 0.2f + 0.8f)

            drawScope.drawCircle(
                color = coral.glowColor.copy(alpha = 0.85f * polypPulse),
                radius = 5.5f * zoom,
                center = branchPos
            )
            drawScope.drawCircle(
                color = Color.White.copy(alpha = 0.9f),
                radius = 2.2f * zoom,
                center = branchPos
            )
        }
    }

    private fun isInView(x: Float, y: Float, l: Float, r: Float, t: Float, b: Float): Boolean {
        return x in l..r && y in t..b
    }
}
