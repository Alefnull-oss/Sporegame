package com.example.spore.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cell_evolution")
data class CellEvolutionEntity(
    @PrimaryKey val id: Int = 1,
    val speciesName: String = "Protocélula Alfa",
    val dnaPoints: Int = 50,
    val biomass: Float = 25f,
    val generation: Int = 1,
    // Mouth / Diet
    val mouthType: String = "HERBIVORE", // HERBIVORE, CARNIVORE, OMNIVORE
    // Locomotion
    val flagellaCount: Int = 1, // 0 to 4
    val ciliaCount: Int = 1,    // 0 to 4
    val jetCount: Int = 0,      // 0 to 2
    // Defense / Offense
    val spikesCount: Int = 0,   // 0 to 4
    val poisonGland: Boolean = false,
    val electricOrgan: Boolean = false,
    val armorPlates: Int = 0,   // 0 to 3
    // Sensory
    val eyeType: String = "BASIC", // NONE, BASIC, COMPOUND
    // Aesthetics
    val primaryColorHex: Long = 0xFF00E5FF,
    val membranePattern: String = "SMOOTH",
    // Spore-like Part Discovery & Epigenetic Chimerism
    val unlockedParts: String = "MOUTH_HERBIVORE,FLAGELLA,CILIA,EYE_BASIC",
    val chimericTraits: String = ""
)
