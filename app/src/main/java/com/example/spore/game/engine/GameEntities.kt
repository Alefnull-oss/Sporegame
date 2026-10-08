package com.example.spore.game.engine

import androidx.compose.ui.graphics.Color
import com.example.spore.game.physics.ElasticAppendageChain
import com.example.spore.game.physics.SoftBodyMembrane

data class FoodParticle(
    val id: Long,
    var position: Vector2,
    var velocity: Vector2 = Vector2.ZERO,
    val kind: FoodKind,
    val radius: Float,
    val valueDna: Int,
    val valueBiomass: Float,
    val color: Color,
    var rotation: Float = 0f,
    var wobblePhase: Float = 0f,
    var isCollected: Boolean = false
)

data class PoisonPuddle(
    val id: Long,
    var position: Vector2,
    var currentRadius: Float = 10f,
    val maxRadius: Float = 45f,
    var remainingSeconds: Float = 4.5f,
    val fromPlayer: Boolean = false,
    val damagePerSecond: Float = 22f
)

data class ElectricBlast(
    val id: Long,
    var position: Vector2,
    var currentRadius: Float = 15f,
    val maxRadius: Float = 120f,
    var remainingSeconds: Float = 0.4f,
    val fromPlayer: Boolean = false,
    val damage: Float = 40f
)

data class FloatingNotice(
    val id: Long,
    var position: Vector2,
    val text: String,
    val color: Color,
    var alpha: Float = 1f,
    var remainingSeconds: Float = 1.2f
)

data class AmbientParticle(
    var position: Vector2,
    var velocity: Vector2,
    var radius: Float,
    var alpha: Float,
    val color: Color
)

/**
 * 2.5D Abyssal Leviathan Silhouette swimming deep beneath the fluid camera.
 * Gives the iconic deep microscopic abyss sensation from Spore.
 */
data class AbyssalSilhouette(
    val id: Long,
    var position: Vector2,
    var velocity: Vector2,
    var angle: Float,
    val length: Float,
    val width: Float,
    val segmentCount: Int = 5,
    val tailWobbleSpeed: Float = 1.4f,
    val tailPhaseOffset: Float = 0f,
    val silhouetteColor: Color,
    val alpha: Float = 0.18f,
    val parallaxFactor: Float = 0.42f
)

/**
 * 2.5D Foreground Floater (out-of-focus bokeh bubbles, diatoms and micro-spores
 * gliding close to the camera lens).
 */
data class ForegroundFloater(
    val id: Long,
    var position: Vector2,
    var velocity: Vector2,
    val radius: Float,
    val alpha: Float,
    val color: Color,
    val wobblePhase: Float,
    val floaterKind: Int // 0: bokeh droplet, 1: amoeba ghost, 2: diatom shell ring
)

enum class AiState {
    ROAMING,
    GRAZING,
    HUNTING,
    FLEEING
}

data class AiMicrobe(
    val id: Long,
    val speciesId: String,
    val name: String,
    var position: Vector2,
    var velocity: Vector2 = Vector2.ZERO,
    var angle: Float = 0f,
    var targetAngle: Float = 0f,
    var targetSpeed: Float = 120f,
    var health: Float,
    var maxHealth: Float,
    var radius: Float,
    val trophicTier: TrophicTier,
    val diet: DietType,
    val primaryColor: Color,
    val flagellaCount: Int = 1,
    val ciliaCount: Int = 1,
    val spikesCount: Int = 0,
    val hasPoison: Boolean = false,
    val hasJet: Boolean = false,
    val armorPlates: Int = 0,
    val eyeType: String = "BASIC",
    var state: AiState = AiState.ROAMING,
    var stateTimer: Float = 0f,
    var poisonCooldown: Float = 0f,
    var biteCooldown: Float = 0f,
    var wobbleTime: Float = 0f,
    val softBody: SoftBodyMembrane = SoftBodyMembrane(16),
    var flagellaChains: List<ElasticAppendageChain> = emptyList(),
    var jawAperture: Float = 1.0f,
    var isBiting: Boolean = false,
    var bankRoll: Float = 0f,
    var pitchAngle: Float = 0f,
    var elevationZ: Float = 0f
)

