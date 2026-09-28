package com.example.spore.data.db

import com.example.spore.data.model.CellEvolutionEntity
import com.example.spore.data.model.GameStatsEntity
import com.example.spore.data.model.PlanetDefinition
import com.example.spore.data.model.PlanetSaveEntity
import com.example.spore.data.model.TrophicSpeciesEntity
import kotlinx.coroutines.flow.Flow

class SporeRepository(private val dao: SporeDao) {
    val cellEvolution: Flow<CellEvolutionEntity?> = dao.getCellEvolution()
    val allSpecies: Flow<List<TrophicSpeciesEntity>> = dao.getAllSpecies()
    val gameStats: Flow<GameStatsEntity?> = dao.getGameStats()
    val allPlanetSaves: Flow<List<PlanetSaveEntity>> = dao.getAllPlanetSaves()

    suspend fun getCellEvolutionSync(): CellEvolutionEntity {
        return dao.getCellEvolutionSync() ?: CellEvolutionEntity().also {
            dao.saveCellEvolution(it)
        }
    }

    suspend fun saveCellEvolution(cell: CellEvolutionEntity) {
        dao.saveCellEvolution(cell)
    }

    suspend fun getPlanetSaveSync(planetId: String): PlanetSaveEntity? {
        return dao.getPlanetSaveSync(planetId)
    }

    suspend fun savePlanetSave(save: PlanetSaveEntity) {
        dao.savePlanetSave(save)
    }

    suspend fun markSpeciesDiscovered(speciesId: String) {
        dao.markDiscovered(speciesId)
    }

    suspend fun recordPreyEaten(speciesId: String) {
        dao.incrementEaten(speciesId)
    }

    suspend fun recordKilledBy(speciesId: String) {
        dao.incrementKilledBy(speciesId)
    }

    suspend fun ensureDefaults() {
        dao.insertDefaultSpecies(SporeDatabase.DEFAULT_TROPHIC_SPECIES)
        dao.insertDefaultPlanetSaves(SporeDatabase.DEFAULT_PLANET_SAVES)
    }

    suspend fun resetPlanet(planetId: String) {
        val def = PlanetDefinition.getById(planetId)
        val defaultSave = SporeDatabase.DEFAULT_PLANET_SAVES.firstOrNull { it.planetId == planetId }
            ?: PlanetSaveEntity(planetId = planetId, planetName = def.name)
        dao.savePlanetSave(defaultSave.copy(hasPlayed = false, dnaPoints = 50, biomass = 25f, generation = 1))
    }

    suspend fun updateStatsOnGameEnd(
        planetId: String,
        score: Int,
        maxBiomass: Float,
        dnaEarned: Int,
        microbesDefeated: Int,
        highestTierDefeated: Int
    ) {
        val currentPlanet = dao.getPlanetSaveSync(planetId)
        if (currentPlanet != null) {
            val updated = currentPlanet.copy(
                dnaPoints = currentPlanet.dnaPoints + dnaEarned,
                biomass = maxOf(currentPlanet.biomass, maxBiomass),
                bestScore = maxOf(currentPlanet.bestScore, score),
                microbesEaten = currentPlanet.microbesEaten + microbesDefeated,
                hasPlayed = true
            )
            dao.savePlanetSave(updated)
            dao.saveCellEvolution(updated.toCellEvolutionEntity())
        }
    }
}
