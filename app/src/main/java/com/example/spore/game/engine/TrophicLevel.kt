package com.example.spore.game.engine

enum class TrophicTier(val level: Int, val displayName: String, val colorHex: Long) {
    PRODUCER(0, "Productor Primario (Algas)", 0xFF00E676),
    PRIMARY_CONSUMER(1, "Consumidor Primario (Herbívoro)", 0xFF76FF03),
    SECONDARY_CONSUMER(2, "Consumidor Secundario (Omnívoro)", 0xFFFFD600),
    PREDATOR(3, "Depredador (Carnívoro)", 0xFFFF3D00),
    APEX(4, "Súper Depredador Ápex", 0xFFD500F9);

    companion object {
        fun fromLevel(lvl: Int): TrophicTier = entries.firstOrNull { it.level == lvl } ?: PRODUCER
    }
}

enum class DietType(val displayName: String) {
    HERBIVORE("Herbívoro (Filtro)"),
    CARNIVORE("Carnívoro (Mandíbula)"),
    OMNIVORE("Omnívoro (Probóscide)");

    companion object {
        fun fromString(str: String): DietType = try {
            valueOf(str.uppercase())
        } catch (_: Exception) {
            HERBIVORE
        }
    }
}

object TrophicWebRules {
    /**
     * Determines whether an attacker/consumer can feed on a target.
     * In a true biological trophic web:
     * - Herbivores eat plant matter/algae.
     * - Carnivores eat flesh/cells smaller or equal with sufficient combat power.
     * - Omnivores can eat algae and smaller cells.
     * - Apex can eat any smaller organism.
     */
    fun canPredatorAttack(
        attackerDiet: DietType,
        attackerTier: TrophicTier,
        attackerRadius: Float,
        targetTier: TrophicTier,
        targetRadius: Float
    ): Boolean {
        // Herbivores cannot hunt other living cells
        if (attackerDiet == DietType.HERBIVORE) return false

        // Apex can attack anything smaller than them
        if (attackerTier == TrophicTier.APEX) {
            return attackerRadius > targetRadius * 0.85f
        }

        // Carnivore can attack tier below it or same tier if larger
        if (attackerDiet == DietType.CARNIVORE) {
            return if (attackerTier.level > targetTier.level) {
                attackerRadius > targetRadius * 0.7f
            } else if (attackerTier == targetTier) {
                attackerRadius > targetRadius * 1.05f
            } else {
                false
            }
        }

        // Omnivore can attack herbivores or smaller omnivores
        if (attackerDiet == DietType.OMNIVORE) {
            return (targetTier == TrophicTier.PRIMARY_CONSUMER && attackerRadius >= targetRadius * 0.9f) ||
                    (targetTier == TrophicTier.SECONDARY_CONSUMER && attackerRadius > targetRadius * 1.1f)
        }

        return false
    }

    /**
     * Checks if a cell can eat a specific food particle type.
     */
    fun canConsumeFood(diet: DietType, foodKind: FoodKind): Boolean {
        return when (foodKind) {
            FoodKind.ALGAE -> diet == DietType.HERBIVORE || diet == DietType.OMNIVORE
            FoodKind.MEAT_CHUNK -> diet == DietType.CARNIVORE || diet == DietType.OMNIVORE
            FoodKind.DNA_STRAND -> true // DNA can be assimilated by all organisms for mutations
        }
    }
}

enum class FoodKind {
    ALGAE,
    MEAT_CHUNK,
    DNA_STRAND
}
