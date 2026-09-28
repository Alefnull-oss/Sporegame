package com.example.spore.game.engine

import com.example.spore.data.model.CellEvolutionEntity

data class CellStats(
    val maxHealth: Float,
    val baseSpeed: Float,
    val turnRate: Float, // rad per second
    val biteDamage: Float,
    val spikeDamage: Float,
    val armorDamageReduction: Float, // percentage
    val sensorRadius: Float,
    val dashSpeedMultiplier: Float,
    val hasPoison: Boolean,
    val hasElectricShock: Boolean,
    val trophicTier: TrophicTier,
    val dietType: DietType
)

object CellEvolutionConfig {
    // Costs in DNA
    const val COST_MOUTH_CHANGE = 20
    const val COST_FLAGELLUM = 15
    const val COST_CILIA = 15
    const val COST_JET = 30
    const val COST_SPIKE = 20
    const val COST_POISON = 35
    const val COST_ELECTRIC = 45
    const val COST_ARMOR = 25
    const val COST_EYE_UPGRADE = 25

    fun calculateStats(entity: CellEvolutionEntity): CellStats {
        val diet = DietType.fromString(entity.mouthType)
        val baseHp = 100f + (entity.armorPlates * 35f) + (entity.biomass * 1.2f)
        
        // Locomotion math
        // Flagella gives forward thrust/top speed
        // Cilia gives agile turning rate and quick micro-adjustments
        // Armor adds mass which slightly reduces speed unless counterbalanced
        val speedPenalty = entity.armorPlates * 15f
        val baseSpeed = (180f + (entity.flagellaCount * 65f) - speedPenalty).coerceAtLeast(120f)
        val turnRate = (3.0f + (entity.ciliaCount * 1.8f)).coerceAtLeast(2.5f) // radians/s
        
        // Combat
        val biteDmg = when (diet) {
            DietType.HERBIVORE -> 5f // Weak scraping mouth
            DietType.OMNIVORE -> 18f // Versatile proboscis
            DietType.CARNIVORE -> 35f // Sharp serrated teeth
        }
        val spikeDmg = entity.spikesCount * 25f
        val armorReduction = (entity.armorPlates * 0.12f).coerceAtMost(0.48f)

        // Sensory
        val sensorRange = when (entity.eyeType) {
            "COMPOUND" -> 900f
            "BASIC" -> 650f
            else -> 450f
        }

        // Trophic classification based on diet and combat parts
        val tier = when (diet) {
            DietType.HERBIVORE -> TrophicTier.PRIMARY_CONSUMER
            DietType.OMNIVORE -> {
                if (entity.biomass >= 80f && (entity.spikesCount >= 2 || entity.poisonGland)) {
                    TrophicTier.PREDATOR
                } else {
                    TrophicTier.SECONDARY_CONSUMER
                }
            }
            DietType.CARNIVORE -> {
                if (entity.biomass >= 100f && (entity.spikesCount >= 2 || entity.electricOrgan)) {
                    TrophicTier.APEX
                } else {
                    TrophicTier.PREDATOR
                }
            }
        }

        val dashMultiplier = if (entity.jetCount > 0) 2.2f + (entity.jetCount * 0.4f) else 1.6f

        return CellStats(
            maxHealth = baseHp,
            baseSpeed = baseSpeed,
            turnRate = turnRate,
            biteDamage = biteDmg,
            spikeDamage = spikeDmg,
            armorDamageReduction = armorReduction,
            sensorRadius = sensorRange,
            dashSpeedMultiplier = dashMultiplier,
            hasPoison = entity.poisonGland,
            hasElectricShock = entity.electricOrgan,
            trophicTier = tier,
            dietType = diet
        )
    }
}
