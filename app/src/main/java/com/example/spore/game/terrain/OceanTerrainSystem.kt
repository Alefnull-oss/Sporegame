package com.example.spore.game.terrain

import androidx.compose.ui.graphics.Color
import com.example.spore.data.model.PlanetDefinition
import com.example.spore.game.engine.Vector2
import com.example.spore.game.noise.FastNoiseLite
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

enum class OceanBiomeType(
    val defaultTitle: String,
    val description: String,
    val avgDepthMeters: Int,
    val causticsIntensity: Float,
    val currentSpeedMultiplier: Float
) {
    SUNLIT_SHALLOWS(
        defaultTitle = "Bancos de Arena Someros",
        description = "Aguas cristalinas con cáusticas solares vívidas",
        avgDepthMeters = 8,
        causticsIntensity = 1.0f,
        currentSpeedMultiplier = 0.6f
    ),
    CORAL_REEF(
        defaultTitle = "Arrecife Bioluminiscente",
        description = "Estructuras de coral vivo y pólipos fluorescentes",
        avgDepthMeters = 24,
        causticsIntensity = 0.8f,
        currentSpeedMultiplier = 0.8f
    ),
    KELP_FOREST(
        defaultTitle = "Bosque de Quelpos Abisales",
        description = "Tallos gigantes de algas meciéndose en el oleaje",
        avgDepthMeters = 42,
        causticsIntensity = 0.65f,
        currentSpeedMultiplier = 1.0f
    ),
    PELAGIC_JET(
        defaultTitle = "Gran Corriente Termohalina",
        description = "Flujo laminar veloz de nutrientes oceánicos",
        avgDepthMeters = 65,
        causticsIntensity = 0.5f,
        currentSpeedMultiplier = 1.7f
    ),
    HADAL_TRENCH(
        defaultTitle = "Fosa Abisal de Hadal",
        description = "Chimeneas hidrotermales y presión extrema",
        avgDepthMeters = 115,
        causticsIntensity = 0.25f,
        currentSpeedMultiplier = 0.9f
    )
}

/**
 * Planet-specific ocean styling and thematic color palette.
 * Ensures each world (especially Ametistia) maintains its authentic colors (purple, violet, magenta)
 * without arbitrary green tints or sudden color flicker.
 */
