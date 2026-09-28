package com.example.spore.data.db

import com.example.spore.data.model.CellEvolutionEntity
import com.example.spore.data.model.GameStatsEntity
import com.example.spore.data.model.TrophicSpeciesEntity
import kotlinx.coroutines.flow.Flow

class SporeRepository(private val dao: SporeDao) {
    val cellEvolution: Flow<CellEvolutionEntity?> = dao.getCellEvolution()
    val allSpecies: Flow<List<TrophicSpeciesEntity>> = dao.getAllSpecies()
    val gameStats: Flow<GameStatsEntity?> = dao.getGameStats()

    suspend fun getCellEvolutionSync(): CellEvolutionEntity {
        return dao.getCellEvolutionSync() ?: CellEvolutionEntity().also {
            dao.saveCellEvolution(it)
        }
    }

    suspend fun saveCellEvolution(cell: CellEvolutionEntity) {
        dao.saveCellEvolution(cell)
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

    suspend fun ensureDefaultSpecies() {
        dao.insertDefaultSpecies(SporeDatabase.DEFAULT_TROPHIC_SPECIES)
    }

    suspend fun updateStatsOnGameEnd(
        score: Int,
        maxBiomass: Float,
        dnaEarned: Int,
        microbesDefeated: Int,
        highestTierDefeated: Int
    ) {
        val current = dao.getCellEvolutionSync()
        if (current != null) {
            val updatedCell = current.copy(
                dnaPoints = current.dnaPoints + dnaEarned,
                biomass = maxOf(current.biomass, maxBiomass)
            )
            dao.saveCellEvolution(updatedCell)
        }
    }
}
