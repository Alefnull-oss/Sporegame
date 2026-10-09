package com.example.spore.game.engine

import androidx.compose.ui.graphics.Color
import com.example.spore.data.model.CellEvolutionEntity
import com.example.spore.data.model.PlanetDefinition
import com.example.spore.game.audio.SporeAudioEngine
import com.example.spore.game.physics.ElasticAppendageChain
import com.example.spore.game.physics.SoftBodyMembrane
import com.example.spore.game.physics.SporePhysicsEngine
import com.example.spore.game.terrain.OceanTerrainSystem
import com.example.spore.game.water.WaterRippleSystem
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

class GameSimulation(
    initialEvolution: CellEvolutionEntity,
    var planetDefinition: PlanetDefinition = PlanetDefinition.PLANETS[0],
    val onDnaCollected: (Int) -> Unit = {},
    val onSpeciesDiscovered: (String) -> Unit = {},
    val onSpeciesEaten: (String) -> Unit = {},
    val onPlayerKilled: (String) -> Unit = {},
    val onHapticImpact: () -> Unit = {},
    val onPartUnlocked: (String, String) -> Unit = { _, _ -> },
    val onMatingDanceComplete: () -> Unit = {}
) {
    companion object {
        const val WORLD_WIDTH = 7200f
        const val WORLD_HEIGHT = 7200f

        const val MAX_FOOD = 75
        const val MAX_DNA_STRANDS = 5
        const val MAX_AI = 22
        const val MAX_METEORS = 4
        const val DRAG_COEFFICIENT = 0.94f
    }

    private val idGen = AtomicLong(1000)

    var evolutionEntity: CellEvolutionEntity = initialEvolution
        private set
    var playerStats: CellStats = CellEvolutionConfig.calculateStats(initialEvolution)
        private set

    val unlockedPartsSet: MutableSet<String> = CellEvolutionConfig.parseUnlockedParts(initialEvolution.unlockedParts).toMutableSet()

    val player: PlayerCell = PlayerCell(
        position = Vector2(WORLD_WIDTH / 2f, WORLD_HEIGHT / 2f),
        stats = playerStats,
        biomass = initialEvolution.biomass,
        dnaPoints = initialEvolution.dnaPoints,
        health = playerStats.maxHealth,
        maxHealth = playerStats.maxHealth
    )

    // Oceanic Systems
    val rippleSystem = WaterRippleSystem(WORLD_WIDTH, WORLD_HEIGHT)
    val oceanTerrain = OceanTerrainSystem(WORLD_WIDTH, planetDefinition)
    val physicsEngine = SporePhysicsEngine()
    private var playerSwimRippleTimer = 0f
    private var photosynthesisTimer = 0f

    val foods = mutableListOf<FoodParticle>()
    val microbes = mutableListOf<AiMicrobe>()
    val poisonPuddles = mutableListOf<PoisonPuddle>()
    val electricBlasts = mutableListOf<ElectricBlast>()
    val notices = mutableListOf<FloatingNotice>()
    val ambientParticles = mutableListOf<AmbientParticle>()
    val abyssalCreatures = mutableListOf<AbyssalSilhouette>()
    val foregroundFloaters = mutableListOf<ForegroundFloater>()

    // Spore-like World Entities
    val meteorShards = mutableListOf<MeteorShard>()
    val partCapsules = mutableListOf<PartCapsule>()
    val acousticWaves = mutableListOf<AcousticWave>()
    val heartBubbles = mutableListOf<HeartBubble>()

    var activeMate: SymbioticMate? = null
    var isMatingDanceActive: Boolean = false
    var matingDanceTimer: Float = 0f
    var isMatingCallActive: Boolean = false
    var newlyDiscoveredPartEvent: String? = null

    // Anti-lag spatial partition grids
    private val foodGrid = SpatialGrid<FoodParticle>(WORLD_WIDTH, WORLD_HEIGHT, cellSize = 450f)
    private val microbeGrid = SpatialGrid<AiMicrobe>(WORLD_WIDTH, WORLD_HEIGHT, cellSize = 450f)
    private var isFoodGridDirty = true
    private val discoveredSpecies = HashSet<String>()

    var isGameOver = false
        private set
    var killerName: String = ""
        private set
    var gameTimeSeconds: Float = 0f
        private set
    var microbesEatenCount: Int = 0
        private set
    var highestTierDefeated: Int = 0
        private set
    var readyToEvolveNoticeShown: Boolean = false

    private var tickFrameCounter: Int = 0

    // ---------------------------------------------------------------------
    // Zero-allocation scratch vectors for hot per-frame paths.
    // The simulation runs on a single thread, so reusing these is safe and
    // eliminates the ~300-600 Vector2 allocations per frame that triggered
    // GC pauses (stutter) on low-RAM devices.
    // ---------------------------------------------------------------------
    private val scratchA = Vector2()
    private val scratchB = Vector2()
    private val scratchRoot = Vector2()
    private val scratchFlow = Vector2()

    init {
        seedAmbientParticles()
        seedAbyssalCreatures()
        seedForegroundFloaters()
        spawnInitialEcosystem()
    }

    private fun seedAbyssalCreatures() {
        abyssalCreatures.clear()
        val abyssColor = Color(planetDefinition.oceanBgColor1)
        val deepCreatureCount = 7
        for (i in 0 until deepCreatureCount) {
            val startAngle = Random.nextFloat() * 2f * PI.toFloat()
            val speed = Random.nextFloat() * 25f + 20f
            abyssalCreatures.add(
                AbyssalSilhouette(
                    id = idGen.incrementAndGet(),
                    position = Vector2(Random.nextFloat() * WORLD_WIDTH, Random.nextFloat() * WORLD_HEIGHT),
                    velocity = Vector2.fromAngle(startAngle, speed),
                    angle = startAngle,
                    length = Random.nextFloat() * 240f + 180f,
                    width = Random.nextFloat() * 70f + 55f,
                    segmentCount = Random.nextInt(4, 7),
                    tailWobbleSpeed = Random.nextFloat() * 0.8f + 0.9f,
                    tailPhaseOffset = Random.nextFloat() * 10f,
                    silhouetteColor = abyssColor,
                    alpha = Random.nextFloat() * 0.12f + 0.16f,
                    parallaxFactor = 0.42f
                )
            )
        }
    }

    private fun seedForegroundFloaters() {
        foregroundFloaters.clear()
        val rimColor = Color(planetDefinition.oceanRimColor)
        val floaterCount = 42
        for (i in 0 until floaterCount) {
            foregroundFloaters.add(
                ForegroundFloater(
                    id = idGen.incrementAndGet(),
                    position = Vector2(Random.nextFloat() * WORLD_WIDTH, Random.nextFloat() * WORLD_HEIGHT),
                    velocity = Vector2(Random.nextFloat() * 12f - 6f, Random.nextFloat() * 12f - 6f),
                    radius = Random.nextFloat() * 14f + 5f,
                    alpha = Random.nextFloat() * 0.28f + 0.12f,
                    color = if (Random.nextBoolean()) rimColor else Color.White,
                    wobblePhase = Random.nextFloat() * 10f,
                    floaterKind = Random.nextInt(3)
                )
            )
        }
    }

    private fun seedAmbientParticles() {
        ambientParticles.clear()
        val pColor = Color(planetDefinition.ambientParticleColor)
        val pColorSecondary = Color(planetDefinition.oceanRimColor)
        for (i in 0 until 50) {
            ambientParticles.add(
                AmbientParticle(
                    position = Vector2(Random.nextFloat() * WORLD_WIDTH, Random.nextFloat() * WORLD_HEIGHT),
                    velocity = Vector2(Random.nextFloat() * 8f - 4f, Random.nextFloat() * 8f - 4f),
                    radius = Random.nextFloat() * 3f + 1f,
                    alpha = Random.nextFloat() * 0.35f + 0.1f,
                    color = if (Random.nextBoolean()) pColor else pColorSecondary
                )
            )
        }
    }

    fun setPlanet(newPlanet: PlanetDefinition) {
        planetDefinition = newPlanet
        oceanTerrain.updatePlanet(newPlanet)
        rippleSystem.clear()
        seedAmbientParticles()
        seedAbyssalCreatures()
        seedForegroundFloaters()
        restart()
    }

    fun updateEvolutionConfig(newEntity: CellEvolutionEntity) {
        evolutionEntity = newEntity
        unlockedPartsSet.clear()
        unlockedPartsSet.addAll(CellEvolutionConfig.parseUnlockedParts(newEntity.unlockedParts))
        playerStats = CellEvolutionConfig.calculateStats(newEntity)
        player.stats = playerStats
        player.maxHealth = playerStats.maxHealth
        player.health = min(player.health, player.maxHealth)
        player.dnaPoints = newEntity.dnaPoints
    }

    private fun spawnInitialEcosystem() {
        while (foods.size < 45) {
            spawnScatteredFood()
        }
        while (microbes.size < 14) {
            spawnAiMicrobe()
        }
        while (meteorShards.size < MAX_METEORS) {
            spawnMeteorShard()
        }
    }

    private fun spawnScatteredFood() {
        val isBloom = Random.nextFloat() < 0.65f
        var pos = Vector2(Random.nextFloat() * WORLD_WIDTH, Random.nextFloat() * WORLD_HEIGHT)
        if (isBloom) {
            for (attempt in 0 until 6) {
                val candidate = Vector2(Random.nextFloat() * WORLD_WIDTH, Random.nextFloat() * WORLD_HEIGHT)
                if (oceanTerrain.isNutrientBloomZone(candidate)) {
                    pos = candidate
                    break
                }
            }
        }

        val dnaCount = foods.count { it.kind == FoodKind.DNA_STRAND }
        val kind = when {
            dnaCount < MAX_DNA_STRANDS && Random.nextInt(100) < 8 -> FoodKind.DNA_STRAND
            Random.nextInt(100) < 25 -> FoodKind.MEAT_CHUNK
            else -> FoodKind.ALGAE
        }

        val food = when (kind) {
            FoodKind.ALGAE -> FoodParticle(
                id = idGen.incrementAndGet(),
                position = pos,
                kind = kind,
                radius = 7.5f,
                valueDna = 3,
                valueBiomass = 1.0f,
                color = Color(0xFF00E676)
            )
            FoodKind.MEAT_CHUNK -> FoodParticle(
                id = idGen.incrementAndGet(),
                position = pos,
                kind = kind,
                radius = 9.5f,
                valueDna = 7,
                valueBiomass = 2.5f,
                color = Color(0xFFFF5252)
            )
            FoodKind.DNA_STRAND -> FoodParticle(
                id = idGen.incrementAndGet(),
                position = pos,
                kind = kind,
                radius = 12f,
                valueDna = 18,
                valueBiomass = 4.0f,
                color = Color(0xFFFFD600)
            )
        }
        foods.add(food)
        isFoodGridDirty = true
    }

    private fun spawnMeteorShard() {
        val lockedCandidates = CellEvolutionConfig.ALL_PARTS.filter { !unlockedPartsSet.contains(it.id) }
        val targetPart = if (lockedCandidates.isNotEmpty()) {
            lockedCandidates.random()
        } else {
            CellEvolutionConfig.ALL_PARTS.random()
        }

        val angle = Random.nextFloat() * 2f * PI.toFloat()
        val dist = Random.nextFloat() * 1400f + 700f
        val pos = Vector2(
            wrapCoord(player.position.x + cos(angle) * dist, WORLD_WIDTH),
            wrapCoord(player.position.y + sin(angle) * dist, WORLD_HEIGHT)
        )

        meteorShards.add(
            MeteorShard(
                id = idGen.incrementAndGet(),
                position = pos,
                velocity = Vector2(Random.nextFloat() * 16f - 8f, Random.nextFloat() * 16f - 8f),
                health = 60f,
                maxHealth = 60f,
                radius = 32f,
                containedPartId = targetPart.id,
                partName = targetPart.name,
                rotation = Random.nextFloat() * 6.28f,
                crustedColor = when (planetDefinition.id) {
                    "planet_rubrum" -> Color(0xFF5D2418)
                    "planet_toxis" -> Color(0xFF2E4B18)
                    "planet_ametistia" -> Color(0xFF38184C)
                    else -> Color(0xFF4E342E)
                },
                coreColor = Color(0xFFFFD54F),
                wobblePhase = Random.nextFloat() * 10f
            )
        )
    }

    private fun spawnAiMicrobe() {
        val roll = Random.nextInt(100)
        val template = when {
            roll < 45 -> {
                if (Random.nextBoolean()) {
                    AiMicrobe(
                        id = idGen.incrementAndGet(),
                        speciesId = "herbivore_ciliado",
                        name = "Ciliado Pacifista",
                        position = randomFarPosition(),
                        radius = 24f,
                        health = 45f,
                        maxHealth = 45f,
                        trophicTier = TrophicTier.PRIMARY_CONSUMER,
                        diet = DietType.HERBIVORE,
                        primaryColor = Color(0xFF76FF03),
                        ciliaCount = 3,
                        flagellaCount = 0,
                        spikesCount = 0,
                        targetSpeed = 135f
                    )
                } else {
                    AiMicrobe(
                        id = idGen.incrementAndGet(),
                        speciesId = "herbivore_rotifer",
                        name = "Rotífero Veloz",
                        position = randomFarPosition(),
                        radius = 27f,
                        health = 55f,
                        maxHealth = 55f,
                        trophicTier = TrophicTier.PRIMARY_CONSUMER,
                        diet = DietType.HERBIVORE,
                        primaryColor = Color(0xFF69F0AE),
                        ciliaCount = 2,
                        flagellaCount = 1,
                        spikesCount = 0,
                        targetSpeed = 175f
                    )
                }
            }
            roll < 75 -> {
                if (Random.nextBoolean()) {
                    AiMicrobe(
                        id = idGen.incrementAndGet(),
                        speciesId = "omnivore_amoeba",
                        name = "Ameba Oportunista",
                        position = randomFarPosition(),
                        radius = 34f,
                        health = 80f,
                        maxHealth = 80f,
                        trophicTier = TrophicTier.SECONDARY_CONSUMER,
                        diet = DietType.OMNIVORE,
                        primaryColor = Color(0xFFFFD600),
                        ciliaCount = 1,
                        flagellaCount = 1,
                        spikesCount = 0,
                        targetSpeed = 125f
                    )
                } else {
                    AiMicrobe(
                        id = idGen.incrementAndGet(),
                        speciesId = "omnivore_versatil",
                        name = "Versátilus Flagelado",
                        position = randomFarPosition(),
                        radius = 36f,
                        health = 90f,
                        maxHealth = 90f,
                        trophicTier = TrophicTier.SECONDARY_CONSUMER,
                        diet = DietType.OMNIVORE,
                        primaryColor = Color(0xFFFFAB00),
                        ciliaCount = 1,
                        flagellaCount = 2,
                        spikesCount = 1,
                        armorPlates = 1,
                        eyeType = "BASIC",
                        targetSpeed = 155f
                    )
                }
            }
            roll < 93 -> {
                if (Random.nextBoolean()) {
                    AiMicrobe(
                        id = idGen.incrementAndGet(),
                        speciesId = "predator_didinium",
                        name = "Didinium Cazador",
                        position = randomFarPosition(),
                        radius = 43f,
                        health = 135f,
                        maxHealth = 135f,
                        trophicTier = TrophicTier.PREDATOR,
                        diet = DietType.CARNIVORE,
                        primaryColor = Color(0xFFFF3D00),
                        ciliaCount = 1,
                        flagellaCount = 2,
                        spikesCount = 1,
                        hasJet = true,
                        armorPlates = 1,
                        eyeType = "COMPOUND",
                        targetSpeed = 185f
                    )
                } else {
                    AiMicrobe(
                        id = idGen.incrementAndGet(),
                        speciesId = "predator_spiketooth",
                        name = "Mordaza Puntiaguda",
                        position = randomFarPosition(),
                        radius = 48f,
                        health = 160f,
                        maxHealth = 160f,
                        trophicTier = TrophicTier.PREDATOR,
                        diet = DietType.CARNIVORE,
                        primaryColor = Color(0xFFDD2C00),
                        ciliaCount = 1,
                        flagellaCount = 1,
                        spikesCount = 3,
                        armorPlates = 2,
                        eyeType = "COMPOUND",
                        targetSpeed = 145f
                    )
                }
            }
            else -> {
                AiMicrobe(
                    id = idGen.incrementAndGet(),
                    speciesId = "apex_megacolossus",
                    name = "Leviatán Abisal",
                    position = randomFarPosition(),
                    radius = 78f,
                    health = 380f,
                    maxHealth = 380f,
                    trophicTier = TrophicTier.APEX,
                    diet = DietType.CARNIVORE,
                    primaryColor = Color(0xFFD500F9),
                    ciliaCount = 3,
                    flagellaCount = 3,
                    spikesCount = 4,
                    hasPoison = true,
                    armorPlates = 3,
                    eyeType = "COMPOUND",
                    targetSpeed = 110f
                )
            }
        }
        microbes.add(template)
    }

    private fun randomFarPosition(): Vector2 {
        var pos: Vector2
        var attempts = 0
        do {
            pos = Vector2(Random.nextFloat() * WORLD_WIDTH, Random.nextFloat() * WORLD_HEIGHT)
            attempts++
        } while (attempts < 10 && pos.wrappedDistanceTo(player.position, WORLD_WIDTH, WORLD_HEIGHT) < 600f)
        return pos
    }

    /**
     * Main simulation update loop with anti-lag delta clamping and spatial partitioning.
     */
    fun update(rawDeltaTime: Float, inputDirection: Vector2) {
        if (isGameOver) return

        val deltaTime = rawDeltaTime.coerceIn(0.005f, 0.033f)
        gameTimeSeconds += deltaTime
        tickFrameCounter++

        oceanTerrain.update(deltaTime, player.position)
        rippleSystem.update(deltaTime)

        // Player timers
        if (player.dashCooldownTimer > 0f) player.dashCooldownTimer -= deltaTime
        if (player.poisonCooldownTimer > 0f) player.poisonCooldownTimer -= deltaTime
        if (player.electricCooldownTimer > 0f) player.electricCooldownTimer -= deltaTime
        if (player.invulnerableTimer > 0f) player.invulnerableTimer -= deltaTime
        if (player.damageFlashTimer > 0f) player.damageFlashTimer -= deltaTime

        if (player.isDashing) {
            player.dashDurationTimer -= deltaTime
            if (player.dashDurationTimer <= 0f) {
                player.isDashing = false
            }
        }

        player.wobbleTimer += deltaTime * 3.5f

        // Courtship dance or player regular movement
        if (isMatingDanceActive) {
            updateCourtshipDance(deltaTime)
        } else {
            updatePlayerMovement(deltaTime, inputDirection)
        }

        // Swimming ripples
        val playerRadius = calculatePlayerRadius()
        val playerSpeed = player.velocity.length()
        playerSwimRippleTimer += deltaTime
        val rippleInterval = if (player.isDashing) 0.05f else if (playerSpeed > 110f) 0.09f else 0.16f
        if (playerSpeed > 16f && playerSwimRippleTimer >= rippleInterval) {
            playerSwimRippleTimer = 0f
            val rippleColor = Color(planetDefinition.oceanRimColor)
            rippleSystem.emitSwimRipple(
                position = player.position,
                velocity = player.velocity,
                cellRadius = playerRadius,
                intensity = if (player.isDashing) 1.6f else 1.0f,
                color = rippleColor
            )
        }

        // Epigenetic Chimeric Trait active effects:
        // 1. Photosynthesis: Passive DNA generation in phytoplankton bloom zones
        if (playerStats.hasPhotosynthesis) {
            photosynthesisTimer += deltaTime
            if (photosynthesisTimer >= 3.5f) {
                photosynthesisTimer = 0f
                if (oceanTerrain.isNutrientBloomZone(player.position)) {
                    player.dnaPoints += 1
                    onDnaCollected(1)
                    addNotice(player.position, "+1 ADN (Fotosíntesis)", Color(0xFF76FF03))
                }
            }
        }

        // 2. Food Magnet vortex ciliary effect (in-place pull, zero allocation)
        if (playerStats.hasFoodMagnet) {
            foodGrid.forEachNeighbor(player.position.x, player.position.y) { food ->
                val d = player.position.wrappedDistanceTo(food.position, WORLD_WIDTH, WORLD_HEIGHT)
                if (d < 190f && d > 12f) {
                    player.position.wrappedDeltaInto(food.position, WORLD_WIDTH, WORLD_HEIGHT, scratchA)
                        .normalizeInPlace()
                    food.position.addScaledInPlace(scratchA, deltaTime * 190f)
                    isFoodGridDirty = true
                }
            }
        }

        // Rebuild anti-lag spatial grids
        rebuildSpatialGrids()

        // Spawning cycle
        if (foods.size < MAX_FOOD && Random.nextFloat() < 0.20f) {
            spawnScatteredFood()
        }
        if (microbes.size < MAX_AI && Random.nextFloat() < 0.05f) {
            spawnAiMicrobe()
        }
        if (meteorShards.size < MAX_METEORS && Random.nextFloat() < 0.03f) {
            spawnMeteorShard()
        }

        // Hazards & AI
        updateHazards(deltaTime)
        updateMicrobes(deltaTime)

        // Physics step & appendages
        physicsEngine.stepSimulation(deltaTime)
        updateAppendages(deltaTime)

        // Collisions
        handlePlayerFoodCollisions()
        handlePlayerMicrobeCollisions()
        handleMicrobeMicrobeCollisions()
        handlePlayerMeteorCollisions()
        handlePlayerCapsuleCollisions()

        // Spore World Updates
        updateMeteorShards(deltaTime)
        updatePartCapsules(deltaTime)
        updateAcousticWaves(deltaTime)
        updateSymbioticMate(deltaTime)
        updateHeartBubbles(deltaTime)

        // Notices & particles
        updateNotices(deltaTime)
        updateAmbient(deltaTime)
        updateAbyssalCreatures(deltaTime)
        updateForegroundFloaters(deltaTime)

        // Evolution readiness notification
        if (!readyToEvolveNoticeShown && player.dnaPoints >= 30) {
            readyToEvolveNoticeShown = true
            addNotice(player.position, "¡Canto de Cortejo Disponible! Toca ❤️", Color(0xFFFF4081))
        }
    }

    private fun rebuildSpatialGrids() {
        if (isFoodGridDirty) {
            foodGrid.clear()
            for (i in 0 until foods.size) {
                val f = foods[i]
                foodGrid.insert(f.position.x, f.position.y, f)
            }
            isFoodGridDirty = false
        }

        microbeGrid.clear()
        for (i in 0 until microbes.size) {
            val m = microbes[i]
            microbeGrid.insert(m.position.x, m.position.y, m)
        }
    }

    private fun updatePlayerMovement(deltaTime: Float, inputDirection: Vector2) {
        // Zero-allocation movement integration (identical math to the previous
        // immutable-operator version, minus the intermediate Vector2 objects).
        val hasIntent = inputDirection.lengthSquared() > 0.04f

        if (hasIntent) {
            scratchA.setFrom(inputDirection).normalizeInPlace() // moveIntent
            val targetAngle = scratchA.angle()
            val diff = angleDifference(targetAngle, player.angle)
            player.angle += diff * min(1f, playerStats.turnRate * deltaTime)

            val targetBank = (diff.coerceIn(-1.5f, 1.5f) * 0.42f)
            player.bankRoll += (targetBank - player.bankRoll) * min(1f, deltaTime * 8f)

            val speedMult = if (player.isDashing) playerStats.dashSpeedMultiplier else 1f
            val thrustMag = playerStats.baseSpeed * speedMult * deltaTime * 12f
            player.velocity.x += cos(player.angle) * thrustMag
            player.velocity.y += sin(player.angle) * thrustMag
        } else {
            player.bankRoll += (0f - player.bankRoll) * min(1f, deltaTime * 5f)
        }

        player.velocity.scaleInPlace(DRAG_COEFFICIENT)
        player.position.addScaledInPlace(player.velocity, deltaTime)

        val speedMagnitude = player.velocity.length()
        val targetElevation = if (player.isDashing) 1.6f else (speedMagnitude / 180f).coerceIn(0f, 1.0f)
        player.elevationZ += (targetElevation - player.elevationZ) * min(1f, deltaTime * 7f)
        player.pitchAngle = (speedMagnitude / 300f).coerceIn(0f, 0.35f)

        oceanTerrain.getCurrentVelocityInto(player.position, scratchFlow)
        player.position.addScaledInPlace(scratchFlow, deltaTime * 0.25f)

        player.position.wrapInPlace(WORLD_WIDTH, WORLD_HEIGHT)
    }

    private fun updateCourtshipDance(deltaTime: Float) {
        val mate = activeMate ?: return
        matingDanceTimer += deltaTime

        val midX = (player.position.x + mate.position.x) / 2f
        val midY = (player.position.y + mate.position.y) / 2f
        val orbitRadius = (calculatePlayerRadius() + mate.radius) * 0.65f
        val orbitAngle = matingDanceTimer * 4.2f

        // Zero-allocation orbital positions (same formulas as before)
        player.position.set(
            wrapCoord(midX + cos(orbitAngle) * orbitRadius, WORLD_WIDTH),
            wrapCoord(midY + sin(orbitAngle) * orbitRadius, WORLD_HEIGHT)
        )
        mate.position.set(
            wrapCoord(midX - cos(orbitAngle) * orbitRadius, WORLD_WIDTH),
            wrapCoord(midY - sin(orbitAngle) * orbitRadius, WORLD_HEIGHT)
        )
        player.angle = orbitAngle + PI.toFloat() * 0.5f
        mate.angle = orbitAngle - PI.toFloat() * 0.5f

        // Emit love heart particles
        if (Random.nextFloat() < 0.38f) {
            heartBubbles.add(
                HeartBubble(
                    id = idGen.incrementAndGet(),
                    position = Vector2(midX + Random.nextFloat() * 40f - 20f, midY + Random.nextFloat() * 40f - 20f),
                    velocity = Vector2(Random.nextFloat() * 30f - 15f, -Random.nextFloat() * 50f - 25f),
                    scale = Random.nextFloat() * 0.5f + 0.8f,
                    alpha = 1f,
                    lifeSeconds = 1.3f
                )
            )
        }

        if (matingDanceTimer >= 1.5f) {
            mate.state = MateState.MATING_COMPLETED
            isMatingDanceActive = false
            activeMate = null
            isMatingCallActive = false
            addNotice(player.position, "¡Cortejo Exitoso! Huevo Primordial 🧬", Color(0xFFFFD600))
            onHapticImpact()
            onMatingDanceComplete()
        }
    }

    private fun updateHazards(deltaTime: Float) {
        val poisonIter = poisonPuddles.iterator()
        while (poisonIter.hasNext()) {
            val p = poisonIter.next()
            p.remainingSeconds -= deltaTime
            if (p.currentRadius < p.maxRadius) {
                p.currentRadius += deltaTime * 20f
            }
            if (!p.fromPlayer && p.position.wrappedDistanceTo(player.position, WORLD_WIDTH, WORLD_HEIGHT) < p.currentRadius + calculatePlayerRadius()) {
                damagePlayer(p.damagePerSecond * deltaTime, "Toxina Abisal")
            }
            microbeGrid.forEachNeighbor(p.position.x, p.position.y) { m ->
                if (p.fromPlayer && p.position.wrappedDistanceTo(m.position, WORLD_WIDTH, WORLD_HEIGHT) < p.currentRadius + m.radius) {
                    m.health -= p.damagePerSecond * deltaTime * 1.5f
                    // Chimeric perk: Paralyzing poison slows enemies by 50%
                    if (playerStats.hasParalyzingPoison) {
                        m.velocity.scaleInPlace(0.5f)
                    }
                }
            }
            if (p.remainingSeconds <= 0f) poisonIter.remove()
        }

        val elecIter = electricBlasts.iterator()
        while (elecIter.hasNext()) {
            val eb = elecIter.next()
            eb.remainingSeconds -= deltaTime
            eb.currentRadius += deltaTime * 350f

            if (eb.fromPlayer) {
                microbeGrid.forEachNeighbor(eb.position.x, eb.position.y) { m ->
                    if (eb.position.wrappedDistanceTo(m.position, WORLD_WIDTH, WORLD_HEIGHT) < eb.currentRadius + m.radius) {
                        m.health -= eb.damage * deltaTime * 3f
                        eb.position.wrappedDeltaInto(m.position, WORLD_WIDTH, WORLD_HEIGHT, scratchA).normalizeInPlace()
                        m.velocity.addScaledInPlace(scratchA, 400f)
                    }
                }
            }
            if (eb.remainingSeconds <= 0f) elecIter.remove()
        }
    }

    private fun updateMicrobes(deltaTime: Float) {
        val playerRadius = calculatePlayerRadius()
        val microbeIter = microbes.iterator()

        while (microbeIter.hasNext()) {
            val m = microbeIter.next()
            m.wobbleTime += deltaTime * 2.5f
            m.stateTimer += deltaTime
            if (m.poisonCooldown > 0f) m.poisonCooldown -= deltaTime
            if (m.biteCooldown > 0f) m.biteCooldown -= deltaTime

            if (m.health <= 0f) {
                onSpeciesEaten(m.speciesId)
                microbesEatenCount++
                highestTierDefeated = max(highestTierDefeated, m.trophicTier.level)
                burstDeadMicrobe(m)
                microbeIter.remove()
                continue
            }

            val distToPlayer = m.position.wrappedDistanceTo(player.position, WORLD_WIDTH, WORLD_HEIGHT)
            val isFar = distToPlayer > 1400f
            if (isFar && (tickFrameCounter % 2 != 0)) {
                // Cheap far-field integration (in-place, zero allocation)
                m.position.addScaledInPlace(m.velocity, deltaTime)
                m.position.wrapInPlace(WORLD_WIDTH, WORLD_HEIGHT)
                continue
            }

            val canEatPlayer = TrophicWebRules.canPredatorAttack(m.diet, m.trophicTier, m.radius, playerStats.trophicTier, playerRadius)
            val playerCanEatMe = TrophicWebRules.canPredatorAttack(playerStats.dietType, playerStats.trophicTier, playerRadius, m.trophicTier, m.radius)

            if (playerCanEatMe && distToPlayer < 400f) {
                m.state = AiState.FLEEING
                // scratchA = (m - player): direction AWAY from the player
                player.position.wrappedDeltaInto(m.position, WORLD_WIDTH, WORLD_HEIGHT, scratchA)
                m.targetAngle = kotlin.math.atan2(scratchA.y, scratchA.x)
            } else if (canEatPlayer && distToPlayer < 480f) {
                m.state = AiState.HUNTING
                // scratchA = (m - player): hunting aims at the player, i.e. -(scratchA)
                player.position.wrappedDeltaInto(m.position, WORLD_WIDTH, WORLD_HEIGHT, scratchA)
                m.targetAngle = kotlin.math.atan2(-scratchA.y, -scratchA.x)
                if (m.hasPoison && m.poisonCooldown <= 0f && distToPlayer < 130f) {
                    m.poisonCooldown = 6f
                    poisonPuddles.add(PoisonPuddle(id = idGen.incrementAndGet(), position = m.position, fromPlayer = false))
                }
            } else if (m.diet == DietType.HERBIVORE) {
                m.state = AiState.GRAZING
                if (m.stateTimer > 1.5f) {
                    var nearestFood: FoodParticle? = null
                    var minDist = 400f
                    foodGrid.forEachNeighbor(m.position.x, m.position.y) { food ->
                        if (food.kind == FoodKind.ALGAE) {
                            val d = m.position.wrappedDistanceTo(food.position, WORLD_WIDTH, WORLD_HEIGHT)
                            if (d < minDist) {
                                minDist = d
                                nearestFood = food
                            }
                        }
                    }
                    if (nearestFood != null) {
                        m.position.wrappedDeltaInto(nearestFood!!.position, WORLD_WIDTH, WORLD_HEIGHT, scratchA)
                        m.targetAngle = kotlin.math.atan2(scratchA.y, scratchA.x)
                    } else if (Random.nextFloat() < 0.25f) {
                        m.targetAngle += (Random.nextFloat() * 1.5f - 0.75f)
                    }
                    m.stateTimer = 0f
                }
            } else {
                if (m.stateTimer > 2.0f) {
                    var huntTarget: AiMicrobe? = null
                    var minDist = 450f
                    microbeGrid.forEachNeighbor(m.position.x, m.position.y) { other ->
                        if (other.id != m.id && TrophicWebRules.canPredatorAttack(m.diet, m.trophicTier, m.radius, other.trophicTier, other.radius)) {
                            val d = m.position.wrappedDistanceTo(other.position, WORLD_WIDTH, WORLD_HEIGHT)
                            if (d < minDist) {
                                minDist = d
                                huntTarget = other
                            }
                        }
                    }
                    if (huntTarget != null) {
                        m.state = AiState.HUNTING
                        m.position.wrappedDeltaInto(huntTarget!!.position, WORLD_WIDTH, WORLD_HEIGHT, scratchA)
                        m.targetAngle = kotlin.math.atan2(scratchA.y, scratchA.x)
                    } else {
                        m.state = AiState.ROAMING
                        m.targetAngle += (Random.nextFloat() * 2f - 1f)
                    }
                    m.stateTimer = 0f
                }
            }

            val diff = angleDifference(m.targetAngle, m.angle)
            val turnSpeed = 2.0f + (m.ciliaCount * 1.2f)
            m.angle += diff * min(1f, turnSpeed * deltaTime)

            val targetBank = (diff.coerceIn(-1.5f, 1.5f) * 0.35f)
            m.bankRoll += (targetBank - m.bankRoll) * min(1f, deltaTime * 6f)
            val mSpeed = m.velocity.length()
            val targetElev = (mSpeed / 160f).coerceIn(0f, 1.0f)
            m.elevationZ += (targetElev - m.elevationZ) * min(1f, deltaTime * 5f)
            m.pitchAngle = (mSpeed / 250f).coerceIn(0f, 0.3f)

            val currentSpeed = when (m.state) {
                AiState.FLEEING -> m.targetSpeed * 1.35f
                AiState.HUNTING -> m.targetSpeed * 1.2f
                else -> m.targetSpeed * 0.85f
            }
            // In-place thrust + drag integration (identical formulas)
            val thrustMag = currentSpeed * deltaTime * 10f
            m.velocity.x = (m.velocity.x + cos(m.angle) * thrustMag) * DRAG_COEFFICIENT
            m.velocity.y = (m.velocity.y + sin(m.angle) * thrustMag) * DRAG_COEFFICIENT
            m.position.addScaledInPlace(m.velocity, deltaTime)
            m.position.wrapInPlace(WORLD_WIDTH, WORLD_HEIGHT)

            if (distToPlayer < 850f && m.velocity.length() > 30f && (tickFrameCounter % 7 == 0)) {
                rippleSystem.emitSwimRipple(
                    position = m.position,
                    velocity = m.velocity,
                    cellRadius = m.radius,
                    intensity = 0.7f,
                    color = m.primaryColor
                )
            }
        }
    }

    private fun updateAppendages(deltaTime: Float) {
        val playerRadius = calculatePlayerRadius()
        val pFlagellaCount = evolutionEntity.flagellaCount.coerceIn(1, 4)
        if (player.flagellaChains.size != pFlagellaCount) {
            val list = mutableListOf<ElasticAppendageChain>()
            for (i in 0 until pFlagellaCount) {
                list.add(ElasticAppendageChain(segmentCount = 5, totalLength = playerRadius * (if (player.isDashing) 2.4f else 1.8f), baseRadius = (playerRadius * 0.16f).coerceAtLeast(3.5f)))
            }
            player.flagellaChains = list
        }

        val pOffsets = when (pFlagellaCount) {
            1 -> floatArrayOf(0f)
            2 -> floatArrayOf(-0.35f, 0.35f)
            3 -> floatArrayOf(-0.55f, 0f, 0.55f)
            else -> floatArrayOf(-0.7f, -0.25f, 0.25f, 0.7f)
        }

        val cosA = cos(player.angle)
        val sinA = sin(player.angle)
        for (i in 0 until player.flagellaChains.size) {
            val chain = player.flagellaChains[i]
            val yOffset = pOffsets[i]
            val localX = -playerRadius * 0.88f
            val localY = playerRadius * yOffset
            // Reused scratch root position: consumed synchronously by chain.update()
            scratchRoot.set(
                player.position.x + cosA * localX - sinA * localY,
                player.position.y + sinA * localX + cosA * localY
            )
            val wavePulse = sin(gameTimeSeconds * (if (player.isDashing) 16f else 8.5f) + i * 1.4f) * (if (player.isDashing) 0.5f else 0.3f)
            chain.update(scratchRoot, player.angle, player.velocity, deltaTime, wavePulse)
        }

        player.softBody.update(deltaTime)
        if (player.jawAperture < 1.0f) player.jawAperture += deltaTime * 3.5f
        if (player.mouthAnimationTimer <= 0f) player.isBiting = false

        for (m in microbes) {
            m.softBody.update(deltaTime)
            if (m.jawAperture < 1.0f) m.jawAperture += deltaTime * 3.5f
            if (m.biteCooldown < 0.4f) m.isBiting = false

            val count = m.flagellaCount
            if (count > 0) {
                if (m.flagellaChains.size != count) {
                    val list = mutableListOf<ElasticAppendageChain>()
                    for (i in 0 until count) {
                        list.add(ElasticAppendageChain(segmentCount = 5, totalLength = m.radius * 1.7f, baseRadius = (m.radius * 0.15f).coerceAtLeast(3f)))
                    }
                    m.flagellaChains = list
                }
                val mOffsets = when (count) {
                    1 -> floatArrayOf(0f)
                    2 -> floatArrayOf(-0.35f, 0.35f)
                    3 -> floatArrayOf(-0.55f, 0f, 0.55f)
                    else -> floatArrayOf(-0.7f, -0.25f, 0.25f, 0.7f)
                }
                val mCos = cos(m.angle)
                val mSin = sin(m.angle)
                for (i in 0 until m.flagellaChains.size) {
                    val chain = m.flagellaChains[i]
                    val yOffset = mOffsets[i]
                    val localX = -m.radius * 0.88f
                    val localY = m.radius * yOffset
                    scratchRoot.set(
                        m.position.x + mCos * localX - mSin * localY,
                        m.position.y + mSin * localX + mCos * localY
                    )
                    val wavePulse = sin(gameTimeSeconds * 8.5f + i * 1.4f) * 0.35f
                    chain.update(scratchRoot, m.angle, m.velocity, deltaTime, wavePulse)
                }
            }
        }
    }

    private fun handlePlayerFoodCollisions() {
        val playerRadius = calculatePlayerRadius()
        var collectedAny = false

        foodGrid.forEachNeighbor(player.position.x, player.position.y) { food ->
            if (!food.isCollected) {
                val dist = player.position.wrappedDistanceTo(food.position, WORLD_WIDTH, WORLD_HEIGHT)
                if (dist < playerRadius + food.radius) {
                    if (TrophicWebRules.canConsumeFood(playerStats.dietType, food.kind)) {
                        food.isCollected = true
                        collectedAny = true
                        player.dnaPoints += food.valueDna
                        val biomassGain = if (playerStats.activeChimericTraits.contains("TRAIT_HYBRID_METABOLISM")) food.valueBiomass * 1.5f else food.valueBiomass
                        player.biomass += biomassGain

                        val healMultiplier = if (playerStats.activeChimericTraits.contains("TRAIT_HERBI_REGEN")) 2.2f else 1.5f
                        player.health = min(player.maxHealth, player.health + food.valueBiomass * healMultiplier)

                        onDnaCollected(food.valueDna)
                        addNotice(food.position, "+${food.valueDna} ADN", food.color)
                        onHapticImpact()
                    }
                }
            }
        }

        if (collectedAny) {
            foods.removeAll { it.isCollected }
            isFoodGridDirty = true
        }
    }

    private fun handlePlayerMicrobeCollisions() {
        val playerRadius = calculatePlayerRadius()

        microbeGrid.forEachNeighbor(player.position.x, player.position.y) { m ->
            val dist = player.position.wrappedDistanceTo(m.position, WORLD_WIDTH, WORLD_HEIGHT)
            val combinedRadius = playerRadius + m.radius

            if (dist < combinedRadius) {
                if (discoveredSpecies.add(m.speciesId)) {
                    onSpeciesDiscovered(m.speciesId)
                }

                val toMicrobe = player.position.wrappedDeltaInto(m.position, WORLD_WIDTH, WORLD_HEIGHT, scratchA)
                val hitAngle = toMicrobe.angle() - player.angle
                player.softBody.applyImpact(hitAngle, impulse = 12f)
                m.softBody.applyImpact(hitAngle + PI.toFloat(), impulse = 12f)

                // 1. Spikes Collision
                if (playerStats.spikeDamage > 0f) {
                    val hitDmg = playerStats.spikeDamage * 0.6f
                    m.health -= hitDmg
                    m.softBody.applyImpact(hitAngle + PI.toFloat(), impulse = 24f)
                    addNotice(m.position, "¡PÚA! -${hitDmg.toInt()}", Color(0xFF00E5FF))
                }
                if (m.spikesCount > 0) {
                    val enemySpikeDmg = m.spikesCount * 18f
                    player.softBody.applyImpact(hitAngle, impulse = 24f)
                    damagePlayer(enemySpikeDmg, "${m.name} (Púas)")
                    // Chimeric reflective spikes: return 50% damage
                    if (playerStats.hasReflectiveSpikes) {
                        m.health -= enemySpikeDmg * 0.5f
                        addNotice(m.position, "¡Reflejo! -${(enemySpikeDmg * 0.5f).toInt()}", Color(0xFF00E5FF))
                    }
                }

                // 2. Predator & Prey interactions
                val canPlayerEatMicrobe = TrophicWebRules.canPredatorAttack(playerStats.dietType, playerStats.trophicTier, playerRadius, m.trophicTier, m.radius)
                val canMicrobeEatPlayer = TrophicWebRules.canPredatorAttack(m.diet, m.trophicTier, m.radius, playerStats.trophicTier, playerRadius)

                if (canPlayerEatMicrobe) {
                    var biteDmg = playerStats.biteDamage * 1.5f
                    // Chimeric Critical bite check
                    if (playerStats.hasCriticalBite && Random.nextFloat() < 0.35f) {
                        biteDmg *= 2.0f
                        addNotice(m.position, "¡CRÍTICO! -${biteDmg.toInt()}", Color(0xFFFF1744))
                    } else {
                        addNotice(m.position, "Mordisco! -${biteDmg.toInt()}", Color(0xFFFF5252))
                    }
                    m.health -= biteDmg

                    // Chimeric Vampiric bite healing check
                    if (playerStats.hasVampiricBite) {
                        val vampHeal = biteDmg * 0.30f
                        player.health = min(player.maxHealth, player.health + vampHeal)
                    }

                    player.mouthAnimationTimer = 0.35f
                    player.isBiting = true
                    player.jawAperture = 0.05f
                    m.softBody.applyImpact(hitAngle + PI.toFloat(), impulse = 28f)
                    onHapticImpact()
                }

                if (canMicrobeEatPlayer && m.biteCooldown <= 0f) {
                    m.biteCooldown = 0.8f
                    m.isBiting = true
                    m.jawAperture = 0.05f
                    val incomingDmg = when (m.trophicTier) {
                        TrophicTier.APEX -> 40f
                        TrophicTier.PREDATOR -> 24f
                        TrophicTier.SECONDARY_CONSUMER -> 14f
                        else -> 8f
                    }
                    player.softBody.applyImpact(hitAngle, impulse = 28f)
                    damagePlayer(incomingDmg, m.name)

                    // Chimeric reflective spikes
                    if (playerStats.hasReflectiveSpikes) {
                        m.health -= incomingDmg * 0.5f
                        addNotice(m.position, "¡Reflejo! -${(incomingDmg * 0.5f).toInt()}", Color(0xFF00E5FF))
                    }
                }

                val overlap = combinedRadius - dist
                val pushDir = m.position.wrappedDeltaInto(player.position, WORLD_WIDTH, WORLD_HEIGHT, scratchB).normalizeInPlace()
                player.position.addScaledInPlace(pushDir, overlap * 0.5f)
                player.position.wrapInPlace(WORLD_WIDTH, WORLD_HEIGHT)
                m.position.x -= pushDir.x * (overlap * 0.5f)
                m.position.y -= pushDir.y * (overlap * 0.5f)
                m.position.wrapInPlace(WORLD_WIDTH, WORLD_HEIGHT)
            }
        }
    }

    private fun handleMicrobeMicrobeCollisions() {
        for (i in 0 until microbes.size) {
            val a = microbes[i]
            microbeGrid.forEachNeighbor(a.position.x, a.position.y) { b ->
                if (a.id < b.id) {
                    val dist = a.position.wrappedDistanceTo(b.position, WORLD_WIDTH, WORLD_HEIGHT)
                    if (dist < a.radius + b.radius) {
                        val toB = a.position.wrappedDeltaInto(b.position, WORLD_WIDTH, WORLD_HEIGHT, scratchA)
                        val angleA = toB.angle() - a.angle
                        a.softBody.applyImpact(angleA, impulse = 10f)
                        b.softBody.applyImpact(angleA + PI.toFloat(), impulse = 10f)

                        val aCanEatB = TrophicWebRules.canPredatorAttack(a.diet, a.trophicTier, a.radius, b.trophicTier, b.radius)
                        val bCanEatA = TrophicWebRules.canPredatorAttack(b.diet, b.trophicTier, b.radius, a.trophicTier, a.radius)

                        if (aCanEatB) {
                            b.health -= 25f
                            a.health = min(a.maxHealth, a.health + 10f)
                            a.isBiting = true
                            a.jawAperture = 0.05f
                            b.softBody.applyImpact(angleA + PI.toFloat(), impulse = 22f)
                        } else if (bCanEatA) {
                            a.health -= 25f
                            b.health = min(b.maxHealth, b.health + 10f)
                            b.isBiting = true
                            b.jawAperture = 0.05f
                            a.softBody.applyImpact(angleA, impulse = 22f)
                        }
                    }
                }
            }
        }
    }

    private fun handlePlayerMeteorCollisions() {
        val playerRadius = calculatePlayerRadius()
        val meteorIter = meteorShards.iterator()

        while (meteorIter.hasNext()) {
            val meteor = meteorIter.next()
            val dist = player.position.wrappedDistanceTo(meteor.position, WORLD_WIDTH, WORLD_HEIGHT)
            val combined = playerRadius + meteor.radius

            if (dist < combined) {
                val hitAngle = player.position.wrappedDeltaTo(meteor.position, WORLD_WIDTH, WORLD_HEIGHT).angle() - player.angle
                player.softBody.applyImpact(hitAngle, impulse = 18f)

                // Damage calculation against the meteorite crust
                val baseDmg = playerStats.biteDamage + (playerStats.spikeDamage * 0.8f) + 12f
                val finalDmg = if (playerStats.activeChimericTraits.contains("TRAIT_DEMOLITION_RAM")) baseDmg * 2.8f else baseDmg

                meteor.health -= finalDmg
                onHapticImpact()
                rippleSystem.emitDashShockwave(meteor.position, meteor.radius * 0.8f, meteor.coreColor)

                if (meteor.health <= 0f) {
                    SporeAudioEngine.playMeteorHit(shattered = true)
                    meteorIter.remove()
                    addNotice(meteor.position, "¡METEORITO DESTROZADO!", Color(0xFFFFD54F))

                    // Drop contained part capsule
                    partCapsules.add(
                        PartCapsule(
                            id = idGen.incrementAndGet(),
                            position = meteor.position,
                            partId = meteor.containedPartId,
                            partName = meteor.partName,
                            iconColor = Color(0xFFFFD54F)
                        )
                    )

                    // Also drop bonus meat and DNA
                    for (i in 0 until 2) {
                        val offset = Vector2(Random.nextFloat() * 40f - 20f, Random.nextFloat() * 40f - 20f)
                        foods.add(
                            FoodParticle(
                                id = idGen.incrementAndGet(),
                                position = meteor.position + offset,
                                kind = FoodKind.DNA_STRAND,
                                radius = 12f,
                                valueDna = 20,
                                valueBiomass = 4.0f,
                                color = Color(0xFFFFD600)
                            )
                        )
                    }
                    isFoodGridDirty = true
                } else {
                    SporeAudioEngine.playMeteorHit(shattered = false)
                    addNotice(meteor.position, "Fractura -${finalDmg.toInt()}", Color(0xFFFFCC80))
                }

                // Bounce bounce
                val pushDir = meteor.position.wrappedDeltaTo(player.position, WORLD_WIDTH, WORLD_HEIGHT).normalized()
                player.velocity = pushDir * 120f
            }
        }
    }

    private fun handlePlayerCapsuleCollisions() {
        val playerRadius = calculatePlayerRadius()
        val capsuleIter = partCapsules.iterator()

        while (capsuleIter.hasNext()) {
            val cap = capsuleIter.next()
            if (cap.isCollected) continue

            val dist = player.position.wrappedDistanceTo(cap.position, WORLD_WIDTH, WORLD_HEIGHT)
            if (dist < playerRadius + cap.radius) {
                cap.isCollected = true
                capsuleIter.remove()
                onHapticImpact()

                if (!unlockedPartsSet.contains(cap.partId)) {
                    unlockedPartsSet.add(cap.partId)
                    evolutionEntity = evolutionEntity.copy(unlockedParts = CellEvolutionConfig.formatUnlockedParts(unlockedPartsSet))
                    player.dnaPoints += 35
                    onDnaCollected(35)
                    newlyDiscoveredPartEvent = cap.partName
                    onPartUnlocked(cap.partId, cap.partName)
                    SporeAudioEngine.playPartDiscovered()
                    addNotice(player.position, "¡GENOMA ASIMILADO: ${cap.partName}! +35 ADN", Color(0xFFFFD600))
                } else {
                    player.dnaPoints += 15
                    onDnaCollected(15)
                    addNotice(player.position, "+15 ADN (Genoma Reciclado)", Color(0xFF80D8FF))
                }
            }
        }
    }

    private fun burstDeadMicrobe(m: AiMicrobe) {
        val meatCount = when (m.trophicTier) {
            TrophicTier.APEX -> 6
            TrophicTier.PREDATOR -> 4
            TrophicTier.SECONDARY_CONSUMER -> 2
            else -> 1
        }
        for (i in 0 until meatCount) {
            val offset = Vector2(Random.nextFloat() * 50f - 25f, Random.nextFloat() * 50f - 25f)
            foods.add(
                FoodParticle(
                    id = idGen.incrementAndGet(),
                    position = m.position + offset,
                    kind = FoodKind.MEAT_CHUNK,
                    radius = 9.5f,
                    valueDna = 8,
                    valueBiomass = 2.8f,
                    color = Color(0xFFFF5252)
                )
            )
        }
        val dnaDrops = if (m.trophicTier == TrophicTier.APEX) 3 else 1
        for (i in 0 until dnaDrops) {
            val offset = Vector2(Random.nextFloat() * 40f - 20f, Random.nextFloat() * 40f - 20f)
            foods.add(
                FoodParticle(
                    id = idGen.incrementAndGet(),
                    position = m.position + offset,
                    kind = FoodKind.DNA_STRAND,
                    radius = 12f,
                    valueDna = 20,
                    valueBiomass = 4.5f,
                    color = Color(0xFFFFD600)
                )
            )
        }

        // Spore Trophy Part drop based on defeated prey species
        val droppedPartId: String? = when {
            m.speciesId == "predator_didinium" && (!unlockedPartsSet.contains("JET") || !unlockedPartsSet.contains("MOUTH_CARNIVORE")) -> {
                if (!unlockedPartsSet.contains("JET")) "JET" else "MOUTH_CARNIVORE"
            }
            m.speciesId == "predator_spiketooth" && (!unlockedPartsSet.contains("SPIKES") || !unlockedPartsSet.contains("ARMOR")) -> {
                if (!unlockedPartsSet.contains("SPIKES")) "SPIKES" else "ARMOR"
            }
            m.speciesId == "omnivore_amoeba" && !unlockedPartsSet.contains("MOUTH_OMNIVORE") -> "MOUTH_OMNIVORE"
            m.speciesId == "apex_megacolossus" && (!unlockedPartsSet.contains("POISON") || !unlockedPartsSet.contains("ELECTRIC")) -> {
                if (!unlockedPartsSet.contains("POISON")) "POISON" else "ELECTRIC"
            }
            Random.nextFloat() < 0.25f -> {
                CellEvolutionConfig.ALL_PARTS.firstOrNull { !unlockedPartsSet.contains(it.id) }?.id
            }
            else -> null
        }

        if (droppedPartId != null) {
            val partDef = CellEvolutionConfig.getPart(droppedPartId)
            if (partDef != null) {
                partCapsules.add(
                    PartCapsule(
                        id = idGen.incrementAndGet(),
                        position = m.position,
                        partId = partDef.id,
                        partName = partDef.name,
                        iconColor = Color(0xFFFFD54F)
                    )
                )
                addNotice(m.position, "¡Órgano Fósil Expulsado!", Color(0xFFFFD54F))
            }
        }

        isFoodGridDirty = true
        addNotice(m.position, "¡${m.name} Devorado!", Color(0xFF00E5FF))
    }

    /**
     * Iconic Spore Mating Call trigger.
     */
    fun triggerMatingCall(): Boolean {
        if (isMatingDanceActive) return false

        // Emit acoustic wave from player
        acousticWaves.add(
            AcousticWave(
                id = idGen.incrementAndGet(),
                origin = player.position,
                color = Color(0xFFFF4081),
                fromPlayer = true
            )
        )
        SporeAudioEngine.playMatingCall()
        isMatingCallActive = true

        // Spawn or direct active mate
        if (activeMate == null) {
            val mateAngle = Random.nextFloat() * 2f * PI.toFloat()
            val mateDist = 800f
            val matePos = Vector2(
                wrapCoord(player.position.x + cos(mateAngle) * mateDist, WORLD_WIDTH),
                wrapCoord(player.position.y + sin(mateAngle) * mateDist, WORLD_HEIGHT)
            )
            activeMate = SymbioticMate(
                id = idGen.incrementAndGet(),
                position = matePos,
                speciesName = evolutionEntity.speciesName,
                radius = calculatePlayerRadius() * 0.95f,
                primaryColor = Color(0xFFFF80AB),
                mouthType = playerStats.dietType,
                state = MateState.SWIMMING_TO_PLAYER,
                sonarPulseTimer = 1.0f
            )
        } else {
            activeMate!!.state = MateState.SWIMMING_TO_PLAYER
        }

        // Mate echoes response
        acousticWaves.add(
            AcousticWave(
                id = idGen.incrementAndGet(),
                origin = activeMate!!.position,
                color = Color(0xFFFF80AB),
                fromPlayer = false
            )
        )
        SporeAudioEngine.playMateResponseEcho()
        addNotice(player.position, "¡Llamada de Cortejo Emitida! ❤️", Color(0xFFFF4081))
        onHapticImpact()
        return true
    }

    fun triggerDash(): Boolean {
        if (player.dashCooldownTimer <= 0f) {
            player.isDashing = true
            player.dashDurationTimer = 0.5f
            player.dashCooldownTimer = if (evolutionEntity.jetCount > 0) 2.5f else 4.0f
            val dashBurst = Vector2.fromAngle(player.angle, playerStats.baseSpeed * playerStats.dashSpeedMultiplier * 0.7f)
            player.velocity = player.velocity + dashBurst
            rippleSystem.emitDashShockwave(player.position, calculatePlayerRadius(), Color(planetDefinition.oceanRimColor))
            onHapticImpact()

            // Chimeric Cavitation Shockwave
            if (playerStats.hasShockwaveDash) {
                microbeGrid.forEachNeighbor(player.position.x, player.position.y) { m ->
                    val d = player.position.wrappedDistanceTo(m.position, WORLD_WIDTH, WORLD_HEIGHT)
                    if (d < 180f) {
                        m.health -= 35f
                        val push = player.position.wrappedDeltaTo(m.position, WORLD_WIDTH, WORLD_HEIGHT).normalized()
                        m.velocity = m.velocity + (push * 350f)
                        addNotice(m.position, "¡Cavitación! -35", Color(0xFF00E5FF))
                    }
                }
            }
            return true
        }
        return false
    }

    fun triggerPoison(): Boolean {
        if (playerStats.hasPoison && player.poisonCooldownTimer <= 0f) {
            player.poisonCooldownTimer = 5.0f
            poisonPuddles.add(
                PoisonPuddle(
                    id = idGen.incrementAndGet(),
                    position = player.position - Vector2.fromAngle(player.angle, calculatePlayerRadius()),
                    fromPlayer = true,
                    damagePerSecond = 30f
                )
            )
            onHapticImpact()
            return true
        }
        return false
    }

    fun triggerElectricShock(): Boolean {
        if (playerStats.hasElectricShock && player.electricCooldownTimer <= 0f) {
            player.electricCooldownTimer = 6.5f
            electricBlasts.add(
                ElectricBlast(
                    id = idGen.incrementAndGet(),
                    position = player.position,
                    fromPlayer = true,
                    damage = 50f
                )
            )
            onHapticImpact()

            // Chimeric Bio-Magnetic Shock: attracts all foods in vicinity (zero-allocation)
            if (playerStats.hasBioMagneticShock) {
                for (food in foods) {
                    val d = player.position.wrappedDistanceTo(food.position, WORLD_WIDTH, WORLD_HEIGHT)
                    if (d < 300f) {
                        food.position.wrappedDeltaInto(player.position, WORLD_WIDTH, WORLD_HEIGHT, scratchA)
                            .normalizeInPlace()
                        food.position.addScaledInPlace(scratchA, 240f)
                    }
                }
                isFoodGridDirty = true
            }
            return true
        }
        return false
    }

    fun damagePlayer(rawAmount: Float, attacker: String) {
        if (player.invulnerableTimer > 0f) return
        val reduced = rawAmount * (1f - playerStats.armorDamageReduction)
        player.health -= reduced
        player.invulnerableTimer = 0.35f
        player.damageFlashTimer = 0.25f
        onHapticImpact()
        addNotice(player.position, "-${reduced.toInt()} HP", Color(0xFFFF1744))

        if (player.health <= 0f) {
            player.health = 0f
            isGameOver = true
            killerName = attacker
            onPlayerKilled(attacker)
        }
    }

    fun calculatePlayerRadius(): Float {
        return 24f + (player.biomass * 0.35f).coerceAtMost(80f)
    }

    private fun addNotice(pos: Vector2, text: String, color: Color) {
        if (notices.size > 10) notices.removeAt(0)
        notices.add(FloatingNotice(id = idGen.incrementAndGet(), position = pos, text = text, color = color))
    }

    private fun updateNotices(deltaTime: Float) {
        val iter = notices.iterator()
        while (iter.hasNext()) {
            val n = iter.next()
            n.remainingSeconds -= deltaTime
            n.position.y -= 30f * deltaTime // identical to += Vector2(0, -30*dt), zero allocation
            n.alpha = (n.remainingSeconds / 1.2f).coerceIn(0f, 1f)
            if (n.remainingSeconds <= 0f) iter.remove()
        }
    }

    private fun updateMeteorShards(deltaTime: Float) {
        for (meteor in meteorShards) {
            meteor.rotation += meteor.rotationSpeed * deltaTime
            meteor.position.addScaledInPlace(meteor.velocity, deltaTime)
            meteor.position.wrapInPlace(WORLD_WIDTH, WORLD_HEIGHT)
        }
    }

    private fun updatePartCapsules(deltaTime: Float) {
        val iter = partCapsules.iterator()
        while (iter.hasNext()) {
            val cap = iter.next()
            cap.lifeTimer -= deltaTime
            cap.wobblePhase += deltaTime * 3f
            cap.position.addScaledInPlace(cap.velocity, deltaTime)
            cap.position.wrapInPlace(WORLD_WIDTH, WORLD_HEIGHT)
            if (cap.lifeTimer <= 0f) iter.remove()
        }
    }

    private fun updateAcousticWaves(deltaTime: Float) {
        val iter = acousticWaves.iterator()
        while (iter.hasNext()) {
            val wave = iter.next()
            wave.currentRadius += wave.speed * deltaTime
            wave.alpha = (1f - (wave.currentRadius / wave.maxRadius)).coerceIn(0f, 1f)
            if (wave.currentRadius >= wave.maxRadius) iter.remove()
        }
    }

    private fun updateSymbioticMate(deltaTime: Float) {
        val mate = activeMate ?: return
        mate.wobbleTimer += deltaTime * 3f
        mate.softBody.update(deltaTime)

        if (mate.state == MateState.SWIMMING_TO_PLAYER) {
            val toPlayer = mate.position.wrappedDeltaInto(player.position, WORLD_WIDTH, WORLD_HEIGHT, scratchA)
            val targetA = toPlayer.angle()
            mate.angle += angleDifference(targetA, mate.angle) * min(1f, 3.5f * deltaTime)
            mate.velocity.setFromAngle(mate.angle, 140f)
            mate.position.addScaledInPlace(mate.velocity, deltaTime)
            mate.position.wrapInPlace(WORLD_WIDTH, WORLD_HEIGHT)

            mate.sonarPulseTimer -= deltaTime
            if (mate.sonarPulseTimer <= 0f) {
                mate.sonarPulseTimer = 2.8f
                acousticWaves.add(
                    AcousticWave(
                        id = idGen.incrementAndGet(),
                        origin = mate.position,
                        color = Color(0xFFFF80AB),
                        fromPlayer = false
                    )
                )
            }

            val dist = mate.position.wrappedDistanceTo(player.position, WORLD_WIDTH, WORLD_HEIGHT)
            if (dist < calculatePlayerRadius() + mate.radius + 15f) {
                mate.state = MateState.COURTSHIP_DANCE
                isMatingDanceActive = true
                matingDanceTimer = 0f
                SporeAudioEngine.playCourtshipZygote()
                addNotice(player.position, "¡Danza de Cortejo Simbiótico! 💕", Color(0xFFFF4081))
                onHapticImpact()
            }
        }
    }

    private fun updateHeartBubbles(deltaTime: Float) {
        val iter = heartBubbles.iterator()
        while (iter.hasNext()) {
            val h = iter.next()
            h.lifeSeconds -= deltaTime
            h.position.addScaledInPlace(h.velocity, deltaTime)
            h.alpha = (h.lifeSeconds / 1.3f).coerceIn(0f, 1f)
            if (h.lifeSeconds <= 0f) iter.remove()
        }
    }

    private fun updateAbyssalCreatures(deltaTime: Float) {
        for (abyss in abyssalCreatures) {
            abyss.position.addScaledInPlace(abyss.velocity, deltaTime)
            abyss.position.wrapInPlace(WORLD_WIDTH, WORLD_HEIGHT)
            if (Random.nextFloat() < 0.02f) {
                abyss.angle += (Random.nextFloat() * 0.3f - 0.15f)
                val speed = abyss.velocity.length()
                abyss.velocity.setFromAngle(abyss.angle, speed)
            }
        }
    }

    private fun updateForegroundFloaters(deltaTime: Float) {
        for (floater in foregroundFloaters) {
            floater.position.addScaledInPlace(floater.velocity, deltaTime)
            floater.position.wrapInPlace(WORLD_WIDTH, WORLD_HEIGHT)
        }
    }

    private fun updateAmbient(deltaTime: Float) {
        for (p in ambientParticles) {
            p.position.addScaledInPlace(p.velocity, deltaTime)
            p.position.wrapInPlace(WORLD_WIDTH, WORLD_HEIGHT)
        }
    }

    private fun angleDifference(target: Float, current: Float): Float {
        var diff = (target - current) % (2f * PI.toFloat())
        if (diff < -PI.toFloat()) diff += 2f * PI.toFloat()
        if (diff > PI.toFloat()) diff -= 2f * PI.toFloat()
        return diff
    }

    private fun wrapCoord(v: Float, max: Float): Float {
        val r = v % max
        return if (r < 0f) r + max else r
    }

    fun restart() {
        isGameOver = false
        killerName = ""
        player.health = playerStats.maxHealth
        player.position = Vector2(WORLD_WIDTH / 2f, WORLD_HEIGHT / 2f)
        player.velocity = Vector2.ZERO
        player.bankRoll = 0f
        player.pitchAngle = 0f
        player.elevationZ = 0f
        activeMate = null
        isMatingDanceActive = false
        isMatingCallActive = false
        matingDanceTimer = 0f
        foods.clear()
        microbes.clear()
        meteorShards.clear()
        partCapsules.clear()
        acousticWaves.clear()
        heartBubbles.clear()
        poisonPuddles.clear()
        electricBlasts.clear()
        rippleSystem.clear()
        discoveredSpecies.clear()
        isFoodGridDirty = true
        seedAbyssalCreatures()
        seedForegroundFloaters()
        spawnInitialEcosystem()
    }
}
