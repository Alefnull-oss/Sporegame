package com.example.spore.game.engine

import androidx.compose.ui.graphics.Color
import com.example.spore.data.model.CellEvolutionEntity
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

class GameSimulation(
    initialEvolution: CellEvolutionEntity,
    val onDnaCollected: (Int) -> Unit = {},
    val onSpeciesDiscovered: (String) -> Unit = {},
    val onSpeciesEaten: (String) -> Unit = {},
    val onPlayerKilled: (String) -> Unit = {},
    val onHapticImpact: () -> Unit = {}
) {
    companion object {
        const val WORLD_WIDTH = 4000f
        const val WORLD_HEIGHT = 4000f
        const val MAX_FOOD = 220
        const val MAX_AI = 24
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

    val foods = mutableListOf<FoodParticle>()
    val microbes = mutableListOf<AiMicrobe>()
    val poisonPuddles = mutableListOf<PoisonPuddle>()
    val electricBlasts = mutableListOf<ElectricBlast>()
    val notices = mutableListOf<FloatingNotice>()
    val ambientParticles = mutableListOf<AmbientParticle>()

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

    init {
        // Seed ambient primordial particles
        for (i in 0 until 90) {
            ambientParticles.add(
                AmbientParticle(
                    position = Vector2(Random.nextFloat() * WORLD_WIDTH, Random.nextFloat() * WORLD_HEIGHT),
                    velocity = Vector2(Random.nextFloat() * 10f - 5f, Random.nextFloat() * 10f - 5f),
                    radius = Random.nextFloat() * 3f + 1f,
                    alpha = Random.nextFloat() * 0.4f + 0.1f,
                    color = if (Random.nextBoolean()) Color(0xFF00E5FF) else Color(0xFF76FF03)
                )
            )
        }
        // Seed initial food & AI
        spawnInitialEcosystem()
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
        // Spawn algae and nutrients
        while (foods.size < 130) {
            spawnFood()
        }
        // Spawn diverse microbes across the trophic web
        while (microbes.size < 16) {
            spawnAiMicrobe()
        }
    }

    private fun spawnFood() {
        val kind = when (Random.nextInt(100)) {
            in 0..65 -> FoodKind.ALGAE
            in 66..88 -> FoodKind.MEAT_CHUNK
            else -> FoodKind.DNA_STRAND
        }
        val pos = Vector2(Random.nextFloat() * WORLD_WIDTH, Random.nextFloat() * WORLD_HEIGHT)
        val food = when (kind) {
            FoodKind.ALGAE -> FoodParticle(
                id = idGen.incrementAndGet(),
                position = pos,
                kind = kind,
                radius = 6.5f,
                valueDna = 2,
                valueBiomass = 0.8f,
                color = Color(0xFF00E676)
            )
            FoodKind.MEAT_CHUNK -> FoodParticle(
                id = idGen.incrementAndGet(),
                position = pos,
                kind = kind,
                radius = 9f,
                valueDna = 6,
                valueBiomass = 2.2f,
                color = Color(0xFFFF5252)
            )
            FoodKind.DNA_STRAND -> FoodParticle(
                id = idGen.incrementAndGet(),
                position = pos,
                kind = kind,
                radius = 11f,
                valueDna = 15,
                valueBiomass = 3.5f,
                color = Color(0xFFFFD600)
            )
        }
        foods.add(food)
    }

    private fun spawnAiMicrobe() {
        val roll = Random.nextInt(100)
        val template = when {
            roll < 45 -> {
                // Tier 1 Herbivore
                if (Random.nextBoolean()) {
                    AiMicrobe(
                        id = idGen.incrementAndGet(),
                        speciesId = "herbivore_ciliado",
                        name = "Ciliado Pacifista",
                        position = randomFarPosition(),
                        radius = 22f,
                        health = 45f,
                        maxHealth = 45f,
                        trophicTier = TrophicTier.PRIMARY_CONSUMER,
                        diet = DietType.HERBIVORE,
                        primaryColor = Color(0xFF76FF03),
                        ciliaCount = 3,
                        flagellaCount = 0,
                        spikesCount = 0,
                        targetSpeed = 140f
                    )
                } else {
                    AiMicrobe(
                        id = idGen.incrementAndGet(),
                        speciesId = "herbivore_rotifer",
                        name = "Rotífero Veloz",
                        position = randomFarPosition(),
                        radius = 26f,
                        health = 55f,
                        maxHealth = 55f,
                        trophicTier = TrophicTier.PRIMARY_CONSUMER,
                        diet = DietType.HERBIVORE,
                        primaryColor = Color(0xFF69F0AE),
                        ciliaCount = 2,
                        flagellaCount = 1,
                        spikesCount = 0,
                        targetSpeed = 180f
                    )
                }
            }
            roll < 75 -> {
                // Tier 2 Omnivore
                if (Random.nextBoolean()) {
                    AiMicrobe(
                        id = idGen.incrementAndGet(),
                        speciesId = "omnivore_amoeba",
                        name = "Ameba Oportunista",
                        position = randomFarPosition(),
                        radius = 32f,
                        health = 80f,
                        maxHealth = 80f,
                        trophicTier = TrophicTier.SECONDARY_CONSUMER,
                        diet = DietType.OMNIVORE,
                        primaryColor = Color(0xFFFFD600),
                        ciliaCount = 1,
                        flagellaCount = 1,
                        spikesCount = 0,
                        targetSpeed = 130f
                    )
                } else {
                    AiMicrobe(
                        id = idGen.incrementAndGet(),
                        speciesId = "omnivore_versatil",
                        name = "Versátilus Flagelado",
                        position = randomFarPosition(),
                        radius = 35f,
                        health = 90f,
                        maxHealth = 90f,
                        trophicTier = TrophicTier.SECONDARY_CONSUMER,
                        diet = DietType.OMNIVORE,
                        primaryColor = Color(0xFFFFAB00),
                        ciliaCount = 1,
                        flagellaCount = 2,
                        spikesCount = 1,
                        targetSpeed = 160f
                    )
                }
            }
            roll < 93 -> {
                // Tier 3 Predator
                if (Random.nextBoolean()) {
                    AiMicrobe(
                        id = idGen.incrementAndGet(),
                        speciesId = "predator_didinium",
                        name = "Didinium Cazador",
                        position = randomFarPosition(),
                        radius = 42f,
                        health = 130f,
                        maxHealth = 130f,
                        trophicTier = TrophicTier.PREDATOR,
                        diet = DietType.CARNIVORE,
                        primaryColor = Color(0xFFFF3D00),
                        ciliaCount = 1,
                        flagellaCount = 2,
                        spikesCount = 1,
                        hasJet = true,
                        targetSpeed = 190f
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
                        targetSpeed = 150f
                    )
                }
            }
            else -> {
                // Tier 4 Apex Leviathan (rare)
                AiMicrobe(
                    id = idGen.incrementAndGet(),
                    speciesId = "apex_megacolossus",
                    name = "Leviatán Abisal",
                    position = randomFarPosition(),
                    radius = 75f,
                    health = 360f,
                    maxHealth = 360f,
                    trophicTier = TrophicTier.APEX,
                    diet = DietType.CARNIVORE,
                    primaryColor = Color(0xFFD500F9),
                    ciliaCount = 3,
                    flagellaCount = 3,
                    spikesCount = 4,
                    hasPoison = true,
                    targetSpeed = 115f
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
        } while (attempts < 10 && pos.distanceTo(player.position) < 550f)
        return pos
    }

    fun update(deltaTime: Float, inputDirection: Vector2) {
        if (isGameOver) return
        gameTimeSeconds += deltaTime

        // Timers
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

        // Player physics
        updatePlayerMovement(deltaTime, inputDirection)

        // Spawning cycle
        if (foods.size < MAX_FOOD && Random.nextFloat() < 0.35f) {
            spawnFood()
        }
        if (microbes.size < MAX_AI && Random.nextFloat() < 0.08f) {
            spawnAiMicrobe()
        }

        // Update hazards
        updateHazards(deltaTime)

        // Update AIs (behavior, trophic hunting, moving)
        updateMicrobes(deltaTime)

        // Collisions
        handlePlayerFoodCollisions()
        handlePlayerMicrobeCollisions()
        handleMicrobeMicrobeCollisions()

        // Update notices & ambient particles
        updateNotices(deltaTime)
        updateAmbient(deltaTime)

        // Evolution readiness check
        if (!readyToEvolveNoticeShown && player.dnaPoints >= 60) {
            readyToEvolveNoticeShown = true
            addNotice(player.position, "¡MUTACIÓN LISTA! Toca ADN", Color(0xFFFFD600))
        }
    }

    private fun updatePlayerMovement(deltaTime: Float, inputDirection: Vector2) {
        val playerRadius = calculatePlayerRadius()
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

        // Drag
        player.velocity = player.velocity * DRAG_COEFFICIENT
        player.position = player.position + (player.velocity * deltaTime)

        // Clamp to world
        player.position = Vector2(
            player.position.x.coerceIn(playerRadius, WORLD_WIDTH - playerRadius),
            player.position.y.coerceIn(playerRadius, WORLD_HEIGHT - playerRadius)
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
            // Damage player if enemy poison
            if (!p.fromPlayer && p.position.distanceTo(player.position) < p.currentRadius + calculatePlayerRadius()) {
                damagePlayer(p.damagePerSecond * deltaTime, "Toxina Abisal")
            }
            // Damage microbes
            for (m in microbes) {
                if (p.fromPlayer && p.position.distanceTo(m.position) < p.currentRadius + m.radius) {
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
                for (m in microbes) {
                    if (eb.position.distanceTo(m.position) < eb.currentRadius + m.radius) {
                        m.health -= eb.damage * deltaTime * 3f
                        // Knockback
                        val away = (m.position - eb.position).normalized()
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

            // Check if dead
            if (m.health <= 0f) {
                onSpeciesEaten(m.speciesId)
                microbesEatenCount++
                highestTierDefeated = max(highestTierDefeated, m.trophicTier.level)
                burstDeadMicrobe(m)
                microbeIter.remove()
                continue
            }

            // AI Decision Making based on Trophic Position
            val distToPlayer = m.position.distanceTo(player.position)
            val canEatPlayer = TrophicWebRules.canPredatorAttack(m.diet, m.trophicTier, m.radius, playerStats.trophicTier, playerRadius)
            val playerCanEatMe = TrophicWebRules.canPredatorAttack(playerStats.dietType, playerStats.trophicTier, playerRadius, m.trophicTier, m.radius)

            if (playerCanEatMe && distToPlayer < 380f) {
                // Flee from player
                m.state = AiState.FLEEING
                val awayAngle = (m.position - player.position).angle()
                m.targetAngle = awayAngle
            } else if (canEatPlayer && distToPlayer < 450f) {
                // Hunt player
                m.state = AiState.HUNTING
                val towardsPlayer = (player.position - m.position).angle()
                m.targetAngle = towardsPlayer
                if (m.hasPoison && m.poisonCooldown <= 0f && distToPlayer < 120f) {
                    m.poisonCooldown = 6f
                    poisonPuddles.add(PoisonPuddle(id = idGen.incrementAndGet(), position = m.position, fromPlayer = false))
                }
            } else if (m.diet == DietType.HERBIVORE) {
                // Look for closest algae
                m.state = AiState.GRAZING
                if (m.stateTimer > 1.5f) {
                    val nearestAlgae = foods.filter { it.kind == FoodKind.ALGAE }
                        .minByOrNull { it.position.distanceTo(m.position) }
                    if (nearestAlgae != null && nearestAlgae.position.distanceTo(m.position) < 350f) {
                        m.targetAngle = (nearestAlgae.position - m.position).angle()
                    } else if (Random.nextFloat() < 0.2f) {
                        m.targetAngle += (Random.nextFloat() * 1.5f - 0.75f)
                    }
                    m.stateTimer = 0f
                }
            } else {
                // Roam or hunt smaller microbes
                if (m.stateTimer > 2.0f) {
                    val huntTarget = microbes.filter { other ->
                        other.id != m.id && TrophicWebRules.canPredatorAttack(m.diet, m.trophicTier, m.radius, other.trophicTier, other.radius)
                    }.minByOrNull { it.position.distanceTo(m.position) }

                    if (huntTarget != null && huntTarget.position.distanceTo(m.position) < 400f) {
                        m.state = AiState.HUNTING
                        m.targetAngle = (huntTarget.position - m.position).angle()
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

            // Forward thrust
            val currentSpeed = when (m.state) {
                AiState.FLEEING -> m.targetSpeed * 1.35f
                AiState.HUNTING -> m.targetSpeed * 1.2f
                else -> m.targetSpeed * 0.85f
            }
            val thrust = Vector2.fromAngle(m.angle, currentSpeed * deltaTime * 10f)
            m.velocity = (m.velocity + thrust) * DRAG_COEFFICIENT
            m.position = m.position + (m.velocity * deltaTime)

            // Wrap or bounce off edges
            if (m.position.x < m.radius || m.position.x > WORLD_WIDTH - m.radius) {
                m.velocity = Vector2(-m.velocity.x, m.velocity.y)
                m.targetAngle = m.velocity.angle()
            }
            if (m.position.y < m.radius || m.position.y > WORLD_HEIGHT - m.radius) {
                m.velocity = Vector2(m.velocity.x, -m.velocity.y)
                m.targetAngle = m.velocity.angle()
            }
        }
    }

    private fun handlePlayerFoodCollisions() {
        val playerRadius = calculatePlayerRadius()
        val foodIter = foods.iterator()
        while (foodIter.hasNext()) {
            val f = foodIter.next()
            val dist = player.position.distanceTo(f.position)
            if (dist < playerRadius + f.radius) {
                if (TrophicWebRules.canConsumeFood(playerStats.dietType, f.kind)) {
                    // Eat food!
                    player.dnaPoints += f.valueDna
                    player.biomass += f.valueBiomass
                    player.health = min(player.maxHealth, player.health + f.valueBiomass * 1.5f)
                    onDnaCollected(f.valueDna)
                    addNotice(f.position, "+${f.valueDna} ADN", f.color)
                    onHapticImpact()
                    foodIter.remove()
                }
            }
        }
    }

    private fun handlePlayerMicrobeCollisions() {
        val playerRadius = calculatePlayerRadius()
        for (m in microbes) {
            val dist = player.position.distanceTo(m.position)
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
                    // Player bites prey
                    val biteDmg = playerStats.biteDamage * 1.5f
                    m.health -= biteDmg
                    player.mouthAnimationTimer = 0.35f
                    addNotice(m.position, "Mordisco! -${biteDmg.toInt()}", Color(0xFFFF5252))
                    onHapticImpact()
                }

                if (canMicrobeEatPlayer && m.biteCooldown <= 0f) {
                    // Microbe bites player
                    m.biteCooldown = 0.8f
                    val incomingDmg = when (m.trophicTier) {
                        TrophicTier.APEX -> 40f
                        TrophicTier.PREDATOR -> 24f
                        TrophicTier.SECONDARY_CONSUMER -> 14f
                        else -> 8f
                    }
                    damagePlayer(incomingDmg, m.name)
                }

                // Elastic collision push
                val overlap = combinedRadius - dist
                val pushDir = (player.position - m.position).normalized()
                player.position = player.position + (pushDir * (overlap * 0.5f))
                m.position = m.position - (pushDir * (overlap * 0.5f))
            }
        }
    }

    private fun handleMicrobeMicrobeCollisions() {
        // Trophic interactions between AI organisms
        for (i in 0 until microbes.size) {
            val a = microbes[i]
            for (j in i + 1 until microbes.size) {
                val b = microbes[j]
                val dist = a.position.distanceTo(b.position)
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

    private fun burstDeadMicrobe(m: AiMicrobe) {
        // Drop organic meat and pure DNA strands upon death
        val meatCount = when (m.trophicTier) {
            TrophicTier.APEX -> 8
            TrophicTier.PREDATOR -> 5
            TrophicTier.SECONDARY_CONSUMER -> 3
            else -> 2
        }
        for (i in 0 until meatCount) {
            val offset = Vector2(Random.nextFloat() * 60f - 30f, Random.nextFloat() * 60f - 30f)
            foods.add(
                FoodParticle(
                    id = idGen.incrementAndGet(),
                    position = m.position + offset,
                    kind = FoodKind.MEAT_CHUNK,
                    radius = 9f,
                    valueDna = 8,
                    valueBiomass = 3.0f,
                    color = Color(0xFFFF5252)
                )
            )
        }
        // Pure DNA helix drops
        val dnaDrops = if (m.trophicTier == TrophicTier.APEX) 4 else 2
        for (i in 0 until dnaDrops) {
            val offset = Vector2(Random.nextFloat() * 50f - 25f, Random.nextFloat() * 50f - 25f)
            foods.add(
                FoodParticle(
                    id = idGen.incrementAndGet(),
                    position = m.position + offset,
                    kind = FoodKind.DNA_STRAND,
                    radius = 12f,
                    valueDna = 20,
                    valueBiomass = 5.0f,
                    color = Color(0xFFFFD600)
                )
            )
        }
        addNotice(m.position, "¡${m.name} Eliminado!", Color(0xFF00E5FF))
    }

    fun triggerDash(): Boolean {
        if (player.dashCooldownTimer <= 0f) {
            player.isDashing = true
            player.dashDurationTimer = 0.5f
            player.dashCooldownTimer = if (evolutionEntity.jetCount > 0) 2.5f else 4.0f
            // Emit burst force forward
            val dashBurst = Vector2.fromAngle(player.angle, playerStats.baseSpeed * playerStats.dashSpeedMultiplier * 0.7f)
            player.velocity = player.velocity + dashBurst
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
        if (notices.size > 15) notices.removeAt(0)
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
            if (p.position.x < 0) p.position = Vector2(WORLD_WIDTH, p.position.y)
            if (p.position.x > WORLD_WIDTH) p.position = Vector2(0f, p.position.y)
            if (p.position.y < 0) p.position = Vector2(p.position.x, WORLD_HEIGHT)
            if (p.position.y > WORLD_HEIGHT) p.position = Vector2(p.position.x, 0f)
        }
    }

    private fun angleDifference(target: Float, current: Float): Float {
        var diff = (target - current) % (2f * PI.toFloat())
        if (diff < -PI.toFloat()) diff += 2f * PI.toFloat()
        if (diff > PI.toFloat()) diff -= 2f * PI.toFloat()
        return diff
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
        spawnInitialEcosystem()
    }
}
