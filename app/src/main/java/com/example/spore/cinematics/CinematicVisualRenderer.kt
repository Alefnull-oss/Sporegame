package com.example.spore.cinematics

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import android.util.SparseArray
import com.example.spore.data.model.CellEvolutionEntity
import com.example.spore.data.model.PlanetDefinition
import com.example.spore.game.engine.DietType
import com.example.spore.ui.components.CellVisualRenderer
import com.example.spore.ui.components.StrokeCache
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

    // =====================================================================
    // ZERO-ALLOCATION INFRASTRUCTURE
    // The cinematic previously allocated two Random instances, ~16 Path
    // objects and ~20 gradient brushes PER FRAME. On low-RAM devices those
    // short-lived objects triggered GC pauses visible as stutter in a
    // scripted, timing-sensitive sequence. Everything below is reused.
    // =====================================================================

    /** 8 reusable scratch paths (one per concurrently-built shape family). */
    private val pathA = Path()
    private val pathB = Path()
    private val pathC = Path()
    private val pathD = Path()
    private val pathE = Path()
    private val pathF = Path()
    private val pathG = Path()
    private val pathH = Path()

    /**
     * Static normalized star table. Generated ONCE with the exact Random(42)
     * sequence used before, so the star field is pixel-identical while the
     * per-frame Random allocation disappears. Coordinates are normalized
     * (0..1) and scaled by the screen size at draw time.
     */
    private val STAR_XY = FloatArray(90 * 2).apply {
        val rng = Random(42)
        for (i in 0 until 90) {
            this[i * 2] = rng.nextFloat()
            this[i * 2 + 1] = rng.nextFloat()
        }
    }

    /** Static motes table (baseX, baseY, speed) - same Random(1337) sequence. */
    private val MOTE_XYS = FloatArray(24 * 3).apply {
        val rng = Random(1337)
        for (i in 0 until 24) {
            this[i * 3] = rng.nextFloat()
            this[i * 3 + 1] = rng.nextFloat()
            this[i * 3 + 2] = 0.15f + rng.nextFloat() * 0.35f
        }
    }

    /** Static molecule types (previously a fresh List of 5 Pairs every frame). */
    private val MOLECULE_TYPES = listOf(
        "H₂O" to Color(0xFF00E5FF),
        "HCN" to Color(0xFFFF5252),
        "NH₃" to Color(0xFF76FF03),
        "CO₂" to Color(0xFFFFD600),
        "PO₄³⁻" to Color(0xFFE040FB)
    )

    /** Static RNA base colors (previously a fresh List every frame). */
    private val RNA_BASE_COLORS = listOf(
        Color(0xFF00E5FF), // A (Adenina)
        Color(0xFFFF9100), // U (Uracilo)
        Color(0xFF76FF03), // G (Guanina)
        Color(0xFFE040FB)  // C (Citosina)
    )

    /** Bounded brush cache with lazy singleton providers (the providers are
     *  object-field-reading vals, so both hits and misses avoid per-call
     *  lambda allocations). Inputs are staged into the b* fields before each
     *  cachedBrush() call - the render is single-threaded. */
    private val brushCache = SparseArray<Brush>()

    private var bPlanet: PlanetDefinition? = null
    private var bW = 0f
    private var bH = 0f
    private var bStartY = 0f
    private var bEndY = 0f
    private var bAlpha = 0f

    private fun cachedBrush(key: Int, provider: () -> Brush): Brush {
        var b = brushCache.get(key)
        if (b == null) {
            if (brushCache.size() > 224) brushCache.clear() // hard bound
            b = provider()
            brushCache.put(key, b)
        }
        return b
    }

    /** Stable small-int pair hash for brush cache keys. */
    private fun keyOf(a: Int, b: Int): Int = (a and 0xFFFFF) * 0x1000 + (b and 0xFFF)

    // ---- Singleton brush providers (read only the staged b* fields) ----

    private val spaceBackdropProvider: () -> Brush = {
        Brush.verticalGradient(listOf(Color(0xFF01040A), Color(bPlanet!!.oceanBgColor2)), 0f, bH)
    }

    private val ionizationHaloProvider: () -> Brush = {
        val atmo = Color(bPlanet!!.atmosphereColorHex)
        Brush.radialGradient(
            listOf(atmo.copy(alpha = 0.85f), atmo.copy(alpha = 0.35f), Color.Transparent),
            center = Offset.Zero,
            radius = 1f
        )
    }

    private val planetBodyProvider: () -> Brush = {
        val p = bPlanet!!
        Brush.radialGradient(
            listOf(Color(p.planetColorHex), Color(p.oceanBgColor1), Color(p.oceanBgColor2)),
            center = Offset(0f, -0.45f),
            radius = 1f
        )
    }

    private val reentryTailProvider: () -> Brush = {
        Brush.verticalGradient(
            listOf(Color(0xFFFF9100), Color(0xFFFF1744), Color.Transparent),
            startY = bStartY,
            endY = bEndY
        )
    }

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
            // Space backdrop (cached: colors are planet-stable, screen size stable)
            bPlanet = planet
            bH = screenHeight
            val spaceBrush = cachedBrush(keyOf(1, (screenHeight / 64f).toInt() xor planet.oceanBgColor2.hashCode()), spaceBackdropProvider)
            drawRect(brush = spaceBrush)

            // Distant stars (static RNG table - identical star field, no per-frame Random)
            for (i in 0 until 90) {
                val sx = STAR_XY[i * 2] * screenWidth
                val sy = STAR_XY[i * 2 + 1] * screenHeight * 0.7f
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

            // Outer Atmospheric Ionization Halo (cached unit gradient + transform)
            val haloBrush = cachedBrush(1001 + planet.atmosphereColorHex.hashCode(), ionizationHaloProvider)
            withTransform({
                translate(planetCenter.x, planetCenter.y)
                val hr = planetRadius * 1.08f
                scale(hr, hr)
            }) {
                drawCircle(brush = haloBrush, radius = 1f, center = Offset.Zero)
            }

            // Planet Surface Body (cached unit gradient with offset center + transform)
            val bodyBrush = cachedBrush(1002 + planet.planetColorHex.hashCode(), planetBodyProvider)
            withTransform({
                translate(planetCenter.x, planetCenter.y)
                scale(planetRadius, planetRadius)
            }) {
                drawCircle(brush = bodyBrush, radius = 1f, center = Offset.Zero)
            }

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
                style = StrokeCache.plain(3f)
            )

            // Re-entry Fiery Plasma Tail (reused path + brush keyed by quantized head Y)
            pathA.reset()
            pathA.moveTo(headPos.x, headPos.y)
            pathA.quadraticTo(
                headPos.x - 45f,
                headPos.y - 120f,
                headPos.x - 70f,
                headPos.y - 280f
            )
            bStartY = headPos.y
            bEndY = headPos.y - 280f
            val tailBrush = cachedBrush(1003 + ((headPos.y / 16f).toInt() and 0xFFF), reentryTailProvider)
            drawPath(
                path = pathA,
                brush = tailBrush,
                style = StrokeCache.round(16f)
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

    private val aqualisBgProvider: () -> Brush = {
        Brush.verticalGradient(
            listOf(Color(0xFF010E1C), Color(0xFF031A2E), Color(0xFF010810)),
            0f, bH
        )
    }

    private val aqualisMoundProvider: () -> Brush = {
        Brush.verticalGradient(
            listOf(Color(0xFF1E3A4C), Color(0xFF0A1822), Color(0xFF040B10)),
            bH * 0.52f, bH
        )
    }

    private val aqualisChimneyProvider: () -> Brush = {
        Brush.horizontalGradient(
            listOf(Color(0xFF455A64), Color(0xFFB0BEC5), Color(0xFF37474F)),
            bW * 0.44f, bW * 0.56f
        )
    }

    private val plumeUnitProvider: () -> Brush = {
        val a = bAlpha
        Brush.radialGradient(
            listOf(
                Color(0xFFE0F7FA).copy(alpha = a),
                Color(0xFF80DEEA).copy(alpha = a * 0.6f),
                Color.Transparent
            ),
            center = Offset.Zero,
            radius = 1f
        )
    }

    private fun DrawScope.renderAqualisHydrothermalVent(
        screenWidth: Float,
        screenHeight: Float,
        progress: Float,
        time: Float
    ) {
        bW = screenWidth
        bH = screenHeight

        // Deep Abyssal Ocean Background (cached)
        drawRect(brush = cachedBrush(keyOf(2, (screenHeight / 64f).toInt()), aqualisBgProvider))

        // Basalt Seabed Mound (reused path + cached gradient)
        pathB.reset()
        pathB.moveTo(0f, screenHeight)
        pathB.lineTo(0f, screenHeight * 0.72f)
        pathB.cubicTo(
            screenWidth * 0.25f, screenHeight * 0.70f,
            screenWidth * 0.38f, screenHeight * 0.55f,
            screenWidth * 0.50f, screenHeight * 0.52f
        )
        pathB.cubicTo(
            screenWidth * 0.62f, screenHeight * 0.55f,
            screenWidth * 0.75f, screenHeight * 0.70f,
            screenWidth, screenHeight * 0.72f
        )
        pathB.lineTo(screenWidth, screenHeight)
        pathB.close()
        drawPath(
            path = pathB,
            brush = cachedBrush(keyOf(3, (screenHeight / 64f).toInt()), aqualisMoundProvider)
        )

        // White/Alkaline Porous Hydrothermal Chimney (Russell Greigite Spire)
        val ventTop = Offset(screenWidth * 0.50f, screenHeight * 0.52f)
        pathC.reset()
        pathC.moveTo(screenWidth * 0.44f, ventTop.y + 120f)
        pathC.lineTo(screenWidth * 0.46f, ventTop.y)
        pathC.lineTo(screenWidth * 0.54f, ventTop.y)
        pathC.lineTo(screenWidth * 0.56f, ventTop.y + 120f)
        pathC.close()
        drawPath(
            path = pathC,
            brush = cachedBrush(keyOf(4, (screenWidth / 64f).toInt()), aqualisChimneyProvider)
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
        // Cached UNIT gradient buckets by quantized alpha + transform per particle.
        val plumeParticles = 60
        for (i in 0 until plumeParticles) {
            val t = (time * 0.45f + i.toFloat() / plumeParticles) % 1.0f
            val py = ventTop.y - t * (screenHeight * 0.48f)
            val spread = (t * 80f) * sin(t * 12f + i)
            val px = ventTop.x + spread

            val pAlpha = (1f - t) * 0.7f
            val pRadius = 6f + t * 24f
            val alphaBucket = (pAlpha * 48f).toInt().coerceIn(1, 48)
            bAlpha = alphaBucket / 48f // deterministic per bucket key
            val plumeBrush = cachedBrush(2000 + alphaBucket, plumeUnitProvider)
            withTransform({
                translate(px, py)
                scale(pRadius, pRadius)
            }) {
                drawCircle(brush = plumeBrush, radius = 1f, center = Offset.Zero)
            }
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
                style = StrokeCache.plain(1.2f)
            )
        }
    }

    private val rubrumBgProvider: () -> Brush = {
        Brush.verticalGradient(
            listOf(Color(0xFF3E0A12), Color(0xFF1E0308), Color(0xFF0D0103)),
            0f, bH
        )
    }

    private val rubrumSeaProvider: () -> Brush = {
        Brush.verticalGradient(
            listOf(Color(0xFFB71C1C), Color(0xFF4A0007), Color(0xFF1B0002)),
            bH * 0.55f, bH
        )
    }

    private val rubrumRockProvider: () -> Brush = {
        Brush.horizontalGradient(
            listOf(Color(0xFF261014), Color(0xFF4E2028), Color(0xFF1A0A0C)),
            bW * 0.15f, bW * 0.42f
        )
    }

    private fun DrawScope.renderRubrumVolcanicLightning(
        screenWidth: Float,
        screenHeight: Float,
        progress: Float,
        time: Float
    ) {
        bW = screenWidth
        bH = screenHeight

        // Red Volcanic Sky & Iron Ocean (cached)
        drawRect(brush = cachedBrush(keyOf(5, (screenHeight / 64f).toInt()), rubrumBgProvider))

        // Turbulent Red Sea Waves (reused path + cached gradient)
        pathD.reset()
        pathD.moveTo(0f, screenHeight)
        pathD.lineTo(0f, screenHeight * 0.60f)
        var x = 0f
        while (x <= screenWidth + 20f) {
            val y = screenHeight * 0.60f + sin(x * 0.02f + time * 3.5f) * 18f + cos(x * 0.05f - time * 2f) * 8f
            pathD.lineTo(x, y)
            x += 20f
        }
        pathD.lineTo(screenWidth, screenHeight)
        pathD.close()
        drawPath(
            path = pathD,
            brush = cachedBrush(keyOf(6, (screenHeight / 64f).toInt()), rubrumSeaProvider)
        )

        // Jagged Obsidian / Pyrite Basalt Columns (reused path + cached gradient)
        pathE.reset()
        pathE.moveTo(screenWidth * 0.15f, screenHeight)
        pathE.lineTo(screenWidth * 0.20f, screenHeight * 0.52f)
        pathE.lineTo(screenWidth * 0.26f, screenHeight * 0.56f)
        pathE.lineTo(screenWidth * 0.32f, screenHeight * 0.50f)
        pathE.lineTo(screenWidth * 0.38f, screenHeight * 0.65f)
        pathE.lineTo(screenWidth * 0.42f, screenHeight)
        pathE.close()
        drawPath(
            path = pathE,
            brush = cachedBrush(keyOf(7, (screenWidth / 64f).toInt()), rubrumRockProvider)
        )

        // Miller-Urey Branching Lightning Strike (Flashes every few moments)
        val flashPhase = (time * 2.5f) % 2.0f
        if (flashPhase < 0.35f) {
            val flashAlpha = (1f - (flashPhase / 0.35f)).coerceIn(0f, 1f)
            // Screen flash glow
            drawRect(color = Color(0xFFFF8A80).copy(alpha = flashAlpha * 0.30f))

            // Lightning Bolt Segments (reused path)
            var curX = screenWidth * 0.62f
            var curY = 0f
            pathF.reset()
            pathF.moveTo(curX, curY)
            val targetY = screenHeight * 0.62f
            while (curY < targetY) {
                val nextY = (curY + 35f).coerceAtMost(targetY)
                val nextX = curX + (sin(curY * 0.15f + time * 10f) * 28f)
                pathF.lineTo(nextX, nextY)
                curX = nextX
                curY = nextY
            }

            drawPath(
                path = pathF,
                color = Color(0xFFFFD54F).copy(alpha = flashAlpha),
                style = StrokeCache.round(4.5f)
            )
            drawPath(
                path = pathF,
                color = Color.White.copy(alpha = flashAlpha),
                style = StrokeCache.round(2f)
            )

            // Spark impact on the water
            drawCircle(
                color = Color.White.copy(alpha = flashAlpha),
                radius = 28f,
                center = Offset(curX, curY)
            )
            drawCircle(
                color = Color(0xFFFF5252).copy(alpha = flashAlpha * 0.7f),
                radius = 55f,
                center = Offset(curX, curY),
                style = StrokeCache.plain(3f)
            )
        }
    }

    private val toxisBgProvider: () -> Brush = {
        Brush.verticalGradient(
            listOf(Color(0xFF031A0B), Color(0xFF082E15), Color(0xFF020F06)),
            0f, bH
        )
    }

    private val toxisTerraceProvider: () -> Brush = {
        Brush.verticalGradient(
            listOf(Color(0xFF33691E), Color(0xFF1B5E20)),
            bStartY, bStartY + 30f
        )
    }

    private val toxisPoolProvider: () -> Brush = {
        Brush.verticalGradient(
            listOf(Color(0xFF76FF03).copy(alpha = 0.85f), Color(0xFF1B5E20)),
            bStartY, bH
        )
    }

    private fun DrawScope.renderToxisAcidicCaldera(
        screenWidth: Float,
        screenHeight: Float,
        progress: Float,
        time: Float
    ) {
        bW = screenWidth
        bH = screenHeight

        // Emerald toxic sulfurous atmosphere (cached)
        drawRect(brush = cachedBrush(keyOf(8, (screenHeight / 64f).toInt()), toxisBgProvider))

        // Terraced Montmorillonite Clay Steps (4 cached brushes keyed by step Y)
        for (step in 0..3) {
            val stepY = screenHeight * (0.50f + step * 0.12f)
            bStartY = stepY
            drawRect(
                brush = cachedBrush(keyOf(9, (stepY / 8f).toInt()), toxisTerraceProvider),
                topLeft = Offset(0f, stepY),
                size = Size(screenWidth, 32f)
            )
        }

        // Steaming Emerald Geothermal Pool (cached)
        val poolY = screenHeight * 0.68f
        bStartY = poolY
        drawRect(
            brush = cachedBrush(keyOf(10, (poolY / 16f).toInt()), toxisPoolProvider),
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
                style = StrokeCache.plain(2f)
            )
        }
    }

    private val ametistiaBgProvider: () -> Brush = {
        Brush.verticalGradient(
            listOf(Color(0xFF120324), Color(0xFF28074D), Color(0xFF090114)),
            0f, bH
        )
    }

    private val ametistiaIceProvider: () -> Brush = {
        Brush.horizontalGradient(
            listOf(Color(0xFFE1BEE7), Color(0xFFBA68C8), Color(0xFFE1BEE7)),
            0f, bW
        )
    }

    private val impactCoreProvider: () -> Brush = {
        val a = bAlpha
        val corePos = Offset(bW * 0.50f, bH * 0.58f + 55f)
        Brush.radialGradient(
            listOf(
                Color(0xFFE040FB).copy(alpha = 0.9f * a),
                Color(0xFF7C4DFF).copy(alpha = 0.45f * a),
                Color.Transparent
            ),
            center = corePos,
            radius = 90f
        )
    }

    private fun DrawScope.renderAmetistiaCryoImpact(
        screenWidth: Float,
        screenHeight: Float,
        progress: Float,
        time: Float
    ) {
        bW = screenWidth
        bH = screenHeight

        // Deep purple cryogenic night sky & methane sea (cached)
        drawRect(brush = cachedBrush(keyOf(11, (screenHeight / 64f).toInt()), ametistiaBgProvider))

        // Fractured Ice Crust Sheets (White/violet floating plates)
        val plateY = screenHeight * 0.58f
        drawRect(
            color = Color(0xFF4A148C),
            topLeft = Offset(0f, plateY),
            size = Size(screenWidth, screenHeight - plateY)
        )

        // Ice Shelf edge (reused path + cached gradient)
        pathG.reset()
        pathG.moveTo(0f, plateY)
        pathG.lineTo(screenWidth * 0.35f, plateY + 10f)
        pathG.lineTo(screenWidth * 0.42f, plateY + 45f) // Impact fissure opening
        pathG.lineTo(screenWidth * 0.58f, plateY + 45f)
        pathG.lineTo(screenWidth * 0.65f, plateY + 8f)
        pathG.lineTo(screenWidth, plateY)
        pathG.lineTo(screenWidth, plateY + 22f)
        pathG.lineTo(0f, plateY + 22f)
        pathG.close()
        drawPath(
            path = pathG,
            brush = cachedBrush(keyOf(12, (screenWidth / 64f).toInt()), ametistiaIceProvider)
        )

        // Cometary Impact Core glowing at the bottom of the fissure
        val corePos = Offset(screenWidth * 0.50f, plateY + 55f)
        val pulse = (sin(time * 3f) * 0.3f + 0.7f)
        val pulseBucket = (pulse * 32f).toInt().coerceIn(1, 32)
        bAlpha = pulseBucket / 32f
        drawCircle(
            brush = cachedBrush(3000 + pulseBucket, impactCoreProvider),
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

    private val solariaBgProvider: () -> Brush = {
        Brush.verticalGradient(
            listOf(Color(0xFF331A00), Color(0xFF5E3500), Color(0xFF1A0A00)),
            0f, bH
        )
    }

    private val solariaSun1Provider: () -> Brush = {
        Brush.radialGradient(
            listOf(Color.White, Color(0xFFFFD54F), Color(0xFFFF6D00), Color.Transparent),
            center = Offset(bW * 0.38f, bH * 0.22f),
            radius = 85f
        )
    }

    private val solariaSun2Provider: () -> Brush = {
        Brush.radialGradient(
            listOf(Color.White, Color(0xFF80D8FF), Color(0xFF00B0FF), Color.Transparent),
            center = Offset(bW * 0.62f, bH * 0.16f),
            radius = 55f
        )
    }

    private val solariaSeaProvider: () -> Brush = {
        Brush.verticalGradient(
            listOf(Color(0xFFFFB300).copy(alpha = 0.8f), Color(0xFFE65100)),
            bStartY, bH
        )
    }

    private val solariaBeamProvider: () -> Brush = {
        Brush.verticalGradient(
            listOf(Color.White.copy(alpha = 0.25f), Color(0xFFFFD54F).copy(alpha = 0.12f), Color.Transparent),
            startY = bH * 0.22f,
            endY = bH
        )
    }

    private fun DrawScope.renderSolariaSolarLagoon(
        screenWidth: Float,
        screenHeight: Float,
        progress: Float,
        time: Float
    ) {
        bW = screenWidth
        bH = screenHeight

        // Blazing twin-star sky & amber sea (cached)
        drawRect(brush = cachedBrush(keyOf(13, (screenHeight / 64f).toInt()), solariaBgProvider))

        // Twin Suns in the Sky (cached gradients; positions derive from screen size)
        val sun1Pos = Offset(screenWidth * 0.38f, screenHeight * 0.22f)
        val sun2Pos = Offset(screenWidth * 0.62f, screenHeight * 0.16f)

        // Primary Sun (Golden Giant)
        drawCircle(
            brush = cachedBrush(keyOf(14, (screenWidth / 64f).toInt() * 31 + (screenHeight / 64f).toInt()), solariaSun1Provider),
            radius = 85f,
            center = sun1Pos
        )

        // Secondary Sun (Ultraviolet White Dwarf)
        drawCircle(
            brush = cachedBrush(keyOf(15, (screenWidth / 64f).toInt() * 31 + (screenHeight / 64f).toInt()), solariaSun2Provider),
            radius = 55f,
            center = sun2Pos
        )

        // Shallow Golden Water Surface (cached)
        val seaY = screenHeight * 0.56f
        bStartY = seaY
        drawRect(
            brush = cachedBrush(keyOf(16, (seaY / 16f).toInt()), solariaSeaProvider),
            topLeft = Offset(0f, seaY),
            size = Size(screenWidth, screenHeight - seaY)
        )

        // UV Radiation Beams / God rays shining through the water
        // (ONE cached brush shared by all 8 beams + one reused path)
        val beamBrush = cachedBrush(keyOf(17, (screenHeight / 64f).toInt()), solariaBeamProvider)
        for (r in 0 until 8) {
            val rx = screenWidth * (0.2f + r * 0.09f)
            pathH.reset()
            pathH.moveTo(sun1Pos.x, sun1Pos.y)
            pathH.lineTo(rx - 25f, screenHeight)
            pathH.lineTo(rx + 25f, screenHeight)
            pathH.close()
            drawPath(path = pathH, brush = beamBrush)
        }

        // Shimmering Caustics on the shallow sulfur bed
        for (c in 0 until 14) {
            val cx = (c * 65f + sin(time * 2f + c) * 20f) % screenWidth
            val cy = seaY + 40f + (c % 4) * 25f
            drawCircle(
                color = Color.White.copy(alpha = 0.45f + sin(time * 4f + c) * 0.25f),
                radius = 12f + (c % 5) * 4f,
                center = Offset(cx, cy),
                style = StrokeCache.plain(2.5f)
            )
        }
    }

    // =========================================================================
    // FASE 2: CATÁLISIS QUÍMICA PREBIÓTICA Y SÍNTESIS DE MONÓMEROS
    // =========================================================================
    private val chemBgProvider: () -> Brush = {
        val p = bPlanet!!
        Brush.radialGradient(
            listOf(Color(p.oceanBgColor1), Color.Black),
            center = Offset(bW / 2f, bH / 2f),
            radius = bW * 0.8f
        )
    }

    private val chemMonomerProvider: () -> Brush = {
        val p = bPlanet!!
        val a = bAlpha
        val monomerPos = Offset(bW / 2f, bH / 2f - 60f)
        Brush.radialGradient(
            listOf(
                Color(p.atmosphereColorHex).copy(alpha = 0.8f * a),
                Color(p.atmosphereColorHex).copy(alpha = 0.25f * a),
                Color.Transparent
            ),
            center = monomerPos,
            radius = 80f
        )
    }

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
            bPlanet = planet
            bW = screenWidth
            bH = screenHeight

            // Dark microscopic broth background (cached)
            drawRect(
                brush = cachedBrush(keyOf(18, planet.oceanBgColor1.hashCode() * 31 + (screenWidth / 64f).toInt()), chemBgProvider)
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
            val moleculeTypes = MOLECULE_TYPES

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
                        style = StrokeCache.plain(2f)
                    )
                }
            }

            // Central Activated Prebiotic Monomer Formed (e.g. Ribonucleotide / Amino Acid)
            val corePulse = (sin(time * 4f) * 0.2f + 0.8f)
            val monomerPos = Offset(center.x, center.y - 60f)
            val pulseBucket = (corePulse * 32f).toInt().coerceIn(1, 32)
            bAlpha = pulseBucket / 32f
            drawCircle(
                brush = cachedBrush(4000 + planet.atmosphereColorHex.hashCode() * 32 + pulseBucket, chemMonomerProvider),
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
    private val asmBgProvider: () -> Brush = {
        val p = bPlanet!!
        Brush.radialGradient(
            listOf(Color(p.oceanBgColor1).copy(alpha = 0.8f), Color(0xFF020710)),
            center = Offset(bW / 2f, bH / 2f),
            radius = bW * 0.85f
        )
    }

    private val asmEncapProvider: () -> Brush = {
        val p = bPlanet!!
        val a = bAlpha
        val vesicleCenter = Offset(bW / 2f, bH / 2f + 70f)
        Brush.radialGradient(
            listOf(
                Color.White.copy(alpha = a),
                Color(p.atmosphereColorHex).copy(alpha = a * 0.5f),
                Color.Transparent
            ),
            center = vesicleCenter,
            radius = bStartY
        )
    }

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
            bPlanet = planet
            bW = screenWidth
            bH = screenHeight

            // Dark soup canvas (cached)
            drawRect(
                brush = cachedBrush(keyOf(19, planet.oceanBgColor1.hashCode() * 31 + (screenWidth / 64f).toInt()), asmBgProvider)
            )

            // 1. RNA World Double/Single Helix Polymeric Strand
            val rnaLength = 32
            val baseColors = RNA_BASE_COLORS

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

            // Interior Encapsulated Ribozyme / Core Energy Glow (cached, quantized)
            if (progress > 0.6f) {
                val encapAlpha = ((progress - 0.6f) / 0.4f).coerceIn(0f, 1f)
                val encapBucket = (encapAlpha * 32f).toInt().coerceIn(1, 32)
                bAlpha = encapBucket / 32f
                bStartY = vesicleRadius * 0.65f // gradient radius
                drawCircle(
                    brush = cachedBrush(5000 + planet.atmosphereColorHex.hashCode() * 40 + encapBucket + ((vesicleRadius * 0.65f / 4f).toInt() and 0x3F), asmEncapProvider),
                    radius = vesicleRadius * 0.65f,
                    center = vesicleCenter
                )
            }
        }
    }

    // =========================================================================
    // FASE 4: DESPERTAR CELULAR Y PRIMER NADO
    // =========================================================================
    private val awakenBgProvider: () -> Brush = {
        val p = bPlanet!!
        Brush.radialGradient(
            listOf(Color(p.oceanBgColor1), Color(p.oceanBgColor2)),
            center = Offset(bW / 2f, bH / 2f),
            radius = bW
        )
    }

    private val bioHaloProvider: () -> Brush = {
        val p = bPlanet!!
        val a = bAlpha
        val radius = bStartY
        Brush.radialGradient(
            listOf(
                Color(p.oceanRimColor).copy(alpha = 0.35f * a),
                Color.Transparent
            ),
            center = Offset(bW / 2f, bH / 2f),
            radius = radius
        )
    }

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
            bPlanet = planet
            bW = screenWidth
            bH = screenHeight

            // Primordial Ocean Water with caustic ripples (cached)
            drawRect(
                brush = cachedBrush(keyOf(20, planet.oceanBgColor1.hashCode() * 31 + (screenWidth / 64f).toInt()), awakenBgProvider)
            )

            // Emergent life shockwave ripple pulses expanding outward
            val pulse1 = (time * 1.4f) % 1.0f
            drawCircle(
                color = Color(planet.oceanRimColor).copy(alpha = (1f - pulse1) * 0.75f),
                radius = 70f + pulse1 * 220f,
                center = center,
                style = StrokeCache.plain(3.5f)
            )

            val pulse2 = (time * 1.4f + 0.5f) % 1.0f
            drawCircle(
                color = Color.White.copy(alpha = (1f - pulse2) * 0.5f),
                radius = 70f + pulse2 * 180f,
                center = center,
                style = StrokeCache.plain(2f)
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

            // Halo of first consciousness / bioenergy (cached, quantized radius & alpha)
            val bioPulse = (sin(time * 5f) * 0.2f + 0.8f)
            val bioBucket = (bioPulse * 32f).toInt().coerceIn(1, 32)
            val haloRadius = cellRadius * 2.2f
            bAlpha = bioBucket / 32f
            bStartY = haloRadius
            drawCircle(
                brush = cachedBrush(6000 + planet.oceanRimColor.hashCode() * 600 + bioBucket * 16 + ((haloRadius / 8f).toInt() and 0xF), bioHaloProvider),
                radius = haloRadius,
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
        // Static RNG table (identical Random(1337) sequence - pixel-identical motes)
        for (i in 0 until moteCount) {
            val baseX = MOTE_XYS[i * 3] * screenWidth
            val baseY = MOTE_XYS[i * 3 + 1] * screenHeight
            val speed = MOTE_XYS[i * 3 + 2]

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
