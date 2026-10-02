package com.example.spore.game.engine

import androidx.compose.ui.graphics.Color
import com.example.spore.data.model.CellEvolutionEntity
import com.example.spore.data.model.PlanetDefinition
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
    val onHapticImpact: () -> Unit = {}
) {
    companion object {
        const val WORLD_WIDTH = 7200f
        const val WORLD_HEIGHT = 7200f

        // Scarcity tuning: food and DNA are scarce and distributed in distinct nutrient patches
        const val MAX_FOOD = 75
        const val MAX_DNA_STRANDS = 5
        const val MAX_AI = 22
        const val DRAG_COEFFICIENT = 0.94f
    }

    private val idGen = AtomicLong(1000)

    var evolutionEntity: CellEvolutionEntity = initialEvolution
        private set
    var playerStats: CellStats = CellEvolutionConfig.calculateStats(initialEvolution)
        private set

    val player: PlayerCell = PlayerCell(
        position = Vector2(WORLD_WIDTH / 2f, WORLD_HEIGHT / 2f),
        stats = playerStats,
        biomass = initialEvolution.biomass,
        dnaPoints = initialEvolution.dnaPoints,
        health = playerStats.maxHealth,
        maxHealth = playerStats.maxHealth
    )

    // Oceanic Systems: Water ripples, hydrodynamic wakes, seabed terrain & currents
    val rippleSystem = WaterRippleSystem(WORLD_WIDTH, WORLD_HEIGHT)
    val oceanTerrain = OceanTerrainSystem(WORLD_WIDTH, planetDefinition)
    private var playerSwimRippleTimer = 0f

    val foods = mutableListOf<FoodParticle>()
    val microbes = mutableListOf<AiMicrobe>()
    val poisonPuddles = mutableListOf<PoisonPuddle>()
    val electricBlasts = mutableListOf<ElectricBlast>()
    val notices = mutableListOf<FloatingNotice>()
    val ambientParticles = mutableListOf<AmbientParticle>()

    // Anti-lag spatial partition grids (O(1) neighbor lookups)
    private val foodGrid = SpatialGrid<FoodParticle>(WORLD_WIDTH, WORLD_HEIGHT, cellSize = 300f)
    private val microbeGrid = SpatialGrid<AiMicrobe>(WORLD_WIDTH, WORLD_HEIGHT, cellSize = 300f)

    // Nutrient bloom centers (hotspots where phytoplankton concentrates)
    private val bloomCenters = listOf(
        Vector2(1200f, 1200f),
        Vector2(3600f, 1400f),
        Vector2(2400f, 2600f),
        Vector2(1100f, 3700f),
        Vector2(3700f, 3600f)
    )

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

    // Anti-lag LOD tick counter
    private var tickFrameCounter: Int = 0

    init {
        seedAmbientParticles()
        spawnInitialEcosystem()
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
        restart()
    }

    fun updateEvolutionConfig(newEntity: CellEvolutionEntity) {
        evolutionEntity = newEntity
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
    }

    private fun spawnScatteredFood() {
        // Choose whether to spawn in a nutrient bloom or drifting in open space
        val isBloom = Random.nextFloat() < 0.70f
        val pos = if (isBloom) {
            val center = bloomCenters.random()
            val angle = Random.nextFloat() * 2f * PI.toFloat()
            val dist = Random.nextFloat() * 380f
            Vector2(
                (center.x + cos(angle) * dist).coerceIn(100f, WORLD_WIDTH - 100f),
                (center.y + sin(angle) * dist).coerceIn(100f, WORLD_HEIGHT - 100f)
            )
        } else {
            Vector2(Random.nextFloat() * WORLD_WIDTH, Random.nextFloat() * WORLD_HEIGHT)
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

        // Anti-lag: Clamp delta time to prevent physics explosions during frame drops
        val deltaTime = rawDeltaTime.coerceIn(0.005f, 0.033f)
        gameTimeSeconds += deltaTime
        tickFrameCounter++

        // Update oceanic environment and ripple physics
        oceanTerrain.update(deltaTime, player.position)
        rippleSystem.update(deltaTime)

        // Update player timers
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

        // Player physics & boundless movement
        updatePlayerMovement(deltaTime, inputDirection)

        // Hydrodynamic swimming ripples & wake generation
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

        // Rebuild anti-lag spatial grids
        rebuildSpatialGrids()

        // Spawning cycle (scarce food replenishing)
        if (foods.size < MAX_FOOD && Random.nextFloat() < 0.20f) {
            spawnScatteredFood()
        }
        if (microbes.size < MAX_AI && Random.nextFloat() < 0.05f) {
            spawnAiMicrobe()
        }

        // Hazards
        updateHazards(deltaTime)

        // Microbes AI (with LOD distance culling)
        updateMicrobes(deltaTime)

        // High-performance spatial collisions
        handlePlayerFoodCollisions()
        handlePlayerMicrobeCollisions()
        handleMicrobeMicrobeCollisions()

        // Notices & particles
        updateNotices(deltaTime)
        updateAmbient(deltaTime)

        // Evolution readiness
        if (!readyToEvolveNoticeShown && player.dnaPoints >= 50) {
            readyToEvolveNoticeShown = true
            addNotice(player.position, "¡MUTACIÓN LISTA! Toca ADN", Color(0xFFFFD600))
        }
    }

    private fun rebuildSpatialGrids() {
        foodGrid.clear()
        for (i in 0 until foods.size) {
            val f = foods[i]
            foodGrid.insert(f.position.x, f.position.y, f)
        }

        microbeGrid.clear()
        for (i in 0 until microbes.size) {
            val m = microbes[i]
            microbeGrid.insert(m.position.x, m.position.y, m)
        }
    }

    private fun updatePlayerMovement(deltaTime: Float, inputDirection: Vector2) {
        val moveIntent = if (inputDirection.lengthSquared() > 0.04f) inputDirection.normalized() else Vector2.ZERO

        if (moveIntent.lengthSquared() > 0f) {
            val targetAngle = moveIntent.angle()
            val diff = angleDifference(targetAngle, player.angle)
            player.angle += diff * min(1f, playerStats.turnRate * deltaTime)

            // Thrust
            val speedMult = if (player.isDashing) playerStats.dashSpeedMultiplier else 1f
            val thrust = Vector2.fromAngle(player.angle, playerStats.baseSpeed * speedMult * deltaTime * 12f)
            player.velocity = (player.velocity + thrust)
        }

        // Fluid Drag
        player.velocity = player.velocity * DRAG_COEFFICIENT
        player.position = player.position + (player.velocity * deltaTime)

        // Oceanic Current Flow Drift
        val currentFlow = oceanTerrain.getCurrentVelocityAt(player.position)
        player.position = player.position + (currentFlow * (deltaTime * 0.25f))

        // Seamless infinite ocean wrap (NO WALLS)
        player.position = Vector2(
            wrapCoord(player.position.x, WORLD_WIDTH),
            wrapCoord(player.position.y, WORLD_HEIGHT)
        )
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
            // Damage microbes in vicinity
            microbeGrid.forEachNeighbor(p.position.x, p.position.y) { m ->
                if (p.fromPlayer && p.position.wrappedDistanceTo(m.position, WORLD_WIDTH, WORLD_HEIGHT) < p.currentRadius + m.radius) {
                    m.health -= p.damagePerSecond * deltaTime * 1.5f
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
                        val away = eb.position.wrappedDeltaTo(m.position, WORLD_WIDTH, WORLD_HEIGHT).normalized()
                        m.velocity = m.velocity + (away * 400f)
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

            // Anti-lag LOD: If microbe is very far (> 1400px), skip complex steering every 2nd frame
            val distToPlayer = m.position.wrappedDistanceTo(player.position, WORLD_WIDTH, WORLD_HEIGHT)
            val isFar = distToPlayer > 1400f
            if (isFar && (tickFrameCounter % 2 != 0)) {
                // Just coast forward
                m.position = m.position + (m.velocity * deltaTime)
                m.position = Vector2(
                    wrapCoord(m.position.x, WORLD_WIDTH),
                    wrapCoord(m.position.y, WORLD_HEIGHT)
                )
                continue
            }

            val canEatPlayer = TrophicWebRules.canPredatorAttack(m.diet, m.trophicTier, m.radius, playerStats.trophicTier, playerRadius)
            val playerCanEatMe = TrophicWebRules.canPredatorAttack(playerStats.dietType, playerStats.trophicTier, playerRadius, m.trophicTier, m.radius)

            if (playerCanEatMe && distToPlayer < 400f) {
                m.state = AiState.FLEEING
                val toPlayer = m.position.wrappedDeltaTo(player.position, WORLD_WIDTH, WORLD_HEIGHT)
                m.targetAngle = (-toPlayer).angle()
            } else if (canEatPlayer && distToPlayer < 480f) {
                m.state = AiState.HUNTING
                val toPlayer = m.position.wrappedDeltaTo(player.position, WORLD_WIDTH, WORLD_HEIGHT)
                m.targetAngle = toPlayer.angle()
                if (m.hasPoison && m.poisonCooldown <= 0f && distToPlayer < 130f) {
                    m.poisonCooldown = 6f
                    poisonPuddles.add(PoisonPuddle(id = idGen.incrementAndGet(), position = m.position, fromPlayer = false))
                }
            } else if (m.diet == DietType.HERBIVORE) {
                m.state = AiState.GRAZING
                if (m.stateTimer > 1.5f) {
                    // Anti-lag: Query only nearby foods using spatial grid instead of full scan
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
                        m.targetAngle = m.position.wrappedDeltaTo(nearestFood!!.position, WORLD_WIDTH, WORLD_HEIGHT).angle()
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
                        m.targetAngle = m.position.wrappedDeltaTo(huntTarget!!.position, WORLD_WIDTH, WORLD_HEIGHT).angle()
                    } else {
                        m.state = AiState.ROAMING
                        m.targetAngle += (Random.nextFloat() * 2f - 1f)
                    }
                    m.stateTimer = 0f
                }
            }

            // Turn towards target angle
            val diff = angleDifference(m.targetAngle, m.angle)
            val turnSpeed = 2.0f + (m.ciliaCount * 1.2f)
            m.angle += diff * min(1f, turnSpeed * deltaTime)

            // Thrust & Ocean Current
            val currentSpeed = when (m.state) {
                AiState.FLEEING -> m.targetSpeed * 1.35f
                AiState.HUNTING -> m.targetSpeed * 1.2f
                else -> m.targetSpeed * 0.85f
            }
            val thrust = Vector2.fromAngle(m.angle, currentSpeed * deltaTime * 10f)
            m.velocity = (m.velocity + thrust) * DRAG_COEFFICIENT
            m.position = m.position + (m.velocity * deltaTime)

            // Seamless infinite ocean wrap (NO WALLS)
            m.position = Vector2(
                wrapCoord(m.position.x, WORLD_WIDTH),
                wrapCoord(m.position.y, WORLD_HEIGHT)
            )

            // Microbe swimming water ripples
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

    /**
     * Anti-lag: Player-Food collision using Spatial Grid with toroidal wrapping.
     */
    private fun handlePlayerFoodCollisions() {
        val playerRadius = calculatePlayerRadius()
        val toRemove = mutableListOf<Long>()

        foodGrid.forEachNeighbor(player.position.x, player.position.y) { food ->
            val dist = player.position.wrappedDistanceTo(food.position, WORLD_WIDTH, WORLD_HEIGHT)
            if (dist < playerRadius + food.radius) {
                if (TrophicWebRules.canConsumeFood(playerStats.dietType, food.kind)) {
                    toRemove.add(food.id)
                    player.dnaPoints += food.valueDna
                    player.biomass += food.valueBiomass
                    player.health = min(player.maxHealth, player.health + food.valueBiomass * 1.5f)
                    onDnaCollected(food.valueDna)
                    addNotice(food.position, "+${food.valueDna} ADN", food.color)
                    onHapticImpact()
                }
            }
        }

        if (toRemove.isNotEmpty()) {
            foods.removeAll { toRemove.contains(it.id) }
        }
    }

    /**
     * Anti-lag: Player-Microbe collision using Spatial Grid with toroidal wrapping.
     */
    private fun handlePlayerMicrobeCollisions() {
        val playerRadius = calculatePlayerRadius()

        microbeGrid.forEachNeighbor(player.position.x, player.position.y) { m ->
            val dist = player.position.wrappedDistanceTo(m.position, WORLD_WIDTH, WORLD_HEIGHT)
            val combinedRadius = playerRadius + m.radius

            if (dist < combinedRadius) {
                onSpeciesDiscovered(m.speciesId)

                // 1. Spikes Collision
                if (playerStats.spikeDamage > 0f) {
                    val hitDmg = playerStats.spikeDamage * 0.6f
                    m.health -= hitDmg
                    addNotice(m.position, "¡PÚA! -${hitDmg.toInt()}", Color(0xFF00E5FF))
                }
                if (m.spikesCount > 0) {
                    val enemySpikeDmg = m.spikesCount * 18f
                    damagePlayer(enemySpikeDmg, "${m.name} (Púas)")
                }

                // 2. Predator & Prey interactions
                val canPlayerEatMicrobe = TrophicWebRules.canPredatorAttack(playerStats.dietType, playerStats.trophicTier, playerRadius, m.trophicTier, m.radius)
                val canMicrobeEatPlayer = TrophicWebRules.canPredatorAttack(m.diet, m.trophicTier, m.radius, playerStats.trophicTier, playerRadius)

                if (canPlayerEatMicrobe) {
                    val biteDmg = playerStats.biteDamage * 1.5f
                    m.health -= biteDmg
                    player.mouthAnimationTimer = 0.35f
                    addNotice(m.position, "Mordisco! -${biteDmg.toInt()}", Color(0xFFFF5252))
                    onHapticImpact()
                }

                if (canMicrobeEatPlayer && m.biteCooldown <= 0f) {
                    m.biteCooldown = 0.8f
                    val incomingDmg = when (m.trophicTier) {
                        TrophicTier.APEX -> 40f
                        TrophicTier.PREDATOR -> 24f
                        TrophicTier.SECONDARY_CONSUMER -> 14f
                        else -> 8f
                    }
                    damagePlayer(incomingDmg, m.name)
                }

                // Push
                val overlap = combinedRadius - dist
                val pushDir = m.position.wrappedDeltaTo(player.position, WORLD_WIDTH, WORLD_HEIGHT).normalized()
                player.position = Vector2(
                    wrapCoord(player.position.x + pushDir.x * (overlap * 0.5f), WORLD_WIDTH),
                    wrapCoord(player.position.y + pushDir.y * (overlap * 0.5f), WORLD_HEIGHT)
                )
                m.position = Vector2(
                    wrapCoord(m.position.x - pushDir.x * (overlap * 0.5f), WORLD_WIDTH),
                    wrapCoord(m.position.y - pushDir.y * (overlap * 0.5f), WORLD_HEIGHT)
                )
            }
        }
    }

    /**
     * Anti-lag: Microbe vs Microbe interaction limited to immediate spatial neighbors.
     */
    private fun handleMicrobeMicrobeCollisions() {
        for (i in 0 until microbes.size) {
            val a = microbes[i]
            microbeGrid.forEachNeighbor(a.position.x, a.position.y) { b ->
                if (a.id < b.id) {
                    val dist = a.position.wrappedDistanceTo(b.position, WORLD_WIDTH, WORLD_HEIGHT)
                    if (dist < a.radius + b.radius) {
                        val aCanEatB = TrophicWebRules.canPredatorAttack(a.diet, a.trophicTier, a.radius, b.trophicTier, b.radius)
                        val bCanEatA = TrophicWebRules.canPredatorAttack(b.diet, b.trophicTier, b.radius, a.trophicTier, a.radius)

                        if (aCanEatB) {
                            b.health -= 25f
                            a.health = min(a.maxHealth, a.health + 10f)
                        } else if (bCanEatA) {
                            a.health -= 25f
                            b.health = min(b.maxHealth, b.health + 10f)
                        }
                    }
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
        addNotice(m.position, "¡${m.name} Devorado!", Color(0xFF00E5FF))
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
            n.position = n.position + Vector2(0f, -30f * deltaTime)
            n.alpha = (n.remainingSeconds / 1.2f).coerceIn(0f, 1f)
            if (n.remainingSeconds <= 0f) iter.remove()
        }
    }

    private fun updateAmbient(deltaTime: Float) {
        for (p in ambientParticles) {
            p.position = p.position + (p.velocity * deltaTime)
            p.position = Vector2(
                wrapCoord(p.position.x, WORLD_WIDTH),
                wrapCoord(p.position.y, WORLD_HEIGHT)
            )
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
        foods.clear()
        microbes.clear()
        poisonPuddles.clear()
        electricBlasts.clear()
        rippleSystem.clear()
        spawnInitialEcosystem()
    }
}
