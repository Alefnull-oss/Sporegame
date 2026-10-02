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
    var state: AiState = AiState.ROAMING,
    var stateTimer: Float = 0f,
    var poisonCooldown: Float = 0f,
    var biteCooldown: Float = 0f,
    var wobbleTime: Float = 0f,
    val softBody: SoftBodyMembrane = SoftBodyMembrane(16),
    var flagellaChains: List<ElasticAppendageChain> = emptyList(),
    var jawAperture: Float = 1.0f,
    var isBiting: Boolean = false
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
    var isBiting: Boolean = false
)