data class PlayerCell(
    var position: Vector2 = Vector2(1800f, 1800f),
    var velocity: Vector2 = Vector2.ZERO,
    var angle: Float = 0f,
    var health: Float = 100f,
    var maxHealth: Float = 100f,
    var biomass: Float = 25f,
    var dnaPoints: Int = 50,
    var stats: CellStats,
    var isDashing: Boolean = false,
    var dashDurationTimer: Float = 0f,
    var dashCooldownTimer: Float = 0f,
    var poisonCooldownTimer: Float = 0f,
    var electricCooldownTimer: Float = 0f,
    var invulnerableTimer: Float = 0f,
    var damageFlashTimer: Float = 0f,
    var mouthAnimationTimer: Float = 0f,
    var wobbleTimer: Float = 0f,
    val softBody: SoftBodyMembrane = SoftBodyMembrane(16),
    var flagellaChains: List<ElasticAppendageChain> = emptyList(),
    var jawAperture: Float = 1.0f,
    var isBiting: Boolean = false,
    var bankRoll: Float = 0f,
    var pitchAngle: Float = 0f,
    var elevationZ: Float = 0f
)

/**
 * Primordial Cosmic Meteorite Shards floating in the ocean with locked ancestral parts inside.
 * Can be cracked open by biting or ramming with spikes.
 */
data class MeteorShard(
    val id: Long,
    var position: Vector2,
    var velocity: Vector2 = Vector2.ZERO,
    var health: Float = 60f,
    val maxHealth: Float = 60f,
    val radius: Float = 32f,
    val containedPartId: String,
    val partName: String,
    var rotation: Float = 0f,
    val rotationSpeed: Float = 0.4f,
    val crustedColor: Color = Color(0xFF5D4037),
    val coreColor: Color = Color(0xFFFFD54F),
    var wobblePhase: Float = 0f
)

/**
 * Floating glowing genetic part capsule dropped from cracked meteorites or defeated trophy rivals.
 */
data class PartCapsule(
    val id: Long,
    var position: Vector2,
    var velocity: Vector2 = Vector2.ZERO,
    val partId: String,
    val partName: String,
    val iconColor: Color,
    val radius: Float = 22f,
    var wobblePhase: Float = 0f,
    var lifeTimer: Float = 65f,
    var isCollected: Boolean = false
)

/**
 * Concentric acoustic sonar wave emitted during the Spore Mating Call.
 */
data class AcousticWave(
    val id: Long,
    var origin: Vector2,
    var currentRadius: Float = 10f,
    val maxRadius: Float = 850f,
    val speed: Float = 420f,
    val color: Color,
    var alpha: Float = 1f,
    val fromPlayer: Boolean = false
)

enum class MateState {
    RESPONDING,
    SWIMMING_TO_PLAYER,
    COURTSHIP_DANCE,
    MATING_COMPLETED
}

/**
 * Symbiotic mating partner that responds to the player's acoustic mating call.
 */
data class SymbioticMate(
    val id: Long,
    var position: Vector2,
    var velocity: Vector2 = Vector2.ZERO,
    var angle: Float = 0f,
    val speciesName: String,
    val radius: Float = 28f,
    val primaryColor: Color,
    val mouthType: DietType,
    var state: MateState = MateState.RESPONDING,
    var danceTimer: Float = 0f,
    var sonarPulseTimer: Float = 0f,
    val softBody: SoftBodyMembrane = SoftBodyMembrane(16),
    var flagellaChains: List<ElasticAppendageChain> = emptyList(),
    var jawAperture: Float = 1.0f,
    var wobbleTimer: Float = 0f
)

/**
 * Microscopic bioluminescent love hearts emitted during the courtship dance.
 */
data class HeartBubble(
    val id: Long,
    var position: Vector2,
    var velocity: Vector2,
    var scale: Float = 1f,
    var alpha: Float = 1f,
    var lifeSeconds: Float = 1.4f
)
