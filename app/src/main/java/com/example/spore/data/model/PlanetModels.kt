package com.example.spore.data.model

import androidx.compose.ui.graphics.Color
import androidx.room.Entity
import androidx.room.PrimaryKey

data class PlanetDefinition(
    val id: String,
    val name: String,
    val oceanName: String,
    val description: String,
    val oceanBgColor1: Long,
    val oceanBgColor2: Long,
    val oceanRimColor: Long,
    val ambientParticleColor: Long,
    val planetColorHex: Long,
    val atmosphereColorHex: Long,
    val continentsColorHex: Long,
    val difficulty: String,
    val galaxyAngle: Float,
    val galaxyDistance: Float,
    val sizeDp: Float
) {
    companion object {
        val PLANETS = listOf(
            PlanetDefinition(
                id = "planet_aqualis",
                name = "Aqualis",
                oceanName = "Océano Cian Primordial",
                description = "Mundo templado de aguas ricas en aminoácidos y minerales disueltos. El caldo primordial por excelencia para iniciar la evolución.",
                oceanBgColor1 = 0xFF04162B,
                oceanBgColor2 = 0xFF010812,
                oceanRimColor = 0xFF00E5FF,
                ambientParticleColor = 0xFF00E5FF,
                planetColorHex = 0xFF0091EA,
                atmosphereColorHex = 0xFF80D8FF,
                continentsColorHex = 0xFF00C853,
                difficulty = "Equilibrado",
                galaxyAngle = 0.5f,
                galaxyDistance = 140f,
                sizeDp = 62f
            ),
            PlanetDefinition(
                id = "planet_rubrum",
                name = "Rubrum IV",
                oceanName = "Mares Rojos de Sangre e Hierro",
                description = "Planeta saturado de óxido de hierro y sales minerales rojizas. La sopa carmesí promueve depredadores agresivos y células resistentes.",
                oceanBgColor1 = 0xFF2A060C,
                oceanBgColor2 = 0xFF0D0104,
                oceanRimColor = 0xFFFF1744,
                ambientParticleColor = 0xFFFF5252,
                planetColorHex = 0xFFD50000,
                atmosphereColorHex = 0xFFFF8A80,
                continentsColorHex = 0xFF4E342E,
                difficulty = "Hostil (Depredador)",
                galaxyAngle = 2.1f,
                galaxyDistance = 210f,
                sizeDp = 70f
            ),
            PlanetDefinition(
                id = "planet_toxis",
                name = "Toxis Acidus",
                oceanName = "Sopa Ácida Esmeralda",
                description = "Océano sulfuroso brillante con alta densidad viscosa. Abundancia de microalgas fotosintéticas pero corrientes traicioneras.",
                oceanBgColor1 = 0xFF06220E,
                oceanBgColor2 = 0xFF010C04,
                oceanRimColor = 0xFF76FF03,
                ambientParticleColor = 0xFF76FF03,
                planetColorHex = 0xFF2E7D32,
                atmosphereColorHex = 0xFFB9F6CA,
                continentsColorHex = 0xFFAEEA00,
                difficulty = "Tóxico / Algas +",
                galaxyAngle = 3.8f,
                galaxyDistance = 175f,
                sizeDp = 58f
            ),
            PlanetDefinition(
                id = "planet_ametistia",
                name = "Ametistia Abisal",
                oceanName = "Mares de Metano Púrpura",
                description = "Planeta gélido de hidrocarburos violetas en las profundidades de la galaxia. Extraña bioluminiscencia y criaturas gigantes.",
                oceanBgColor1 = 0xFF1C0633,
                oceanBgColor2 = 0xFF080112,
                oceanRimColor = 0xFFD500F9,
                ambientParticleColor = 0xFFE040FB,
                planetColorHex = 0xFF6A1B9A,
                atmosphereColorHex = 0xFFEA80FC,
                continentsColorHex = 0xFF283593,
                difficulty = "Abisal (Ápex +)",
                galaxyAngle = 5.2f,
                galaxyDistance = 245f,
                sizeDp = 66f
            ),
            PlanetDefinition(
                id = "planet_solaria",
                name = "Solaria Prime",
                oceanName = "Mares Solares de Azufre Dorado",
                description = "Bañado por la radiación de una estrella doble. El caldo ámbar brilla con fragmentos de ADN condensado e hipermutación.",
                oceanBgColor1 = 0xFF281804,
                oceanBgColor2 = 0xFF0D0701,
                oceanRimColor = 0xFFFFD600,
                ambientParticleColor = 0xFFFFD600,
                planetColorHex = 0xFFFF8F00,
                atmosphereColorHex = 0xFFFFE57F,
                continentsColorHex = 0xFFBF360C,
                difficulty = "Mutagénico",
                galaxyAngle = 4.4f,
                galaxyDistance = 110f,
                sizeDp = 54f
            )
        )

        fun getById(id: String): PlanetDefinition =
            PLANETS.firstOrNull { it.id == id } ?: PLANETS[0]
    }
}

@Entity(tableName = "planet_saves")
data class PlanetSaveEntity(
    @PrimaryKey val planetId: String,
    val planetName: String,
    val speciesName: String = "Protocélula",
    val generation: Int = 1,
    val dnaPoints: Int = 50,
    val biomass: Float = 25f,
    val mouthType: String = "HERBIVORE",
    val flagellaCount: Int = 1,
    val ciliaCount: Int = 1,
    val jetCount: Int = 0,
    val spikesCount: Int = 0,
    val poisonGland: Boolean = false,
    val electricOrgan: Boolean = false,
    val armorPlates: Int = 0,
    val eyeType: String = "BASIC",
    val primaryColorHex: Long = 0xFF00E5FF,
    val hasPlayed: Boolean = false,
    val bestScore: Int = 0,
    val microbesEaten: Int = 0
) {
    fun toCellEvolutionEntity(): CellEvolutionEntity = CellEvolutionEntity(
        id = 1,
        speciesName = speciesName,
        dnaPoints = dnaPoints,
        biomass = biomass,
        generation = generation,
        mouthType = mouthType,
        flagellaCount = flagellaCount,
        ciliaCount = ciliaCount,
        jetCount = jetCount,
        spikesCount = spikesCount,
        poisonGland = poisonGland,
        electricOrgan = electricOrgan,
        armorPlates = armorPlates,
        eyeType = eyeType,
        primaryColorHex = primaryColorHex
    )
}
