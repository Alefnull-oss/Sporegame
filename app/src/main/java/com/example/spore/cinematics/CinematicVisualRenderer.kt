package com.example.spore.cinematics

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.spore.data.model.CellEvolutionEntity
import com.example.spore.data.model.PlanetDefinition
import com.example.spore.game.engine.DietType
import com.example.spore.ui.components.CellVisualRenderer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Procedural Scientific Visual Renderer for Planet Genesis Cinematics.
 * Renders atmospheric entry, planetary geology, catalytic chemical reactions,
 * RNA World molecular polymerization, lipid vesicles, and cellular awakening.
 */
object CinematicVisualRenderer {

    fun renderCinematicFrame(
        drawScope: DrawScope,
        screenWidth: Float,
        screenHeight: Float,
        planet: PlanetDefinition,
        cinematicDef: PlanetCinematicDefinition,
        currentPhaseIndex: Int,
        phaseProgress: Float, // 0.0f to 1.0f within the phase
        totalElapsedTime: Float,
        cellEvolution: CellEvolutionEntity
    ) {
        val center = Offset(screenWidth / 2f, screenHeight / 2f)

        when (currentPhaseIndex) {
            0 -> renderAtmosphereEntry(
                drawScope = drawScope,
                screenWidth = screenWidth,
                screenHeight = screenHeight,
                center = center,
                planet = planet,
                progress = phaseProgress,
                time = totalElapsedTime
            )
            1 -> renderGeologicalDescent(
                drawScope = drawScope,
                screenWidth = screenWidth,
                screenHeight = screenHeight,
                center = center,
                planet = planet,
                progress = phaseProgress,
                time = totalElapsedTime
            )
            2 -> renderChemicalReaction(
                drawScope = drawScope,
                screenWidth = screenWidth,
                screenHeight = screenHeight,
                center = center,
                planet = planet,
                progress = phaseProgress,
                time = totalElapsedTime
            )
            3 -> renderMolecularAssembly(
                drawScope = drawScope,
                screenWidth = screenWidth,
                screenHeight = screenHeight,
                center = center,
                planet = planet,
                progress = phaseProgress,
                time = totalElapsedTime
            )
            4 -> renderCellularAwakening(
                drawScope = drawScope,
                screenWidth = screenWidth,
                screenHeight = screenHeight,
                center = center,
                planet = planet,
                cellEvolution = cellEvolution,
                progress = phaseProgress,
                time = totalElapsedTime
            )
        }

        // Ambient floating organic dust motes across all stages
        renderCinematicMotes(drawScope, screenWidth, screenHeight, planet, totalElapsedTime)
    }