data class PlanetOceanTheme(
    val planetId: String,
    val shallowColors: Map<OceanBiomeType, Color>,
    val deepColors: Map<OceanBiomeType, Color>,
    val kelpFrondColors: List<Color>,
    val coralGlowColors: List<Color>,
    val ventGlowColors: List<Color>,
    val duneColor: Color,
    val causticColor: Color,
    val sunbeamColor: Color,
    val wakeTroughColor: Color,
    val defaultRippleColor: Color,
    val biomeTitles: Map<OceanBiomeType, String>
) {
    fun getShallowColor(biome: OceanBiomeType): Color =
        shallowColors[biome] ?: shallowColors.values.firstOrNull() ?: Color(0xFF00E5FF)

    fun getDeepColor(biome: OceanBiomeType): Color =
        deepColors[biome] ?: deepColors.values.firstOrNull() ?: Color(0xFF010812)

    /**
     * Interpolates continuous shallow water color across normalized depth (0.0 to 1.0).
     */
    fun getInterpolatedShallowColor(depth: Float): Color {
        val d = depth.coerceIn(0f, 1f)
        val s0 = getShallowColor(OceanBiomeType.SUNLIT_SHALLOWS)
        val s1 = getShallowColor(OceanBiomeType.CORAL_REEF)
        val s2 = getShallowColor(OceanBiomeType.KELP_FOREST)
        val s3 = getShallowColor(OceanBiomeType.PELAGIC_JET)
        val s4 = getShallowColor(OceanBiomeType.HADAL_TRENCH)

        return when {
            d < 0.25f -> lerpColor(s0, s1, d / 0.25f)
            d < 0.50f -> lerpColor(s1, s2, (d - 0.25f) / 0.25f)
            d < 0.75f -> lerpColor(s2, s3, (d - 0.50f) / 0.25f)
            else -> lerpColor(s3, s4, (d - 0.75f) / 0.25f)
        }
    }

    /**
     * Interpolates continuous deep water color across normalized depth (0.0 to 1.0).
     */
    fun getInterpolatedDeepColor(depth: Float): Color {
        val d = depth.coerceIn(0f, 1f)
        val d0 = getDeepColor(OceanBiomeType.SUNLIT_SHALLOWS)
        val d1 = getDeepColor(OceanBiomeType.CORAL_REEF)
        val d2 = getDeepColor(OceanBiomeType.KELP_FOREST)
        val d3 = getDeepColor(OceanBiomeType.PELAGIC_JET)
        val d4 = getDeepColor(OceanBiomeType.HADAL_TRENCH)

        return when {
            d < 0.25f -> lerpColor(d0, d1, d / 0.25f)
            d < 0.50f -> lerpColor(d1, d2, (d - 0.25f) / 0.25f)
            d < 0.75f -> lerpColor(d2, d3, (d - 0.50f) / 0.25f)
            else -> lerpColor(d3, d4, (d - 0.75f) / 0.25f)
        }
    }

    companion object {
        fun createForPlanet(planet: PlanetDefinition): PlanetOceanTheme {
            return when (planet.id) {
                "planet_ametistia" -> PlanetOceanTheme(
                    planetId = planet.id,
                    shallowColors = mapOf(
                        OceanBiomeType.SUNLIT_SHALLOWS to Color(0xFFBA68C8),
                        OceanBiomeType.CORAL_REEF to Color(0xFFE040FB),
                        OceanBiomeType.KELP_FOREST to Color(0xFF9C27B0),
                        OceanBiomeType.PELAGIC_JET to Color(0xFF7B1FA2),
                        OceanBiomeType.HADAL_TRENCH to Color(0xFF4A148C)
                    ),
                    deepColors = mapOf(
                        OceanBiomeType.SUNLIT_SHALLOWS to Color(0xFF22083D),
                        OceanBiomeType.CORAL_REEF to Color(0xFF19042E),
                        OceanBiomeType.KELP_FOREST to Color(0xFF130224),
                        OceanBiomeType.PELAGIC_JET to Color(0xFF0F011E),
                        OceanBiomeType.HADAL_TRENCH to Color(0xFF080112)
                    ),
                    kelpFrondColors = listOf(
                        Color(0xFFD500F9),
                        Color(0xFFAB47BC),
                        Color(0xFF8E24AA)
                    ),
                    coralGlowColors = listOf(
                        Color(0xFFE040FB),
                        Color(0xFFD500F9),
                        Color(0xFFEA80FC)
                    ),
                    ventGlowColors = listOf(
                        Color(0xFFFF007F),
                        Color(0xFFD500F9)
                    ),
                    duneColor = Color(0xFF38084F),
                    causticColor = Color(0xFFEA80FC),
                    sunbeamColor = Color(0xFFF3E5F5),
                    wakeTroughColor = Color(0xFF0A0114),
                    defaultRippleColor = Color(0xFFE040FB),
                    biomeTitles = mapOf(
                        OceanBiomeType.SUNLIT_SHALLOWS to "Bancos de Amatista Someros",
                        OceanBiomeType.CORAL_REEF to "Arrecife de Cuarzo Púrpura",
                        OceanBiomeType.KELP_FOREST to "Bosque de Quelpos Violetas",
                        OceanBiomeType.PELAGIC_JET to "Corriente de Metano Líquido",
                        OceanBiomeType.HADAL_TRENCH to "Fosa Abisal Ultra-Violeta"
                    )
                )

                "planet_rubrum" -> PlanetOceanTheme(
                    planetId = planet.id,
                    shallowColors = mapOf(
                        OceanBiomeType.SUNLIT_SHALLOWS to Color(0xFFFF5252),
                        OceanBiomeType.CORAL_REEF to Color(0xFFFF1744),
                        OceanBiomeType.KELP_FOREST to Color(0xFFD50000),
                        OceanBiomeType.PELAGIC_JET to Color(0xFFFF6E40),
                        OceanBiomeType.HADAL_TRENCH to Color(0xFF880E4F)
                    ),
                    deepColors = mapOf(
                        OceanBiomeType.SUNLIT_SHALLOWS to Color(0xFF3B0810),
                        OceanBiomeType.CORAL_REEF to Color(0xFF2A050C),
                        OceanBiomeType.KELP_FOREST to Color(0xFF1E0308),
                        OceanBiomeType.PELAGIC_JET to Color(0xFF160205),
                        OceanBiomeType.HADAL_TRENCH to Color(0xFF0D0104)
                    ),
                    kelpFrondColors = listOf(
                        Color(0xFFFF1744),
                        Color(0xFFD50000),
                        Color(0xFFFF5252)
                    ),
                    coralGlowColors = listOf(
                        Color(0xFFFF1744),
                        Color(0xFFFF6E40),
                        Color(0xFFFFAB40)
                    ),
                    ventGlowColors = listOf(
                        Color(0xFFFF3D00),
                        Color(0xFFFF6E40)
                    ),
                    duneColor = Color(0xFF420810),
                    causticColor = Color(0xFFFF8A80),
                    sunbeamColor = Color(0xFFFFEBEE),
                    wakeTroughColor = Color(0xFF140205),
                    defaultRippleColor = Color(0xFFFF1744),
                    biomeTitles = mapOf(
                        OceanBiomeType.SUNLIT_SHALLOWS to "Bancos de Hierro Carmesí",
                        OceanBiomeType.CORAL_REEF to "Arrecife de Coral Rojo Sangre",
                        OceanBiomeType.KELP_FOREST to "Bosque de Quelpos Rojos",
                        OceanBiomeType.PELAGIC_JET to "Corriente de Óxido Ferrítico",
                        OceanBiomeType.HADAL_TRENCH to "Fosa Ígnea de Rubrum"
                    )
                )

                "planet_toxis" -> PlanetOceanTheme(
                    planetId = planet.id,
                    shallowColors = mapOf(
                        OceanBiomeType.SUNLIT_SHALLOWS to Color(0xFF76FF03),
                        OceanBiomeType.CORAL_REEF to Color(0xFFAEEA00),
                        OceanBiomeType.KELP_FOREST to Color(0xFF64DD17),
                        OceanBiomeType.PELAGIC_JET to Color(0xFF00E676),
                        OceanBiomeType.HADAL_TRENCH to Color(0xFF1B5E20)
                    ),
                    deepColors = mapOf(
                        OceanBiomeType.SUNLIT_SHALLOWS to Color(0xFF0B2E14),
                        OceanBiomeType.CORAL_REEF to Color(0xFF07210E),
                        OceanBiomeType.KELP_FOREST to Color(0xFF04190B),
                        OceanBiomeType.PELAGIC_JET to Color(0xFF031408),
                        OceanBiomeType.HADAL_TRENCH to Color(0xFF010C04)
                    ),
                    kelpFrondColors = listOf(
                        Color(0xFF00E676),
                        Color(0xFF76FF03)
                    ),
                    coralGlowColors = listOf(
                        Color(0xFF76FF03),
                        Color(0xFFAEEA00),
                        Color(0xFF00E676)
                    ),
                    ventGlowColors = listOf(
                        Color(0xFF76FF03),
                        Color(0xFFCCFF90)
                    ),
                    duneColor = Color(0xFF12360B),
                    causticColor = Color(0xFFCCFF90),
                    sunbeamColor = Color(0xFFF1F8E9),
                    wakeTroughColor = Color(0xFF021005),
                    defaultRippleColor = Color(0xFF76FF03),
                    biomeTitles = mapOf(
                        OceanBiomeType.SUNLIT_SHALLOWS to "Bancos de Azufre Esmeralda",
                        OceanBiomeType.CORAL_REEF to "Arrecife Bio-Radiactivo",
                        OceanBiomeType.KELP_FOREST to "Bosque de Quelpos Ácidos",
                        OceanBiomeType.PELAGIC_JET to "Corriente Tóxica Fluorescente",
                        OceanBiomeType.HADAL_TRENCH to "Fosa Sulfurosa Abisal"
                    )
                )

                "planet_solaria" -> PlanetOceanTheme(
                    planetId = planet.id,
                    shallowColors = mapOf(
                        OceanBiomeType.SUNLIT_SHALLOWS to Color(0xFFFFD600),
                        OceanBiomeType.CORAL_REEF to Color(0xFFFFAB00),
                        OceanBiomeType.KELP_FOREST to Color(0xFFFF8F00),
                        OceanBiomeType.PELAGIC_JET to Color(0xFFFF6D00),
                        OceanBiomeType.HADAL_TRENCH to Color(0xFFBF360C)
                    ),
                    deepColors = mapOf(
                        OceanBiomeType.SUNLIT_SHALLOWS to Color(0xFF382005),
                        OceanBiomeType.CORAL_REEF to Color(0xFF281804),
                        OceanBiomeType.KELP_FOREST to Color(0xFF1F1202),
                        OceanBiomeType.PELAGIC_JET to Color(0xFF170C01),
                        OceanBiomeType.HADAL_TRENCH to Color(0xFF0D0701)
                    ),
                    kelpFrondColors = listOf(
                        Color(0xFFFFD600),
                        Color(0xFFFFB300)
                    ),
                    coralGlowColors = listOf(
                        Color(0xFFFFD600),
                        Color(0xFFFFAB00),
                        Color(0xFFFF6D00)
                    ),
                    ventGlowColors = listOf(
                        Color(0xFFFF6D00),
                        Color(0xFFFFD54F)
                    ),
                    duneColor = Color(0xFF3D2405),
                    causticColor = Color(0xFFFFE082),
                    sunbeamColor = Color(0xFFFFFDE7),
                    wakeTroughColor = Color(0xFF140801),
                    defaultRippleColor = Color(0xFFFFD600),
                    biomeTitles = mapOf(
                        OceanBiomeType.SUNLIT_SHALLOWS to "Bancos de Arena Solar",
                        OceanBiomeType.CORAL_REEF to "Arrecife Dorado de Ámbar",
                        OceanBiomeType.KELP_FOREST to "Bosque de Quelpos Radiantes",
                        OceanBiomeType.PELAGIC_JET to "Corriente Térmica Helio-6",
                        OceanBiomeType.HADAL_TRENCH to "Fosa de Fusión Abisal"
                    )
                )

                else -> PlanetOceanTheme(
                    planetId = planet.id,
                    shallowColors = mapOf(
                        OceanBiomeType.SUNLIT_SHALLOWS to Color(0xFF00E5FF),
                        OceanBiomeType.CORAL_REEF to Color(0xFF00B0FF),
                        OceanBiomeType.KELP_FOREST to Color(0xFF00BFA5),
                        OceanBiomeType.PELAGIC_JET to Color(0xFF2979FF),
                        OceanBiomeType.HADAL_TRENCH to Color(0xFF1565C0)
                    ),
                    deepColors = mapOf(
                        OceanBiomeType.SUNLIT_SHALLOWS to Color(0xFF052A54),
                        OceanBiomeType.CORAL_REEF to Color(0xFF031E3C),
                        OceanBiomeType.KELP_FOREST to Color(0xFF02172F),
                        OceanBiomeType.PELAGIC_JET to Color(0xFF011022),
                        OceanBiomeType.HADAL_TRENCH to Color(0xFF010812)
                    ),
                    kelpFrondColors = listOf(
                        Color(0xFF00BFA5),
                        Color(0xFF00E5FF)
                    ),
                    coralGlowColors = listOf(
                        Color(0xFF00E5FF),
                        Color(0xFF00B0FF),
                        Color(0xFF40C4FF)
                    ),
                    ventGlowColors = listOf(
                        Color(0xFF00E5FF),
                        Color(0xFF80D8FF)
                    ),
                    duneColor = Color(0xFF002F38),
                    causticColor = Color(0xFF80D8FF),
                    sunbeamColor = Color(0xFFE0F7FA),
                    wakeTroughColor = Color(0xFF010B16),
                    defaultRippleColor = Color(0xFF00E5FF),
                    biomeTitles = mapOf(
                        OceanBiomeType.SUNLIT_SHALLOWS to "Bancos de Arena Someros",
                        OceanBiomeType.CORAL_REEF to "Arrecife Bioluminiscente",
                        OceanBiomeType.KELP_FOREST to "Bosque de Quelpos Abisales",
                        OceanBiomeType.PELAGIC_JET to "Gran Corriente Termohalina",
                        OceanBiomeType.HADAL_TRENCH to "Fosa Abisal de Hadal"
                    )
                )
            }
        }

        fun lerpColor(a: Color, b: Color, fraction: Float): Color {
            val t = fraction.coerceIn(0f, 1f)
            return Color(
                red = a.red + (b.red - a.red) * t,
                green = a.green + (b.green - a.green) * t,
                blue = a.blue + (b.blue - a.blue) * t,
                alpha = a.alpha + (b.alpha - a.alpha) * t
            )
        }
    }
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
    var position: Vector2 = Vector2.ZERO,
    var velocity: Vector2 = Vector2.ZERO,
    var radius: Float = 2f,
    var alpha: Float = 0.5f,
    var life: Float = 0f,
    var maxLife: Float = 1.5f,
    var color: Color = Color.White,
    var isActive: Boolean = false
)

