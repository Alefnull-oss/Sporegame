package com.example.spore.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.spore.data.model.CellEvolutionEntity
import com.example.spore.data.model.GameStatsEntity
import com.example.spore.data.model.TrophicSpeciesEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SporeDao {
    @Query("SELECT * FROM cell_evolution WHERE id = 1")
    fun getCellEvolution(): Flow<CellEvolutionEntity?>

    @Query("SELECT * FROM cell_evolution WHERE id = 1")
    suspend fun getCellEvolutionSync(): CellEvolutionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveCellEvolution(cell: CellEvolutionEntity)

    @Query("SELECT * FROM trophic_species ORDER BY trophicLevel ASC")
    fun getAllSpecies(): Flow<List<TrophicSpeciesEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDefaultSpecies(species: List<TrophicSpeciesEntity>)

    @Update
    suspend fun updateSpecies(species: TrophicSpeciesEntity)

    @Query("UPDATE trophic_species SET discovered = 1 WHERE speciesId = :speciesId")
    suspend fun markDiscovered(speciesId: String)

    @Query("UPDATE trophic_species SET timesEatenByPlayer = timesEatenByPlayer + 1, discovered = 1 WHERE speciesId = :speciesId")
    suspend fun incrementEaten(speciesId: String)

    @Query("UPDATE trophic_species SET timesKilledPlayer = timesKilledPlayer + 1 WHERE speciesId = :speciesId")
    suspend fun incrementKilledBy(speciesId: String)

    @Query("SELECT * FROM game_stats WHERE id = 1")
    fun getGameStats(): Flow<GameStatsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveGameStats(stats: GameStatsEntity)
}