    // =========================================================================
    // FASE 0: ENTRADA ATMOSFÉRICA PLANETARIA
    // =========================================================================
    private fun renderAtmosphereEntry(
        drawScope: DrawScope,
        screenWidth: Float,
        screenHeight: Float,
        center: Offset,
        planet: PlanetDefinition,
        progress: Float,
        time: Float
    ) {
        with(drawScope) {
            // Space backdrop
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF01040A), Color(planet.oceanBgColor2)),
                    startY = 0f,
                    endY = screenHeight
                )
            )

            // Distant stars
            val rng = Random(42)
            for (i in 0 until 90) {
                val sx = rng.nextFloat() * screenWidth
                val sy = rng.nextFloat() * screenHeight * 0.7f
                val twinkle = (sin(time * 3f + i) * 0.4f + 0.6f).coerceIn(0.2f, 1f)
                drawCircle(
                    color = Color.White.copy(alpha = twinkle * 0.7f),
                    radius = if (i % 8 == 0) 2.2f else 1.2f,
                    center = Offset(sx, sy)
                )
            }

            // Planet Horizon (Enormous curved sphere rising from the bottom)
            // Camera descends closer as progress goes from 0 to 1
            val planetRadius = screenWidth * (1.2f + progress * 0.8f)
            val planetCenterY = screenHeight * (1.55f - progress * 0.55f)
            val planetCenter = Offset(screenWidth / 2f, planetCenterY)

            // Outer Atmospheric Ionization Halo
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(planet.atmosphereColorHex).copy(alpha = 0.85f),
                        Color(planet.atmosphereColorHex).copy(alpha = 0.35f),
                        Color.Transparent
                    ),
                    center = planetCenter,
                    radius = planetRadius * 1.08f
                ),
                radius = planetRadius * 1.08f,
                center = planetCenter
            )

            // Planet Surface Body
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(planet.planetColorHex),
                        Color(planet.oceanBgColor1),
                        Color(planet.oceanBgColor2)
                    ),
                    center = planetCenter - Offset(0f, planetRadius * 0.45f),
                    radius = planetRadius
                ),
                radius = planetRadius,
                center = planetCenter
            )

            // Continents / Primordial landmasses on the sphere
            drawCircle(
                color = Color(planet.continentsColorHex).copy(alpha = 0.45f),
                radius = planetRadius * 0.35f,
                center = planetCenter - Offset(planetRadius * 0.25f, planetRadius * 0.65f)
            )
            drawCircle(
                color = Color(planet.continentsColorHex).copy(alpha = 0.35f),
                radius = planetRadius * 0.28f,
                center = planetCenter - Offset(-planetRadius * 0.35f, planetRadius * 0.60f)
            )

            // Atmospheric Re-entry Fireball / Meteor Streak
            val entryY = screenHeight * (0.15f + progress * 0.55f)
            val entryX = screenWidth * (0.35f + progress * 0.15f)
            val headPos = Offset(entryX, entryY)

            // Plasma Shockwave Rings
            val wavePulse = (time * 6f) % 1f
            drawCircle(
                color = Color(0xFFFFD54F).copy(alpha = (1f - wavePulse) * 0.8f),
                radius = 25f + wavePulse * 55f,
                center = headPos,
                style = Stroke(width = 3f)
            )

            // Re-entry Fiery Plasma Tail
            val tailPath = Path().apply {
                moveTo(headPos.x, headPos.y)
                quadraticTo(
                    headPos.x - 45f,
                    headPos.y - 120f,
                    headPos.x - 70f,
                    headPos.y - 280f
                )
            }
            drawPath(
                path = tailPath,
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFFF9100), Color(0xFFFF1744), Color.Transparent),
                    startY = headPos.y,
                    endY = headPos.y - 280f
                ),
                style = Stroke(width = 16f, cap = StrokeCap.Round)
            )

            // Core Meteor / Probe Spark
            drawCircle(
                color = Color.White,
                radius = 14f,
                center = headPos
            )
            drawCircle(
                color = Color(0xFFFFD54F),
                radius = 22f,
                center = headPos
            )
        }
    }

    // =========================================================================
    // FASE 1: DESCENSO GEOLÓGICO Y AMBIENTES HIDROTERMALES
    // =========================================================================
    private fun renderGeologicalDescent(
        drawScope: DrawScope,
        screenWidth: Float,
        screenHeight: Float,
        center: Offset,
        planet: PlanetDefinition,
        progress: Float,
        time: Float
    ) {
        with(drawScope) {
            when (planet.id) {
                "planet_aqualis" -> renderAqualisHydrothermalVent(screenWidth, screenHeight, progress, time)
                "planet_rubrum" -> renderRubrumVolcanicLightning(screenWidth, screenHeight, progress, time)
                "planet_toxis" -> renderToxisAcidicCaldera(screenWidth, screenHeight, progress, time)
                "planet_ametistia" -> renderAmetistiaCryoImpact(screenWidth, screenHeight, progress, time)
                "planet_solaria" -> renderSolariaSolarLagoon(screenWidth, screenHeight, progress, time)
                else -> renderAqualisHydrothermalVent(screenWidth, screenHeight, progress, time)
            }
        }
    }

    private fun DrawScope.renderAqualisHydrothermalVent(
        screenWidth: Float,
        screenHeight: Float,
        progress: Float,
        time: Float
    ) {
        // Deep Abyssal Ocean Background
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF010E1C), Color(0xFF031A2E), Color(0xFF010810)),
                startY = 0f,
                endY = screenHeight
            )
        )

        // Basalt Seabed Mound
        val moundPath = Path().apply {
            moveTo(0f, screenHeight)
            lineTo(0f, screenHeight * 0.72f)
            cubicTo(
                screenWidth * 0.25f, screenHeight * 0.70f,
                screenWidth * 0.38f, screenHeight * 0.55f,
                screenWidth * 0.50f, screenHeight * 0.52f
            )
            cubicTo(
                screenWidth * 0.62f, screenHeight * 0.55f,
                screenWidth * 0.75f, screenHeight * 0.70f,
                screenWidth, screenHeight * 0.72f
            )
            lineTo(screenWidth, screenHeight)
            close()
        }
        drawPath(
            path = moundPath,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF1E3A4C), Color(0xFF0A1822), Color(0xFF040B10)),
                startY = screenHeight * 0.52f,
                endY = screenHeight
            )
        )

        // White/Alkaline Porous Hydrothermal Chimney (Russell Greigite Spire)
        val ventTop = Offset(screenWidth * 0.50f, screenHeight * 0.52f)
        val chimneyPath = Path().apply {
            moveTo(screenWidth * 0.44f, ventTop.y + 120f)
            lineTo(screenWidth * 0.46f, ventTop.y)
            lineTo(screenWidth * 0.54f, ventTop.y)
            lineTo(screenWidth * 0.56f, ventTop.y + 120f)
            close()
        }
        drawPath(
            path = chimneyPath,
            brush = Brush.horizontalGradient(
                colors = listOf(Color(0xFF455A64), Color(0xFFB0BEC5), Color(0xFF37474F)),
                startX = screenWidth * 0.44f,
                endX = screenWidth * 0.56f
            )
        )

        // Porous Microcavern Cavities in the Chimney Wall
        for (i in 0 until 14) {
            val cx = screenWidth * (0.47f + (i % 3 - 1) * 0.035f)
            val cy = ventTop.y + 20f + i * 7f
            drawCircle(
                color = Color(0xFF00E5FF).copy(alpha = 0.55f + sin(time * 3f + i) * 0.25f),
                radius = 3.5f + (i % 4),
                center = Offset(cx, cy)
            )
        }

        // Mineral Smoker Plume (Hot alkaline fluid meeting cold ocean)
        val plumeParticles = 60
        for (i in 0 until plumeParticles) {
            val t = (time * 0.45f + i.toFloat() / plumeParticles) % 1.0f
            val py = ventTop.y - t * (screenHeight * 0.48f)
            val spread = (t * 80f) * sin(t * 12f + i)
            val px = ventTop.x + spread

            val pAlpha = (1f - t) * 0.7f
            val pRadius = 6f + t * 24f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFE0F7FA).copy(alpha = pAlpha),
                        Color(0xFF80DEEA).copy(alpha = pAlpha * 0.6f),
                        Color.Transparent
                    ),
                    center = Offset(px, py),
                    radius = pRadius
                ),
                radius = pRadius,
                center = Offset(px, py)
            )
        }

        // Rising H2 and CH4 Bubbles with refraction
        for (b in 0 until 18) {
            val bt = (time * 0.7f + b * 0.15f) % 1.0f
            val by = ventTop.y + 40f - bt * (screenHeight * 0.55f)
            val bx = ventTop.x + sin(by * 0.04f + b) * 35f
            drawCircle(
                color = Color.White.copy(alpha = (1f - bt) * 0.85f),
                radius = 3.5f + (b % 4),
                center = Offset(bx, by)
            )
            drawCircle(
                color = Color(0xFF00E5FF).copy(alpha = (1f - bt) * 0.9f),
                radius = 5.5f + (b % 4),
                center = Offset(bx, by),
                style = Stroke(width = 1.2f)
            )
        }
    }

    private fun DrawScope.renderRubrumVolcanicLightning(
        screenWidth: Float,
        screenHeight: Float,
        progress: Float,
        time: Float
    ) {
        // Red Volcanic Sky & Iron Ocean
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF3E0A12), Color(0xFF1E0308), Color(0xFF0D0103)),
                startY = 0f,
                endY = screenHeight
            )
        )

        // Turbulent Red Sea Waves
        val wavePath = Path().apply {
            moveTo(0f, screenHeight)
            lineTo(0f, screenHeight * 0.60f)
            var x = 0f
            while (x <= screenWidth + 20f) {
                val y = screenHeight * 0.60f + sin(x * 0.02f + time * 3.5f) * 18f + cos(x * 0.05f - time * 2f) * 8f
                lineTo(x, y)
                x += 20f
            }
            lineTo(screenWidth, screenHeight)
            close()
        }
        drawPath(
            path = wavePath,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFB71C1C), Color(0xFF4A0007), Color(0xFF1B0002)),
                startY = screenHeight * 0.55f,
                endY = screenHeight
            )
        )

        // Jagged Obsidian / Pyrite Basalt Columns
        val rockPath = Path().apply {
            moveTo(screenWidth * 0.15f, screenHeight)
            lineTo(screenWidth * 0.20f, screenHeight * 0.52f)
            lineTo(screenWidth * 0.26f, screenHeight * 0.56f)
            lineTo(screenWidth * 0.32f, screenHeight * 0.50f)
            lineTo(screenWidth * 0.38f, screenHeight * 0.65f)
            lineTo(screenWidth * 0.42f, screenHeight)
            close()
        }
        drawPath(
            path = rockPath,
            brush = Brush.horizontalGradient(
                colors = listOf(Color(0xFF261014), Color(0xFF4E2028), Color(0xFF1A0A0C)),
                startX = screenWidth * 0.15f,
                endX = screenWidth * 0.42f
            )
        )

        // Miller-Urey Branching Lightning Strike (Flashes every few moments)
        val flashPhase = (time * 2.5f) % 2.0f
        if (flashPhase < 0.35f) {
            val flashAlpha = (1f - (flashPhase / 0.35f)).coerceIn(0f, 1f)
            // Screen flash glow
            drawRect(color = Color(0xFFFF8A80).copy(alpha = flashAlpha * 0.30f))

            // Lightning Bolt Segments
            var cur = Offset(screenWidth * 0.62f, 0f)
            val boltPath = Path().apply {
                moveTo(cur.x, cur.y)
                val targetY = screenHeight * 0.62f
                while (cur.y < targetY) {
                    val nextY = (cur.y + 35f).coerceAtMost(targetY)
                    val nextX = cur.x + (sin(cur.y * 0.15f + time * 10f) * 28f)
                    lineTo(nextX, nextY)
                    cur = Offset(nextX, nextY)
                }
            }

            drawPath(
                path = boltPath,
                color = Color(0xFFFFD54F).copy(alpha = flashAlpha),
                style = Stroke(width = 4.5f, cap = StrokeCap.Round)
            )
            drawPath(
                path = boltPath,
                color = Color.White.copy(alpha = flashAlpha),
                style = Stroke(width = 2f, cap = StrokeCap.Round)
            )

            // Spark impact on the water
            drawCircle(
                color = Color.White.copy(alpha = flashAlpha),
                radius = 28f,
                center = cur
            )
            drawCircle(
                color = Color(0xFFFF5252).copy(alpha = flashAlpha * 0.7f),
                radius = 55f,
                center = cur,
                style = Stroke(width = 3f)
            )
        }
    }

    private fun DrawScope.renderToxisAcidicCaldera(
        screenWidth: Float,
        screenHeight: Float,
        progress: Float,
        time: Float
    ) {
        // Emerald toxic sulfurous atmosphere
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF031A0B), Color(0xFF082E15), Color(0xFF020F06)),
                startY = 0f,
                endY = screenHeight
            )
        )

        // Terraced Montmorillonite Clay Steps
        for (step in 0..3) {
            val stepY = screenHeight * (0.50f + step * 0.12f)
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF33691E), Color(0xFF1B5E20)),
                    startY = stepY,
                    endY = stepY + 30f
                ),
                topLeft = Offset(0f, stepY),
                size = Size(screenWidth, 32f)
            )
        }

        // Steaming Emerald Geothermal Pool
        val poolY = screenHeight * 0.68f
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF76FF03).copy(alpha = 0.85f), Color(0xFF1B5E20)),
                startY = poolY,
                endY = screenHeight
            ),
            topLeft = Offset(0f, poolY),
            size = Size(screenWidth, screenHeight - poolY)
        )

        // Acid Steam Vapors rising from the pool
        for (i in 0 until 24) {
            val t = (time * 0.35f + i * 0.08f) % 1.0f
            val vy = poolY - t * 240f
            val vx = (i * 45f + sin(vy * 0.05f + time) * 30f) % screenWidth
            drawCircle(
                color = Color(0xFFCCFF90).copy(alpha = (1f - t) * 0.35f),
                radius = 18f + t * 25f,
                center = Offset(vx, vy)
            )
        }

        // Bubbling acid pop rings
        for (b in 0 until 8) {
            val bTime = (time * 1.2f + b * 0.4f) % 1.0f
            val bx = screenWidth * (0.15f + (b * 0.11f))
            val by = poolY + 25f + (b % 3) * 20f
            drawCircle(
                color = Color(0xFF76FF03).copy(alpha = (1f - bTime)),
                radius = 6f + bTime * 22f,
                center = Offset(bx, by),
                style = Stroke(width = 2f)
            )
        }
    }

    private fun DrawScope.renderAmetistiaCryoImpact(
        screenWidth: Float,
        screenHeight: Float,
        progress: Float,
        time: Float
    ) {
        // Deep purple cryogenic night sky & methane sea
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF120324), Color(0xFF28074D), Color(0xFF090114)),
                startY = 0f,
                endY = screenHeight
            )
        )

        // Fractured Ice Crust Sheets (White/violet floating plates)
        val plateY = screenHeight * 0.58f
        drawRect(
            color = Color(0xFF4A148C),
            topLeft = Offset(0f, plateY),
            size = Size(screenWidth, screenHeight - plateY)
        )

        // Ice Shelf edge
        val icePath = Path().apply {
            moveTo(0f, plateY)
            lineTo(screenWidth * 0.35f, plateY + 10f)
            lineTo(screenWidth * 0.42f, plateY + 45f) // Impact fissure opening
            lineTo(screenWidth * 0.58f, plateY + 45f)
            lineTo(screenWidth * 0.65f, plateY + 8f)
            lineTo(screenWidth, plateY)
            lineTo(screenWidth, plateY + 22f)
            lineTo(0f, plateY + 22f)
            close()
        }
        drawPath(
            path = icePath,
            brush = Brush.horizontalGradient(
                colors = listOf(Color(0xFFE1BEE7), Color(0xFFBA68C8), Color(0xFFE1BEE7)),
                startX = 0f,
                endX = screenWidth
            )
        )

        // Cometary Impact Core glowing at the bottom of the fissure
        val corePos = Offset(screenWidth * 0.50f, plateY + 55f)
        val pulse = (sin(time * 3f) * 0.3f + 0.7f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFE040FB).copy(alpha = 0.9f * pulse),
                    Color(0xFF7C4DFF).copy(alpha = 0.45f * pulse),
                    Color.Transparent
                ),
                center = corePos,
                radius = 90f
            ),
            radius = 90f,
            center = corePos
        )
        drawCircle(
            color = Color.White,
            radius = 16f,
            center = corePos
        )

        // Rising Bioluminescent cryo-plumes
        for (i in 0 until 30) {
            val t = (time * 0.4f + i * 0.05f) % 1.0f
            val py = corePos.y - t * 280f
            val px = corePos.x + sin(py * 0.04f + i) * (20f + t * 50f)
            drawCircle(
                color = Color(0xFFEA80FC).copy(alpha = (1f - t) * 0.75f),
                radius = 3f + (i % 4),
                center = Offset(px, py)
            )
        }
    }

    private fun DrawScope.renderSolariaSolarLagoon(
        screenWidth: Float,
        screenHeight: Float,
        progress: Float,
        time: Float
    ) {
        // Blazing twin-star sky & amber sea
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF331A00), Color(0xFF5E3500), Color(0xFF1A0A00)),
                startY = 0f,
                endY = screenHeight
            )
        )

        // Twin Suns in the Sky
        val sun1Pos = Offset(screenWidth * 0.38f, screenHeight * 0.22f)
        val sun2Pos = Offset(screenWidth * 0.62f, screenHeight * 0.16f)

        // Primary Sun (Golden Giant)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White, Color(0xFFFFD54F), Color(0xFFFF6D00), Color.Transparent),
                center = sun1Pos,
                radius = 85f
            ),
            radius = 85f,
            center = sun1Pos
        )

        // Secondary Sun (Ultraviolet White Dwarf)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White, Color(0xFF80D8FF), Color(0xFF00B0FF), Color.Transparent),
                center = sun2Pos,
                radius = 55f
            ),
            radius = 55f,
            center = sun2Pos
        )

        // Shallow Golden Water Surface
        val seaY = screenHeight * 0.56f
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFFFB300).copy(alpha = 0.8f), Color(0xFFE65100)),
                startY = seaY,
                endY = screenHeight
            ),
            topLeft = Offset(0f, seaY),
            size = Size(screenWidth, screenHeight - seaY)
        )

        // UV Radiation Beams / God rays shining through the water
        for (r in 0 until 8) {
            val rx = screenWidth * (0.2f + r * 0.09f)
            val beamPath = Path().apply {
                moveTo(sun1Pos.x, sun1Pos.y)
                lineTo(rx - 25f, screenHeight)
                lineTo(rx + 25f, screenHeight)
                close()
            }
            drawPath(
                path = beamPath,
                brush = Brush.verticalGradient(
                    colors = listOf(Color.White.copy(alpha = 0.25f), Color(0xFFFFD54F).copy(alpha = 0.12f), Color.Transparent),
                    startY = sun1Pos.y,
                    endY = screenHeight
                )
            )
        }

        // Shimmering Caustics on the shallow sulfur bed
        for (c in 0 until 14) {
            val cx = (c * 65f + sin(time * 2f + c) * 20f) % screenWidth
            val cy = seaY + 40f + (c % 4) * 25f
            drawCircle(
                color = Color.White.copy(alpha = 0.45f + sin(time * 4f + c) * 0.25f),
                radius = 12f + (c % 5) * 4f,
                center = Offset(cx, cy),
                style = Stroke(width = 2.5f)
            )
        }
    }

    // =========================================================================
    // FASE 2: CATÁLISIS QUÍMICA PREBIÓTICA Y SÍNTESIS DE MONÓMEROS
    // =========================================================================
    private fun renderChemicalReaction(
        drawScope: DrawScope,
        screenWidth: Float,
        screenHeight: Float,
        center: Offset,
        planet: PlanetDefinition,
        progress: Float,
        time: Float
    ) {
        with(drawScope) {
            // Dark microscopic broth background
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color(planet.oceanBgColor1), Color.Black),
                    center = center,
                    radius = screenWidth * 0.8f
                )
            )

            // Catalytic Mineral Lattice Grid (Pyrite cubes / montmorillonite layers / greigite sheets)
            val gridCols = 7
            val gridRows = 5
            val gridW = screenWidth * 0.85f
            val gridH = screenHeight * 0.35f
            val startX = (screenWidth - gridW) / 2f
            val startY = center.y + 40f

            for (r in 0 until gridRows) {
                for (c in 0 until gridCols) {
                    val px = startX + (c.toFloat() / (gridCols - 1)) * gridW
                    val py = startY + (r.toFloat() / (gridRows - 1)) * gridH
                    // Mineral node
                    val nodeColor = when (planet.id) {
                        "planet_rubrum" -> Color(0xFFFFD700) // Golden pyrite FeS2
                        "planet_toxis" -> Color(0xFF81C784) // Silicate clay sheets
                        "planet_solaria" -> Color(0xFFFFB300) // Sulfite salt crystal
                        else -> Color(0xFF90A4AE) // Basaltic greigite Fe3S4
                    }
                    drawCircle(
                        color = nodeColor.copy(alpha = 0.65f),
                        radius = 6f,
                        center = Offset(px, py)
                    )
                    // Crystal lattice bond lines
                    if (c < gridCols - 1) {
                        val nextX = startX + ((c + 1).toFloat() / (gridCols - 1)) * gridW
                        drawLine(
                            color = nodeColor.copy(alpha = 0.25f),
                            start = Offset(px, py),
                            end = Offset(nextX, py),
                            strokeWidth = 1.5f
                        )
                    }
                    if (r < gridRows - 1) {
                        val nextY = startY + ((r + 1).toFloat() / (gridRows - 1)) * gridH
                        drawLine(
                            color = nodeColor.copy(alpha = 0.25f),
                            start = Offset(px, py),
                            end = Offset(px, nextY),
                            strokeWidth = 1.5f
                        )
                    }
                }
            }

            // Chemical Molecules (Ball-and-stick representations) floating and reacting
            val moleculeTypes = listOf(
                "H₂O" to Color(0xFF00E5FF),
                "HCN" to Color(0xFFFF5252),
                "NH₃" to Color(0xFF76FF03),
                "CO₂" to Color(0xFFFFD600),
                "PO₄³⁻" to Color(0xFFE040FB)
            )

            for (i in 0 until 18) {
                val orbitRadius = 70f + (i * 12f)
                val angle = (time * 0.8f + i * (2f * PI / 18f)).toFloat()
                val mx = center.x + cos(angle) * orbitRadius
                val my = (center.y - 60f) + sin(angle) * (orbitRadius * 0.65f)
                val (molName, molColor) = moleculeTypes[i % moleculeTypes.size]

                // Central atom
                drawCircle(
                    color = molColor,
                    radius = 8f,
                    center = Offset(mx, my)
                )
                // Bonded hydrogen / oxygen atoms
                val subAngle1 = angle * 2f
                val sub1 = Offset(mx + cos(subAngle1) * 16f, my + sin(subAngle1) * 16f)
                drawLine(
                    color = Color.White.copy(alpha = 0.6f),
                    start = Offset(mx, my),
                    end = sub1,
                    strokeWidth = 2f
                )
                drawCircle(
                    color = Color.White,
                    radius = 4f,
                    center = sub1
                )

                // Reaction Catalyst Spark when molecule hits the mineral grid
                if (progress > 0.4f && (i % 3 == 0)) {
                    val sparkPulse = (sin(time * 6f + i) * 0.5f + 0.5f)
                    drawCircle(
                        color = Color.White.copy(alpha = sparkPulse * 0.8f),
                        radius = 18f * sparkPulse,
                        center = Offset(mx, my),
                        style = Stroke(width = 2f)
                    )
                }
            }

            // Central Activated Prebiotic Monomer Formed (e.g. Ribonucleotide / Amino Acid)
            val corePulse = (sin(time * 4f) * 0.2f + 0.8f)
            val monomerPos = Offset(center.x, center.y - 60f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(planet.atmosphereColorHex).copy(alpha = 0.8f * corePulse),
                        Color(planet.atmosphereColorHex).copy(alpha = 0.25f * corePulse),
                        Color.Transparent
                    ),
                    center = monomerPos,
                    radius = 80f
                ),
                radius = 80f,
                center = monomerPos
            )
            drawCircle(
                color = Color.White,
                radius = 16f * corePulse,
                center = monomerPos
            )
        }
    }

    // =========================================================================
    // FASE 3: AUTOENSAMBLAJE MOLECULAR (MUNDO DE ARN Y BICAPA LIPÍDICA)
    // =========================================================================
    private fun renderMolecularAssembly(
        drawScope: DrawScope,
        screenWidth: Float,
        screenHeight: Float,
        center: Offset,
        planet: PlanetDefinition,
        progress: Float,
        time: Float
    ) {
        with(drawScope) {
            // Dark soup canvas
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color(planet.oceanBgColor1).copy(alpha = 0.8f), Color(0xFF020710)),
                    center = center,
                    radius = screenWidth * 0.85f
                )
            )

            // 1. RNA World Double/Single Helix Polymeric Strand
            val rnaLength = 32
            val baseColors = listOf(
                Color(0xFF00E5FF), // A (Adenina)
                Color(0xFFFF9100), // U (Uracilo)
                Color(0xFF76FF03), // G (Guanina)
                Color(0xFFE040FB)  // C (Citosina)
            )

            val helixCenter = Offset(center.x, center.y - 70f)
            val helixAmplitude = 45f

            for (i in 0 until rnaLength) {
                val t = (i.toFloat() / rnaLength) - 0.5f
                val hx = helixCenter.x + t * (screenWidth * 0.75f)
                val wavePhase = (t * 4.5f * PI.toFloat()) + time * 3f
                val hy1 = helixCenter.y + sin(wavePhase) * helixAmplitude
                val hy2 = helixCenter.y - sin(wavePhase) * helixAmplitude

                val baseColor = baseColors[i % baseColors.size]

                // Backbone bond
                drawCircle(
                    color = Color(0xFFFFD600), // Sugar-phosphate backbone
                    radius = 5.5f,
                    center = Offset(hx, hy1)
                )

                // Complementary rungs if assembling
                if (progress > 0.25f) {
                    drawLine(
                        color = baseColor.copy(alpha = 0.75f),
                        start = Offset(hx, hy1),
                        end = Offset(hx, hy2),
                        strokeWidth = 2.5f
                    )
                    drawCircle(
                        color = baseColor,
                        radius = 4.5f,
                        center = Offset(hx, (hy1 + hy2) / 2f)
                    )
                    drawCircle(
                        color = Color(0xFFFFD600),
                        radius = 5.5f,
                        center = Offset(hx, hy2)
                    )
                }
            }

            // 2. Lipid Bilayer Vesicle Auto-Assembly
            // As progress increases, circular lipid heads and hydrophobic tails close into a sphere!
            val vesicleRadius = 85f + sin(time * 2f) * 4f
            val vesicleCenter = Offset(center.x, center.y + 70f)
            val lipidCount = 42

            // Completion angle increases with progress (closing the sphere)
            val maxAngle = (2f * PI * progress.coerceIn(0.3f, 1f)).toFloat()

            for (i in 0 until lipidCount) {
                val angle = (i.toFloat() / lipidCount) * (2f * PI.toFloat())
                if (angle <= maxAngle) {
                    // Hydrophilic outer head
                    val hx = vesicleCenter.x + cos(angle) * vesicleRadius
                    val hy = vesicleCenter.y + sin(angle) * vesicleRadius
                    drawCircle(
                        color = Color(planet.atmosphereColorHex),
                        radius = 4.5f,
                        center = Offset(hx, hy)
                    )

                    // Hydrophobic tail pointing inward
                    val tx = vesicleCenter.x + cos(angle) * (vesicleRadius - 14f)
                    val ty = vesicleCenter.y + sin(angle) * (vesicleRadius - 14f)
                    drawLine(
                        color = Color(0xFFFFF9C4).copy(alpha = 0.8f),
                        start = Offset(hx, hy),
                        end = Offset(tx, ty),
                        strokeWidth = 1.8f
                    )

                    // Inner bilayer leaflet
                    val inHeadX = vesicleCenter.x + cos(angle) * (vesicleRadius - 22f)
                    val inHeadY = vesicleCenter.y + sin(angle) * (vesicleRadius - 22f)
                    drawCircle(
                        color = Color(planet.atmosphereColorHex).copy(alpha = 0.75f),
                        radius = 3.5f,
                        center = Offset(inHeadX, inHeadY)
                    )
                }
            }

            // Interior Encapsulated Ribozyme / Core Energy Glow
            if (progress > 0.6f) {
                val encapAlpha = ((progress - 0.6f) / 0.4f).coerceIn(0f, 1f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = encapAlpha),
                            Color(planet.atmosphereColorHex).copy(alpha = encapAlpha * 0.5f),
                            Color.Transparent
                        ),
                        center = vesicleCenter,
                        radius = vesicleRadius * 0.65f
                    ),
                    radius = vesicleRadius * 0.65f,
                    center = vesicleCenter
                )
            }
        }
    }

    // =========================================================================
    // FASE 4: DESPERTAR CELULAR Y PRIMER NADO
    // =========================================================================
    private fun renderCellularAwakening(
        drawScope: DrawScope,
        screenWidth: Float,
        screenHeight: Float,
        center: Offset,
        planet: PlanetDefinition,
        cellEvolution: CellEvolutionEntity,
        progress: Float,
        time: Float
    ) {
        with(drawScope) {
            // Primordial Ocean Water with caustic ripples
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color(planet.oceanBgColor1), Color(planet.oceanBgColor2)),
                    center = center,
                    radius = screenWidth
                )
            )

            // Emergent life shockwave ripple pulses expanding outward
            val pulse1 = (time * 1.4f) % 1.0f
            drawCircle(
                color = Color(planet.oceanRimColor).copy(alpha = (1f - pulse1) * 0.75f),
                radius = 70f + pulse1 * 220f,
                center = center,
                style = Stroke(width = 3.5f)
            )

            val pulse2 = (time * 1.4f + 0.5f) % 1.0f
            drawCircle(
                color = Color.White.copy(alpha = (1f - pulse2) * 0.5f),
                radius = 70f + pulse2 * 180f,
                center = center,
                style = Stroke(width = 2f)
            )

            // Player's actual cell, waking up and undulating
            val cellRadius = (45f + progress * 15f).coerceIn(40f, 65f)
            val swimAngle = sin(time * 1.5f) * 0.25f
            val diet = try {
                DietType.valueOf(cellEvolution.mouthType)
            } catch (_: Exception) {
                DietType.HERBIVORE
            }

            CellVisualRenderer.drawCell(
                drawScope = this,
                center = center,
                radius = cellRadius,
                angleRad = swimAngle,
                primaryColor = Color(cellEvolution.primaryColorHex),
                mouthType = diet,
                flagellaCount = cellEvolution.flagellaCount.coerceAtLeast(1),
                ciliaCount = cellEvolution.ciliaCount.coerceAtLeast(1),
                spikesCount = cellEvolution.spikesCount,
                hasPoison = cellEvolution.poisonGland,
                hasElectric = cellEvolution.electricOrgan,
                eyeType = cellEvolution.eyeType,
                timeSeconds = time,
                armorPlates = cellEvolution.armorPlates,
                isDashing = progress > 0.8f,
                drawShadow = true
            )

            // Halo of first consciousness / bioenergy
            val bioPulse = (sin(time * 5f) * 0.2f + 0.8f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(planet.oceanRimColor).copy(alpha = 0.35f * bioPulse),
                        Color.Transparent
                    ),
                    center = center,
                    radius = cellRadius * 2.2f
                ),
                radius = cellRadius * 2.2f,
                center = center
            )
        }
    }

    // =========================================================================
    // DUST MOTES & ORGANIC FLOATING PARTICLES
    // =========================================================================
    private fun renderCinematicMotes(
        drawScope: DrawScope,
        screenWidth: Float,
        screenHeight: Float,
        planet: PlanetDefinition,
        time: Float
    ) {
        val moteCount = 24
        val rng = Random(1337)
        for (i in 0 until moteCount) {
            val baseX = rng.nextFloat() * screenWidth
            val baseY = rng.nextFloat() * screenHeight
            val speed = 0.15f + rng.nextFloat() * 0.35f

            val mx = (baseX + sin(time * speed + i) * 35f) % screenWidth
            val my = (baseY - time * (25f * speed))
            val wrappedY = if (my < 0f) screenHeight + (my % screenHeight) else my % screenHeight

            val alpha = (0.25f + sin(time * 2f + i) * 0.25f).coerceIn(0.1f, 0.7f)
            drawScope.drawCircle(
                color = Color(planet.ambientParticleColor).copy(alpha = alpha),
                radius = if (i % 5 == 0) 3.5f else 1.8f,
                center = Offset(mx, wrappedY)
            )
        }
    }
}
