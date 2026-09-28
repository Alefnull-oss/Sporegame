package com.example.spore.game.terrain

import androidx.compose.ui.graphics.Color
import com.example.spore.data.model.PlanetDefinition
import com.example.spore.game.engine.Vector2
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.random.Random

enum class OceanBiomeType(
    val title: String,
    val description: String,
    val avgDepthMeters: Int,
    val baseDeepTint: Long,
    val baseShallowTint: Long,
    val causticsIntensity: Float,
    val currentSpeedMultiplier: Float
) {
    SUNLIT_SHALLOWS(
        title = "Bancos de Arena Someros",
        description = "Aguas cristalinas con cáusticas solares vívidas",
        avgDepthMeters = 8,
        baseDeepTint = 0xFF03254C,
        baseShallowTint = 0xFF00E5FF,
        causticsIntensity = 1.0f,
        currentSpeedMultiplier = 0.6f
    ),
    CORAL_REEF(
        title = "Arrecife Bioluminiscente",
        description = "Estructuras de coral vivo y pólipos fluorescentes",
        avgDepthMeters = 24,
        baseDeepTint = 0xFF021B35,
        baseShallowTint = 0xFF00B0FF,
        causticsIntensity = 0.8f,
        currentSpeedMultiplier = 0.8f
    ),
    KELP_FOREST(
        title = "Bosque de Quelpos Abisales",
        description = "Tallos gigantes de algas meciéndose en el oleaje",
        avgDepthMeters = 42,
        baseDeepTint = 0xFF022018,
        baseShallowTint = 0xFF00E676,
        causticsIntensity = 0.65f,
        currentSpeedMultiplier = 1.0f
    ),
    PELAGIC_JET(
        title = "Gran Corriente Termohalina",
        description = "Flujo laminar veloz de nutrientes oceánicos",
        avgDepthMeters = 65,
        baseDeepTint = 0xFF011627,
        baseShallowTint = 0xFF2979FF,
        causticsIntensity = 0.5f,
        currentSpeedMultiplier = 1.7f
    ),
    HADAL_TRENCH(
        title = "Fosa Abisal de Hadal",
        description = "Chimeneas hidrotermales y presión extrema",
        avgDepthMeters = 115,
        baseDeepTint = 0xFF060317,
        baseShallowTint = 0xFF651FFF,
        causticsIntensity = 0.25f,
        currentSpeedMultiplier = 0.9f
    )
}

/**
 * Data structures for living ocean floor elements.
 */
data class KelpPlant(
    val anchor: Vector2,
    val height: Float,
    val segmentCount: Int = 6,
    val swayPhase: Float,
    val swayFrequency: Float,
    val frondColor: Color,
    val stalkWidth: Float = 4f
)

data class HydrothermalVent(
    val position: Vector2,
    val chimneyWidth: Float = 42f,
    val chimneyHeight: Float = 55f,
    val glowColor: Color = Color(0xFFFF5722),
    var bubbleTimer: Float = 0f
)

data class CoralStructure(
    val position: Vector2,
    val radius: Float,
    val branchCount: Int,
    val glowColor: Color,
    val secondaryColor: Color
)

data class VentBubble(
    var position: Vector2,
    var velocity: Vector2,
    var radius: Float,
    var alpha: Float,
    var life: Float,
    var maxLife: Float,
    var color: Color
)

/**
 * Comprehensive Ocean Terrain System.
 * Generates and maintains a continuous, wall-free oceanic landscape:
 * - Fluid ocean currents that affect drift
 * - Living kelp forests that respond to current and nearby swimmers
 * - Hydrothermal volcanic vents that emit thermal bubble plumes
 * - Coral reef formations with bioluminescent glow
 * - Dynamic seabed sand ripples
 */