/**
 * Comprehensive Ocean Terrain System.
 * Generates and maintains a continuous, wall-free oceanic landscape with:
 * - Seamless toroidal coordinate wrapping (no walls)
 * - Continuous smooth depth blending to eliminate all color flickering
 * - Planet-adapted ocean theme palettes (purple/amethyst in Ametistia, never green)
 * - Hydrothermal volcanic vents, corals, swaying kelp, and dynamic sand silt
 */
class OceanTerrainSystem(
    val worldSize: Float = 7200f,
    var planet: PlanetDefinition
) {
    var theme: PlanetOceanTheme = PlanetOceanTheme.createForPlanet(planet)
        private set

    val kelpPlants = mutableListOf<KelpPlant>()
    val vents = mutableListOf<HydrothermalVent>()
    val corals = mutableListOf<CoralStructure>()
    companion object {
        const val MAX_VENT_BUBBLES = 40
    }
    val ventBubbles = Array(MAX_VENT_BUBBLES) { VentBubble() }

    var timeSeconds: Float = 0f
        private set

    // Continuous, filtered depth and smooth color transitions (Zero-flicker)
    var currentDepthFactor: Float = 0.5f
        private set

    var smoothedDeepColor: Color = Color(planet.oceanBgColor2)
        private set

    var smoothedShallowColor: Color = Color(planet.oceanRimColor)
        private set

    var smoothedCausticColor: Color = Color(planet.ambientParticleColor)
        private set

    var activeBiome: OceanBiomeType = OceanBiomeType.SUNLIT_SHALLOWS
        private set

    // FastNoiseLite Procedural Generators for Bathymetry, Voronoi Trenches, Curl Gyres, and Nutrient Blooms
    private val bathymetryNoise = FastNoiseLite(planet.id.hashCode()).apply {
        noiseType = FastNoiseLite.NoiseType.OpenSimplex2
        fractalType = FastNoiseLite.FractalType.FBm
        octaves = 3
        frequency = 0.0018f
    }

    private val cellularTrenchNoise = FastNoiseLite(planet.id.hashCode() + 101).apply {
        noiseType = FastNoiseLite.NoiseType.Cellular
        cellularDistanceFunction = FastNoiseLite.CellularDistanceFunction.EuclideanSq
        cellularReturnType = FastNoiseLite.CellularReturnType.Distance2Sub
        frequency = 0.0012f
    }

    private val currentCurlNoise = FastNoiseLite(planet.id.hashCode() + 777).apply {
        noiseType = FastNoiseLite.NoiseType.OpenSimplex2
        fractalType = FastNoiseLite.FractalType.FBm
        octaves = 2
        frequency = 0.0015f
    }

    private val nutrientBloomNoise = FastNoiseLite(planet.id.hashCode() + 555).apply {
        noiseType = FastNoiseLite.NoiseType.Cellular
        cellularDistanceFunction = FastNoiseLite.CellularDistanceFunction.Euclidean
        cellularReturnType = FastNoiseLite.CellularReturnType.Distance
        frequency = 0.0022f
    }

    init {
        generateTerrainFeatures()
        // Initialize smoothed colors to target
        smoothedDeepColor = theme.getDeepColor(OceanBiomeType.SUNLIT_SHALLOWS)
        smoothedShallowColor = theme.getShallowColor(OceanBiomeType.SUNLIT_SHALLOWS)
        smoothedCausticColor = theme.causticColor
    }

    fun updatePlanet(newPlanet: PlanetDefinition) {
        planet = newPlanet
        theme = PlanetOceanTheme.createForPlanet(newPlanet)
        val seed = newPlanet.id.hashCode()
        bathymetryNoise.seed = seed
        cellularTrenchNoise.seed = seed + 101
        currentCurlNoise.seed = seed + 777
        nutrientBloomNoise.seed = seed + 555
        smoothedDeepColor = theme.getDeepColor(activeBiome)
        smoothedShallowColor = theme.getShallowColor(activeBiome)
        smoothedCausticColor = theme.causticColor
        generateTerrainFeatures()
    }

    private fun generateTerrainFeatures() {
        kelpPlants.clear()
        vents.clear()
        corals.clear()
        for (i in 0 until MAX_VENT_BUBBLES) {
            ventBubbles[i].isActive = false
        }

        val random = Random(planet.id.hashCode() + 42)
        val frondColors = theme.kelpFrondColors
        val ventGlows = theme.ventGlowColors
        val coralGlows = theme.coralGlowColors

        // Procedural distribution across world using FastNoiseLite bathymetry and biomes
        val gridSize = 24
        val step = worldSize / gridSize
        for (gx in 0 until gridSize) {
            for (gy in 0 until gridSize) {
                val samplePos = Vector2(
                    (gx + random.nextFloat() * 0.8f + 0.1f) * step,
                    (gy + random.nextFloat() * 0.8f + 0.1f) * step
                )
                val biome = getBiomeAt(samplePos)

                when (biome) {
                    OceanBiomeType.HADAL_TRENCH -> {
                        if (vents.size < 18 && random.nextFloat() < 0.40f) {
                            val glow = ventGlows[random.nextInt(ventGlows.size)]
                            vents.add(
                                HydrothermalVent(
                                    position = samplePos,
                                    chimneyWidth = random.nextFloat() * 18f + 36f,
                                    chimneyHeight = random.nextFloat() * 25f + 45f,
                                    glowColor = glow
                                )
                            )
                        }
                    }
                    OceanBiomeType.CORAL_REEF -> {
                        if (corals.size < 45 && random.nextFloat() < 0.65f) {
                            val glow = coralGlows[random.nextInt(coralGlows.size)]
                            corals.add(
                                CoralStructure(
                                    position = samplePos,
                                    radius = random.nextFloat() * 26f + 22f,
                                    branchCount = random.nextInt(4) + 4,
                                    glowColor = glow,
                                    secondaryColor = Color(planet.oceanRimColor).copy(alpha = 0.85f)
                                )
                            )
                        }
                    }
                    OceanBiomeType.KELP_FOREST -> {
                        if (kelpPlants.size < 70 && random.nextFloat() < 0.75f) {
                            val frondColor = frondColors[random.nextInt(frondColors.size)]
                            kelpPlants.add(
                                KelpPlant(
                                    anchor = samplePos,
                                    height = random.nextFloat() * 130f + 150f,
                                    segmentCount = 6,
                                    swayPhase = random.nextFloat() * 2f * PI.toFloat(),
                                    swayFrequency = random.nextFloat() * 0.6f + 0.8f,
                                    frondColor = frondColor.copy(alpha = 0.82f),
                                    stalkWidth = random.nextFloat() * 3f + 3f
                                )
                            )
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    /**
     * Seamless toroidal 2D noise projection to ensure 100% boundary-free continuity.
     */
    private fun getToroidalNoise(noise: FastNoiseLite, pos: Vector2): Float {
        val wx = (pos.x % worldSize + worldSize) % worldSize
        val wy = (pos.y % worldSize + worldSize) % worldSize
        val u = wx / worldSize
        val v = wy / worldSize
        val r = worldSize * 0.15f
        val pi2 = 2f * PI.toFloat()
        val nx1 = cos(u * pi2) * r
        val ny1 = sin(u * pi2) * r
        val nx2 = cos(v * pi2) * r
        val ny2 = sin(v * pi2) * r
        return (noise.getNoise(nx1 + nx2, ny1 - ny2) + noise.getNoise(nx1 - nx2, ny1 + ny2)) * 0.5f
    }

    /**
     * Calculates the raw normalized bathymetric depth factor (0.0 to 1.0) using FastNoiseLite
     * combining multi-octave Simplex bathymetry with Voronoi fault line trenches.
     */
    fun getDepthFactor(pos: Vector2): Float {
        // Multi-octave Simplex terrain
        val simplexDepth = (getToroidalNoise(bathymetryNoise, pos) + 1.0f) * 0.5f
        // Cellular Voronoi trench fissures (Hadal trenches along cell boundaries)
        val voronoiTrench = (getToroidalNoise(cellularTrenchNoise, pos) + 1.0f) * 0.5f

        // Deepen into abyss where voronoi fault line occurs
        val combined = simplexDepth * 0.7f + voronoiTrench * 0.3f
        return combined.coerceIn(0f, 1f)
    }

    /**
     * Determines whether a location is inside a rich plankton bloom zone using Cellular noise.
     */
    fun isNutrientBloomZone(pos: Vector2): Boolean {
        val n = (getToroidalNoise(nutrientBloomNoise, pos) + 1.0f) * 0.5f
        return n > 0.68f
    }

    /**
     * Determines the ocean biome at any coordinates using continuous 2D procedural mapping.
     */
    fun getBiomeAt(pos: Vector2): OceanBiomeType {
        val depthVal = getDepthFactor(pos)
        return when {
            depthVal < 0.22f -> OceanBiomeType.SUNLIT_SHALLOWS
            depthVal < 0.44f -> OceanBiomeType.CORAL_REEF
            depthVal < 0.66f -> OceanBiomeType.KELP_FOREST
            depthVal < 0.84f -> OceanBiomeType.PELAGIC_JET
            else -> OceanBiomeType.HADAL_TRENCH
        }
    }

    fun getBiomeTitle(biome: OceanBiomeType): String {
        return theme.biomeTitles[biome] ?: biome.defaultTitle
    }

    /**
     * Oceanic current flow vector at any location powered by FastNoiseLite Curl Noise.
     * Divergence-free fluid flow field generates authentic spiral gyres, eddies and jet streams.
     */
    fun getCurrentVelocityAt(pos: Vector2): Vector2 {
        val delta = 30f
        val biome = getBiomeAt(pos)
        val speedBase = 20f * biome.currentSpeedMultiplier

        // Incompressible 2D Curl Noise: v = (dPsi/dy, -dPsi/dx)
        val psiY1 = getToroidalNoise(currentCurlNoise, Vector2(pos.x, pos.y + delta))
        val psiY0 = getToroidalNoise(currentCurlNoise, Vector2(pos.x, pos.y - delta))
        val psiX1 = getToroidalNoise(currentCurlNoise, Vector2(pos.x + delta, pos.y))
        val psiX0 = getToroidalNoise(currentCurlNoise, Vector2(pos.x - delta, pos.y))

        val dPsiDy = (psiY1 - psiY0) / (2f * delta)
        val dPsiDx = (psiX1 - psiX0) / (2f * delta)

        var vx = dPsiDy * speedBase * 160f
        var vy = -dPsiDx * speedBase * 160f

        // Pelagic Jet biome adds strong laminar jet stream
        if (biome == OceanBiomeType.PELAGIC_JET) {
            vx += 16f * biome.currentSpeedMultiplier
        }

        return Vector2(vx, vy)
    }

    fun update(deltaTime: Float, playerPosition: Vector2? = null) {
        val dt = deltaTime.coerceIn(0.005f, 0.05f)
        timeSeconds += dt

        // 1. Smooth Depth Factor & Fluid Color Transitions (NO FLICKER)
        if (playerPosition != null) {
            val targetDepth = getDepthFactor(playerPosition)
            currentDepthFactor += (targetDepth - currentDepthFactor) * (dt * 3.0f).coerceIn(0f, 1f)

            // Evaluate active biome with deadband / hysteresis to prevent HUD text flickering
            val hysteresisDeadband = 0.035f
            when (activeBiome) {
                OceanBiomeType.SUNLIT_SHALLOWS -> {
                    if (currentDepthFactor > 0.22f + hysteresisDeadband) activeBiome = OceanBiomeType.CORAL_REEF
                }
                OceanBiomeType.CORAL_REEF -> {
                    if (currentDepthFactor < 0.22f - hysteresisDeadband) activeBiome = OceanBiomeType.SUNLIT_SHALLOWS
                    else if (currentDepthFactor > 0.44f + hysteresisDeadband) activeBiome = OceanBiomeType.KELP_FOREST
                }
                OceanBiomeType.KELP_FOREST -> {
                    if (currentDepthFactor < 0.44f - hysteresisDeadband) activeBiome = OceanBiomeType.CORAL_REEF
                    else if (currentDepthFactor > 0.66f + hysteresisDeadband) activeBiome = OceanBiomeType.PELAGIC_JET
                }
                OceanBiomeType.PELAGIC_JET -> {
                    if (currentDepthFactor < 0.66f - hysteresisDeadband) activeBiome = OceanBiomeType.KELP_FOREST
                    else if (currentDepthFactor > 0.84f + hysteresisDeadband) activeBiome = OceanBiomeType.HADAL_TRENCH
                }
                OceanBiomeType.HADAL_TRENCH -> {
                    if (currentDepthFactor < 0.84f - hysteresisDeadband) activeBiome = OceanBiomeType.PELAGIC_JET
                }
            }

            // Continuous interpolated target colors from theme
            val targetDeep = theme.getInterpolatedDeepColor(currentDepthFactor)
            val targetShallow = theme.getInterpolatedShallowColor(currentDepthFactor)

            // Smooth continuous exponential blend
            val blendSpeed = (dt * 3.5f).coerceIn(0f, 1f)
            smoothedDeepColor = PlanetOceanTheme.lerpColor(smoothedDeepColor, targetDeep, blendSpeed)
            smoothedShallowColor = PlanetOceanTheme.lerpColor(smoothedShallowColor, targetShallow, blendSpeed)
            smoothedCausticColor = PlanetOceanTheme.lerpColor(smoothedCausticColor, theme.causticColor, blendSpeed)
        }

        // 2. Update hydrothermal vent bubble plumes for nearby vents only
        if (playerPosition != null) {
            for (vent in vents) {
                val dist = vent.position.wrappedDistanceTo(playerPosition, worldSize, worldSize)
                if (dist > 1300f) continue

                vent.bubbleTimer += dt
                if (vent.bubbleTimer >= 0.15f) {
                    vent.bubbleTimer = 0f
                    val b = obtainVentBubble()
                    if (b != null) {
                        val angle = -PI.toFloat() / 2f + (Random.nextFloat() * 0.5f - 0.25f)
                        val speed = Random.nextFloat() * 80f + 60f
                        b.position = vent.position + Vector2(Random.nextFloat() * 16f - 8f, -vent.chimneyHeight * 0.8f)
                        b.velocity = Vector2.fromAngle(angle, speed)
                        b.radius = Random.nextFloat() * 3f + 1.5f
                        b.alpha = Random.nextFloat() * 0.4f + 0.5f
                        b.life = 0f
                        b.maxLife = Random.nextFloat() * 1.5f + 1.2f
                        b.color = vent.glowColor.copy(alpha = 0.85f)
                        b.isActive = true
                    }
                }
            }
        }

        // 3. Update vent bubbles with zero iterator allocation
        for (i in 0 until MAX_VENT_BUBBLES) {
            val b = ventBubbles[i]
            if (!b.isActive) continue
            b.life += dt
            b.position = b.position + (b.velocity * dt)
            b.velocity = b.velocity * 0.98f + Vector2(0f, -8f * dt) // buoyancy
            b.alpha = ((1f - b.life / b.maxLife) * 0.7f).coerceIn(0f, 1f)
            if (b.life >= b.maxLife) {
                b.isActive = false
            }
        }
    }

    private fun obtainVentBubble(): VentBubble? {
        for (i in 0 until MAX_VENT_BUBBLES) {
            if (!ventBubbles[i].isActive) return ventBubbles[i]
        }
        return null
    }

    fun wrapVector(v: Vector2): Vector2 {
        var x = v.x % worldSize
        if (x < 0f) x += worldSize
        var y = v.y % worldSize
        if (y < 0f) y += worldSize
        return Vector2(x, y)
    }
}