class OceanTerrainSystem(
    val worldSize: Float = 7200f,
    var planet: PlanetDefinition
) {
    val kelpPlants = mutableListOf<KelpPlant>()
    val vents = mutableListOf<HydrothermalVent>()
    val corals = mutableListOf<CoralStructure>()
    val ventBubbles = mutableListOf<VentBubble>()

    var timeSeconds: Float = 0f
        private set

    init {
        generateTerrainFeatures()
    }

    private fun generateTerrainFeatures() {
        kelpPlants.clear()
        vents.clear()
        corals.clear()
        ventBubbles.clear()

        val random = Random(42)

        // 1. Generate Kelp Forests (dense groves in kelp biome zones)
        val kelpZoneCenters = listOf(
            Vector2(worldSize * 0.2f, worldSize * 0.75f),
            Vector2(worldSize * 0.7f, worldSize * 0.3f),
            Vector2(worldSize * 0.85f, worldSize * 0.85f)
        )

        for (center in kelpZoneCenters) {
            for (i in 0 until 24) {
                val offset = Vector2(
                    random.nextFloat() * 700f - 350f,
                    random.nextFloat() * 700f - 350f
                )
                val pos = wrapVector(center + offset)
                val kelpColor = if (random.nextBoolean()) Color(0xFF00BFA5) else Color(0xFF00E676)
                kelpPlants.add(
                    KelpPlant(
                        anchor = pos,
                        height = random.nextFloat() * 140f + 160f,
                        segmentCount = 6,
                        swayPhase = random.nextFloat() * 2f * PI.toFloat(),
                        swayFrequency = random.nextFloat() * 0.6f + 0.8f,
                        frondColor = kelpColor.copy(alpha = 0.75f),
                        stalkWidth = random.nextFloat() * 3f + 3f
                    )
                )
            }
        }

        // 2. Generate Hydrothermal Vents in deep trench zones
        val trenchZoneCenters = listOf(
            Vector2(worldSize * 0.5f, worldSize * 0.5f),
            Vector2(worldSize * 0.15f, worldSize * 0.25f),
            Vector2(worldSize * 0.8f, worldSize * 0.65f)
        )

        for (center in trenchZoneCenters) {
            for (i in 0 until 5) {
                val offset = Vector2(
                    random.nextFloat() * 600f - 300f,
                    random.nextFloat() * 600f - 300f
                )
                val pos = wrapVector(center + offset)
                vents.add(
                    HydrothermalVent(
                        position = pos,
                        chimneyWidth = random.nextFloat() * 20f + 36f,
                        chimneyHeight = random.nextFloat() * 30f + 45f,
                        glowColor = if (random.nextBoolean()) Color(0xFFFF6E40) else Color(0xFFFFD54F)
                    )
                )
            }
        }

        // 3. Generate Coral Formations in reef zones
        val reefZoneCenters = listOf(
            Vector2(worldSize * 0.35f, worldSize * 0.2f),
            Vector2(worldSize * 0.65f, worldSize * 0.8f),
            Vector2(worldSize * 0.15f, worldSize * 0.85f)
        )

        for (center in reefZoneCenters) {
            for (i in 0 until 18) {
                val offset = Vector2(
                    random.nextFloat() * 650f - 325f,
                    random.nextFloat() * 650f - 325f
                )
                val pos = wrapVector(center + offset)
                val glow = when (random.nextInt(3)) {
                    0 -> Color(0xFF00E5FF)
                    1 -> Color(0xFFD500F9)
                    else -> Color(0xFF76FF03)
                }
                corals.add(
                    CoralStructure(
                        position = pos,
                        radius = random.nextFloat() * 28f + 22f,
                        branchCount = random.nextInt(4) + 4,
                        glowColor = glow,
                        secondaryColor = Color(planet.oceanRimColor).copy(alpha = 0.8f)
                    )
                )
            }
        }
    }

    /**
     * Determines the ocean biome at any coordinates using continuous 2D procedural voronoi/harmonic mapping.
     */
    fun getBiomeAt(pos: Vector2): OceanBiomeType {
        val nx = (pos.x / worldSize) * 2f * PI.toFloat()
        val ny = (pos.y / worldSize) * 2f * PI.toFloat()

        // Continuous harmonic depth noise on a 2D torus (seamless wrap)
        val n1 = sin(nx) * cos(ny)
        val n2 = sin(nx * 2f + 0.8f) * sin(ny * 2f) * 0.5f
        val n3 = cos(nx * 3f + ny * 2f) * 0.25f
        val depthVal = (n1 + n2 + n3 + 1.75f) / 3.5f // normalized 0..1

        return when {
            depthVal < 0.22f -> OceanBiomeType.SUNLIT_SHALLOWS
            depthVal < 0.44f -> OceanBiomeType.CORAL_REEF
            depthVal < 0.66f -> OceanBiomeType.KELP_FOREST
            depthVal < 0.84f -> OceanBiomeType.PELAGIC_JET
            else -> OceanBiomeType.HADAL_TRENCH
        }
    }

    /**
     * Oceanic current flow vector at any location.
     * Generates swirling gyres and jet streams seamlessly wrapped.
     */
    fun getCurrentVelocityAt(pos: Vector2): Vector2 {
        val nx = (pos.x / worldSize) * 2f * PI.toFloat()
        val ny = (pos.y / worldSize) * 2f * PI.toFloat()

        val biome = getBiomeAt(pos)
        val speedBase = 18f * biome.currentSpeedMultiplier

        // Oceanic gyre stream vector field
        val vx = (-sin(ny) + cos(nx * 2f) * 0.35f) * speedBase
        val vy = (cos(nx) + sin(ny * 2f) * 0.35f) * speedBase

        return Vector2(vx, vy)
    }

    fun update(deltaTime: Float) {
        val dt = deltaTime.coerceIn(0.005f, 0.05f)
        timeSeconds += dt

        // Update hydrothermal vent bubble plumes
        for (vent in vents) {
            vent.bubbleTimer += dt
            if (vent.bubbleTimer >= 0.12f) {
                vent.bubbleTimer = 0f
                if (ventBubbles.size < 90) {
                    val angle = -PI.toFloat() / 2f + (Random.nextFloat() * 0.5f - 0.25f)
                    val speed = Random.nextFloat() * 80f + 60f
                    val bubblePos = vent.position + Vector2(Random.nextFloat() * 16f - 8f, -vent.chimneyHeight * 0.8f)
                    ventBubbles.add(
                        VentBubble(
                            position = bubblePos,
                            velocity = Vector2.fromAngle(angle, speed),
                            radius = Random.nextFloat() * 3f + 1.5f,
                            alpha = Random.nextFloat() * 0.4f + 0.5f,
                            life = 0f,
                            maxLife = Random.nextFloat() * 1.5f + 1.2f,
                            color = vent.glowColor.copy(alpha = 0.85f)
                        )
                    )
                }
            }
        }

        // Update vent bubbles
        val iter = ventBubbles.iterator()
        while (iter.hasNext()) {
            val b = iter.next()
            b.life += dt
            b.position = b.position + (b.velocity * dt)
            b.velocity = b.velocity * 0.98f + Vector2(0f, -8f * dt) // buoyancy
            b.alpha = ((1f - b.life / b.maxLife) * 0.7f).coerceIn(0f, 1f)
            if (b.life >= b.maxLife) {
                iter.remove()
            }
        }
    }

    fun wrapVector(v: Vector2): Vector2 {
        var x = v.x % worldSize
        if (x < 0f) x += worldSize
        var y = v.y % worldSize
        if (y < 0f) y += worldSize
        return Vector2(x, y)
    }
}
